package com.chanjun.ticker.compose

import androidx.compose.ui.unit.sp

object TickerUtils {
    const val EMPTY_CHAR = 0.toChar()
    val DEFAULT_FONT_SIZE = 16.sp
    val DEFAULT_LINE_HEIGHT = 16.sp

    fun provideNumberList(): String {
        return "0123456789"
    }

    fun provideAlphabeticalList(): String {
        return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}