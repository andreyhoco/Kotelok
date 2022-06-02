package com.designdrivendevelopment.kotelok.trainer

import com.designdrivendevelopment.kotelok.entities.LearnableDefinition

data class UserAnswer<T>(
    val answer: T,
    val definition: LearnableDefinition
)
