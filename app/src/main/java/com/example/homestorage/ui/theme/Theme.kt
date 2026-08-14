package com.example.homestorage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 浅色配色（扁平：低阴影、圆角适中、蓝白主流风） */
private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = SlateGray,
    onSecondary = Color.White,
    secondaryContainer = SlateGrayLight,
    onSecondaryContainer = SlateGrayDark,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
)

/** 深色配色 */
private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = BrandBlueDark,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBlueLight,
    secondary = SlateGrayLight,
    onSecondary = SlateGrayDark,
    secondaryContainer = SlateGrayDark,
    onSecondaryContainer = SlateGrayLight,
    background = BackgroundDark,
    onBackground = Color(0xFFE6E3DF),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E3DF),
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = Color(0xFF9E9B97),
    outline = OutlineDark,
    outlineVariant = OutlineDark,
)

/** 圆角形状（扁平现代：圆角适中不夸张） */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * HomeStorage 主题：固定品牌色（关闭动态色保证品牌一致），扁平浅灰背景 + 暖橙主色 + 大圆角
 *
 * @param darkTheme 是否深色模式，默认跟随系统
 * @param content 内容
 */
@Composable
fun HomeStorageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
