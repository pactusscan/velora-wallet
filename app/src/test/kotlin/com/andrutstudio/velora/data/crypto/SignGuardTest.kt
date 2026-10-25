package com.andrutstudio.velora.data.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignGuardTest {

    // Builds "x<char>y" so control characters never appear literally in source.
    private fun withChar(code: Int) = "x" + code.toChar() + "y"

    @Test
    fun `accepts plain sign-in message`() {
        assertTrue(SignGuard.isSignableMessage("Sign in to PactusScan Play\nNonce: 8f3a2c"))
    }

    @Test
    fun `accepts tab newline and carriage return`() {
        assertTrue(SignGuard.isSignableMessage("line1\r\nline2\tend"))
    }

    @Test
    fun `accepts unicode text`() {
        assertTrue(SignGuard.isSignableMessage("Halo dunia — préviens ✓ 你好"))
    }

    @Test
    fun `accepts empty message`() {
        assertTrue(SignGuard.isSignableMessage(""))
    }

    @Test
    fun `rejects NUL and other C0 control characters`() {
        assertFalse(SignGuard.isSignableMessage(withChar(0x00))) // NUL
        assertFalse(SignGuard.isSignableMessage(withChar(0x01))) // SOH
        assertFalse(SignGuard.isSignableMessage(withChar(0x1B))) // ESC
        assertFalse(SignGuard.isSignableMessage(withChar(0x1F))) // US
    }

    @Test
    fun `rejects DEL and C1 control characters`() {
        assertFalse(SignGuard.isSignableMessage(withChar(0x7F))) // DEL
        assertFalse(SignGuard.isSignableMessage(withChar(0x85))) // NEL
        assertFalse(SignGuard.isSignableMessage(withChar(0x9F))) // APC
    }

    @Test
    fun `rejects transaction-like binary payload`() {
        // Unsigned transaction bytes always contain control bytes (version,
        // lock-time varints, etc.), which is exactly what the guard relies on.
        val txLikeBytes = byteArrayOf(0x01, 0x02, 0x00, 0x2a, 0x1f, 0x64)
        assertFalse(SignGuard.isSignableMessage(String(txLikeBytes, Charsets.ISO_8859_1)))
    }

    @Test
    fun `assertSignableMessage throws on binary`() {
        try {
            SignGuard.assertSignableMessage(withChar(0x00))
            throw AssertionError("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
