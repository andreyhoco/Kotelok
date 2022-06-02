package com.designdrivendevelopment.kotelok.screens.trainers.writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.designdrivendevelopment.kotelok.trainer.TrainerWriter
import kotlinx.coroutines.CoroutineDispatcher

class TrainWriteViewModelFactory(
    private val trainerWriter: TrainerWriter,
    private val dispatcher: CoroutineDispatcher
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(TrainWriteViewModel::class.java)) {
            return TrainWriteViewModel(trainerWriter, dispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
