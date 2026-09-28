package com.whalert.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whalert.app.core.AccountState
import com.whalert.app.ui.*

@Composable
fun HomeScreen(
    accountState: AccountState,
    online: Boolean?,
    transport: String,
    backendConfigured: Boolean,
    onNewReport: () -> Unit,
    onHistory: () -> Unit,
    onGuide: () -> Unit,
    onSettings: () -> Unit,
    onTransparency: () -> Unit,
    onConsole: () -> Unit,
    onAccountSetup: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 4 }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo (vector shield)
                Surface(modifier = Modifier.size(96.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.Shield, contentDescription = "WhAlert", modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(Space.m))
                Text("WhAlert", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Documentez et signalez légitimement", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Space.s))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StatusChip(accountState.labelFr, accountColor(accountState))
                    StatusChip(if (online == true) "En ligne ($transport)" else "Hors ligne",
                        if (online == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(Space.l))
        if (!backendConfigured) {
            InfoBanner("Aucun backend distant n'est configuré dans cette build : la synchronisation cloud est désactivée. Toutes les fonctions locales et les canaux officiels restent disponibles.")
            Spacer(Modifier.height(Space.m))
        }
        MenuTile(Icons.Filled.AddAlert, "Signaler un compte", "Créer un dossier de signalement", onNewReport)
        MenuTile(Icons.Filled.History, "Mes signalements", "Historique local chiffré", onHistory)
        MenuTile(Icons.Filled.School, "Guide de sécurité", "Reconnaître et se protéger", onGuide)
        MenuTile(Icons.Filled.Visibility, "Ce que l'application peut / ne peut pas faire", "Transparence", onTransparency)
        MenuTile(Icons.Filled.PersonAddAlt, "Connecter mon compte", "Numéro + e-mail (avec consentement)", onAccountSetup)
        MenuTile(Icons.Filled.Settings, "Paramètres", "Limite quotidienne, sécurité", onSettings)
        MenuTile(Icons.Filled.AdminPanelSettings, "Console privée", "Réservé au propriétaire autorisé", onConsole)
        Spacer(Modifier.height(Space.m))
        Text(
            "Cette application n'utilise aucune API privée WhatsApp. Le signalement effectif s'accomplit via les mécanismes officiels de WhatsApp.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.padding(bottom = Space.s)) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        )
    }
}
