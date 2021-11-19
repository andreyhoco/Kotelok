package com.designdrivendevelopment.kotelok.screens.dictionaries.dictionaryDetailsScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DictDetailsViewModelFactory(
    private val dictionaryId: Long,
    private val dictWordDefinitionsRepository: DictionaryWordDefinitionsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DictDetailsViewModel::class.java)) {
            return DictDetailsViewModel(dictionaryId, dictWordDefinitionsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
