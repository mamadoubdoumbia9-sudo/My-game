package com.whalert.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.whalert.app.core.CaseFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates the factual report text ("dossier de signalement") from user-entered data ONLY.
 * - No invented accusations, no threats, no abusive language.
 * - The text is a documentation package; it is NOT a "script to force WhatsApp to ban".
 */
object ReportTextGenerator {

    private val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)

    fun generate(c: CaseFile, reporterEmail: String?): String = buildString {
        appendLine("Objet : Signalement d'un compte WhatsApp suspect")
        appendLine()
        appendLine("Numéro concerné : ${c.targetNumberE164}")
        appendLine("Motif : ${c.category.labelFr}")
        appendLine()
        appendLine("Résumé des faits :")
        appendLine(c.description.trim())
        appendLine()
        if (!c.incidentDate.isNullOrBlank() || !c.incidentTime.isNullOrBlank()) {
            appendLine("Date(s) de l'incident : ${c.incidentDate ?: "?"} ${c.incidentTime ?: ""}".trim())
        }
        if (c.contextInfo.isNotBlank()) {
            appendLine()
            appendLine("Contexte complémentaire :")
            appendLine(c.contextInfo.trim())
        }
        if (c.evidences.isNotEmpty()) {
            appendLine()
            appendLine("Éléments disponibles (${c.evidences.size}) :")
            c.evidences.forEachIndexed { i, e ->
                when (e.type) {
                    "image" -> appendLine("  ${i + 1}. Capture d'écran / image jointe (${df.format(Date(e.createdAtEpochMs))})")
                    "text" -> appendLine("  ${i + 1}. Preuve texte : ${e.text?.take(300)}")
                    else -> appendLine("  ${i + 1}. Fichier joint")
                }
            }
        }
        if (!reporterEmail.isNullOrBlank()) {
            appendLine()
            appendLine("Contact du déclarant : $reporterEmail")
        }
        appendLine()
        appendLine("Demande :")
        appendLine("Je demande à WhatsApp d'examiner les éléments transmis conformément à ses procédures et à ses conditions d'utilisation.")
        appendLine()
        appendLine("Dossier WhAlert : ${c.reference} — créé le ${df.format(Date(c.createdAtEpochMs))}")
        appendLine("Ce document est un dossier de signalement factuel. Il ne garantit aucune décision de modération ; la décision appartient à WhatsApp.")
    }

    /** Anti-abuse guard: refuses multiplicity requests. Returns null if OK, message otherwise. */
    fun refusalForMultiplicity(count: Int): String? =
        if (count > 1) "Les signalements doivent correspondre à des événements ou expériences réels. Cette application ne peut pas multiplier artificiellement un même signalement."
        else null

    /**
     * Sanitizes free text before it is embedded in a generated dossier / handed to an
     * email client: strips control characters (CR/LF/VT/FF/NUL…) that could be used for
     * header-injection or log-forging attempts, collapses excessive blank lines and
     * caps the length so malformed/huge payloads never leave the device.
     * User content is otherwise preserved verbatim — we never invent or alter facts.
     */
    fun sanitize(text: String?, maxLength: Int = 20_000): String {
        if (text.isNullOrBlank()) return ""
        val cleaned = buildString(minOf(text.length, maxLength)) {
            var prevWasNewline = false
            for (ch in text) {
                when {
                    ch == '\n' -> { // keep at most one consecutive newline
                        if (!prevWasNewline) append(ch)
                        prevWasNewline = true
                    }
                    ch.code < 32 || ch.code == 127 -> { /* drop other control chars */ }
                    else -> { append(ch); prevWasNewline = false }
                }
            }
        }
        return cleaned.trim().take(maxLength)
    }
}

object NetworkMonitor {
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }

    fun transport(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "aucun"
        val n = cm.activeNetwork ?: return "aucun"
        val caps = cm.getNetworkCapabilities(n) ?: return "inconnu"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles"
            else -> "autre"
        }
    }
}

/** Uri helpers for evidence images chosen via SAF. */
object EvidenceIO {
    fun readBytes(context: Context, uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) { null }
}
