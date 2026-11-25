package com.mizanati.invest.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Lock {
    private const val KEY_ALIAS = "mz_lock_key"
    private const val ITER = 200_000
    private const val MAX_TRIES = 5
    private const val LOCKOUT_MS = 60_000.0

    fun randomKey(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

    private fun unhex(s: String): ByteArray =
        ByteArray(s.length / 2) { i -> ((Character.digit(s[i * 2], 16) shl 4) + Character.digit(s[i * 2 + 1], 16)).toByte() }

    fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITER, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(spec).encoded
    }

    fun pinHash(pin: String, salt: ByteArray): String = hex(salt) + ":" + hex(pbkdf2(pin, salt))

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return try {
            MessageDigest.isEqual(unhex(parts[1]), pbkdf2(pin, unhex(parts[0])))
        } catch (e: Exception) {
            false
        }
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = c.iv
        return hex(iv) + ":" + hex(c.doFinal(plain.toByteArray()))
    }

    fun decrypt(stored: String): String? = try {
        val parts = stored.split(":")
        if (parts.size != 2) null
        else {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, unhex(parts[0])))
            String(c.doFinal(unhex(parts[1])))
        }
    } catch (e: Exception) {
        null
    }

    fun enabled(S: JSONObject): Boolean = S.truthy("lockEnabled") && S.str("lockPin").contains(":")

    fun hint(S: JSONObject): String {
        val h = S.optString("lockHint")
        return if (S.truthy("lockHintEnc")) Lock.decrypt(h) ?: "" else h
    }

    fun setPin(S: JSONObject, pin: String, hint: String) {
        val salt = randomKey()
        S.put("lockPin", pinHash(pin, salt))
        S.put("lockPinLen", pin.length)
        if (hint.isBlank()) {
            S.remove("lockHint")
            S.remove("lockHintEnc")
        } else {
            S.put("lockHint", hint)
            S.remove("lockHintEnc")
        }
        S.put("lockEnabled", true)
        S.remove("lockFail")
    }

    fun disable(S: JSONObject) {
        S.remove("lockPin")
        S.remove("lockPinLen")
        S.remove("lockHint")
        S.remove("lockHintEnc")
        S.remove("lockEnabled")
        S.remove("lockFail")
    }

    fun pinLen(S: JSONObject): Int {
        val n = S.optInt("lockPinLen", 0)
        return if (n in 4..6) n else 4
    }

    private fun tries(S: JSONObject): JSONObject {
        if (!S.has("lockFail")) S.put("lockFail", JSONObject())
        return S.getJSONObject("lockFail")
    }

    fun lockedOut(S: JSONObject): Double {
        val t = tries(S)
        val until = t.num("until")
        if (until.isNaN()) return 0.0
        val left = until - System.currentTimeMillis()
        if (left <= 0) return 0.0
        return left
    }

    fun unlock(S: JSONObject, pin: String): Boolean {
        val t = tries(S)
        val left = lockedOut(S)
        if (left > 0) return false
        return if (verify(pin, S.str("lockPin"))) {
            S.remove("lockFail")
            true
        } else {
            val n = t.optDouble("n", 0.0) + 1
            t.put("n", n)
            if (n >= MAX_TRIES) {
                t.put("n", 0)
                t.put("until", System.currentTimeMillis() + n * LOCKOUT_MS)
            }
            false
        }
    }
}
