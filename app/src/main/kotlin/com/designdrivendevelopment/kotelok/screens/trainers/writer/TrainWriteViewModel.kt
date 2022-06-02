package com.designdrivendevelopment.kotelok.screens.trainers.writer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.designdrivendevelopment.kotelok.entities.LearnableDefinition
import com.designdrivendevelopment.kotelok.screens.trainers.ScoreState
import com.designdrivendevelopment.kotelok.trainer.TrainerWriter
import com.designdrivendevelopment.kotelok.trainer.UserAnswer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrainWriteViewModel(
    private val trainerWriter: TrainerWriter,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private var currentDefinition: LearnableDefinition? = null
    private var correctCount: Int = 0
    private var wrongCount: Int = 0
    private val _answerState = MutableLiveData<AnswerState>(AnswerState.NotAnswered)
    private val _currentWord: MutableLiveData<Word> = MutableLiveData()
    private val _scoreState: MutableLiveData<ScoreState> = MutableLiveData(ScoreState.Hide)
    val answerState: LiveData<AnswerState>
        get() = _answerState
    val currentWord: LiveData<Word>
        get() = _currentWord
    val scoreState: LiveData<ScoreState>
        get() = _scoreState

    fun loadDict(dictionaryId: Long, onlyUnlearned: Boolean) {
        viewModelScope.launch(dispatcher) {
            trainerWriter.loadDictionary(dictionaryId, loadOnlyUnlearned = onlyUnlearned)
            _answerState.postValue(AnswerState.NotAnswered)
            tryNext()
        }
    }

    fun repeat(dictionaryId: Long) {
        correctCount = 0
        wrongCount = 0
        _scoreState.value = ScoreState.Hide
        loadDict(dictionaryId, onlyUnlearned = false)
    }

    private fun tryNext() {
        if (trainerWriter.hasNext()) {
            val newDefinition = trainerWriter.next()
            currentDefinition = newDefinition
            _currentWord.postValue(Word.fromLearnableDefinition(newDefinition))
        } else {
            val text = if (wrongCount == 0 && correctCount == 0) {
                ScoreState.TEXT_ALREADY_COMPLETED
            } else {
                ScoreState.getScoreText(correctCount, wrongCount)
            }
            _scoreState.postValue(ScoreState.Show(text))
        }
    }

    fun onNext() {
        _answerState.value = AnswerState.NotAnswered
        tryNext()
    }

    fun onUserAnswer(answer: String) {
        val definition = currentDefinition ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = trainerWriter.checkUserAnswer(UserAnswer(answer, definition))
            if (result) {
                _answerState.postValue(AnswerState.Answered.Right(definition.writing))
                correctCount++
            } else {
                _answerState.postValue(AnswerState.Answered.Wrong(definition.writing))
                wrongCount++
            }
        }
    }
}
