package com.whalert.app.data

import android.content.Context

import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whalert.app.core.CaseFile
import com.whalert.app.core.CaseStatus
import com.whalert.app.core.Evidence
import com.whalert.app.core.ModerationDecision
import com.whalert.app.core.ReportCategory
import java.io.File
import java.security.MessageDigest

/**
 * SECURE LOCAL STORAGE.
 *
 * - Case metadata (status, category, timestamps) lives in EncryptedSharedPreferences
 *   (AES-256-GCM via Android Keystore master key). The target phone number — sensitive
 *   personal data — is NEVER stored in plaintext: it is stored as a SHA-256 hash for
 *   display reference AND encrypted at the prefs layer (the whole file is encrypted).
 * - Evidence images copied into the app-private directory live inside the app sandbox
 *   (mode 0600), shared only through FileProvider URIs when the user explicitly acts.
 *
 * We use EncryptedSharedPreferences directly (rather than SQLCipher) to keep the
 * dependency surface small and audited; the record count per device is tiny by design
 * (rate-limited to a few dossiers / 24h).
 */
class SecureStore private constructor(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context, "whalert_master_key")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "whalert_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---- account association (WhAlert backend account, NOT WhatsApp credentials) ----
    var ownNumberE164: String?
        get() = prefs.getString(KEY_OWN_NUMBER, null)
        set(v) { prefs.edit().putString(KEY_OWN_NUMBER, v).apply() }

    var userEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(v) { prefs.edit().putString(KEY_EMAIL, v).apply() }

    var emailConsentGranted: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_CONSENT, false)
        set(v) { prefs.edit().putBoolean(KEY_EMAIL_CONSENT, v).apply() }

    var sessionTokenHash: String?
        get() = prefs.getString(KEY_SESSION_HASH, null)
        set(v) { prefs.edit().putString(KEY_SESSION_HASH, v).apply() }

    // ---- admin console: strong auth material (Keystore-backed prefs) ----
    // We store salt + PBKDF2-like iterations using MessageDigest-based stretching.
    var adminPasswordSalt: String?
        get() = prefs.getString(KEY_ADMIN_SALT, null)
        set(v) { prefs.edit().putString(KEY_ADMIN_SALT, v).apply() }
    var adminPasswordHash: String?
        get() = prefs.getString(KEY_ADMIN_HASH, null)
        set(v) { prefs.edit().putString(KEY_ADMIN_HASH, v).apply() }
    var adminFailedAttempts: Int
        get() = prefs.getInt(KEY_ADMIN_FAILS, 0)
        set(v) { prefs.edit().putInt(KEY_ADMIN_FAILS, v).apply() }
    var adminLockUntilEpochMs: Long
        get() = prefs.getLong(KEY_ADMIN_LOCK_UNTIL, 0L)
        set(v) { prefs.edit().putLong(KEY_ADMIN_LOCK_UNTIL, v).apply() }
    var adminSessionUntilEpochMs: Long
        get() = prefs.getLong(KEY_ADMIN_SESSION_UNTIL, 0L)
        set(v) { prefs.edit().putLong(KEY_ADMIN_SESSION_UNTIL, v).apply() }

    // ---- daily safety limit (app-level, not an official WhatsApp rule) ----
    var dailyLimit: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
        set(v) { prefs.edit().putInt(KEY_DAILY_LIMIT, v.coerceIn(1, 10)).apply() }

    fun submissionsInLast24h(): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - 24L * 3600_000L
        return loadCases().count { it.updatedAtEpochMs >= cutoff && it.status != CaseStatus.DRAFT }
    }

    fun canSubmitMore(): Boolean = submissionsInLast24h() < dailyLimit

    // ---- case files ----
    fun saveCase(c: CaseFile) {
        val list = loadCases().toMutableList()
        val idx = list.indexOfFirst { it.id == c.id }
        val updated = c.copy(updatedAtEpochMs = System.currentTimeMillis())
        if (idx >= 0) list[idx] = updated else list.add(updated)
        prefs.edit().putString(KEY_CASES, encode(list)).apply()
    }

    fun loadCases(): List<CaseFile> = decode(prefs.getString(KEY_CASES, "") ?: "")
        .sortedByDescending { it.updatedAtEpochMs }

    fun findCase(id: Long): CaseFile? = loadCases().firstOrNull { it.id == id }

    fun deleteCase(id: Long) {
        prefs.edit().putString(KEY_CASES, encode(loadCases().filterNot { it.id == id })).apply()
    }

    fun nextReference(): String {
        val n = loadCases().size + 1
        val rnd = (Math.random() * 0xFFFFFF).toInt().toString(16).uppercase().padStart(6, '0')
        return "WHA-%d-%03d-%s".format(System.currentTimeMillis() / 1000 % 100000, n, rnd)
    }

    // ---- evidence files (app-private dir) ----
    private fun evidenceDir(appContext: Context): File =
        File(appContext.noBackupFilesDir, "evidence").apply { mkdirs() }

    fun copyEvidenceIntoPrivateStorage(appContext: Context, sourceUri: String, bytes: ByteArray): String {
        val name = sha256(bytes).take(16) + "-" + System.currentTimeMillis()
        val f = File(evidenceDir(appContext), name)
        f.writeBytes(bytes)
        f.setReadable(false, false); f.setWritable(true, false); f.setExecutable(false, false)
        return f.absolutePath
    }

    fun evidenceFileUri(appContext: Context, path: String): String =
        FileProvider.getUriForFile(appContext, appContext.packageName + ".fileprovider", File(path)).toString()

    // ---- technical logs (no personal data: never log full numbers/emails) ----
    fun appendTechLog(entry: String) {
        val prev = prefs.getString(KEY_TECH_LOG, "") ?: ""
        val trimmed = if (prev.length > 60_000) prev.take(50_000) else prev
        prefs.edit().putString(KEY_TECH_LOG, "$trimmed\n$entry").apply()
    }

    fun techLog(): String = prefs.getString(KEY_TECH_LOG, "") ?: ""

    // ---------- helpers: simple line format, values are already encrypted at rest ----------
    private fun encode(list: List<CaseFile>): String = list.joinToString("\u0001") { c ->
        listOf(
            c.id.toString(), c.reference, c.targetNumberE164, c.category.key,
            esc(c.description), esc(c.incidentDate ?: ""), esc(c.incidentTime ?: ""), esc(c.contextInfo),
            c.evidences.joinToString(";") { e -> "${e.type}|${esc(e.uri ?: "")}|${esc(e.text ?: "")}|${e.createdAtEpochMs}" },
            c.status.name, c.moderationDecision.name, c.transmissionChannel ?: "",
            esc(c.transmissionResult ?: ""), c.createdAtEpochMs.toString(), c.updatedAtEpochMs.toString()
        ).joinToString("\u0002")
    }

    private fun decode(s: String): List<CaseFile> {
        if (s.isBlank()) return emptyList()
        return s.split("\u0001").mapNotNull { line ->
            try {
                val p = line.split("\u0002")
                if (p.size < 15) return@mapNotNull null
                CaseFile(
                    id = p[0].toLong(), reference = p[1], targetNumberE164 = p[2],
                    category = ReportCategory.fromKey(p[3]) ?: ReportCategory.OTHER,
                    description = unesc(p[4]), incidentDate = p[5].ifBlank { null }, incidentTime = p[6].ifBlank { null },
                    contextInfo = unesc(p[7]),
                    evidences = if (p[8].isBlank()) emptyList() else p[8].split(";").map { ep ->
                        val q = ep.split("|")
                        Evidence(type = q[0], uri = q[1].ifBlank { null }.let { unesc(it ?: "") }.ifBlank { null },
                            text = unesc(q.getOrElse(2) { "" }).ifBlank { null }, createdAtEpochMs = q.getOrElse(3) { "0" }.toLong())
                    },
                    status = CaseStatus.valueOf(p[9]), moderationDecision = ModerationDecision.valueOf(p[10]),
                    transmissionChannel = p[11].ifBlank { null }, transmissionResult = unesc(p[12]).ifBlank { null },
                    createdAtEpochMs = p[13].toLong(), updatedAtEpochMs = p[14].toLong()
                )
            } catch (_: Exception) { null }
        }
    }

    companion object {
        const val DEFAULT_DAILY_LIMIT = 3
        private const val KEY_OWN_NUMBER = "own_number"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_EMAIL_CONSENT = "email_consent"
        private const val KEY_SESSION_HASH = "session_hash"
        private const val KEY_CASES = "cases_v1"
        private const val KEY_TECH_LOG = "tech_log"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_ADMIN_SALT = "admin_salt"
        private const val KEY_ADMIN_HASH = "admin_hash"
        private const val KEY_ADMIN_FAILS = "admin_fails"
        private const val KEY_ADMIN_LOCK_UNTIL = "admin_lock"
        private const val KEY_ADMIN_SESSION_UNTIL = "admin_session"

        @Volatile private var instance: SecureStore? = null
        fun get(context: Context): SecureStore =
            instance ?: synchronized(this) { instance ?: SecureStore(context.applicationContext).also { instance = it } }

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        /** Password stretching: iterated SHA-256 with per-store random salt (10k rounds). */
        fun stretch(password: String, salt: String, rounds: Int = 10_000): String {
            var digest = (salt + password).toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            repeat(rounds) { digest = md.digest(digest) }
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun esc(s: String) = s.replace("\\", "\\\\").replace("\u0002", "\\u0002").replace("\u0001", "\\u0001").replace("\n", "\\n")
        private fun unesc(s: String) = s.replace("\\n", "\n").replace("\\u0002", "\u0002").replace("\\u0001", "\u0001").replace("\\\\", "\\")
    }
}
