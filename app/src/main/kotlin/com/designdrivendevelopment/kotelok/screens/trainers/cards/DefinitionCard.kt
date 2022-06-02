package com.designdrivendevelopment.kotelok.screens.trainers.cards

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition

data class DefinitionCard(
    val id: Long,
    val writing: String,
    val example: String,
    val translation: String,
    val exampleTranslation: String
) {
    companion object {
        fun fromLearnableDefinition(learnableDefinition: LearnableDefinition): DefinitionCard {
            val topExample = learnableDefinition.examples.firstOrNull()

            return DefinitionCard(
                id = learnableDefinition.definitionId,
                writing = learnableDefinition.writing,
                translation = learnableDefinition.mainTranslation,
                example = topExample?.originalText ?: "",
                exampleTranslation = topExample?.translatedText ?: ""
            )
        }
    }
}
