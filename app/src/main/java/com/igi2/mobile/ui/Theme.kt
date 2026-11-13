package com.igi2.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Igi2Black = Color(0xFF05070A)
val Igi2Surface = Color(0xFF0B1117)
val Igi2Surface2 = Color(0xFF121C24)
val Igi2Amber = Color(0xFFFFB347)
val Igi2AmberDim = Color(0xFF8A5A1E)
val Igi2Cyan = Color(0xFF63D9E8)
val Igi2Text = Color(0xFFC9D6D2)
val Igi2Dim = Color(0xFF6C7C82)
val Igi2Red = Color(0xFFFF5C5C)

private val Igi2Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        letterSpacing = 2.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp
    )
)

@Composable
fun Igi2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Igi2Amber,
            onPrimary = Color.Black,
            primaryContainer = Igi2AmberDim,
            onPrimaryContainer = Color.White,
            secondary = Igi2Cyan,
            onSecondary = Color.Black,
            background = Igi2Black,
            onBackground = Igi2Text,
            surface = Igi2Surface,
            onSurface = Igi2Text,
            surfaceVariant = Igi2Surface2,
            onSurfaceVariant = Igi2Dim,
            outline = Igi2Amber.copy(alpha = 0.35f),
            error = Igi2Red,
            onError = Color.Black
        ),
        typography = Igi2Typography,
        content = content
    )
}
