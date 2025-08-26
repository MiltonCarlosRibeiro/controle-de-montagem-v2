package com.pakmatic.controledemotagem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pakmatic.controledemotagem.data.local.AppDatabase
import com.pakmatic.controledemotagem.ui.screen.ApontamentoScreen
import com.pakmatic.controledemotagem.ui.theme.ControleDeMontagemTheme
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModel
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModelFactory

class MainActivity : ComponentActivity() {

    // Inicializa o banco de dados
    private val database by lazy { AppDatabase.getDatabase(this) }

    // Cria o ViewModel usando a factory
    private val viewModel: ApontamentoViewModel by viewModels {
        ApontamentoViewModelFactory(database.apontamentoDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControleDeMontagemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Exibe a tela principal, passando o viewModel
                    ApontamentoScreen(viewModel = viewModel)
                }
            }
        }
    }
}