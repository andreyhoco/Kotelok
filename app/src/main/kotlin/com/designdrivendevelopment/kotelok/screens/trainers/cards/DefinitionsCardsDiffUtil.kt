package com.designdrivendevelopment.kotelok.screens.trainers.cards

import androidx.recyclerview.widget.DiffUtil

class DefinitionsCardsDiffUtil : DiffUtil.ItemCallback<DefinitionCard>() {
    override fun areItemsTheSame(oldItem: DefinitionCard, newItem: DefinitionCard): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DefinitionCard, newItem: DefinitionCard): Boolean {
        return oldItem == newItem
    }
}
