package com.designdrivendevelopment.kotelok.screens.trainers.cards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.designdrivendevelopment.kotelok.R

class CardsAdapter(
    private val context: Context,
    private val flashCardAnimations: FlashCardAnimations
) : ListAdapter<DefinitionCard, CardsAdapter.ViewHolder>(DefinitionsCardsDiffUtil()) {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val buttonOrig: LinearLayout = view.findViewById(R.id.flashcardButton_orig)
        private val buttonTranslation: LinearLayout = view.findViewById(R.id.flashcardButton_tr)
        private val container: FrameLayout = view.findViewById(R.id.card_container)
        private val writing: TextView = view.findViewById(R.id.word_writing_orig)
        private val translation: TextView = view.findViewById(R.id.word_writing_tr)
        private val example: TextView = view.findViewById(R.id.word_example_orig)
        private val exampleTranslation: TextView = view.findViewById(R.id.word_example_tr)

        init {
            buttonOrig.setOnClickListener {
                flashCardAnimations.endAnimations()
                flashCardAnimations.flipCard(buttonOrig, buttonTranslation)
                flashCardAnimations.flipCardContainer(context, container)
            }
            buttonTranslation.setOnClickListener {
                flashCardAnimations.endAnimations()
                flashCardAnimations.flipCard(buttonTranslation, buttonOrig)
                flashCardAnimations.flipCardContainer(context, container)
            }
        }

        fun bind(card: DefinitionCard) {
            buttonTranslation.visibility = View.INVISIBLE

            writing.text = card.writing
            translation.text = card.translation
            example.text = card.example
            exampleTranslation.text = card.exampleTranslation
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
