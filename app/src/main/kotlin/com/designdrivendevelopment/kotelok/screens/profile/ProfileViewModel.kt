package com.designdrivendevelopment.kotelok.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val statisticsRepository: GetStatisticsRepository
) : ViewModel() {
    private val _answersValues: MutableLiveData<List<Float>> = MutableLiveData()
    private val _bestBySkillStat: MutableLiveData<List<Pair<String, Float>>> = MutableLiveData()
    private val _bestBySizeStat: MutableLiveData<List<Pair<String, Float>>> = MutableLiveData()
    val answersValues: LiveData<List<Float>>
        get() = _answersValues
    val bestBySkillStat: LiveData<List<Pair<String, Float>>>
        get() = _bestBySkillStat
    val bestBySizeStat: LiveData<List<Pair<String, Float>>>
        get() = _bestBySizeStat

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                statisticsRepository.getAnswersStatistic().collect { answerStat ->
                    val values = listOf(
                        (answerStat.totalAnswers - answerStat.successfullyAnswers).toFloat(),
                        answerStat.successfullyAnswers.toFloat(),
                        answerStat.totalAnswers.toFloat()
                    )
                    _answersValues.postValue(values)
                }
            }
            launch {
                statisticsRepository.getStatisticsForAllDict().collect { dictionariesStat ->
                    val bestBySize = dictionariesStat
                        .filter { it.size > SIZE_EMPTY }
                        .sortedBy { it.size }
                        .take(SHOWED_DICT_NUM)
                        .map { dictionaryStat ->
                            dictionaryStat.label.reduceLengthTo(MAX_LABEL_LEN) to dictionaryStat.size.toFloat()
                        }
                    val bestBySkill = dictionariesStat
                        .filter { it.size > SIZE_EMPTY }
                        .sortedBy { it.averageSkillLevel }
                        .take(SHOWED_DICT_NUM)
                        .map { dictionaryStat ->
                            dictionaryStat.label.reduceLengthTo(MAX_LABEL_LEN) to dictionaryStat.averageSkillLevel
                        }

                    _bestBySkillStat.postValue(bestBySkill)
                    _bestBySizeStat.postValue(bestBySize)
                }
            }
        }
    }

    private fun String.reduceLengthTo(length: Int): String {
        return this.take(length) + if (this.length > length) "..." else ""
    }

    companion object {
        private const val SIZE_EMPTY = 0
        private const val MAX_LABEL_LEN = 7
        private const val SHOWED_DICT_NUM = 5
    }
}
