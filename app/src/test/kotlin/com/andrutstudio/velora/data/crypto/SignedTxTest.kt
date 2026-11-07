package com.andrutstudio.velora.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SignedTxTest {
    private val sig = ByteArray(64) { 0x11 }
    private val pub = ByteArray(32) { 0x22 }

    @Test
    fun `plain transaction drops the not-signed bit to 0x00`() {
        val raw = byteArrayOf(0x02, 0x01, 0x7f, 0x00)
        val signed = SignedTx.assemble(raw, sig, pub)
        assertEquals(0x00, signed[0].toInt())
        assertArrayEquals(byteArrayOf(0x01, 0x7f, 0x00), signed.copyOfRange(1, 4))
        assertEquals(1 + 3 + 64 + 32, signed.size)
    }

    @Test
    fun `delegated bond keeps the delegation flag 0x04`() {
        val raw = byteArrayOf(0x06, 0x01, 0x05)
        assertEquals(0x04, SignedTx.assemble(raw, sig, pub)[0].toInt())
    }

    @Test
    fun `sign bytes skip the flags byte`() {
        assertArrayEquals(byteArrayOf(0x01, 0x05), SignedTx.signBytes(byteArrayOf(0x06, 0x01, 0x05)))
    }
}
