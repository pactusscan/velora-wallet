package com.andrutstudio.velora.data.crypto

import org.junit.Assert.*
import org.junit.Test
import com.andrutstudio.velora.data.crypto.CryptoUtils.zeroOut
import javax.crypto.AEADBadTagException

class CryptoUtilsTest {

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val password = "test_password_123".toCharArray()
        val salt = CryptoUtils.generateSalt()
        val iv = CryptoUtils.generateIv()
        val key = CryptoUtils.deriveKey(password, salt)
        val plaintext = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
            .toByteArray(Charsets.UTF_8)

        val encrypted = CryptoUtils.encrypt(plaintext, key, iv)
        val decrypted = CryptoUtils.decrypt(encrypted, key)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `decrypt with wrong key throws exception`() {
        val salt = CryptoUtils.generateSalt()
        val iv = CryptoUtils.generateIv()
        val key = CryptoUtils.deriveKey("correct_password".toCharArray(), salt)
        val wordKey = CryptoUtils.deriveKey("wrong_password".toCharArray(), salt)
        val plaintext = "secret mnemonic".toByteArray()

        val encrypted = CryptoUtils.encrypt(plaintext, key, iv)

        assertThrows(Exception::class.java) {
            CryptoUtils.decrypt(encrypted, wordKey)
        }
    }

    @Test
    fun `generateSalt produces 16 bytes`() {
        val salt = CryptoUtils.generateSalt()
        assertEquals(16, salt.size)
    }

    @Test
    fun `generateIv produces 12 bytes`() {
        val iv = CryptoUtils.generateIv()
        assertEquals(12, iv.size)
    }

    @Test
    fun `two salts are different`() {
        val salt1 = CryptoUtils.generateSalt()
        val salt2 = CryptoUtils.generateSalt()
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `deriveKey same inputs produce same key`() {
        val password = "my_password".toCharArray()
        val salt = CryptoUtils.generateSalt()
        val key1 = CryptoUtils.deriveKey(password, salt)
        val key2 = CryptoUtils.deriveKey(password, salt)
        assertArrayEquals(key1.encoded, key2.encoded)
    }

    @Test
    fun `zeroOut fills array with zeros`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        data.zeroOut()
        assertTrue(data.all { it == 0.toByte() })
    }
}
