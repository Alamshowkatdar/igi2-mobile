#!/usr/bin/env python3
"""Reference host/client for the IGI 2 Mobile LAN foundation.

This speaks the same custom UDP protocol as the app (not the retail I.G.I. 2
protocol). Use it to prove discovery and the lobby handshake from a PC:

    python3 igi2lan.py host --name "PC HOST"
    python3 igi2lan.py scan
    python3 igi2lan.py join 192.168.1.42 --name "PC CLIENT"

Both devices must be on the same network. UDP port 26099.
"""

from __future__ import annotations

import argparse
import select
import socket
import sys
import time
import uuid
from dataclasses import dataclass, field
from urllib.parse import quote_plus, unquote_plus

PORT = 26099
MAGIC = "IGI2LAN/v1"
MAX_PLAYERS = 8
HEARTBEAT = 2.0
TIMEOUT = 8.0


def message(*parts: str) -> bytes:
    return "|".join((MAGIC, *parts)).encode("utf-8")


def parse(data: bytes) -> list[str] | None:
    try:
        parts = data.decode("utf-8").split("|")
    except UnicodeDecodeError:
        return None
    if len(parts) < 2 or parts[0] != MAGIC:
        return None
    return parts


def enc(value: str) -> str:
    return quote_plus(value, safe="")


def dec(value: str) -> str:
    return unquote_plus(value)


def broadcast_targets() -> list[str]:
    targets = {"255.255.255.255"}
    try:
        import socket as _socket

        for info in _socket.getaddrinfo(_socket.gethostname(), None, _socket.AF_INET):
            pass
    except OSError:
        pass
    # Add the usual /24 broadcast for every local IPv4 address.
    try:
        hostname = socket.gethostname()
        for addr in socket.gethostbyname_ex(hostname)[2]:
            if addr.startswith("127."):
                continue
            targets.add(addr.rsplit(".", 1)[0] + ".255")
    except OSError:
        pass
    return sorted(targets)


@dataclass
class Peer:
    name: str
    address: tuple[str, int]
    last_seen: float = field(default_factory=time.monotonic)


def run_host(name: str, port: int) -> int:
    session_id = uuid.uuid4().hex[:8].upper()
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.bind(("", port))
    peers: dict[str, Peer] = {}

    print(f"hosting '{name}' session {session_id} on UDP {port}")
    print("press Ctrl+C to stop")

    last_tick = time.monotonic()
    try:
        while True:
            ready, _, _ = select.select([sock], [], [], 0.5)
            if ready:
                data, address = sock.recvfrom(4096)
                parts = parse(data)
                if parts:
                    handle(sock, session_id, name, peers, parts, address)

            now = time.monotonic()
            if now - last_tick >= HEARTBEAT:
                last_tick = now
                expired = [pid for pid, p in peers.items() if now - p.last_seen > TIMEOUT]
                for pid in expired:
                    del peers[pid]
                    print(f"  - peer {pid} timed out")
                if expired:
                    broadcast_state(sock, session_id, peers)
    except KeyboardInterrupt:
        print("\nstopping")
    finally:
        sock.close()
    return 0


def handle(
    sock: socket.socket,
    session_id: str,
    host_name: str,
    peers: dict[str, Peer],
    parts: list[str],
    address: tuple[str, int],
) -> None:
    kind = parts[1]
    if kind == "DISCOVER":
        offer = message(
            "OFFER",
            session_id,
            enc(host_name),
            str(len(peers)),
            str(MAX_PLAYERS),
        )
        sock.sendto(offer, address)
    elif kind == "JOIN" and len(parts) >= 4:
        client_id = parts[2]
        player = dec(parts[3])[:24] or "Player"
        peers[client_id] = Peer(player, address)
        print(f"  + {player} joined from {address[0]}:{address[1]}")
        sock.sendto(message("WELCOME", session_id, enc(host_name), client_id), address)
        broadcast_state(sock, session_id, peers)
        print(f"  roster: {host_name}, " + ", ".join(p.name for p in peers.values()))
    elif kind == "PING" and len(parts) >= 3:
        peer = peers.get(parts[2])
        if peer:
            peer.last_seen = time.monotonic()
    elif kind == "LEAVE" and len(parts) >= 3:
        peer = peers.pop(parts[2], None)
        if peer:
            print(f"  - {peer.name} left")
            broadcast_state(sock, session_id, peers)


