package com.whalert.app.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.whalert.app.core.*
import com.whalert.app.data.EvidenceIO
import com.whalert.app.data.ReportTextGenerator
import com.whalert.app.data.SecureStore
import com.whalert.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ÉCRAN 2 — Nouveau signalement (multi-step wizard).
 * Steps: numéro → catégorie → description/preuves → vérification → aperçu texte → double confirmation → canaux.
 */
@Composable
fun NewReportScreen(
    dailyLimitReached: Boolean,
    onSaveDraft: (CaseFile) -> Unit,
    onCaseUpdated: (CaseFile) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val vm: com.whalert.app.ui.WhAlertViewModel = viewModel()
    var step by remember { mutableIntStateOf(0) }

    // form state
    var numberRaw by remember { mutableStateOf("") }
    var numberOutcome by remember { mutableStateOf<PhoneNumberValidator.Outcome?>(null) }
    var category by remember { mutableStateOf<ReportCategory?>(null) }
    var description by remember { mutableStateOf("") }
    var incidentDate by remember { mutableStateOf("") }
    var incidentTime by remember { mutableStateOf("") }
    var contextInfo by remember { mutableStateOf("") }
    var evidences by remember { mutableStateOf(listOf<Evidence>()) }
    var multiplicityInput by remember { mutableStateOf("1") }
    var abuseWarning by remember { mutableStateOf<String?>(null) }
    var caseRef by remember { mutableStateOf<CaseFile?>(null) }
    var reportText by remember { mutableStateOf("") }
    var editedText by remember { mutableStateOf<String?>(null) }
    var showFinalConfirm by remember { mutableStateOf(false) }
    var pendingTransmission by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.viewModelScope.launch(Dispatchers.IO) {
                val bytes = EvidenceIO.readBytes(context, uri)
                if (bytes != null && bytes.size < 8_000_000) {
                    val path = SecureStore.get(context).copyEvidenceIntoPrivateStorage(context, uri.toString(), bytes)
                    evidences = evidences + Evidence(type = "image", uri = path, text = null, createdAtEpochMs = System.currentTimeMillis())
                } else if (bytes == null) {
                    abuseWarning = "Lecture du fichier impossible."
                } else {
                    abuseWarning = "Image trop volumineuse (max 8 Mo)."
                }
            }
        }
    }

    if (dailyLimitReached && step == 0) {
        Column(Modifier.fillMaxSize().padding(Space.l), verticalArrangement = Arrangement.Center) {
            InfoBanner("Limite de sécurité de l'application atteinte : ${SecureStore.get(context).dailyLimit} dossiers transmis par 24 heures. Cette limite est une règle de sécurité de L'APPLICATION, pas une règle officielle de WhatsApp.", BannerTone.WARN)
            Spacer(Modifier.height(Space.m))
            OutlinedButton(onClick = onFinish, modifier = Modifier.height(52.dp)) { Text("Retour") }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
        // Stepper
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf("Numéro", "Motif", "Faits", "Vérification", "Aperçu").forEachIndexed { i, t ->
                StepDot(i <= step, t)
            }
        }
        Spacer(Modifier.height(Space.l))

        when (step) {
            0 -> {
                Text("Numéro du compte à signaler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Space.s))
                InfoBanner("Utilisez uniquement un numéro correspondant à un compte que vous souhaitez réellement signaler.")
                Spacer(Modifier.height(Space.m))
                OutlinedTextField(
                    value = numberRaw, onValueChange = {
                        numberRaw = it
                        numberOutcome = PhoneNumberValidator.validate(it)
                    },
                    label = { Text("+ indicatif pays et numéro (ex. +223XXXXXXXX)") },
                    singleLine = true, isError = numberOutcome?.isValid == false,
                    supportingText = { numberOutcome?.let { Text(it.message) } },
                    modifier = Modifier.fillMaxWidth()
                )
                if (numberOutcome?.isValid == true) {
                    Spacer(Modifier.height(Space.s))
                    // HONEST: format validity ≠ account existence. No public API to confirm.
                    InfoBanner("Impossible de confirmer publiquement l'état de ce compte avec les informations disponibles. Un numéro valide au format E.164 ne prouve pas l'existence d'un compte WhatsApp.")
                }
                Spacer(Modifier.height(Space.m))
                PrimaryButton(
                    text = "Continuer", enabled = numberOutcome?.isValid == true,
                    onClick = { step = 1 }
                )
            }
            1 -> {
                Text("Catégorie du problème", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Space.m))
                ReportCategory.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        row.forEach { cat ->
                            val sel = category == cat
                            Card(
                                onClick = { category = cat },
                                modifier = Modifier.weight(1f).height(84.dp),
                                shape = RoundedCornerShape(Radius.card),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (sel) CardDefaults.outlinedCardBorder() else null
                            ) {
                                Column(Modifier.padding(Space.s), verticalArrangement = Arrangement.Center) {
                                    Icon(categoryIcon(cat), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(cat.labelFr, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Space.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    OutlinedButton(onClick = { step = 0 }, modifier = Modifier.height(52.dp)) { Text("Retour") }
                    PrimaryButton(text = "Continuer", enabled = category != null, onClick = { step = 2 },
                        modifier = Modifier.weight(1f))
                }
            }
            2 -> {
                Text("Décrivez précisément ce qui s'est passé", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Space.m))
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description des faits (obligatoire)") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    OutlinedTextField(value = incidentDate, onValueChange = { incidentDate = it },
                        label = { Text("Date (JJ/MM/AAAA)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = incidentTime, onValueChange = { incidentTime = it },
                        label = { Text("Heure (HH:mm)") }, singleLine = true, modifier = Modifier.weight(0.6f))
                }
                OutlinedTextField(value = contextInfo, onValueChange = { contextInfo = it },
                    label = { Text("Informations complémentaires / contexte") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.s))
                Text("Preuves (uniquement si légalement approprié)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Filled.Image, null); Spacer(Modifier.width(Space.xs)); Text("Ajouter capture/image")
                    }
                    var addingText by remember { mutableStateOf(false) }
                    var textEv by remember { mutableStateOf("") }
                    OutlinedButton(onClick = { addingText = true }) { Text("Ajouter texte") }
                    if (addingText) {
                        OutlinedTextField(value = textEv, onValueChange = { textEv = it },
                            label = { Text("Texte de preuve") }, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (textEv.isNotBlank()) {
                                evidences = evidences + Evidence(type = "text", uri = null, text = textEv, createdAtEpochMs = System.currentTimeMillis())
                                textEv = ""; addingText = false
                            }
                        }) { Text("OK") }
                    }
                }
                if (evidences.isNotEmpty()) {
                    LazyColumn { items(evidences) { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                            if (e.type == "image" && e.uri != null) {
                                AsyncImage(model = java.io.File(e.uri), contentDescription = "preuve",
                                    modifier = Modifier.size(48.dp))
                            } else Icon(Icons.Filled.Description, null)
                            Spacer(Modifier.width(Space.s))
                            Text(if (e.type == "image") "Image jointe" else e.text?.take(60) ?: "Fichier",
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { evidences = evidences - e }) { Icon(Icons.Filled.Delete, null) }
                        }
                    } }
                }
                Spacer(Modifier.height(Space.s))
                OutlinedTextField(value = multiplicityInput, onValueChange = { v ->
                    multiplicityInput = v
                    val n = v.toIntOrNull() ?: 1
                    abuseWarning = ReportTextGenerator.refusalForMultiplicity(n)
                }, label = { Text("Nombre de signalements") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                abuseWarning?.let { InfoBanner(it, BannerTone.WARN); }
                Spacer(Modifier.height(Space.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    OutlinedButton(onClick = { step = 1 }, modifier = Modifier.height(52.dp)) { Text("Retour") }
                    PrimaryButton(
                        text = "Vérifier",
                        enabled = description.isNotBlank() && abuseWarning == null && (multiplicityInput.toIntOrNull() ?: 1) == 1,
                        onClick = {
                            val store = SecureStore.get(context)
                            val c = CaseFile(
                                id = System.currentTimeMillis(), reference = store.nextReference(),
                                targetNumberE164 = numberOutcome!!.e164!!, category = category!!,
                                description = description, incidentDate = incidentDate.ifBlank { null },
                                incidentTime = incidentTime.ifBlank { null }, contextInfo = contextInfo,
                                evidences = evidences, status = CaseStatus.PREPARED,
                                createdAtEpochMs = System.currentTimeMillis(), updatedAtEpochMs = System.currentTimeMillis()
                            )
                            caseRef = c
                            reportText = ReportTextGenerator.generate(c, store.userEmail?.takeIf { com.whalert.app.data.EmailValidator.isValid(it) })
                            editedText = null
                            onSaveDraft(c)
                            step = 3
                        }, modifier = Modifier.weight(1f)
                    )
                }
            }
            3 -> VerificationStep(caseRef!!, onBack = { step = 2 }, onContinue = { step = 4 })
            4 -> PreviewStep(
                text = editedText ?: reportText,
                onEdit = { editedText = it },
                onCopy = {
                    val ctxLocal = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    ctxLocal.setPrimaryClip(android.content.ClipData.newPlainText("WhAlert", editedText ?: reportText))
                },
                onCancel = onFinish,
                onSend = { showFinalConfirm = true }
            )
        }

        if (pendingTransmission && caseRef != null) {
            TransmissionSheet(context, caseRef!!, editedText ?: reportText, evidenceUris = evidences.mapNotNull { it.uri }) { channel, result ->
                caseRef = caseRef!!.copy(transmissionChannel = channel, transmissionResult = result)
                onCaseUpdated(caseRef!!)
                pendingTransmission = false
            }
        }

        if (showFinalConfirm) {
            FinalConfirmationDialog(
                c = caseRef!!,
                text = editedText ?: reportText,
                onCancel = { showFinalConfirm = false },
                onYes = {
                    showFinalConfirm = false
                    val submitted = caseRef!!.copy(status = CaseStatus.SUBMITTED)
                    caseRef = submitted
                    onSaveDraft(submitted)
                    pendingTransmission = true
                }
            )
        }
    }
}

@Composable private fun RowScope.StepDot(active: Boolean, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 2.dp)
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun categoryIcon(c: ReportCategory) = when (c) {
    ReportCategory.SPAM -> Icons.Filled.SmsFailed
    ReportCategory.SCAM -> Icons.Filled.AccountBalanceWallet
    ReportCategory.IMPERSONATION -> Icons.Filled.PersonOff
    ReportCategory.HARASSMENT -> Icons.Filled.Mic
    ReportCategory.FRAUDULENT_CONTENT -> Icons.Filled.Report
    ReportCategory.MALICIOUS_BEHAVIOR -> Icons.Filled.BugReport
    ReportCategory.OTHER -> Icons.Filled.MoreHoriz
}

@Composable
private fun VerificationStep(c: CaseFile, onBack: () -> Unit, onContinue: () -> Unit) {
    Text("Vérification du dossier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(Space.m))
    SectionCard {
        SummaryRow("Numéro", c.targetNumberE164)
        SummaryRow("Motif", c.category.labelFr)
        SummaryRow("Description", c.description)
        SummaryRow("Date", listOfNotNull(c.incidentDate, c.incidentTime).joinToString(" ").ifBlank { "—" })
        SummaryRow("Pièces jointes", "${c.evidences.size}")
        SummaryRow("Dossier", c.reference)
    }
    Spacer(Modifier.height(Space.m))
    InfoBanner("Cette application ne peut pas garantir une suspension du compte. La décision appartient à WhatsApp.", BannerTone.WARN)
    Spacer(Modifier.height(Space.m))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.height(52.dp)) { Text("Retour") }
        PrimaryButton(text = "Continuer vers le signalement officiel", onClick = onContinue, modifier = Modifier.weight(1f))
    }
}

@Composable private fun SummaryRow(k: String, v: String) {
    Column(Modifier.padding(vertical = Space.xs)) {
        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(v, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PreviewStep(text: String, onEdit: (String) -> Unit, onCopy: () -> Unit, onCancel: () -> Unit, onSend: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(text) }
    Text("Aperçu obligatoire du texte transmis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(Space.s))
    InfoBanner("Ce texte est un dossier de signalement factuel. Ce n'est pas un moyen de forcer WhatsApp à suspendre un compte.")
    Spacer(Modifier.height(Space.m))
    if (editing) {
        OutlinedTextField(value = draft, onValueChange = { draft = it }, minLines = 12, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            OutlinedButton(onClick = { editing = false }) { Text("Annuler édition") }
            Button(onClick = { onEdit(draft); editing = false }) { Text("Enregistrer") }
        }
    } else {
        Surface(shape = RoundedCornerShape(Radius.card), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(text, Modifier.padding(Space.m).fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(Space.m))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { editing = true }) { Text("Modifier") }
        OutlinedButton(onClick = onCopy) { Text("Copier") }
        OutlinedButton(onClick = onCancel) { Text("Annuler") }
        Button(onClick = onSend) { Text("ENVOYER") }
    }
}

@Composable
private fun FinalConfirmationDialog(c: CaseFile, text: String, onCancel: () -> Unit, onYes: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Vous êtes sur le point de transmettre un signalement réel.") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Numéro concerné : ${c.targetNumberE164}")
                Text("Motif : ${c.category.labelFr}")
                Text("Description : ${c.description.take(200)}")
                Text("Preuves : ${c.evidences.size} élément(s)")
                Text("Canal : choisi à l'étape suivante (mécanismes officiels uniquement)")
                Text("Identité utilisée : ${if (com.whalert.app.data.SecureStore.get(androidx.compose.ui.platform.LocalContext.current).userEmail != null) "e-mail associé" else "aucun e-mail"}")
                Spacer(Modifier.height(Space.s))
                Text("Confirmez-vous l'envoi ?", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = onYes) { Text("OUI, CONTINUER") } },
        dismissButton = { OutlinedButton(onClick = onCancel) { Text("NON, ANNULER") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransmissionSheet(context: Context, c: CaseFile, text: String, evidenceUris: List<String>, onResult: (String, String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { }, sheetState = sheetState) {
        Column(Modifier.padding(Space.l)) {
            Text("Canaux réels disponibles", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.s))
            InfoBanner("WhatsApp indique que le processus officiel de signalement se déroule dans WhatsApp. Un contact extérieur n'accélère pas la décision. L'e-mail est un canal documentaire.")
            Spacer(Modifier.height(Space.m))
            val waAvailable = com.whalert.app.data.OfficialChannels.isInAppChannelAvailable(context)
            ItemAction(
                enabled = waAvailable,
                title = if (waAvailable) "Signaler dans WhatsApp (officiel)" else "WhatsApp non installé sur cet appareil",
                subtitle = "Ouvre la conversation ; utilisez le menu « Signaler » de WhatsApp vous-même."
            ) {
                val ok = com.whalert.app.data.OfficialChannels.openWhatsAppConversation(context, c.targetNumberE164)
                onResult("official_in_app", if (ok) "WHATSAPP_OPENED_USER_ACTION_REQUIRED" else "OPEN_FAILED")
            }
            ItemAction(enabled = true, title = "Formulaire officiel d'assistance WhatsApp",
                subtitle = com.whalert.app.data.OfficialChannels.SUPPORT_FORM_URL) {
                val ok = com.whalert.app.data.OfficialChannels.openSupportForm(context)
                onResult("support_form", if (ok) "FORM_OPENED_USER_ACTION_REQUIRED" else "OPEN_FAILED")
            }
            val store = com.whalert.app.data.SecureStore.get(context)
            val email = store.userEmail?.takeIf { com.whalert.app.data.EmailValidator.isValid(it) }
            val consent = store.emailConsentGranted
            ItemAction(
                enabled = email != null && consent,
                title = if (email != null && consent) "Transmettre le dossier par e-mail (votre client mail)" else "E-mail indisponible (association/consentement requis)",
                subtitle = if (email != null) "Destinataire visible avant envoi : $email" else "Configurez « Connecter mon compte »."
            ) {
                val uris = evidenceUris.mapNotNull { p ->
                    runCatching { android.net.Uri.parse(com.whalert.app.data.SecureStore.get(context).evidenceFileUri(context, p)) }.getOrNull()
                }
                val ok = com.whalert.app.data.OfficialChannels.composeEmail(
                    context, email!!, "Signalement WhatsApp — dossier ${c.reference}", text, uris
                )
                onResult("email", if (ok) "EMAIL_HANDED_TO_CLIENT_NOT_SENT_CONFIRMED" else "EMAIL_CLIENT_UNAVAILABLE")
            }
            Spacer(Modifier.height(Space.m))
            OutlinedButton(onClick = { onResult("none", "TRANSMISSION_CANCELLED_BY_USER") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Fermer sans transmettre")
            }
        }
    }
}

@Composable
private fun ItemAction(enabled: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = Space.s)) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(title) }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun androidx.compose.ui.graphics.Shape.border() {}
