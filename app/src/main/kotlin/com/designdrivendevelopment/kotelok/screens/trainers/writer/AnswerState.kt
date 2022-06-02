package com.designdrivendevelopment.kotelok.screens.trainers.writer

sealed class AnswerState {
    object NotAnswered : AnswerState()
    sealed class Answered(val writing: String) : AnswerState() {
        class Right(word: String) : Answered(word)
        class Wrong(word: String) : Answered(word)
    }
}
