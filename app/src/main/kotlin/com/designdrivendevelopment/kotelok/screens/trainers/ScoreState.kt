package com.designdrivendevelopment.kotelok.screens.trainers

import kotlin.math.roundToInt

sealed class ScoreState {
    object Hide : ScoreState()
    class Show(val text: String) : ScoreState()

    companion object {
        private const val FLOAT_TO_PERCENT_MULT = 100
        const val TEXT_ALREADY_COMPLETED = "Ого!"

        fun getScoreText(right: Int, wrong: Int): String {
            return if (right == 0 && wrong == 0) TEXT_ALREADY_COMPLETED
            else {
                val percent = ((right.toFloat() / (wrong + right).toFloat()) * FLOAT_TO_PERCENT_MULT).roundToInt()
                "Правильных ответов - $percent%"
            }
        }
    }
}
