package com.whalert.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whalert.app.core.CaseFile
import com.whalert.app.core.CaseStatus
import com.whalert.app.core.ModerationDecision
import com.whalert.app.data.ReportTextGenerator
import com.whalert.app.data.SecureStore
import com.whalert.app.ui.*

/** ÉCRAN 4 — État du dossier : timeline honnête, jamais de faux bannissement. */
@Composable
fun CaseDetailScreen(c: CaseFile, onUpdate: (CaseFile) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(ReportTextGenerator.generate(c, SecureStore.get(context).userEmail)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text("Dossier ${c.reference}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            TimelineRow("1. Dossier créé", true, null)
            TimelineArrow()
            TimelineRow("2. Confirmation utilisateur", c.status != CaseStatus.DRAFT && c.status != CaseStatus.PREPARED, null)
            TimelineArrow()
            when (c.transmissionChannel) {
                null -> TimelineRow("3. Transmission tentée", false, "Aucune transmission effectuée depuis cette application.")
                else -> TimelineRow("3. Transmission via canal (${c.transmissionChannel})", true, c.transmissionResult)
            }
            TimelineArrow()
            TimelineRow("4. Confirmation technique",
                c.transmissionResult?.contains("OK") == true || c.status == CaseStatus.CONFIRMED,
                "Basée uniquement sur le résultat technique réel ; « ouvert dans WhatsApp » ≠ « reçu par WhatsApp ».")
            TimelineArrow()
            TimelineRow("5. Décision de WhatsApp", false,
                "WhatsApp ne fournit pas de confirmation publique permettant à cette application de vérifier directement si ce compte a été suspendu.")
        }
        Spacer(Modifier.height(Space.m))
        ModerationNotice(c.moderationDecision)
        Spacer(Modifier.height(Space.m))
        Text("Distinction claire :", style = MaterialTheme.typography.titleSmall)
        Bullet("« Signalement envoyé depuis cette application » : action locale réelle (ouverture d'un canal).")
        Bullet("« Signalement transmis au canal officiel » : dépend de l'action que vous finalisez dans WhatsApp.")
        Bullet("« Aucune confirmation disponible » : état par défaut après ouverture d'un canal.")
        Bullet("« Décision de WhatsApp inconnue » : aucune API publique ne permet de la consulter.")
        Spacer(Modifier.height(Space.m))
        Text("Statut du dossier", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            CaseStatus.entries.forEach { s ->
                FilterChip(selected = c.status == s, onClick = { onUpdate(c.copy(status = s)) },
                    label = { Text(s.name.lowercase()) })
            }
        }
        Spacer(Modifier.height(Space.m))
        OutlinedButton(onClick = { /* copy handled by system long-press on text below */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Texte du dossier (appui long pour copier)")
        }
        Surface(shape = RoundedCornerShape(Radius.card), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(text, Modifier.padding(Space.m), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun TimelineRow(title: String, done: Boolean, note: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(Space.s))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun TimelineArrow() {
    Box(Modifier.padding(start = 12.dp)) { Icon(Icons.Filled.ArrowDownward, null, modifier = Modifier.size(20.dp)) }
}

@Composable private fun Bullet(t: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("• ", color = MaterialTheme.colorScheme.primary)
        Text(t, style = MaterialTheme.typography.bodyMedium)
    }
}
