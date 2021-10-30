package com.designdrivendevelopment.kotelok.persistence.dto

import androidx.room.ColumnInfo
import androidx.room.Relation
import com.designdrivendevelopment.kotelok.persistence.roomEntities.ExampleEntity
import com.designdrivendevelopment.kotelok.persistence.roomEntities.PartOfSpeechEntity
import com.designdrivendevelopment.kotelok.persistence.roomEntities.SynonymEntity
import com.designdrivendevelopment.kotelok.persistence.roomEntities.TranslationEntity
import java.util.Date

data class WordDef(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "word_id")
    val wordId: Long,
    @ColumnInfo(name = "writing")
    val writing: String,
    @ColumnInfo(name = "language")
    val language: String,
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeechTitle: String,
    @ColumnInfo(name = "transcription")
    val transcription: String,
    @ColumnInfo(name = "main_translation")
    val mainTranslation: String,
    @ColumnInfo(name = "next_repeat_date")
    val nextRepeatDate: Date,
    @Relation(
        parentColumn = "part_of_speech",
        entityColumn = "original_title"
    )
    val partOfSpeechEntity: PartOfSpeechEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_def_id"
    )
    val synonyms: List<SynonymEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_def_id"
    )
    val translations: List<TranslationEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_def_id"
    )
    val exampleEntities: List<ExampleEntity>,
)
