package com.whalert.app

import com.whalert.app.core.AccountState
import com.whalert.app.core.CaseFile
import com.whalert.app.core.CaseStatus
import com.whalert.app.core.Evidence
import com.whalert.app.core.ModerationDecision
import com.whalert.app.core.NetResult
import com.whalert.app.core.ReportCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain-model & security-policy tests: honest statuses, masking of sensitive data,
 * no fabricated moderation outcomes.
 */
class DomainModelTest {

    private fun case(status: CaseStatus = CaseStatus.DRAFT) = CaseFile(
        reference = "WHA-2026-AB12CD",
        targetNumberE164 = "+22378123456",
        category = ReportCategory.SPAM,
        description = "Messages non sollicités en masse.",
        incidentDate = null,
        incidentTime = null,
        contextInfo = "",
        evidences = listOf(Evidence(type = "image", uri = "content://x", text = null, createdAtEpochMs = 1L)),
        status = status,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L
    )

    @Test
    fun `masked number hides digits but keeps country prefix`() {
        val masked = case().maskedNumber()
        assertFalse(masked.contains("123456"))
        assertTrue(masked.startsWith("+22"))
        assertTrue(masked.contains("•••"))
    }

    @Test
    fun `moderation decision default is UNKNOWN — never a fake ban`() {
        assertEquals(ModerationDecision.UNKNOWN, case().moderationDecision)
    }

    @Test
    fun `no enum value ever claims a confirmed ban`() {
        val forbidden = listOf("banni", "suspendu définitivement")
        ModerationDecision.entries.forEach { d ->
            val l = d.labelFr.lowercase()
            forbidden.forEach { f -> assertFalse("${d.name} must not say '$f'", l.contains(f)) }
        }
    }

    @Test
    fun `category round-trips by key`() {
        ReportCategory.entries.forEach { c ->
            assertEquals(c, ReportCategory.fromKey(c.key))
        }
        assertEquals(null, ReportCategory.fromKey("inexistant"))
    }

    @Test
    fun `status labels distinguish prepared vs submitted vs confirmed`() {
        val labels = CaseStatus.entries.map { it.labelFr }
        assertTrue(labels.any { it == "Brouillon" })
        assertTrue(labels.any { it == "Préparé" })
        assertTrue(labels.any { it.startsWith("Soumis") })
        assertTrue(labels.any { it.contains("Confirmation technique") })
        assertTrue(labels.any { it == "Clôturé" })
    }

    @Test
    fun `account states are the four honest values`() {
        assertEquals(
            setOf("CONNECTÉ", "NON CONNECTÉ", "SESSION EXPIRÉE", "ERREUR DE CONNEXION"),
            AccountState.entries.map { it.labelFr }.toSet()
        )
    }

    @Test
    fun `NetResult Disabled is a distinct honest state (never Success)`() {
        val r: NetResult<Boolean> = NetResult.Disabled
        assertFalse(r is NetResult.Success)
    }
}
