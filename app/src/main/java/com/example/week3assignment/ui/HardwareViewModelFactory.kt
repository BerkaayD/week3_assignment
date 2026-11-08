package com.example.week3assignment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.week3assignment.data.InMemoryHardwareRepository

class HardwareViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == HardwareViewModel::class.java) { "Unsupported ViewModel: ${modelClass.name}" }
        @Suppress("UNCHECKED_CAST")
        return HardwareViewModel(InMemoryHardwareRepository()) as T
    }
}
