package com.andrutstudio.velora.data.crypto

import org.bouncycastle.crypto.ec.CustomNamedCurves
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** BIP39 mnemonics with the English wordlist (app resources). */
object Bip39 {
    private val words: List<String> by lazy {
        val stream = Bip39::class.java.getResourceAsStream("/bip39/english.txt")
            ?: error("BIP39 wordlist missing")
        stream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .also { check(it.size == 2048) { "BIP39 wordlist must have 2048 words" } }
    }

    fun generate(wordCount: Int): String {
        require(wordCount in setOf(12, 15, 18, 21, 24)) { "Unsupported word count" }
        val entropy = ByteArray(wordCount * 4 / 3).also { SecureRandom().nextBytes(it) }
        return fromEntropy(entropy)
    }

    fun fromEntropy(entropy: ByteArray): String {
        val checksumBits = entropy.size * 8 / 32
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        val bits = BigInteger(1, entropy + hash[0]).shiftRight(8 - checksumBits)
        val total = entropy.size * 8 + checksumBits
        return (0 until total / 11).joinToString(" ") { i ->
            words[bits.shiftRight(total - 11 * (i + 1)).toInt() and 0x7ff]
        }
    }

    fun isValid(mnemonic: String): Boolean {
        val parts = normalize(mnemonic).split(" ")
        if (parts.size !in setOf(12, 15, 18, 21, 24)) return false
        val indexes = parts.map { words.indexOf(it).takeIf { i -> i >= 0 } ?: return false }
        val total = parts.size * 11
        val checksumBits = total / 33
        val bits = indexes.fold(BigInteger.ZERO) { acc, i -> acc.shiftLeft(11).or(BigInteger.valueOf(i.toLong())) }
        val entropy = Bls12381.toBytes(bits.shiftRight(checksumBits), (total - checksumBits) / 8)
        return fromEntropy(entropy) == parts.joinToString(" ")
    }

    /** Seed with an empty passphrase, as every Pactus wallet uses. */
    fun toSeed(mnemonic: String): ByteArray {
        val spec = PBEKeySpec(normalize(mnemonic).toCharArray(), "mnemonic".toByteArray(), 2048, 512)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
    }

    private fun normalize(mnemonic: String): String =
        Normalizer.normalize(mnemonic.trim().lowercase(), Normalizer.Form.NFKD).split(Regex("\\s+")).joinToString(" ")
}

/** Hierarchical key derivation for the three Pactus account key types. */
object HdKeys {
    const val HARDENED = 0x80000000L

    fun hardened(index: Int): Long = index.toLong() + HARDENED

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA512").run {
            init(SecretKeySpec(key, "HmacSHA512"))
            doFinal(data)
        }

    private fun ser32(index: Long) = byteArrayOf(
        (index shr 24).toByte(), (index shr 16).toByte(), (index shr 8).toByte(), index.toByte(),
    )

    /** SLIP-0010 Ed25519, hardened only. Returns the 32-byte private key. */
    fun slip10Ed25519(seed: ByteArray, path: List<Long>): ByteArray {
        var i = hmacSha512("ed25519 seed".toByteArray(), seed)
        for (index in path) {
            require(index >= HARDENED) { "SLIP-0010 Ed25519 only supports hardened derivation" }
            i = hmacSha512(i.copyOfRange(32, 64), byteArrayOf(0) + i.copyOfRange(0, 32) + ser32(index))
        }
        return i.copyOfRange(0, 32)
    }

    /** BIP32 secp256k1 private derivation (master key "Bitcoin seed"). */
    fun bip32Secp256k1(seed: ByteArray, path: List<Long>): ByteArray {
        val curve = CustomNamedCurves.getByName("secp256k1")
        val n = curve.n
        var i = hmacSha512("Bitcoin seed".toByteArray(), seed)
        var key = i.copyOfRange(0, 32)
        for (index in path) {
            val data = if (index >= HARDENED) {
                byteArrayOf(0) + key + ser32(index)
            } else {
                curve.g.multiply(BigInteger(1, key)).normalize().getEncoded(true) + ser32(index)
            }
            i = hmacSha512(i.copyOfRange(32, 64), data)
            val il = BigInteger(1, i.copyOfRange(0, 32))
            val child = il.add(BigInteger(1, key)).mod(n)
            // Probability below 2^-127; BIP32 says to use the next index.
            check(il < n && child.signum() != 0) { "Invalid child key, use the next index" }
            key = Bls12381.toBytes(child, 32)
        }
        return key
    }

    /**
     * PIP-11 BLS derivation with the public key on G2 (accounts), as
     * crypto/bls/hdkeychain in the Go node: master key "BLS12381 seed" and
     * BLS KeyGen for the master scalar.
     */
    fun pip11Bls(seed: ByteArray, path: List<Long>): ByteArray {
        require(seed.size in 16..64) { "Invalid seed length" }
        var i = hmacSha512("BLS12381 seed".toByteArray(), seed)
        var key = blsKeyGen(i.copyOfRange(0, 32))
        var chainCode = i.copyOfRange(32, 64)
        for (index in path) {
            val indexBytes = ser32(index)
            var data = if (index >= HARDENED) {
                byteArrayOf(0) + key + indexBytes
            } else {
                Bls12381.publicKey(key) + indexBytes
            }
            while (true) {
                // Retries keep the parent chain code as the HMAC key, as the Go code does
                i = hmacSha512(chainCode, data)
                val il = BigInteger(1, i.copyOfRange(0, 32))
                val childChainCode = i.copyOfRange(32, 64)
                if (il.signum() != 0 && il < Bls12381.R) {
                    key = Bls12381.toBytes(il.add(BigInteger(1, key)).mod(Bls12381.R), 32)
                    chainCode = childChainCode
                    break
                }
                data = byteArrayOf(1) + childChainCode + indexBytes
            }
        }
        return key
    }

    /** BLS KeyGen (draft-irtf-cfrg-bls-signature-04 2.3) with empty key info, as bls.KeyGen. */
    fun blsKeyGen(ikm: ByteArray): ByteArray {
        require(ikm.size >= 32) { "ikm is too short" }
        val sha256 = { data: ByteArray -> MessageDigest.getInstance("SHA-256").digest(data) }
        val hmac = { key: ByteArray, data: ByteArray ->
            Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); doFinal(data) }
        }
        val secret = ikm + byteArrayOf(0)
        val info = byteArrayOf(0, 48) // key info (empty) || I2OSP(48, 2)
        var salt = "BLS-SIG-KEYGEN-SALT-".toByteArray()
        var num = BigInteger.ZERO
        while (num.signum() == 0) {
            salt = sha256(salt)
            val prk = hmac(salt, secret)
            // HKDF-Expand to 48 bytes: T(1) || T(2)
            val t1 = hmac(prk, info + byteArrayOf(1))
            val t2 = hmac(prk, t1 + info + byteArrayOf(2))
            num = BigInteger(1, (t1 + t2).copyOf(48)).mod(Bls12381.R)
        }
        return Bls12381.toBytes(num, 32)
    }
}
