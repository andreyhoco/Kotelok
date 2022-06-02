package com.designdrivendevelopment.kotelok.screens.trainers.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.designdrivendevelopment.kotelok.trainer.TrainerCards

class TrainFlashcardsViewModelFactory(
    private val trainerCards: TrainerCards,
    private val cardsOnScreen: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(TrainFlashcardsViewModel::class.java)) {
            return TrainFlashcardsViewModel(cardsOnScreen, trainerCards) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
