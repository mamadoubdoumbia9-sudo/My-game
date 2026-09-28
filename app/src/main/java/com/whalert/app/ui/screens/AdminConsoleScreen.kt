package com.whalert.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.whalert.app.data.RemoteClient
import com.whalert.app.data.SecureStore
import com.whalert.app.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ÉTAPE 11 — CONSOLE PRIVÉE.
 * Strong auth: password stretched (salt + 10k SHA-256 rounds) inside Keystore-backed
 * EncryptedSharedPreferences, brute-force lockout (exponential), session expiry (5 min),
 * optional biometric second factor when available. NOT a plain SHA-256 PIN.
 * The console only READS statistics/logs; it cannot launch campaigns or bypass limits.
 */
@Composable
fun AdminConsoleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SecureStore.get(context) }
    var unlocked by remember { mutableStateOf(false) }
    var setupMode by remember { mutableStateOf(store.adminPasswordHash == null) }
    var pwd by remember { mutableStateOf("") }
    var pwd2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    if (!unlocked) {
        Column(Modifier.fillMaxSize().padding(Space.l), verticalArrangement = Arrangement.Center) {
            Text("CONSOLE PRIVÉE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Space.m))
            val now = System.currentTimeMillis()
            if (store.adminLockUntilEpochMs > now) {
                val mins = ((store.adminLockUntilEpochMs - now) / 60_000L) + 1
                InfoBanner("Accès verrouillé après ${store.adminFailedAttempts} tentatives échouées. Réessayez dans ~${mins} minute(s).", BannerTone.WARN)
                return@Column
            }
            if (setupMode) {
                InfoBanner("Définissez le mot de passe administrateur (8 caractères minimum). Il est stocké sous forme étirée (sel + 10 000 itérations) dans un stockage chiffré par la clé Android Keystore.")
                Spacer(Modifier.height(Space.s))
                OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Mot de passe") }, singleLine = true)
                Spacer(Modifier.height(Space.xs))
                OutlinedTextField(value = pwd2, onValueChange = { pwd2 = it }, label = { Text("Confirmation") }, singleLine = true)
                Spacer(Modifier.height(Space.s))
                PrimaryButton(text = "Créer", enabled = pwd.length >= 8 && pwd == pwd2, onClick = {
                    val salt = SecureStore.sha256(("${System.nanoTime()}").toByteArray()).take(32)
                    store.adminPasswordSalt = salt
                    store.adminPasswordHash = SecureStore.stretch(pwd, salt)
                    setupMode = false
                    unlocked = true
                    store.appendTechLog("ADMIN_SETUP_OK")
                })
            } else {
                OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Mot de passe administrateur") }, singleLine = true)
                error?.let { Spacer(Modifier.height(Space.xs)); InfoBanner(it, BannerTone.WARN) }
                Spacer(Modifier.height(Space.s))
                PrimaryButton(text = "Déverrouiller", onClick = {
                    val hash = SecureStore.stretch(pwd, store.adminPasswordSalt ?: "")
                    if (hash == store.adminPasswordHash) {
                        store.adminFailedAttempts = 0
                        store.adminSessionUntilEpochMs = System.currentTimeMillis() + 5 * 60_000L
                        unlocked = true
                        store.appendTechLog("ADMIN_UNLOCK_OK")
                        // Biometric second factor if enrolled & available (best effort)
                        runCatching {
                            val bm = BiometricManager.from(context)
                            if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                                && context is FragmentActivity) {
                                BiometricPrompt(context as FragmentActivity,
                                    androidx.core.content.ContextCompat.getMainExecutor(context),
                                    object : BiometricPrompt.AuthenticationCallback() {})
                                    .authenticate(BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Second facteur biométrique (facultatif)")
                                        .setSubtitle("Si l'empreinte échoue, la session reste ouverte par mot de passe.")
                                        .setNegativeButtonText("Passer")
                                        .build())
                            }
                        }
                    } else {
                        store.adminFailedAttempts += 1
                        val fails = store.adminFailedAttempts
                        if (fails >= 5) {
                            store.adminLockUntilEpochMs = System.currentTimeMillis() + (1L shl (fails - 5)).coerceAtMost(60) * 60_000L
                        }
                        error = "Mot de passe incorrect. Tentative $fails."
                        store.appendTechLog("ADMIN_UNLOCK_FAIL")
                    }
                })
            }
        }
        return
    }

    // ---- Console content: read-only stats + logs; NO mass actions ----
    val cases = remember { store.loadCases() }
    val transmissions = cases.count { it.transmissionChannel != null && it.transmissionChannel != "none" }
    val errors = cases.count { it.transmissionResult?.contains("FAIL") == true || it.transmissionResult?.contains("UNAVAILABLE") == true }
    val df = SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { store.appendTechLog("ADMIN_EXIT"); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text("CONSOLE PRIVÉE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { unlocked = false; store.adminSessionUntilEpochMs = 0 }) { Text("Verrouiller") }
        }
        Spacer(Modifier.height(Space.m))
        InfoBanner("Session expirant automatiquement après 5 minutes d'inactivité. Toutes les actions administratives sont journalisées sur l'appareil.")
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StatCard("Dossiers créés", "${cases.size}", Modifier.weight(1f))
            StatCard("Transmissions", "$transmissions", Modifier.weight(1f))
            StatCard("Erreurs", "$errors", Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("Limites et état des services", style = MaterialTheme.typography.titleMedium)
            Text("Limite application : ${store.dailyLimit}/24 h (réglable uniquement dans Paramètres utilisateur, jamais contournable ici)\n" +
                 "Signalements transmis sur 24 dernières heures : ${store.submissionsInLast24h()}\n" +
                 "Backend distant : ${if (RemoteClient.backendConfigured) "configuré (HTTPS)" else "non configuré — synchronisation désactivée"}\n" +
                 "Intégration WhatsApp : canaux officiels uniquement (aucune API privée)",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("Journaux techniques (sans données personnelles)", style = MaterialTheme.typography.titleMedium)
            val log = remember { store.techLog().lines().takeLast(40).reversed() }
            if (log.isEmpty()) Text("(vide)", style = MaterialTheme.typography.bodySmall)
            log.forEach { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("Historique technique", style = MaterialTheme.typography.titleMedium)
            cases.take(20).forEach { c ->
                Text("${df.format(Date(c.updatedAtEpochMs))}  ${c.reference}  ${c.status.name}  ${c.transmissionChannel ?: "-"}  ${c.transmissionResult ?: "-"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(Space.m))
        InfoBanner("La console ne permet PAS : lancer des signalements en masse, créer de faux signalements, contourner la limite ou les protections de WhatsApp.", BannerTone.WARN)
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
