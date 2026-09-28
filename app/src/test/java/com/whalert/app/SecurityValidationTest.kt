package com.whalert.app

import com.whalert.app.data.EmailValidator
import com.whalert.app.data.ReportTextGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security-focused unit tests: input validation and injection hardening.
 * These run on the JVM (no Android framework required).
 */
class SecurityValidationTest {

    // ---------- Email validation ----------
    @Test
    fun `valid emails accepted`() {
        listOf(
            "user@example.org", "a.b+c@mail.mali.fr", "mariam_223@sub.domain.co",
            "test+tag@gmail.com"
        ).forEach { assertTrue("should accept $it", EmailValidator.isValid(it)) }
    }

    @Test
    fun `malformed and injection-prone emails rejected`() {
        val bad = listOf(
            null, "", "   ", "no-at-sign", "two@@at.org", "a@b", "a@b.", "a..b@c.org",
            ".leading@c.org", "trailing.@c.org", "space.in@c.org",
            "cr" + "lf-inject@c.org",                    // contains CR/LF control chars
            "nul" + "char@c.org",
            "quote\"inject@c.org", "back\\slash@c.org",
            "a".repeat(65) + "@c.org",                   // local part too long
            "u@" + "a".repeat(64) + ".org"               // label too long
        )
        bad.forEach { assertFalse("should reject ${it?.take(30)}", EmailValidator.isValid(it)) }
    }

    @Test
    fun `email with CRLF is rejected not sanitized`() {
        assertFalse(EmailValidator.isValid("victim@ex.org\r\nBcc: spam@evil.tld"))
    }

    // ---------- Text sanitization (log/header injection hardening) ----------
    @Test
    fun `sanitize strips control characters but keeps newlines and text`() {
        val dirty = "Bonjour\u0000 monde\r\n\u001B[31mROUGE\u001B[m fini\u0007!"
        val clean = ReportTextGenerator.sanitize(dirty)
        assertFalse(clean.contains("\u0000"))
        assertFalse(clean.contains("\r"))
        assertFalse(clean.contains("\u001B"))
        assertFalse(clean.contains("\u0007"))
        assertTrue(clean.contains("Bonjour"))
        assertTrue(clean.contains("monde"))
        assertTrue(clean.contains("fini!"))
    }

    @Test
    fun `sanitize collapses blank-line floods`() {
        val flooded = "ligne1\n\n\n\n\n\n\nligne2"
        val out = ReportTextGenerator.sanitize(flooded)
        assertEquals("ligne1\nligne2", out)
    }

    @Test
    fun `sanitize caps oversized payloads`() {
        val huge = "A".repeat(50_000)
        val out = ReportTextGenerator.sanitize(huge, maxLength = 1000)
        assertEquals(1000, out.length)
    }

    @Test
    fun `sanitize handles null and blank`() {
        assertEquals("", ReportTextGenerator.sanitize(null))
        assertEquals("", ReportTextGenerator.sanitize("   "))
    }

    // ---------- Anti-abuse multiplicity guard ----------
    @Test
    fun `single report allowed`() {
        assertEquals(null, ReportTextGenerator.refusalForMultiplicity(1))
    }

    @Test
    fun `mass reporting refused with honest message`() {
        for (n in listOf(2, 10, 100, 1000)) {
            val msg = ReportTextGenerator.refusalForMultiplicity(n)
            assertTrue(msg != null)
            assertTrue(msg!!.contains("ne peut pas multiplier artificiellement"))
        }
    }

    // ---------- Honest moderation wording ----------
    @Test
    fun `generated dossier never claims official outcome`() {
        val c = com.whalert.app.core.CaseFile(
            reference = "WHA-TEST-1",
            targetNumberE164 = "+22378123456",
            category = com.whalert.app.core.ReportCategory.SPAM,
            description = "Messages non sollicités en masse.",
            incidentDate = null, incidentTime = null, contextInfo = "",
            createdAtEpochMs = 1_700_000_000_000L, updatedAtEpochMs = 1_700_000_000_000L
        )
        val t = ReportTextGenerator.generate(c, null).lowercase()
        assertFalse(t.contains("banni"))
        assertFalse(t.contains("suspendu"))
        assertFalse(t.contains("whatsapp a examiné"))
        assertTrue(t.contains("décision appartient à whatsapp"))
    }
}

/** Password stretching + hashing determinism (admin console auth core). */
class CryptoStretchTest {
    @org.junit.Test
    fun `sha256 is deterministic hex`() {
        val h = com.whalert.app.data.SecureStore.sha256("abc".toByteArray())
        org.junit.Assert.assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", h)
    }

    @org.junit.Test
    fun `stretch depends on password and salt and is stable`() {
        val a = com.whalert.app.data.SecureStore.stretch("motdepasse", "saltA")
        val b = com.whalert.app.data.SecureStore.stretch("motdepasse", "saltB")
        val c = com.whalert.app.data.SecureStore.stretch("motdepasse", "saltA")
        org.junit.Assert.assertEquals(64, a.length)
        org.junit.Assert.assertNotEquals(a, b)   // different salts => different hashes
        org.junit.Assert.assertEquals(a, c)      // deterministic
    }
}
