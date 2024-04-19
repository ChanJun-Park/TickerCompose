package com.chanjun.ticker.compose

class TickerCharacterList(characterList: String) {
    private val numOriginalCharacters: Int

    // The saved character list will always be of the format: EMPTY, list, list
    private val characterList: CharArray

    // A minor optimization so that we can cache the indices of each character.
    private val characterIndicesMap: Map<Char, Int>

    init {
        require(!characterList.contains(TickerUtils.EMPTY_CHAR)) {
            "You cannot include TickerUtils.EMPTY_CHAR in the character list."
        }

        val charsArray = characterList.toCharArray()
        val length = charsArray.size
        numOriginalCharacters = length

        characterIndicesMap = HashMap(length)
        for (i in 0 until length) {
            characterIndicesMap.put(charsArray[i], i)
        }

        this.characterList = CharArray(length * 2 + 1)
        this.characterList[0] = TickerUtils.EMPTY_CHAR
        for (i in 0 until length) {
            this.characterList[1 + i] = charsArray[i]
            this.characterList[1 + length + i] = charsArray[i]
        }
    }

    /**
     * @param start the character that we want to animate from
     * @param end the character that we want to animate to
     * @param direction the preferred {@Link TickerView#ScrollingDirection}
     * @return a valid pair of start and end indices, or null if the inputs are not supported.
     */
    fun getCharacterIndices(
        start: Char,
        end: Char,
        direction: TickerScrollingDirection
    ): CharacterIndices? {
        var startIndex = getIndexOfChar(start)
        var endIndex = getIndexOfChar(end)

        if (startIndex < 0 || endIndex < 0) {
            return null
        }

        when (direction) {
            TickerScrollingDirection.DOWN -> if (end == TickerUtils.EMPTY_CHAR) {
                endIndex = characterList.size
            } else if (endIndex < startIndex) {
                endIndex += numOriginalCharacters
            }

            TickerScrollingDirection.UP -> if (startIndex < endIndex) {
                startIndex += numOriginalCharacters
            }

            TickerScrollingDirection.ANY ->
                // see if the wrap-around animation is shorter distance than the original animation
                if (start != TickerUtils.EMPTY_CHAR && end != TickerUtils.EMPTY_CHAR) {
                    if (endIndex < startIndex) {
                        // If we are potentially going backwards
                        val nonWrapDistance = startIndex - endIndex
                        val wrapDistance = numOriginalCharacters - startIndex + endIndex
                        if (wrapDistance < nonWrapDistance) {
                            endIndex += numOriginalCharacters
                        }
                    } else if (startIndex < endIndex) {
                        // If we are potentially going forwards
                        val nonWrapDistance = endIndex - startIndex
                        val wrapDistance = numOriginalCharacters - endIndex + startIndex
                        if (wrapDistance < nonWrapDistance) {
                            startIndex += numOriginalCharacters
                        }
                    }
                }
        }
        return CharacterIndices(startIndex, endIndex)
    }

    fun getSupportedCharacters(): Set<Char> {
        return characterIndicesMap.keys
    }

    fun getCharacterList(): CharArray {
        return characterList
    }

    private fun getIndexOfChar(c: Char): Int {
        return if (c == TickerUtils.EMPTY_CHAR) {
            0
        } else {
            characterIndicesMap[c]?.let { it + 1 } ?: 0
        }
    }

    data class CharacterIndices(
        val startIndex: Int,
        val endIndex: Int
    )
}