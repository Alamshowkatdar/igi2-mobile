package com.igi2.mobile.data

/**
 * A campaign mission and its published briefing.
 *
 * The briefings are the ones playigi.com publishes for I.G.I. 2: Covert Strike. They are text only:
 * no game data or artwork is bundled with this app.
 */
data class Mission(
    val number: Int,
    val campaign: Int,
    val name: String,
    val objectives: List<String>
)

object Missions {

    val all: List<Mission> = listOf(
        Mission(
            number = 1,
            campaign = 1,
            name = "Infiltration",
            objectives = listOf(
                "Infiltrate the main compound.",
                "Cross the river to gain access to the upper compound.",
                "Activate, and use the lift down to the mines."
            )
        ),
        Mission(
            number = 2,
            campaign = 1,
            name = "Deep in the Mines",
            objectives = listOf(
                "Gain access to the lower cave system.",
                "Disable the security system.",
                "Open the doors to the tunnel.",
                "Ride the train to the ventilation shaft.",
                "Use the ventilation shaft to gain access to the weather station."
            )
        ),
        Mission(
            number = 3,
            campaign = 1,
            name = "The Weather Station",
            objectives = listOf(
                "Pick up the laser cutter from the airdrop.",
                "Steal the papers from the safe.",
                "Disable the security system and override the airlock to get access to the lab.",
                "Steal the prototype EMP chip from the lab.",
                "Raise the weather balloon and await evacuation."
            )
        ),
        Mission(
            number = 4,
            campaign = 1,
            name = "Bridge Across the Dnestr",
            objectives = listOf(
                "Infiltrate the warehouse and acquire four C4 charges.",
                "Acquire the timers and fuses from a patrol near the bridge.",
                "Set a C4 charge on each of the four supporting pillars of the bridge.",
                "Get to safety before the bridge explodes, on the side of the river where the convoy is."
            )
        ),
        Mission(
            number = 5,
            campaign = 1,
            name = "Ambush",
            objectives = listOf(
                "Destroy the APC using the rocket launcher found in the convoy.",
                "Pick up the EMP chips from the trucks in the convoy.",
                "Resist the enemy forces and kill them all.",
                "Await the helicopter for evacuation."
            )
        ),
        Mission(
            number = 6,
            campaign = 1,
            name = "Production Facility",
            objectives = listOf(
                "Disable the security cameras.",
                "Steal the blueprints for the EMP chips.",
                "Turn off the power for the electrical fences.",
                "Turn on the power for the factory machinery.",
                "Hack the computer controlling the assembly line machinery.",
                "Get to the helicopter for evacuation."
            )
        ),
        Mission(
            number = 7,
            campaign = 1,
            name = "Border Crossing",
            objectives = listOf(
                "Ambush the communications vehicle guards.",
                "Send a radio signal for the rescue helicopter.",
                "Move to the extraction zone."
            )
        ),
        Mission(
            number = 8,
            campaign = 2,
            name = "Libyan Rendezvous",
            objectives = listOf(
                "Meet the bribed local militia man to be guided to the surveillance room.",
                "Shut down the surveillance cameras for the hotel.",
                "Pick up the explosive charge.",
                "Blow up the refuelling vehicle in the market place.",
                "Neutralise the intelligence operatives guarding Priboi.",
                "Get to the hotel and rescue Priboi."
            )
        ),
        Mission(
            number = 9,
            campaign = 2,
            name = "Prison Escape",
            objectives = listOf(
                "Escape from your cell.",
                "Get your map computer and lockpicks back.",
                "Locate Priboi in the main prison building.",
                "Get to the security control building and hack the terminal to reconfigure the prison security system.",
                "Open the gates to the inner and outer yards, and the main gates.",
                "Take down the two guards covering the gate by the prison, and provide covering sniper fire for Priboi as he makes a dash for the getaway truck."
            )
        ),
        Mission(
            number = 10,
            campaign = 2,
            name = "Priboi's Villa",
            objectives = listOf(
                "Take a backup of Priboi's computer records.",
                "Pick up the envelope located in the safe in Priboi's bedroom.",
                "Get out of there in Priboi's limousine, located in the garage compound."
            )
        ),
        Mission(
            number = 11,
            campaign = 2,
            name = "The Airfield",
            objectives = listOf(
                "Infiltrate the compound.",
                "Sabotage the radio.",
                "Acquire ammunition for the helicopter's gun.",
                "Power up the fuel pumps.",
                "Activate the fuel pump."
            )
        ),
        Mission(
            number = 12,
            campaign = 2,
            name = "Zaleb's Stronghold",
            objectives = listOf(
                "Eliminate the enemies heading for the radio station before they warn of your arrival.",
                "Get Priboi's briefcase, and eliminate Zaleb.",
                "Get back to the helicopter with the briefcase."
            )
        ),
        Mission(
            number = 13,
            campaign = 2,
            name = "Showdown at the Docks",
            objectives = listOf(
                "Search the database for information on the secure area.",
                "Turn on the power to the crane.",
                "Destroy the secure compound wall.",
                "Search the offices in the secure warehouse.",
                "Confront Robert Quest and the Chinese soldiers.",
                "Get aboard the Ekranoplane."
            )
        ),
        Mission(
            number = 14,
            campaign = 3,
            name = "Island Assault",
            objectives = listOf(
                "Turn off the power supply for the radar facility.",
                "Disable the radar terminal.",
                "Disable the communications terminal."
            )
        ),
        Mission(
            number = 15,
            campaign = 3,
            name = "Air-Strike",
            objectives = listOf(
                "Get in range and tag the first SAM launcher.",
                "Get in range of the second SAM launcher before the friendly fighter is shot down.",
                "Tag the second SAM launcher.",
                "Move to the drop zone and collect the equipment canister."
            )
        ),
        Mission(
            number = 16,
            campaign = 3,
            name = "The Ancient Temple",
            objectives = listOf(
                "Neutralise the enemy snipers.",
                "Gain access to the main gatehouse.",
                "Disable the security cameras.",
                "Re-align the satellite dish.",
                "Gain access to the main temple building."
            )
        ),
        Mission(
            number = 17,
            campaign = 3,
            name = "Secret Weapons Lab",
            objectives = listOf(
                "Get the blueprints of the laboratory.",
                "Customise the transmitter.",
                "Upload data on the EMP weapons.",
                "Defeat Wu Xing and his squad of elite troops."
            )
        ),
        Mission(
            number = 18,
            campaign = 3,
            name = "Mission Control",
            objectives = listOf(
                "Sabotage the radar control.",
                "Call for repairs.",
                "Gain access to the launch control compound.",
                "Acquire the launch codes.",
                "Reprogram the navigational computer.",
                "Override launch control.",
                "Slide down the wire to the launch pad."
            )
        ),
        Mission(
            number = 19,
            campaign = 3,
            name = "The Launch Pad",
            objectives = listOf(
                "Shut off the rocket fuelling process.",
                "Place the tracking device inside the rocket.",
                "Move the gantry.",
                "Seal off the launch bunker.",
                "Initiate the rocket launch."
            )
        )
    )

    val campaigns: List<Int> = all.map { it.campaign }.distinct().sorted()

    fun forCampaign(campaign: Int): List<Mission> = all.filter { it.campaign == campaign }

    fun campaignTitle(campaign: Int): String = when (campaign) {
        1 -> "THE CARPATHIANS"
        2 -> "THE MEDITERRANEAN"
        3 -> "THE ISLAND"
        else -> "CAMPAIGN $campaign"
    }

    fun campaignRange(campaign: Int): String {
        val missions = forCampaign(campaign)
        if (missions.isEmpty()) return ""
        return "MISSIONS ${missions.first().number.toString().padStart(2, '0')}–" +
            missions.last().number.toString().padStart(2, '0')
    }
}
