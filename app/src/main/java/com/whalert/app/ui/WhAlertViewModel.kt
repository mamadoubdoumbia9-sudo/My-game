package com.whalert.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.whalert.app.core.*
import com.whalert.app.data.RemoteClient
import com.whalert.app.data.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val accountState: AccountState = AccountState.NOT_CONNECTED,
    val online: Boolean? = null,
    val transport: String = "inconnu",
    val backendConfigured: Boolean = RemoteClient.backendConfigured,
    val cases: List<CaseFile> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null
)

/** Single app-level ViewModel (MVVM). All IO runs off the UI thread. */
class WhAlertViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state

    fun refresh(context: Context) {
        viewModelScope.launch {
            val store = SecureStore.get(context)
            val online = withContext(Dispatchers.IO) {
                com.whalert.app.data.NetworkMonitor.isConnected(context)
            }
            val transport = com.whalert.app.data.NetworkMonitor.transport(context)
            val acct = when {
                !RemoteClient.backendConfigured -> AccountState.NOT_CONNECTED
                store.sessionTokenHash == null -> AccountState.NOT_CONNECTED
                !online -> AccountState.CONNECTION_ERROR
                else -> AccountState.CONNECTED
            }
            _state.value = _state.value.copy(
                accountState = acct, online = online, transport = transport, cases = store.loadCases()
            )
        }
    }

    fun associateAccount(context: Context, ownNumberE164: String, email: String?, consent: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            // Strict input validation at the ViewModel boundary (defense in depth):
            // never persist malformed numbers/emails, never forward them to any backend.
            if (!PhoneNumberValidator.isValid(ownNumberE164)) {
                _state.value = _state.value.copy(
                    busy = false, accountState = AccountState.NOT_CONNECTED,
                    message = "Numéro invalide : format international E.164 requis."
                )
                return@launch
            }
            val cleanEmail = email?.trim()?.lowercase()
            if (cleanEmail != null && !com.whalert.app.data.EmailValidator.isValid(cleanEmail)) {
                _state.value = _state.value.copy(
                    busy = false, accountState = AccountState.NOT_CONNECTED,
                    message = "Adresse e-mail invalide — aucune donnée malformée n'a été enregistrée."
                )
                return@launch
            }
            val store = SecureStore.get(context)
            store.ownNumberE164 = ownNumberE164
            cleanEmail?.let { store.userEmail = it }
            store.emailConsentGranted = consent
            if (!RemoteClient.backendConfigured) {
                _state.value = _state.value.copy(
                    busy = false, accountState = AccountState.NOT_CONNECTED,
                    message = "Compte enregistré localement. Synchronisation distante indisponible : aucun backend réel n'est configuré dans cette build."
                )
                return@launch
            }
            when (val r = RemoteClient.register(ownNumberE164, email)) {
                is NetResult.Success -> {
                    store.sessionTokenHash = SecureStore.sha256(r.data.toByteArray())
                    _state.value = _state.value.copy(busy = false, accountState = AccountState.CONNECTED, message = "Compte connecté au backend.")
                }
                is NetResult.HttpError -> _state.value = _state.value.copy(busy = false, accountState = AccountState.CONNECTION_ERROR, message = "Erreur serveur : ${r.message}")
                is NetResult.NetworkFailure -> _state.value = _state.value.copy(busy = false, accountState = AccountState.CONNECTION_ERROR, message = "Échec réseau : ${r.reason}")
                NetResult.Disabled -> _state.value = _state.value.copy(busy = false, accountState = AccountState.NOT_CONNECTED, message = "Backend non configuré.")
            }
        }
    }

    fun saveCase(context: Context, c: CaseFile) {
        viewModelScope.launch {
            val store = SecureStore.get(context)
            store.saveCase(c)
            _state.value = _state.value.copy(cases = store.loadCases())
            // Honest remote sync attempt (only if a real backend exists)
            if (RemoteClient.backendConfigured && c.status != CaseStatus.DRAFT) {
                when (val r = RemoteClient.syncCase(c.reference, c.category.key, c.status.name)) {
                    is NetResult.Success -> store.appendTechLog("SYNC_OK ${c.reference}")
                    is NetResult.HttpError -> store.appendTechLog("SYNC_HTTP_${r.code} ${c.reference}")
                    is NetResult.NetworkFailure -> store.appendTechLog("SYNC_NET ${c.reference}")
                    NetResult.Disabled -> {}
                }
                _state.value = _state.value.copy(cases = store.loadCases())
            }
        }
    }

    fun deleteCase(context: Context, id: Long) {
        viewModelScope.launch {
            val store = SecureStore.get(context)
            store.deleteCase(id)
            _state.value = _state.value.copy(cases = store.loadCases())
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
}
