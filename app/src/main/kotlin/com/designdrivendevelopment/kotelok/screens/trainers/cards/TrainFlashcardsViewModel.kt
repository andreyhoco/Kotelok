package com.designdrivendevelopment.kotelok.screens.trainers.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.designdrivendevelopment.kotelok.entities.LearnableDefinition
import com.designdrivendevelopment.kotelok.screens.trainers.ScoreState
import com.designdrivendevelopment.kotelok.trainer.TrainerCards
import com.designdrivendevelopment.kotelok.trainer.UserAnswer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TrainFlashcardsViewModel(
    private val maxCardsOnScreen: Int,
    private val trainerCards: TrainerCards,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private var currentDefinitionsList: List<LearnableDefinition> = emptyList()
    private val _scoreState: MutableLiveData<ScoreState> = MutableLiveData(ScoreState.Hide)
    private val _isSwipeLocked = MutableLiveData(true)
    private val _definitionsCards: MutableLiveData<List<DefinitionCard>> =
        MutableLiveData(emptyList())
    val definitionsCards: LiveData<List<DefinitionCard>>
        get() = _definitionsCards
    val scoreState: LiveData<ScoreState>
        get() = _scoreState

    init {
        viewModelScope.launch(dispatcher) {
            trainerCards.outputFlow.collect { definitions ->
                val cards = definitions.map { DefinitionCard.fromLearnableDefinition(it) }
                _definitionsCards.postValue(cards)
                currentDefinitionsList = definitions
                if (definitions.isEmpty()) {
                    _scoreState.postValue(ScoreState.Show(ScoreState.TEXT_ALREADY_COMPLETED))
                }
            }
        }
    }

    fun loadDict(dictionaryId: Long, onlyUnlearned: Boolean) {
        _scoreState.value = ScoreState.Hide
        _isSwipeLocked.value = false
        viewModelScope.launch(dispatcher) {
            trainerCards.loadDictionary(dictionaryId, loadOnlyUnlearned = onlyUnlearned)
        }
    }

    fun onUserAnswer(position: Int, answer: Boolean) {
        _isSwipeLocked.value = true
        viewModelScope.launch {
            val learnableDefinition = currentDefinitionsList[position]
            trainerCards.checkUserAnswer(UserAnswer(answer, learnableDefinition))
            val itemsRest = currentDefinitionsList.size - (position + 1)
            when {
                (itemsRest in 1..maxCardsOnScreen) -> {
                    trainerCards.updateDefinitionsFlow(currentDefinitionsList.takeLast(itemsRest))
                }

                (itemsRest == 0) -> {
                    trainerCards.updateDefinitionsFlow()
                    if (!answer) {
                        trainerCards.checkUserAnswer(UserAnswer(answer = true, learnableDefinition))
                        trainerCards.updateDefinitionsFlow()
                    }
                }
            }
        }
    }
}
