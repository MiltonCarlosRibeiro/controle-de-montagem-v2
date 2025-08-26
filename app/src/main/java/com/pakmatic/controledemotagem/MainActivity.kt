package com.pakmatic.controledemotagem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pakmatic.controledemotagem.data.local.AppDatabase
import com.pakmatic.controledemotagem.ui.screen.ApontamentoScreen
import com.pakmatic.controledemotagem.ui.screen.FullScreenImageScreen
import com.pakmatic.controledemotagem.ui.theme.ControleDeMontagemTheme
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModel
import com.pakmatic.controledemotagem.viewmodel.ApontamentoViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: ApontamentoViewModel by viewModels {
        ApontamentoViewModelFactory(database.apontamentoDao(), application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControleDeMontagemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ControleDeMontagemApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ControleDeMontagemApp(viewModel: ApontamentoViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "apontamentos") {
        composable("apontamentos") {
            ApontamentoScreen(viewModel = viewModel, navController = navController)
        }
        composable(
            "fullScreenImage/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            FullScreenImageScreen(navController = navController, imageUri = imageUri)
        }
    }
}