package com.cocoa.expenses.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CocoaColorScheme = lightColorScheme(
    primary = RichCocoaBeanAccent,
    onPrimary = CrispWhiteText,
    secondary = CaramelGanacheAccent,
    onSecondary = CrispWhiteText,
    tertiary = DarkEspressoChocolate,
    background = WarmWhiteBackground,
    onBackground = DeepEspressoText,
    surface = AlabasterMilkSurface,
    onSurface = DeepEspressoText,
    surfaceVariant = AlabasterMilkSurface,
    onSurfaceVariant = RoastedMochaText,
    outline = HairlineBorderDivider,
    error = StatusOverBudgetRed
)

@Composable
fun CocoaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CocoaColorScheme,
        content = content
    )
}
