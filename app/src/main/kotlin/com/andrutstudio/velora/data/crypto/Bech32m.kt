package com.andrutstudio.velora.data.crypto

import java.io.ByteArrayOutputStream

/**
 * Bech32m implementation for Pactus.
 * Based on BIP-0173 and BIP-0350.
 */
object Bech32m {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val M = 0x2bc830a3

    fun encode(hrp: String, data: ByteArray): String {
        val converted = convertBits(data, 8, 5, true)
        return encode5Bit(hrp, converted)
    }

    fun encodeWithType(hrp: String, type: Byte, data: ByteArray): String {
        val converted = convertBits(data, 8, 5, true)
        val combined = ByteArray(converted.size + 1)
        combined[0] = type
        System.arraycopy(converted, 0, combined, 1, converted.size)
        return encode5Bit(hrp, combined)
    }

    private fun encode5Bit(hrp: String, data: ByteArray): String {
        val checksum = createChecksum(hrp, data)
        val combined = data + checksum
        val result = StringBuilder(hrp.length + 1 + combined.size)
        result.append(hrp.lowercase())
        result.append('1')
        for (b in combined) {
            result.append(CHARSET[b.toInt() and 0x1f])
        }
        return result.toString()
    }

    fun decode(bechString: String): Triple<String, Byte, ByteArray> {
        val bech = bechString.lowercase()
        val pos = bech.lastIndexOf('1')
        if (pos < 1 || pos + 7 > bech.length) throw Exception("Invalid Bech32m format")
        val hrp = bech.substring(0, pos)
        val data = ByteArray(bech.length - pos - 1)
        for (i in data.indices) {
            val charPos = CHARSET.indexOf(bech[pos + 1 + i])
            if (charPos == -1) throw Exception("Invalid character in Bech32m")
            data[i] = charPos.toByte()
        }
        if (polymod(hrpExpand(hrp) + data) != M) throw Exception("Invalid Bech32m checksum")
        
        val type = data[0]
        val converted = convertBits(data.copyOfRange(1, data.size - 6), 5, 8, false)
        return Triple(hrp, type, converted)
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        for (v in values) {
            val top = chk ushr 25
            chk = (chk and 0x1ffffff shl 5) xor (v.toInt() and 0xff)
            if (top and 1 != 0) chk = chk xor 0x3b6a57b2
            if (top and 2 != 0) chk = chk xor 0x26508e6d
            if (top and 4 != 0) chk = chk xor 0x1ea119fa
            if (top and 8 != 0) chk = chk xor 0x3d4233dd
            if (top and 16 != 0) chk = chk xor 0x2a1462b3
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val res = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            res[i] = (hrp[i].code ushr 5).toByte()
            res[i + hrp.length + 1] = (hrp[i].code and 31).toByte()
        }
        res[hrp.length] = 0
        return res
    }

    private fun createChecksum(hrp: String, data: ByteArray): ByteArray {
        val values = hrpExpand(hrp) + data + ByteArray(6)
        val poly = polymod(values) xor M
        val res = ByteArray(6)
        for (i in 0..5) {
            res[i] = ((poly ushr (5 * (5 - i))) and 31).toByte()
        }
        return res
    }

    private fun convertBits(data: ByteArray, from: Int, to: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val out = ByteArrayOutputStream()
        val maxv = (1 shl to) - 1
        for (value in data) {
            val b = value.toInt() and 0xff
            acc = (acc shl from) or b
            bits += from
            while (bits >= to) {
                bits -= to
                out.write((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) {
                out.write((acc shl (to - bits)) and maxv)
            }
        } else if (bits >= from || (acc shl (to - bits)) and maxv != 0) {
            throw Exception("Invalid data")
        }
        return out.toByteArray()
    }
}
