package com.whalert.app

import com.whalert.app.core.PhoneNumberValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de validation stricte des numéros (E.164 via libphonenumber).
 * HONESTY: these tests assert FORMAT validity only — never WhatsApp membership.
 */
class PhoneNumberValidatorTest {

    @Test
    fun `empty input is invalid`() {
        val r = PhoneNumberValidator.validate("")
        assertFalse(r.isValid)
        assertNotNull(r.message)
    }

    @Test
    fun `valid Mali number passes E164`() {
        val r = PhoneNumberValidator.validate("+223 78 12 34 56")
        assertTrue(r.isValid)
        assertEquals("+22378123456", r.e164)
    }

    @Test
    fun `valid France number with spaces and dots`() {
        val r = PhoneNumberValidator.validate("+33.6.12.34.56.78")
        assertTrue(r.isValid)
        assertEquals("+33612345678", r.e164)
    }

    @Test
    fun `too short number is rejected`() {
        val r = PhoneNumberValidator.validate("+22378")
        assertFalse(r.isValid)
    }

    @Test
    fun `letters are rejected`() {
        val r = PhoneNumberValidator.validate("+33-six-one-two")
        assertFalse(r.isValid)
    }

    @Test
    fun `missing plus falls back to default region`() {
        // national format with explicit region -> valid E.164
        val r = PhoneNumberValidator.validate("0612345678", defaultRegion = "FR")
        assertTrue(r.isValid)
        assertEquals("+33612345678", r.e164)
    }

    @Test
    fun `invalid country code rejected`() {
        val r = PhoneNumberValidator.validate("+999999123456")
        assertFalse(r.isValid)
    }

    @Test
    fun `regions list is non empty and sorted by dial code`() {
        val regions = PhoneNumberValidator.availableRegions()
        assertTrue(regions.isNotEmpty())
        val codes = regions.mapNotNull { it.second.toIntOrNull() }
        assertEquals(codes.sorted(), codes)
    }
}
