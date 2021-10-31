package com.designdrivendevelopment.kotelok.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationResponse(
    @SerialName("pos")
    val partOfSpeech: String? = null,
    @SerialName("mean")
    val synonyms: List<SynonymResponse>? = null,
    @SerialName("text")
    val text: String,
    @SerialName("syn")
    val otherTranslations: List<OtherTranslationResponse>? = null,
    @SerialName("ex")
    val examples: List<ExampleResponse>? = null,
)
