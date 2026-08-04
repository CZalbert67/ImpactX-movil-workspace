package com.example.impactx.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    IMPACTX_NEON,
    PROFESSIONAL,
    CLARO
}

object ThemeConfig {
    var currentTheme by mutableStateOf(AppTheme.IMPACTX_NEON)

    val primaryColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF6EF6C3)
            AppTheme.PROFESSIONAL -> Color(0xFF1FB6D5)
            AppTheme.CLARO -> Color(0xFF00A9A5)
        }

    val secondaryColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF60A5FF)
            AppTheme.PROFESSIONAL -> Color(0xFF00D0A6)
            AppTheme.CLARO -> Color(0xFF2563EB)
        }

    val backgroundColorStart: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF07051A)
            AppTheme.PROFESSIONAL -> Color(0xFF06111F)
            AppTheme.CLARO -> Color(0xFFEEF5F8)
        }

    val backgroundColorEnd: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF111035)
            AppTheme.PROFESSIONAL -> Color(0xFF040D17)
            AppTheme.CLARO -> Color(0xFFDFEEF4)
        }

    val cardBackgroundColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF151438)
            AppTheme.PROFESSIONAL -> Color(0xFF102238)
            AppTheme.CLARO -> Color(0xFFFFFFFF)
        }

    val cardElevatedColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF1C1A4A)
            AppTheme.PROFESSIONAL -> Color(0xFF132C47)
            AppTheme.CLARO -> Color(0xFFF7FBFD)
        }

    val textPrimaryColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFFF6F7FF)
            AppTheme.PROFESSIONAL -> Color(0xFFF7FBFF)
            AppTheme.CLARO -> Color(0xFF142231)
        }

    val textSecondaryColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFFB9BFDF)
            AppTheme.PROFESSIONAL -> Color(0xFFBFD0DF)
            AppTheme.CLARO -> Color(0xFF5B6B79)
        }

    val textMutedColor: Color
        get() = when (currentTheme) {
            AppTheme.IMPACTX_NEON -> Color(0xFF8790B7)
            AppTheme.PROFESSIONAL -> Color(0xFF7E94A8)
            AppTheme.CLARO -> Color(0xFF81919E)
        }

    val isDark: Boolean
        get() = currentTheme != AppTheme.CLARO
}

// ============ PACKAGE LEVEL DYNAMIC COLOR ACCESSORS ============

val DarkBlue: Color
    get() = ThemeConfig.backgroundColorStart

val DarkBlueEnd: Color
    get() = ThemeConfig.backgroundColorEnd

val TealPrimary: Color
    get() = ThemeConfig.primaryColor

val GrayMuted: Color
    get() = ThemeConfig.textMutedColor

val CardBgColor: Color
    get() = ThemeConfig.cardBackgroundColor

val CardElevatedColor: Color
    get() = ThemeConfig.cardElevatedColor

val TextPrimaryColor: Color
    get() = ThemeConfig.textPrimaryColor

val TextSecondaryColor: Color
    get() = ThemeConfig.textSecondaryColor
