package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition
import com.designdrivendevelopment.kotelok.screens.trainers.LearnableDefinitionsRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class CoreTrainer<AnswerType>(
    private val learnableDefinitionsRepository: LearnableDefinitionsRepository,
    private val trainerWeight: Float,
    protected val dispatcher: CoroutineDispatcher
) {
    var loadedDefinitions = emptyList<LearnableDefinition>()
        private set

    suspend fun loadDictionary(
        dictionaryId: Long,
        loadOnlyUnlearned: Boolean
    ) = withContext(dispatcher) {
        loadedDefinitions = if (loadOnlyUnlearned) {
            learnableDefinitionsRepository
                .getByDictionaryIdAndRepeatDate(
                    dictionaryId = dictionaryId,
                    repeatDate = with(Calendar.getInstance()) { time }
                )
        } else {
            learnableDefinitionsRepository.getByDictionaryId(dictionaryId = dictionaryId)
        }
        loadedDefinitions = loadedDefinitions.shuffled()
        onLoadDefinitions()
    }

    private suspend fun handleAnswer(
        definition: LearnableDefinition,
        scoreEF: Int
    ): Boolean = withContext(dispatcher) {
        definition.changeEFBasedOnNewGrade(scoreEF, trainerWeight)
        val isRight = scoreEF >= LearnableDefinition.PASSING_GRADE
        learnableDefinitionsRepository.updateLearnableDefinition(definition)
        if (isRight) {
            onAnswerRight(definition)
        } else {
            onAnswerWrong(definition)
        }

        isRight
    }

    suspend fun checkUserAnswer(userAnswer: UserAnswer<AnswerType>): Boolean {
        val rate = rateEF(userAnswer)
        return handleAnswer(userAnswer.definition, rate)
    }

    abstract suspend fun onLoadDefinitions()

    abstract suspend fun onAnswerRight(definition: LearnableDefinition)

    abstract suspend fun onAnswerWrong(definition: LearnableDefinition)

    abstract fun rateEF(userAnswer: UserAnswer<AnswerType>): Int
}
