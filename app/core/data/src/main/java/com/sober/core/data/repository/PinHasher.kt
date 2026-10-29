package com.sober.core.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashes the override PIN with PBKDF2-HMAC-SHA256.
 *
 * The record is self-describing (`iterations:salt:hash`) so the iteration
 * count can be raised later without invalidating PINs already stored: a verify
 * always uses the count the record was written with.
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(pin: String, random: SecureRandom = SecureRandom()): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(pin, salt, ITERATIONS)
        return "$ITERATIONS:${encode(salt)}:${encode(hash)}"
    }

    fun verify(pin: String, record: String): Boolean {
        val parts = record.split(':')
        if (parts.size != 3) return false

        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = decode(parts[1]) ?: return false
        val expected = decode(parts[2]) ?: return false

        // Constant-time comparison: a byte-by-byte early exit leaks how much of
        // the hash matched.
        return MessageDigest.isEqual(expected, derive(pin, salt, iterations))
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS))
            .encoded

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(value) }.getOrNull()
}
