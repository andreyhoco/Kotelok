package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.screens.trainers.LearnableDefinitionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow

abstract class ReactiveTrainer<I, O>(
    learnableDefinitionsRepository: LearnableDefinitionsRepository,
    trainerWeight: Float,
    dispatcher: CoroutineDispatcher
) : CoreTrainer<I>(learnableDefinitionsRepository, trainerWeight, dispatcher) {
    abstract val outputFlow: SharedFlow<O>
}
