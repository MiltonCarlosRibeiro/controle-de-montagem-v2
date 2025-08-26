package com.pakmatic.controledemotagem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pakmatic.controledemotagem.data.local.ApontamentoDao

class ApontamentoViewModelFactory(private val dao: ApontamentoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApontamentoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ApontamentoViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}