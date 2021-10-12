package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.trainer.entities.LearnableWord
import com.designdrivendevelopment.kotelok.trainer.entities.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsTrainerTest {

    private fun getDictionaryData(): List<LearnableWord> {
        return listOf(
            LearnableWord(
                0,
                "dog",
                Translation(
                    id = 20,
                    description = listOf(""),
                    transcription = "",
                    examples = listOf(""),
                    learntIndex = 0.7f
                )
            ),
            LearnableWord(
                1,
                "cat",
                Translation(
                    id = 13,
                    description = listOf(""),
                    transcription = "",
                    examples = listOf(""),
                    learntIndex = 0.8f
                )
            ),
            LearnableWord(
                2,
                "bird",
                Translation(
                    id = 58,
                    description = listOf(""),
                    transcription = "",
                    examples = listOf(""),
                    learntIndex = 0.9f
                )
            ),
        )
    }

    @Test
    fun cardsTest() {
        val dictionaryData = getDictionaryData()
        val trainer = CardsTrainer(dictionaryData)
        assertEquals(dictionaryData.size, trainer.size)
        var previousWord = trainer.getNextWord()
        previousWord = previousWord.copy(translation = previousWord.translation.copy())
        for (i in 0..3) {
            // if the word was incorrect then the method should return the same word again
            trainer.setUserInput(isRight = false)

            var nextWord = trainer.getNextWord()
            nextWord = nextWord.copy(translation = nextWord.translation.copy())

            assertEquals(previousWord, nextWord)
            // learntIndex should decrease
            val prevIdx = previousWord.translation.learntIndex
            val nextIdx = nextWord.translation.learntIndex
            assertTrue("Previous (${prevIdx}) should be greater than current (${nextIdx})",
                prevIdx > nextIdx)
            previousWord = nextWord
        }
        trainer.setUserInput(isRight = true)
        var i = 0  // one word was guessed incorrectly => iterator become one step longer
        while (!trainer.isDone) {
            trainer.getNextWord()
            trainer.setUserInput(isRight = true)
            i += 1
        }
        assertEquals(dictionaryData.size, i)
    }
}
