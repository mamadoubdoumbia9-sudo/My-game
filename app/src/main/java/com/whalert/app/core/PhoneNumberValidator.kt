package com.whalert.app.core

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

/**
 * Strict E.164 validation via libphonenumber (Apache-2.0).
 *
 * HONESTY RULE: a syntactically valid number does NOT mean a WhatsApp account exists.
 * This validator only checks phone-number format/possibility. It never claims anything
 * about WhatsApp membership or ban status.
 */
object PhoneNumberValidator {

    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    data class Outcome(
        val isValid: Boolean,
        val e164: String?,
        val countryCode: String?,   // region code e.g. "ML"
        val message: String
    )

    fun validate(rawInput: String, defaultRegion: String = "ZZ"): Outcome {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return Outcome(false, null, null, "Numéro requis.")
        try {
            val parsed: Phonenumber.PhoneNumber = util.parse(trimmed, defaultRegion.uppercase())
            val possibleShort = util.isPossibleNumber(parsed)
            val valid = util.isValidNumber(parsed)
            val e164 = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            return when {
                valid -> Outcome(true, e164, util.getRegionCodeForNumber(parsed), "Format international valide (E.164).")
                possibleShort -> Outcome(false, e164, null, "Le numéro semble incomplet pour l'indicatif indiqué.")
                else -> Outcome(false, null, null, "Numéro invalide. Utilisez le format international : + indicatif pays puis numéro.")
            }
        } catch (e: NumberParseException) {
            return Outcome(false, null, null, "Numéro invalide. Format attendu : +XX suivi du numéro (ex. +223XXXXXXXX).")
        }
    }

    fun availableRegions(): List<Pair<String, String>> =
        util.supportedRegions
            .filter { it != "ZZ" }
            .map { r -> r to util.getCountryCodeForRegion(r).toString() }
            .sortedBy { it.second.toIntOrNull() ?: 999 }

    /** Simple boolean check used at ViewModel boundaries (defense in depth). */
    fun isValid(e164: String?): Boolean = e164 != null && validate(e164).isValid
}
