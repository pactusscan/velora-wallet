package com.andrutstudio.velora.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VaultStorageTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var storage: VaultStorage

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
        storage = VaultStorage(prefs)
    }

    @Test
    fun `encryptMnemonic then decryptMnemonic with correct password returns original`() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val password = "secure_password_123".toCharArray()

        val (encrypted, salt) = storage.encryptMnemonic(mnemonic, password)
        val decrypted = storage.decryptMnemonic(encrypted, salt, "secure_password_123".toCharArray())

        assertEquals(mnemonic, decrypted)
    }

    @Test
    fun `decryptMnemonic with wrong password throws WrongPasswordException`() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val (encrypted, salt) = storage.encryptMnemonic(mnemonic, "correct".toCharArray())

        assertThrows(WrongPasswordException::class.java) {
            storage.decryptMnemonic(encrypted, salt, "wrong".toCharArray())
        }
    }

    @Test
    fun `two encryptions of same mnemonic produce different ciphertext`() {
        val mnemonic = "test mnemonic phrase here"
        val password = "password".toCharArray()

        val (encrypted1, _) = storage.encryptMnemonic(mnemonic, password)
        val (encrypted2, _) = storage.encryptMnemonic(mnemonic, password)

        // IV is random, so ciphertexts must differ
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `hasVault returns false when prefs has no vault key`() {
        every { prefs.contains("vault_data") } returns false
        assertFalse(storage.hasVault())
    }

    @Test
    fun `hasVault returns true when prefs has vault key`() {
        every { prefs.contains("vault_data") } returns true
        assertTrue(storage.hasVault())
    }
}
