package com.chanjun.ticker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

@Composable
fun rememberTickerDrawMetrics(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
): TickerDrawMetrics {
    return remember(textMeasurer, textStyle) {
        TickerDrawMetrics(textMeasurer, textStyle)
    }
}

@Immutable
class TickerDrawMetrics(
    val textMeasurer: TextMeasurer,
    val textStyle: TextStyle
) {

    // These are attributes on the text paint used for measuring and drawing the text on the
    // canvas. These attributes are reset whenever anything on the text paint changes.
    private val charWidths: MutableMap<Char, Float> = HashMap(256)
    private var charHeights: MutableMap<Char, Float> = HashMap(256)

    private var preferredScrollingDirection: TickerScrollingDirection = TickerScrollingDirection.ANY

    fun getCharWidth(character: Char): Float {
        if (character == TickerUtils.EMPTY_CHAR) {
            return 0f
        }

        // This method will lazily initialize the char width map.
        val value = charWidths[character]
        return if (value != null) {
            value
        } else {
            val width = textMeasurer.measure(
                text = character.toString(),
                style = textStyle
            ).size.width.toFloat()

            charWidths[character] = width
            width
        }
    }

    fun getCharHeight(character: Char): Float {
        if (character == TickerUtils.EMPTY_CHAR) {
            return 0f
        }

        // This method will lazily initialize the char height map.
        val value = charHeights[character]
        return if (value != null) {
            value
        } else {
            val width = textMeasurer.measure(
                text = character.toString(),
                style = textStyle
            ).size.height.toFloat()

            charHeights[character] = width
            width
        }
    }

    fun getPreferredScrollingDirection(): TickerScrollingDirection {
        return preferredScrollingDirection
    }

    fun setPreferredScrollingDirection(preferredScrollingDirection: TickerScrollingDirection) {
        this.preferredScrollingDirection = preferredScrollingDirection
    }
}