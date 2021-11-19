package com.designdrivendevelopment.kotelok.application

import android.content.Context
import com.designdrivendevelopment.kotelok.CardsLearnableDefinitionsRepository
import com.designdrivendevelopment.kotelok.DictWordDefinitionRepositoryImpl
import com.designdrivendevelopment.kotelok.DictionariesFragment
import com.designdrivendevelopment.kotelok.DictionariesRepositoryImpl
import com.designdrivendevelopment.kotelok.EditWordDefRepositoryImpl
import com.designdrivendevelopment.kotelok.PairsLearnableDefinitionsRepository
import com.designdrivendevelopment.kotelok.ProfileFragment
import com.designdrivendevelopment.kotelok.R
import com.designdrivendevelopment.kotelok.RecognizeFragment
import com.designdrivendevelopment.kotelok.StatisticsRepositoryImpl
import com.designdrivendevelopment.kotelok.WriterLearnableDefinitionsRepository
import com.designdrivendevelopment.kotelok.persistence.database.KotelokDatabase
import com.designdrivendevelopment.kotelok.screens.bottomNavigation.BottomNavigator
import com.designdrivendevelopment.kotelok.screens.bottomNavigation.Tab
import com.designdrivendevelopment.kotelok.yandexDictApi.RetrofitModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AppComponent(applicationContext: Context) {
    val bottomNavigator by lazy {
        BottomNavigator(
            listOf(
                Tab("DICTIONARIES") { DictionariesFragment.newInstance() },
                Tab("RECOGNIZE") { RecognizeFragment.newInstance() },
                Tab("PROFILE") { ProfileFragment.newInstance() },
            ),
            R.id.fragment_container
        )
    }

    private val yandexDictionaryApiService = RetrofitModule().yandexDictionaryService
    private val db = KotelokDatabase.create(applicationContext, CoroutineScope(Dispatchers.IO))
    val dictionariesRepository by lazy {
        DictionariesRepositoryImpl(
            db.dictionariesDao,
            db.dictionaryWordDefCrossRefDao
        )
    }
    val dictDefinitionsRepository by lazy {
        DictWordDefinitionRepositoryImpl(db.wordDefinitionsDao)
    }
    val cardsLearnDefRepository by lazy {
        CardsLearnableDefinitionsRepository(db.cardsLearnableDefDao)
    }
    val writerLearnDefRepository by lazy {
        WriterLearnableDefinitionsRepository(db.writerLearnableDefDao)
    }
    val pairsLearnDefRepository by lazy {
        PairsLearnableDefinitionsRepository(db.pairsLearnableDefDao)
    }
    val statisticsRepository by lazy { StatisticsRepositoryImpl(db.statisticsDao) }
    val editWordDefinitionsRepository by lazy {
        EditWordDefRepositoryImpl(
            yandexDictApiService = yandexDictionaryApiService,
            wordDefinitionsDao = db.wordDefinitionsDao,
            dictWordDefCrossRefDao = db.dictionaryWordDefCrossRefDao,
            translationsDao = db.translationsDao,
            synonymsDao = db.synonymsDao,
            examplesDao = db.examplesDao
        )
    }
}
