package com.whalert.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whalert.app.data.SecureStore
import com.whalert.app.ui.screens.*

sealed class Dest {
    object Home : Dest()
    object NewReport : Dest()
    object History : Dest()
    data class CaseDetail(val id: Long) : Dest()
    object Guide : Dest()
    object Settings : Dest()
    object AccountSetup : Dest()
    object Transparency : Dest()
    object AdminConsole : Dest()
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val vm: WhAlertViewModel = viewModel()
    val state by vm.state.collectAsState()
    var dest by remember { mutableStateOf<Dest>(Dest.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(dest, state.online) { vm.refresh(context) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        AnimatedContent(
            targetState = dest,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 })
                    .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { -it / 12 })
            },
            label = "screen",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { d ->
        when (d) {
            Dest.Home -> HomeScreen(
                accountState = state.accountState, online = state.online, transport = state.transport,
                backendConfigured = state.backendConfigured,
                onNewReport = { dest = Dest.NewReport },
                onHistory = { dest = Dest.History },
                onGuide = { dest = Dest.Guide },
                onSettings = { dest = Dest.Settings },
                onTransparency = { dest = Dest.Transparency },
                onConsole = { dest = Dest.AdminConsole },
                onAccountSetup = { dest = Dest.AccountSetup }
            )
            Dest.NewReport -> NewReportScreen(
                dailyLimitReached = !SecureStore.get(context).canSubmitMore(),
                onSaveDraft = { c -> vm.saveCase(context, c) },
                onCaseUpdated = { c -> vm.saveCase(context, c); dest = Dest.CaseDetail(c.id) },
                onFinish = { dest = Dest.Home }
            )
            Dest.History -> HistoryScreen(
                cases = state.cases,
                onOpen = { dest = Dest.CaseDetail(it.id) },
                onDelete = { vm.deleteCase(context, it) },
                onBack = { dest = Dest.Home }
            )
            is Dest.CaseDetail -> {
                val c = state.cases.firstOrNull { it.id == d.id }
                if (c != null) CaseDetailScreen(c, onUpdate = { vm.saveCase(context, it) }, onBack = { dest = Dest.History })
                else HistoryScreen(state.cases, { dest = Dest.CaseDetail(it.id) }, { vm.deleteCase(context, it) }, { dest = Dest.Home })
            }
            Dest.Guide -> GuideScreen(onBack = { dest = Dest.Home })
            Dest.Settings -> SettingsScreen(onBack = { dest = Dest.Home }, onAccountSaved = { vm.refresh(context) })
            Dest.AccountSetup -> SettingsScreen(
                onBack = { dest = Dest.Home }, onAccountSaved = { vm.refresh(context) },
                isAccountSetup = true, onDone = { dest = Dest.Home }
            )
            Dest.Transparency -> TransparencyScreen(onBack = { dest = Dest.Home })
            Dest.AdminConsole -> AdminConsoleScreen(onBack = { dest = Dest.Home })
        }
        } // AnimatedContent
    } // Scaffold

    val snackbarHostStateLocal = snackbarHostState
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostStateLocal.showSnackbar(it); vm.consumeMessage() }
    }
}
