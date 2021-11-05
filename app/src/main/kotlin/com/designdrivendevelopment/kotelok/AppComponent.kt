package com.designdrivendevelopment.kotelok

import android.content.Context
import com.designdrivendevelopment.kotelok.persistence.database.KotelokDatabase
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
    val learnableDefinitionsRepository by lazy {
        LearnableDefinitionsRepositoryImpl(db.wordDefinitionsDao)
    }
    val statisticsRepository by lazy { StatisticsRepositoryImpl(db.statisticsDao) }
}
