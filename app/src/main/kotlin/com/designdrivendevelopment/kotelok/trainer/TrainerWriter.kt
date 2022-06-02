package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition
import com.designdrivendevelopment.kotelok.screens.trainers.LearnableDefinitionsRepository
import com.designdrivendevelopment.kotelok.trainer.utils.WordChangeArray
import com.designdrivendevelopment.kotelok.trainer.utils.levenshteinDifference
import java.util.LinkedList
import kotlin.NoSuchElementException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainerWriter(
    learnableDefinitionsRepository: LearnableDefinitionsRepository,
    private val changeStatisticsRepository: ChangeStatisticsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IteratorTrainer<String, LearnableDefinition>(
    learnableDefinitionsRepository,
    TRAINER_WRITER_WEIGHT,
    dispatcher
) {
    private val repeatList: LinkedList<LearnableDefinition> = LinkedList<LearnableDefinition>()

    /*
    expectedStr: cats
    userStr: cut
    curWordChange: [(c, KEEP), (a, REPLACE), (t, KEEP), (s, INSERT)]
     */
    var curWordChange: WordChangeArray = emptyArray()
        private set

    override fun hasNext(): Boolean = repeatList.isNotEmpty()

    override fun next(): LearnableDefinition {
        if (hasNext()) {
            return repeatList.removeFirst()
        } else throw NoSuchElementException("RepeatList is empty")
    }

    override suspend fun onLoadDefinitions(): Unit = withContext(dispatcher) {
        repeatList.clear()
        repeatList.addAll(loadedDefinitions)
    }

    override suspend fun onAnswerRight(definition: LearnableDefinition) = withContext(dispatcher) {
        changeStatisticsRepository.addSuccessfulResultToWordDef(definition.definitionId)
    }

    override suspend fun onAnswerWrong(definition: LearnableDefinition) = withContext(dispatcher) {
        changeStatisticsRepository.addFailedResultToWordDef(definition.definitionId)
        repeatList.addFirst(definition)
        repeatList.shuffle()
    }

    override fun rateEF(userAnswer: UserAnswer<String>): Int {
        val (levenshteinDistance, path) = levenshteinDifference(
            expectedStr = userAnswer.definition.writing,
            userStr = userAnswer.answer
        )
        curWordChange = path
        // levenshteinDistance ∈ [0, max(expectedStr.length, userStr.length)]
        val errorRate = levenshteinDistance.toDouble() / userAnswer.definition.writing.length
        val correctRate = (1 - minOf(1.0, errorRate)) // ∈ [0, 1]
        val normalizedCorrectRate = correctRate * (LearnableDefinition.GRADE_ARRAY.size - 1)

        return LearnableDefinition.GRADE_ARRAY[normalizedCorrectRate.roundToInt()]
    }

    companion object {
        const val TRAINER_WRITER_WEIGHT = 1f
    }
}
