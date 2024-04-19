package com.chanjun.ticker.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

@Composable
fun rememberTickerColumnManager(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
): TickerColumnManager {
    val tickerDrawMetrics = rememberTickerDrawMetrics(textMeasurer, textStyle)
    return remember(tickerDrawMetrics) {
        TickerColumnManager(tickerDrawMetrics)
    }
}

class TickerColumnManager(private val metrics: TickerDrawMetrics) {
    val tickerColumns = ArrayList<TickerColumn>()

    var characterLists: Array<TickerCharacterList>? = null
        private set
    private lateinit var supportedCharacters: Set<Char>

    /**
     * @inheritDoc TickerView#setCharacterLists
     */
    fun setCharacterLists(vararg characterLists: String) {
        val tickerCharacterLists = characterLists.map { TickerCharacterList(it) }.toTypedArray()
        this.characterLists = tickerCharacterLists

        supportedCharacters = HashSet<Char>().apply {
            for (i in characterLists.indices) {
                addAll(tickerCharacterLists[i].getSupportedCharacters())
            }
        }

        // Update character lists in current columns
        for (tickerColumn in tickerColumns) {
            tickerColumn.setCharacterLists(tickerCharacterLists)
        }
    }

    fun setText(text: CharArray) {
        val characterLists = this.characterLists
        checkNotNull(characterLists) { "Need to call #setCharacterLists first." }

        // First remove any zero-width columns
        var i = 0
        while (i < tickerColumns.size) {
            val tickerColumn = tickerColumns[i]
            if (tickerColumn.getCurrentWidth() > 0) {
                i++
            } else {
                tickerColumns.removeAt(i)
            }
        }

        // Use Levenshtein distance algorithm to figure out how to manipulate the columns
        val actions = LevenshteinUtils.computeColumnActions(
            getCurrentText(), text, supportedCharacters
        )

        var columnIndex = 0
        var textIndex = 0
        for (index in actions.indices) {
            when (actions[index]) {
                LevenshteinUtils.ACTION_INSERT -> {
                    tickerColumns.add(
                        columnIndex,
                        TickerColumn(characterLists, metrics)
                    )
                    tickerColumns[columnIndex].setTargetChar(text[textIndex])
                    columnIndex++
                    textIndex++
                }

                LevenshteinUtils.ACTION_SAME -> {
                    tickerColumns[columnIndex].setTargetChar(text[textIndex])
                    columnIndex++
                    textIndex++
                }

                LevenshteinUtils.ACTION_DELETE -> {
                    tickerColumns[columnIndex].setTargetChar(TickerUtils.EMPTY_CHAR)
                    columnIndex++
                }

                else -> throw IllegalArgumentException("Unknown action: " + actions[index])
            }
        }
    }

    fun onAnimationEnd() {
        tickerColumns.forEach {
            it.onAnimationEnd()
        }
    }

    fun setAnimationProgress(animationProgress: Float) {
        tickerColumns.forEach {
            it.setAnimationProgress(animationProgress)
        }
    }

    fun getMinimumRequiredWidth(): Float {
        var width = 0f
        tickerColumns.forEach {
            width += it.getCurrentWidth()
        }

        return width
    }

    fun getCurrentWidth(): Float {
        var width = 0f
        tickerColumns.forEach {
            width += it.getCurrentWidth()
        }
        return width
    }

    fun getCurrentText(): CharArray {
        val currentText = CharArray(tickerColumns.size)
        tickerColumns.forEachIndexed { index, tickerColumn ->
            currentText[index] = tickerColumn.currentChar
        }
        return currentText
    }

    /**
     * This method will draw onto the canvas the appropriate UI state of each column dictated
     * by {@param animationProgress}. As a side effect, this method will also translate the canvas
     * accordingly for the draw procedures.
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        var y = 0f
        tickerColumns.forEach { tickerColumn ->
            translate(left = y, top = 0f) {
                tickerColumn.draw(this)
            }

            y += tickerColumn.getCurrentWidth()
        }
    }
}