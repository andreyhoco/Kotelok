package com.designdrivendevelopment.kotelok.screens.trainers.writer

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition

data class Word(
    val id: Long,
    val writingTr: String,
    val exampleTr: String
) {
    companion object {
        fun fromLearnableDefinition(learnableDefinition: LearnableDefinition): Word {
            return Word(
                id = learnableDefinition.definitionId,
                writingTr = learnableDefinition.mainTranslation,
                exampleTr = learnableDefinition.examples.firstOrNull()?.translatedText.orEmpty()
            )
        }
    }
}
