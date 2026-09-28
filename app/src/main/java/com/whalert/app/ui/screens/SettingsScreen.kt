package com.whalert.app.ui.screens

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
import com.whalert.app.core.PhoneNumberValidator
import com.whalert.app.data.SecureStore
import com.whalert.app.ui.*
import java.util.Locale

/** ÉCRAN Paramètres + association de compte (numéro perso + e-mail consenti). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountSaved: () -> Unit,
    isAccountSetup: Boolean = false,
    onDone: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { SecureStore.get(context) }
    var ownNumber by remember { mutableStateOf(store.ownNumberE164 ?: "") }
    var numberOk by remember { mutableStateOf<PhoneNumberValidator.Outcome?>(null) }
    var email by remember { mutableStateOf(store.userEmail ?: "") }
    var consent by remember { mutableStateOf(store.emailConsentGranted) }
    var limit by remember { mutableIntStateOf(store.dailyLimit) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text(if (isAccountSetup) "Connecter mon compte" else "Paramètres",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(Space.m))

        if (isAccountSetup) {
            SectionCard {
                Text("Votre numéro WhatsApp", style = MaterialTheme.typography.titleMedium)
                Text("L'association utilise uniquement votre propre numéro saisi par vous. Cette application ne demande JAMAIS de code SMS/OTP WhatsApp, ni PIN 2FA, et n'utilise aucune API privée WhatsApp.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.s))
                OutlinedTextField(value = ownNumber, onValueChange = {
                    ownNumber = it; numberOk = PhoneNumberValidator.validate(it)
                }, label = { Text("+ indicatif pays et numéro") }, singleLine = true,
                isError = numberOk?.isValid == false,
                supportingText = { numberOk?.let { Text(it.message) } }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.s))
                Text("Adresse e-mail (facultative)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("votre@email.exemple") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text("J'autorise cette application à utiliser mon adresse e-mail pour transmettre un dossier de signalement, uniquement après confirmation explicite à chaque envoi.")
                }
                Spacer(Modifier.height(Space.s))
                PrimaryButton(text = "Enregistrer", enabled = numberOk?.isValid == true, onClick = {
                    store.ownNumberE164 = numberOk!!.e164
                    if (email.isNotBlank()) store.userEmail = email
                    store.emailConsentGranted = consent
                    savedMsg = "Enregistré localement de façon chiffrée."
                    onAccountSaved()
                    onDone?.invoke()
                })
            }
            Spacer(Modifier.height(Space.m))
        }

        SectionCard {
            Text("Limite quotidienne de sécurité", style = MaterialTheme.typography.titleMedium)
            Text("Limite de sécurité de l'application : $limit dossiers par 24 heures.\nCette limite n'est pas présentée comme une règle officielle de WhatsApp.",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = limit.toFloat(), onValueChange = { limit = it.toInt().coerceIn(1, 10) },
                valueRange = 1f..10f, steps = 8)
            Button(onClick = { store.dailyLimit = limit; savedMsg = "Limite enregistrée." }) { Text("Appliquer") }
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("Sécurité", style = MaterialTheme.typography.titleMedium)
            Text("• Stockage local chiffré (Android Keystore / AES-256-GCM)\n• HTTPS obligatoire, trafic clair interdit\n• Aucun secret dans le code source\n• Journaux techniques sans données personnelles",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("Langue", style = MaterialTheme.typography.titleMedium)
            Text("Interface en français (${Locale.getDefault().country}).", style = MaterialTheme.typography.bodyMedium)
        }
        savedMsg?.let { Spacer(Modifier.height(Space.m)); InfoBanner(it, BannerTone.OK) }
    }
}
