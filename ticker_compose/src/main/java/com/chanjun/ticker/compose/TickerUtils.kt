package com.chanjun.ticker.compose

object TickerUtils {
    const val EMPTY_CHAR = 0.toChar()

    fun provideNumberList(): String {
        return "0123456789"
    }

    fun provideAlphabeticalList(): String {
        return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}