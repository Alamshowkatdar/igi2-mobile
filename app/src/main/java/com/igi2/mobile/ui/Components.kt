package com.igi2.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Igi2Surface, RoundedCornerShape(3.dp))
            .border(1.dp, Igi2Amber.copy(alpha = 0.26f), RoundedCornerShape(3.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Igi2Amber) {
    Text(
        text = "> $text",
        modifier = modifier,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp
    )
}

@Composable
fun Igi2Button(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        border = if (primary) null else BorderStroke(1.dp, Igi2Amber.copy(alpha = 0.5f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Igi2Amber else Color.Transparent,
            contentColor = if (primary) Color.Black else Igi2Amber,
            disabledContainerColor = if (primary) Igi2Amber.copy(alpha = 0.22f) else Color.Transparent,
            disabledContentColor = Igi2Dim
        )
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun TextLink(text: String, color: Color = Igi2Cyan, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "<",
                color = Igi2Amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
        Text(
            text = title,
            color = Igi2Text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun StatusPill(label: String, value: String, tone: StatusTone) {
    val color = when (tone) {
        StatusTone.GOOD -> Igi2Amber
        StatusTone.LIVE -> Igi2Cyan
        StatusTone.WAIT -> Igi2Dim
        StatusTone.BAD -> Igi2Red
    }
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label $value",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

enum class StatusTone { GOOD, LIVE, WAIT, BAD }

@Composable
fun KeyValueRow(key: String, value: String, valueColor: Color = Igi2Text) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(key, color = Igi2Dim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
fun BodyText(text: String, color: Color = Igi2Text, size: Int = 13, lineHeight: Int = 19) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        lineHeight = lineHeight.sp
    )
}

@Composable
fun EmptyState(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        color = Igi2Dim,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun RowScope.Grow() {
    Spacer(Modifier.weight(1f))
}
