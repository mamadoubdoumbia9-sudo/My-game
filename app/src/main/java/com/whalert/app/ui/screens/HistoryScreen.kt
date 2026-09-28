package com.whalert.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.whalert.app.core.CaseFile
import com.whalert.app.core.CaseStatus
import com.whalert.app.ui.*

/** ÉCRAN 5 — Historique : numéros masqués par défaut, statut, référence. */
@Composable
fun HistoryScreen(
    cases: List<CaseFile>,
    onOpen: (CaseFile) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    var reveal by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(Space.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text("Mes signalements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Masquer" else "Afficher les numéros") }
        }
        if (cases.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun dossier.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                items(cases) { c ->
                    Card(onClick = { onOpen(c) }, shape = RoundedCornerShape(Radius.card),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                        Row(Modifier.padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.reference, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(2.dp))
                                Text(if (reveal) c.targetNumberE164 else c.maskedNumber(),
                                    style = MaterialTheme.typography.titleSmall)
                                Text(c.category.labelFr, style = MaterialTheme.typography.bodySmall)
                            }
            StatusChip(c.status.labelFr.substringBefore(" ("), caseStatusColor(c.status))
                            IconButton(onClick = { onDelete(c.id) }) { Icon(Icons.Filled.DeleteOutline, "Supprimer") }
                        }
                    }
                }
            }
        }
    }
}

