package com.whalert.app.core

/**
 * Plain Old Kotlin Objects used across layers (domain <-> data <-> UI).
 * Kept free of Android imports so they can be unit-tested on the JVM.
 */

enum class ReportCategory(val key: String, val labelFr: String) {
    SPAM("spam", "Spam"),
    SCAM("arnaque", "Arnaque"),
    IMPERSONATION("usurpation", "Usurpation d'identité"),
    HARASSMENT("harcelement", "Harcèlement"),
    FRAUDULENT_CONTENT("contenu_frauduleux", "Contenu frauduleux"),
    MALICIOUS_BEHAVIOR("malveillant", "Comportement malveillant"),
    OTHER("autre", "Autre");

    companion object {
        fun fromKey(k: String): ReportCategory? = entries.firstOrNull { it.key == k }
    }
}

/** Honest, non-simulated lifecycle of a case file. See STATUS semantics in README. */
enum class CaseStatus(val labelFr: String) {
    DRAFT("Brouillon"),
    PREPARED("Préparé"),
    SUBMITTED("Soumis (transmission tentée depuis cette application)"),
    CONFIRMED("Confirmation technique obtenue"),
    FOLLOW_UP("Suivi nécessaire"),
    CLOSED("Clôturé");
}

/**
 * Moderation decision — we NEVER invent a value.
 * WHATSAPP_DECISION_UNKNOWN is the honest default: WhatsApp does not expose a public
 * API to query moderation outcomes.
 */
enum class ModerationDecision(val labelFr: String) {
    UNKNOWN("Inconnue"),
    NO_OFFICIAL_CONFIRMATION("Aucune confirmation officielle"),
    OFFICIAL_INFO_AVAILABLE("Information officielle disponible"),
    ACTION_REPORTED_BY_WHATSAPP("Action signalée par WhatsApp");
}

/**
 * Connection state of the user's OWN account inside WhAlert's backend
 * (NOT a WhatsApp session — this app never touches WhatsApp login/SMS/OTP).
 */
enum class AccountState(val labelFr: String) {
    CONNECTED("CONNECTÉ"),
    NOT_CONNECTED("NON CONNECTÉ"),
    SESSION_EXPIRED("SESSION EXPIRÉE"),
    CONNECTION_ERROR("ERREUR DE CONNEXION");
}

data class Evidence(
    val id: Long = 0,
    val type: String,          // "image" | "text" | "file"
    val uri: String?,          // content:// URI for images/files
    val text: String?,         // pasted textual evidence
    val createdAtEpochMs: Long
)

data class Incident(
    val id: Long = 0,
    val caseId: Long = 0,
    val occurredAtEpochMs: Long?,
    val note: String
)

data class CaseFile(
    val id: Long = 0,
    val reference: String,            // human-readable dossier id, e.g. WHA-2026-AB12CD
    val targetNumberE164: String,     // sensitive: stored in encrypted prefs/DB
    val category: ReportCategory,
    val description: String,
    val incidentDate: String?,        // yyyy-MM-dd as typed by user
    val incidentTime: String?,        // HH:mm as typed by user
    val contextInfo: String,
    val evidences: List<Evidence> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val status: CaseStatus = CaseStatus.DRAFT,
    val moderationDecision: ModerationDecision = ModerationDecision.UNKNOWN,
    val transmissionChannel: String? = null,   // "official_in_app" | "support_form" | "email" | "backend"
    val transmissionResult: String? = null,    // real technical result only ("EMAIL_QUEUED_OK", error…)
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    /** Masked display: +223 76 ••• ••• style — history hides sensitive data by default. */
    fun maskedNumber(): String {
        val digits = targetNumberE164.filter { it.isDigit() }
        if (digits.length < 5) return "+${digits.take(2)} ••"
        return "+${digits.take(2)} ${digits.drop(2).take(2)} ••• •••"
    }
}

/** Result of a real network operation — never fabricated. */
sealed class NetResult<out T> {
    data class Success<T>(val data: T) : NetResult<T>()
    data class HttpError(val code: Int, val message: String) : NetResult<Nothing>()
    data class NetworkFailure(val reason: String) : NetResult<Nothing>()
    object Disabled : NetResult<Nothing>() // no real backend configured -> feature honestly off
}
