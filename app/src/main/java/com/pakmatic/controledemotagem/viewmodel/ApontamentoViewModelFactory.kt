package com.pakmatic.controledemotagem.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pakmatic.controledemotagem.data.local.ApontamentoDao

class ApontamentoViewModelFactory(private val dao: ApontamentoDao, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApontamentoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ApontamentoViewModel(dao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}