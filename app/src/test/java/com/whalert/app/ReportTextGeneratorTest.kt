package com.whalert.app

import com.whalert.app.core.CaseFile
import com.whalert.app.core.Evidence
import com.whalert.app.core.ReportCategory
import com.whalert.app.data.ReportTextGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated report text must be FACTUAL, built only from user-entered data,
 * and must NEVER claim a ban/suspension outcome.
 */
class ReportTextGeneratorTest {

    private fun sampleCase() = CaseFile(
        reference = "WA-TEST-0001",
        targetNumberE164 = "+22378123456",
        category = ReportCategory.SCAM,
        description = "On m'a demandé un paiement Mobile Money pour une fausse offre d'emploi.",
        incidentDate = "2026-09-01",
        incidentTime = "14:30",
        contextInfo = "Message reçu sur WhatsApp depuis ce numéro.",
        evidences = listOf(Evidence(type = "text", uri = null, text = "Copie du message : envoyez 5000 FCFA", createdAtEpochMs = 1_700_000_000_000L)),
        createdAtEpochMs = 1_700_000_000_000L,
        updatedAtEpochMs = 1_700_000_000_000L
    )

    @Test
    fun `text contains the required factual sections`() {
        val t = ReportTextGenerator.generate(sampleCase(), reporterEmail = "moi@exemple.org")
        assertTrue(t.contains("Objet : Signalement"))
        assertTrue(t.contains("+22378123456"))
        assertTrue(t.contains("Motif"))
        assertTrue(t.contains("Résumé des faits"))
        assertTrue(t.contains("Je demande à WhatsApp d'examiner"))
        assertTrue(t.contains("moi@exemple.org"))
    }

    @Test
    fun `text never claims a ban or suspension outcome`() {
        val t = ReportTextGenerator.generate(sampleCase(), null).lowercase()
        assertFalse(t.contains("compte banni"))
        assertFalse(t.contains("a été suspendu"))
        assertFalse(t.contains("est garanti"))
        assertTrue(t.contains("ne garantit aucune décision"))
        assertTrue(t.contains("la décision appartient à whatsapp"))
    }

    @Test
    fun `email omitted when not provided`() {
        val t = ReportTextGenerator.generate(sampleCase(), null)
        assertFalse(t.contains("Contact du déclarant"))
    }
}
