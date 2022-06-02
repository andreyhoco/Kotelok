package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.screens.trainers.LearnableDefinitionsRepository
import kotlinx.coroutines.CoroutineDispatcher

abstract class IteratorTrainer<I, O>(
    learnableDefinitionsRepository: LearnableDefinitionsRepository,
    trainerWeight: Float,
    dispatcher: CoroutineDispatcher
) : CoreTrainer<I>(learnableDefinitionsRepository, trainerWeight, dispatcher), Iterator<O>
