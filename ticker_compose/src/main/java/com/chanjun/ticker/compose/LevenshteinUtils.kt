package com.chanjun.ticker.compose

import kotlin.math.max
import kotlin.math.min

internal object LevenshteinUtils {
    const val ACTION_SAME = 0
    const val ACTION_INSERT = 1
    const val ACTION_DELETE = 2

    /**
     * This is a wrapper function around [.appendColumnActionsForSegment] that
     * additionally takes in supportedCharacters. It uses supportedCharacters to compute whether
     * the current character should be animated or if it should remain in-place.
     *
     * For specific implementation details, see [.appendColumnActionsForSegment].
     *
     * @param source the source char array to animate from
     * @param target the target char array to animate to
     * @param supportedCharacters all characters that support custom animation.
     * @return an int array of size min(source.length, target.length) where each index
     * corresponds to one of [.ACTION_SAME], [.ACTION_INSERT],
     * [.ACTION_DELETE] to represent if we update, insert, or delete a character
     * at the particular index.
     */
    fun computeColumnActions(
        source: CharArray,
        target: CharArray,
        supportedCharacters: Set<Char>
    ): IntArray {
        var sourceIndex = 0
        var targetIndex = 0

        val columnActions: MutableList<Int> = ArrayList()
        while (true) {
            // Check for terminating conditions
            val reachedEndOfSource = sourceIndex == source.size
            val reachedEndOfTarget = targetIndex == target.size

            if (reachedEndOfSource && reachedEndOfTarget) {
                break
            } else if (reachedEndOfSource) {
                fillWithActions(columnActions, target.size - targetIndex, ACTION_INSERT)
                break
            } else if (reachedEndOfTarget) {
                fillWithActions(columnActions, source.size - sourceIndex, ACTION_DELETE)
                break
            }

            val containsSourceChar = supportedCharacters.contains(source[sourceIndex])
            val containsTargetChar = supportedCharacters.contains(target[targetIndex])

            if (containsSourceChar && containsTargetChar) {
                // We reached a segment that we can perform animations on
                val sourceEndIndex =
                    findNextUnsupportedChar(source, sourceIndex + 1, supportedCharacters)
                val targetEndIndex =
                    findNextUnsupportedChar(target, targetIndex + 1, supportedCharacters)

                appendColumnActionsForSegment(
                    columnActions,
                    source,
                    target,
                    sourceIndex,
                    sourceEndIndex,
                    targetIndex,
                    targetEndIndex
                )
                sourceIndex = sourceEndIndex
                targetIndex = targetEndIndex
            } else if (containsSourceChar) {
                // We are animating in a target character that isn't supported
                columnActions.add(ACTION_INSERT)
                targetIndex++
            } else if (containsTargetChar) {
                // We are animating out a source character that isn't supported
                columnActions.add(ACTION_DELETE)
                sourceIndex++
            } else {
                // Both characters are not supported, perform default animation to replace
                columnActions.add(ACTION_SAME)
                sourceIndex++
                targetIndex++
            }
        }

        // Concat all of the actions into one array
        val result = IntArray(columnActions.size)
        for (i in columnActions.indices) {
            result[i] = columnActions[i]
        }
        return result
    }

    private fun findNextUnsupportedChar(
        chars: CharArray,
        startIndex: Int,
        supportedCharacters: Set<Char?>
    ): Int {
        for (i in startIndex until chars.size) {
            if (!supportedCharacters.contains(chars[i])) {
                return i
            }
        }
        return chars.size
    }

    private fun fillWithActions(actions: MutableList<Int>, num: Int, action: Int) {
        for (i in 0 until num) {
            actions.add(action)
        }
    }

    /**
     * Run a slightly modified version of Levenshtein distance algorithm to compute the minimum
     * edit distance between the current and the target text within the start and end bounds.
     * Unlike the traditional algorithm, we force return all [.ACTION_SAME] for inputs that
     * are the same length (so optimize update over insertion/deletion).
     *
     * @param columnActions the target list to append actions into
     * @param source the source character array
     * @param target the target character array
     * @param sourceStart the start index of source to compute column actions (inclusive)
     * @param sourceEnd the end index of source to compute column actions (exclusive)
     * @param targetStart the start index of target to compute column actions (inclusive)
     * @param targetEnd the end index of target to compute column actions (exclusive)
     */
    private fun appendColumnActionsForSegment(
        columnActions: MutableList<Int>,
        source: CharArray,
        target: CharArray,
        sourceStart: Int,
        sourceEnd: Int,
        targetStart: Int,
        targetEnd: Int
    ) {
        val sourceLength = sourceEnd - sourceStart
        val targetLength = targetEnd - targetStart
        val resultLength = max(sourceLength.toDouble(), targetLength.toDouble()).toInt()

        if (sourceLength == targetLength) {
            // No modifications needed if the length of the strings are the same
            fillWithActions(columnActions, resultLength, ACTION_SAME)
            return
        }

        val numRows = sourceLength + 1
        val numCols = targetLength + 1

        // Compute the Levenshtein matrix
        val matrix = Array(numRows) {
            IntArray(
                numCols
            )
        }
        for (i in 0 until numRows) {
            matrix[i][0] = i
        }
        for (j in 0 until numCols) {
            matrix[0][j] = j
        }

        var cost: Int
        for (row in 1 until numRows) {
            for (col in 1 until numCols) {
                cost = if (source[row - 1 + sourceStart] == target[col - 1 + targetStart]) 0 else 1

                matrix[row][col] = min(
                    matrix[row - 1][col] + 1,
                    matrix[row][col - 1] + 1,
                    matrix[row - 1][col - 1] + cost
                )
            }
        }

        // Reverse trace the matrix to compute the necessary actions
        val resultList: MutableList<Int> = ArrayList(resultLength * 2)
        var row = numRows - 1
        var col = numCols - 1

        while (row > 0 || col > 0) {
            if (row == 0) {
                // At the top row, can only move left, meaning insert column
                resultList.add(ACTION_INSERT)
                col--
            } else if (col == 0) {
                // At the left column, can only move up, meaning delete column
                resultList.add(ACTION_DELETE)
                row--
            } else {
                val insert = matrix[row][col - 1]
                val delete = matrix[row - 1][col]
                val replace = matrix[row - 1][col - 1]

                if (insert < delete && insert < replace) {
                    resultList.add(ACTION_INSERT)
                    col--
                } else if (delete < replace) {
                    resultList.add(ACTION_DELETE)
                    row--
                } else {
                    resultList.add(ACTION_SAME)
                    row--
                    col--
                }
            }
        }

        // Reverse the actions to get the correct ordering
        val resultSize = resultList.size
        for (i in resultSize - 1 downTo 0) {
            columnActions.add(resultList[i])
        }
    }

    private fun min(first: Int, second: Int, third: Int): Int {
        return min(first.toDouble(), min(second.toDouble(), third.toDouble())).toInt()
    }
}