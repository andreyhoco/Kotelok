package com.designdrivendevelopment.kotelok

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class DictionariesFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dictionaries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tempTrainButton = view.findViewById<Button>(R.id.temp_train_button)
        tempTrainButton.setOnClickListener {
            val intent = Intent(this@DictionariesFragment.context, TrainFlashcardsActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        const val OPEN_DICTIONARIES_TAG = "open_dictionaries"

        @JvmStatic
        fun newInstance() = DictionariesFragment()
    }
}