def broadcast_state(sock: socket.socket, session_id: str, peers: dict[str, Peer]) -> None:
    state = message(
        "STATE",
        session_id,
        str(len(peers)),
        *(enc(p.name) for p in peers.values()),
    )
    for peer in peers.values():
        sock.sendto(state, peer.address)


def run_scan(seconds: float) -> int:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    probe = message("DISCOVER")

    for target in broadcast_targets():
        try:
            sock.sendto(probe, (target, PORT))
        except OSError:
            pass

    found: dict[str, tuple[str, str, int, int]] = {}
    deadline = time.monotonic() + seconds
    sock.settimeout(0.2)
    while time.monotonic() < deadline:
        try:
            data, address = sock.recvfrom(4096)
        except socket.timeout:
            continue
        parts = parse(data)
        if not parts or parts[1] != "OFFER" or len(parts) < 6:
            continue
        found[parts[2]] = (
            dec(parts[3]) or "IGI 2 host",
            address[0],
            int(parts[4]),
            int(parts[5]),
        )

    if not found:
        print("no sessions found (same Wi-Fi? broadcast blocked? try join-by-IP)")
        return 1

    for session_id, (host_name, address, players, maximum) in found.items():
        print(f"{host_name}  {address}:{PORT}  {players}/{maximum}  [{session_id}]")
    return 0


def run_join(host: str, name: str, port: int) -> int:
    client_id = uuid.uuid4().hex
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(0.5)
    target = (host, port)

    sock.sendto(message("JOIN", client_id, enc(name)), target)
    deadline = time.monotonic() + 3.5
    connected = False
    session_id = ""
    host_name = ""
    last_ping = 0.0

    print(f"joining {host}:{port} as '{name}'")
    try:
        while True:
            now = time.monotonic()
            if not connected and now > deadline:
                print("no answer (check the IP, the host screen, and the Wi-Fi network)")
                return 1
            if connected and now - last_ping >= HEARTBEAT:
                last_ping = now
                sock.sendto(message("PING", client_id), target)

            try:
                data, address = sock.recvfrom(4096)
            except socket.timeout:
                continue
            parts = parse(data)
            if not parts:
                continue
            if parts[1] == "WELCOME" and len(parts) >= 4:
                connected = True
                session_id = parts[2]
                host_name = dec(parts[3])
                last_ping = time.monotonic()
                print(f"connected to '{host_name}' session {session_id}")
            elif parts[1] == "STATE" and connected:
                names = [dec(p) for p in parts[4:]]
                print("  roster: " + (", ".join(names) if names else "(empty)"))
    except KeyboardInterrupt:
        sock.sendto(message("LEAVE", client_id), target)
        print("\nleft")
    finally:
        sock.close()
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="IGI 2 Mobile LAN reference tool")
    sub = parser.add_subparsers(dest="command", required=True)

    host = sub.add_parser("host", help="host a lobby")
    host.add_argument("--name", default="PC HOST")
    host.add_argument("--port", type=int, default=PORT)

    scan = sub.add_parser("scan", help="find hosts on the LAN")
    scan.add_argument("--seconds", type=float, default=1.5)

    join = sub.add_parser("join", help="join a host by IP")
    join.add_argument("address")
    join.add_argument("--name", default="PC CLIENT")
    join.add_argument("--port", type=int, default=PORT)

    args = parser.parse_args()
    if args.command == "host":
        return run_host(args.name, args.port)
    if args.command == "scan":
        return run_scan(args.seconds)
    if args.command == "join":
        return run_join(args.address, args.name, args.port)
    return 2


if __name__ == "__main__":
    sys.exit(main())
