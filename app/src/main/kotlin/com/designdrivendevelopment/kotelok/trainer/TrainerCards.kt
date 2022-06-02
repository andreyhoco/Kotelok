package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition
import com.designdrivendevelopment.kotelok.screens.trainers.LearnableDefinitionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TrainerCards(
    learnableDefinitionsRepository: LearnableDefinitionsRepository,
    private val changeStatisticsRepository: ChangeStatisticsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ReactiveTrainer<Boolean, List<LearnableDefinition>>(
    learnableDefinitionsRepository,
    TRAINER_CARDS_WEIGHT,
    dispatcher
) {
    private val repeatListMutex = Mutex()
    private val repeatList = mutableListOf<LearnableDefinition>()
    private val definitionsFlow = MutableSharedFlow<List<LearnableDefinition>>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val outputFlow: SharedFlow<List<LearnableDefinition>>
        get() = definitionsFlow.asSharedFlow()

    override suspend fun onLoadDefinitions() = withContext(dispatcher) {
        repeatListMutex.withLock { repeatList.clear() }
        definitionsFlow.emit(loadedDefinitions)
    }

    override suspend fun onAnswerRight(definition: LearnableDefinition) = withContext(dispatcher) {
        changeStatisticsRepository.addSuccessfulResultToWordDef(definition.definitionId)
    }

    override suspend fun onAnswerWrong(definition: LearnableDefinition) = withContext(dispatcher) {
        changeStatisticsRepository.addFailedResultToWordDef(definition.definitionId)
        repeatListMutex.withLock {
            repeatList.add(definition)
            repeatList.shuffle()
        }
    }

    override fun rateEF(userAnswer: UserAnswer<Boolean>): Int {
        return if (userAnswer.answer) LearnableDefinition.GRADE_FOUR else LearnableDefinition.GRADE_ZERO
    }

    suspend fun updateDefinitionsFlow(
        remainingDefinitions: List<LearnableDefinition> = emptyList()
    ) = withContext(dispatcher) {
        definitionsFlow.emit(remainingDefinitions + repeatList.toList())
        repeatListMutex.withLock { repeatList.clear() }
    }

    companion object {
        const val TRAINER_CARDS_WEIGHT = 0.4f
    }
}
