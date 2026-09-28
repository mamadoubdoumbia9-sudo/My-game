package com.whalert.app.data

/**
 * Strict, dependency-free email validation (no regex backtracking / ReDoS risk).
 * Used before any mailto: hand-off so malformed data never leaves the device.
 * Rejects injection-prone characters (CR/LF) to protect mail clients from header injection.
 */
object EmailValidator {
    private const val MAX_LEN = 254

    fun isValid(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        if (email.length > MAX_LEN) return false
        if (email.any { it.isWhitespace() || it == '"' || it == '\\' || it.code < 33 || it.code > 126 }) return false
        val at = email.lastIndexOf('@')
        if (at <= 0 || at == email.length - 1) return false
        // reject consecutive dots anywhere
        if (email.contains("..")) return false
        val local = email.substring(0, at)
        val domain = email.substring(at + 1)
        if (local.length > 64) return false
        val allowedLocal = CharRange('a', 'z').toList() + CharRange('A', 'Z').toList() +
            CharRange('0', '9').toList() + listOf('.', '_', '%', '+', '-')
        if (!local.all { it in allowedLocal } || local.startsWith(".") || local.endsWith(".")) return false
        val labels = domain.split('.')
        if (labels.size < 2) return false
        val allowedDomainChar = CharRange('a', 'z').toList() + CharRange('A', 'Z').toList() +
            CharRange('0', '9').toList() + listOf('-', '.')
        return labels.all { l ->
            l.isNotEmpty() && l.length <= 63 &&
                !l.startsWith('-') && !l.endsWith('-') &&
                l.all { it in allowedDomainChar }
        } && labels.last().length >= 2 && labels.last().none { it.isDigit() }
    }
}
