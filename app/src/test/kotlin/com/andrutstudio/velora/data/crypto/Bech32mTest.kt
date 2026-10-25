package com.andrutstudio.velora.data.crypto

import org.junit.Assert.*
import org.junit.Test

class Bech32mTest {

    @Test
    fun `decode user sample private key`() {
        val encoded = "SECRET1R04K42VKVWS3K4U7S0YREUTLU7ZPSJZKD7RR0T767X68R97ZGHSGSXZ2QQ4"
        val (hrp, type, data) = Bech32m.decode(encoded)
        
        assertEquals("secret", hrp)
        assertEquals(3.toByte(), type) // Ed25519 signature type
        assertEquals(32, data.size)
    }

    @Test
    fun `decode reference test case`() {
        val encoded = "SECRET1PRDRK0K7GFA86RSJMXSHTTNPJ49F4R873HUT396LTT6KY2Q4JU30Q5NTC44"
        val (hrp, type, data) = Bech32m.decode(encoded)
        
        assertEquals("secret", hrp)
        assertEquals(1.toByte(), type) // BLS12-381 private key type
        assertEquals(32, data.size)
        
        // Verified independently against a reference bech32m implementation
        val expected = byteArrayOf(
            0x1b.toByte(), 0x47.toByte(), 0x67.toByte(), 0xdb.toByte(), 0xc8.toByte(), 0x4f.toByte(), 0x4f.toByte(), 0xa1.toByte(), 0xc2.toByte(), 0x5b.toByte(), 0x34.toByte(), 0x2e.toByte(), 0xb5.toByte(), 0xcc.toByte(), 0x32.toByte(), 0xa9.toByte(),
            0x53.toByte(), 0x51.toByte(), 0x9f.toByte(), 0xd1.toByte(), 0xbf.toByte(), 0x17.toByte(), 0x12.toByte(), 0xeb.toByte(), 0xeb.toByte(), 0x5e.toByte(), 0xac.toByte(), 0x45.toByte(), 0x02.toByte(), 0xb2.toByte(), 0xe4.toByte(), 0x5e.toByte(),
        )
        assertArrayEquals(expected, data)
    }
}
