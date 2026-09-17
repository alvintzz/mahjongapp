package com.alvintz.mahjongapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alvintz.mahjongapp.data.GameRepository

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == GameViewModel::class.java) { "Unknown ViewModel class $modelClass" }
        return GameViewModel(repository) as T
    }
}
