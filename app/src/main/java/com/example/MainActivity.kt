package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.AmortizationRepository
import com.example.ui.AmortizationViewModel
import com.example.ui.AmortizationViewModelFactory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.AmortizaFlowTheme
import kotlinx.coroutines.launch

enum class AppDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    INSTALLMENTS("Parcelas", Icons.Default.FormatListNumbered),
    SIMULATOR("Simulador", Icons.Default.Calculate),
    REPORTS("Relatórios", Icons.Default.Assessment)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = AmortizationRepository(database.installmentDao())

        val viewModel: AmortizationViewModel by viewModels {
            AmortizationViewModelFactory(repository)
        }

        setContent {
            AmortizaFlowTheme {
                AmortizaFlowApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AmortizaFlowApp(viewModel: AmortizationViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToInstallments = { currentDestination = AppDestination.INSTALLMENTS },
                    onNavigateToSimulator = { currentDestination = AppDestination.SIMULATOR }
                )
                AppDestination.INSTALLMENTS -> InstallmentsScreen(viewModel = viewModel)
                AppDestination.SIMULATOR -> SimulatorScreen(viewModel = viewModel)
                AppDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
            }
        }
    }
}
