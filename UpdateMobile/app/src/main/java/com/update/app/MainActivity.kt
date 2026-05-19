package com.update.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.update.app.data.network.RetrofitClient
import com.update.app.ui.navigation.AppNavGraph
import com.update.app.ui.theme.UpDateTheme
import com.update.app.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        enableEdgeToEdge()
        setContent {
            UpDateTheme {
                val context = LocalContext.current
                val authViewModel: AuthViewModel = viewModel()
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    authViewModel.checkLogin(context)
                }

                AppNavGraph(navController = navController, authViewModel = authViewModel)
            }
        }
    }
}
