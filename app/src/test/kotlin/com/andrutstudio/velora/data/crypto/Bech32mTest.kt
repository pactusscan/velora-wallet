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
        assertEquals(3.toByte(), type)
        assertEquals(32, data.size)
        
        val expected = byteArrayOf(
            0x96.toByte(), 0xa0.toByte(), 0xb9.toByte(), 0xaf.toByte(), 0xbe.toByte(), 0x24.toByte(), 0x42.toByte(), 0xa0.toByte(), 0x7c.toByte(), 0x09.toByte(), 0x3f.toByte(), 0x74.toByte(), 0x0c.toByte(), 0x19.toByte(), 0xf4.toByte(), 0x4a.toByte(),
            0xaf.toByte(), 0x45.toByte(), 0xa6.toByte(), 0x79.toByte(), 0xa0.toByte(), 0xe1.toByte(), 0x77.toByte(), 0xeb.toByte(), 0x56.toByte(), 0x16.toByte(), 0xf3.toByte(), 0x57.toByte(), 0x31.toByte(), 0x26.toByte(), 0xb6.toByte(), 0x7c.toByte(),
        )
        assertArrayEquals(expected, data)
    }
}
