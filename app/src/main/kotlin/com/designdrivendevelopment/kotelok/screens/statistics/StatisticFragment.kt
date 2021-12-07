package com.designdrivendevelopment.kotelok.screens.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.designdrivendevelopment.kotelok.R
import com.designdrivendevelopment.kotelok.application.KotelokApplication
import com.designdrivendevelopment.kotelok.screens.trainers.TrainFlashcardsViewModel
import com.designdrivendevelopment.kotelok.screens.trainers.TrainFlashcardsViewModelFactory

class StatisticFragment : Fragment(){
    lateinit var viewModel: StatisticViewModel

    private var textView2 : TextView? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView2 = view.findViewById(R.id.textView2)

        val factory = StatisticViewModelFactory(
            (requireActivity().application as KotelokApplication)
                .appComponent.statisticsRepository
        )
        viewModel = ViewModelProvider(this, factory).get(StatisticViewModel::class.java)

        textView2?.text = "Статистика: ${viewModel.totalStat?.totalNumOfWordDefinitions}"
    }
}
