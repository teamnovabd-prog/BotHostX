package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SessionManager
import com.example.data.repository.SmartHostRepository
import com.example.navigation.BotHostNavHost
import com.example.ui.theme.BotHostXTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserPortalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appContext = applicationContext
            val sessionManager = remember { SessionManager(appContext) }
            val repository = remember { SmartHostRepository(appContext) }

            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(sessionManager, repository)
            )
            val portalViewModel: UserPortalViewModel = viewModel(
                factory = UserPortalViewModel.Factory(repository)
            )
            val adminViewModel: AdminViewModel = viewModel(
                factory = AdminViewModel.Factory(repository)
            )

            val session by authViewModel.sessionState.collectAsState()

            BotHostXTheme(themeMode = session.themeMode) {
                BotHostNavHost(
                    authViewModel = authViewModel,
                    portalViewModel = portalViewModel,
                    adminViewModel = adminViewModel,
                )
            }
        }
    }
}
