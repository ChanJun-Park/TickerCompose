package com.chanjun.ticker.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import kotlin.math.abs
import kotlin.math.max

class TickerColumn(
    private var characterLists: Array<TickerCharacterList>,
    private val metrics: TickerDrawMetrics
) {
    var currentChar = TickerUtils.EMPTY_CHAR
        private set
    var targetChar = TickerUtils.EMPTY_CHAR
        private set

    // The indices characters simply signify what positions are for the current and target
    // characters in the assigned characterList. This tells us how to animate from the current
    // to the target characters.
    private lateinit var currentCharacterList: CharArray
    private var startIndex = 0
    private var endIndex = 0

    // Drawing state variables that get updated whenever animation progress gets updated.
    private var bottomCharIndex = 0
    private var bottomDelta = 0f
    private var charHeight = 0f

    // Drawing state variables for handling size transition
    private var sourceWidth = 0f  // Drawing state variables for handling size transition
    private var currentWidth = 0f  // Drawing state variables for handling size transition
    private var targetWidth = 0f  // Drawing state variables for handling size transition
    private var minimumRequiredWidth = 0f

    // The bottom delta variables signifies the vertical offset that the bottom drawn character
    // is seeing. If the delta is 0, it means that the character is perfectly centered. If the
    // delta is negative, it means that the bottom character is poking out from the bottom and
    // part of the top character is visible. The delta should never be positive because it means
    // that the bottom character is not actually the bottom character.
    private var currentBottomDelta = 0f
    private var previousBottomDelta = 0f
    private var directionAdjustment = 0

    /**
     * Tells the column that the next character it should show is {@param targetChar}. This can
     * change can either be animated or instant depending on the animation progress set by
     * [.setAnimationProgress].
     */
    fun setTargetChar(targetChar: Char) {
        // Set the current and target characters for the animation
        this.targetChar = targetChar
        sourceWidth = currentWidth
        targetWidth = metrics.getCharWidth(targetChar)
        minimumRequiredWidth = max(sourceWidth.toDouble(), targetWidth.toDouble()).toFloat()

        // Calculate the current indices
        setCharacterIndices()
        val scrollDown = endIndex >= startIndex
        directionAdjustment = if (scrollDown) 1 else -1

        // Save the currentBottomDelta as previousBottomDelta in case this call to setTargetChar
        // interrupted a previously running animation. The deltas will then be used to compute
        // offset so that the interruption feels smooth on the UI.
        previousBottomDelta = currentBottomDelta
        currentBottomDelta = 0f
    }

    fun getCurrentWidth(): Float {
        checkForDrawMetricsChanges()
        return currentWidth
    }

    fun getMinimumRequiredWidth(): Float {
        checkForDrawMetricsChanges()
        return minimumRequiredWidth
    }

    /**
     * A helper method for populating [.startIndex] and [.endIndex] given the
     * current and target characters for the animation.
     */
    private fun setCharacterIndices() {
        var characterList: CharArray? = null

        for (i in characterLists.indices) {
            val indices: TickerCharacterList.CharacterIndices? =
                characterLists[i].getCharacterIndices(
                    currentChar,
                    targetChar,
                    metrics.getPreferredScrollingDirection()
                )
            if (indices != null) {
                characterList = characterLists[i].getCharacterList()
                startIndex = indices.startIndex
                endIndex = indices.endIndex
            }
        }

        // If we didn't find a list that contains both characters, just perform a default animation
        // going straight from source to target
        if (characterList == null) {
            if (currentChar == targetChar) {
                currentCharacterList = charArrayOf(currentChar)
                endIndex = 0
                startIndex = 0
            } else {
                currentCharacterList = charArrayOf(currentChar, targetChar)
                startIndex = 0
                endIndex = 1
            }
        } else {
            currentCharacterList = characterList
        }
    }

    fun onAnimationEnd() {
        checkForDrawMetricsChanges()
        minimumRequiredWidth = currentWidth
    }

    private fun checkForDrawMetricsChanges() {
        val currentTargetWidth = metrics.getCharWidth(targetChar)
        // Only resize due to DrawMetrics changes when we are done with whatever animation we
        // are running.
        if (currentWidth == targetWidth && targetWidth != currentTargetWidth) {
            targetWidth = currentTargetWidth
            currentWidth = targetWidth
            minimumRequiredWidth = currentWidth
        }
    }

    fun setAnimationProgress(animationProgress: Float) {
        if (animationProgress == 1f) {
            // Animation finished (or never started), set to stable state.
            currentChar = targetChar
            currentBottomDelta = 0f
            previousBottomDelta = 0f
        }
        val charHeight = metrics.getCharHeight()

        // First let's find the total height of this column between the start and end chars.
        val totalHeight = (charHeight * abs((endIndex - startIndex).toDouble())).toFloat()

        // The current base is then the part of the total height that we have progressed to
        // from the animation. For example, there might be 5 characters, each character is
        // 2px tall, so the totalHeight is 10. If we are at 50% progress, then our baseline
        // in this column is at 5 out of 10 (which is the 3rd character with a -50% offset
        // to the baseline).
        val currentBase = animationProgress * totalHeight

        // Given the current base, we now can find which character should drawn on the bottom.
        // Note that this position is a float. For example, if the bottomCharPosition is
        // 4.5, it means that the bottom character is the 4th character, and it has a -50%
        // offset relative to the baseline.
        val bottomCharPosition = currentBase / charHeight

        // By subtracting away the integer part of bottomCharPosition, we now have the
        // percentage representation of the bottom char's offset.
        val bottomCharOffsetPercentage = bottomCharPosition - bottomCharPosition.toInt()

        // We might have interrupted a previous animation if previousBottomDelta is not 0f.
        // If that's the case, we need to take this delta into account so that the previous
        // character offset won't be wiped away when we start a new animation.
        // We multiply by the inverse percentage so that the offset contribution from the delta
        // progresses along with the rest of the animation (from full delta to 0).
        val additionalDelta = previousBottomDelta * (1f - animationProgress)

        // Now, using the bottom char's offset percentage and the delta we have from the
        // previous animation, we can now compute what's the actual offset of the bottom
        // character in the column relative to the baseline.
        bottomDelta =
            (bottomCharOffsetPercentage * charHeight * directionAdjustment + additionalDelta)

        // Figure out what the actual character index is in the characterList, and then
        // draw the character with the computed offset.
        bottomCharIndex = startIndex + bottomCharPosition.toInt() * directionAdjustment
        this.charHeight = charHeight
        currentWidth = sourceWidth + (targetWidth - sourceWidth) * animationProgress
    }

    /**
     * Draw the current state of the column as it's animating from one character in the list
     * to another. This method will take into account various factors such as animation
     * progress and the previously interrupted animation state to render the characters
     * in the correct position on the canvas.
     */
    fun draw(drawScope: DrawScope) = with(drawScope) {
        if (
            drawText(
                characterList = currentCharacterList,
                index = bottomCharIndex,
                verticalOffset = bottomDelta
            )
        ) {
            // Save the current drawing state in case our animation gets interrupted
            if (bottomCharIndex >= 0) {
                currentChar = currentCharacterList[bottomCharIndex]
            }
            currentBottomDelta = bottomDelta
        }

        // Draw the corresponding top and bottom characters if applicable
        drawText(
            characterList = currentCharacterList,
            index = bottomCharIndex + 1,
            verticalOffset = bottomDelta - charHeight
        )
        // Drawing the bottom character here might seem counter-intuitive because we've been
        // computing for the bottom character this entire time. But the bottom character
        // computed above might actually be above the baseline if we interrupted a previous
        // animation that gave us a positive additionalDelta.
        drawText(
            characterList = currentCharacterList,
            index = bottomCharIndex - 1,
            verticalOffset = bottomDelta + charHeight
        )
    }

    /**
     * @return whether the text was successfully drawn on the canvas
     */
    private fun DrawScope.drawText(
        characterList: CharArray,
        index: Int,
        verticalOffset: Float
    ): Boolean {
        if (index >= 0 && index < characterList.size) {
            drawText(
                textMeasurer = metrics.textMeasurer,
                text = characterList[index].toString(),
                topLeft = Offset(x = 0f, y = verticalOffset),
                style = metrics.textStyle,
                size = Size(width = currentWidth, height = charHeight)
            )
            return true
        }
        return false
    }

    fun setCharacterLists(characterLists: Array<TickerCharacterList>) {
        this.characterLists = characterLists
    }
}