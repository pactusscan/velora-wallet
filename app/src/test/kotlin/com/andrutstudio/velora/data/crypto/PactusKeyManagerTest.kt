package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Official vectors from pactus-project/pactus main (crypto/ed25519, crypto/bls,
 * crypto/secp256k1 TestEncoding; bls/hdkeychain and secp256k1/hdkeychain
 * derivation tests; wallet/vault TestAddressRecovery), the same ones the
 * browser extension checks.
 */
class PactusKeyManagerTest {

    private val km = PactusKeyManager()
    private val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon cactus"
    private val key00to1f = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
    private val pactus = "pactus".toByteArray()

    @Test
    fun `ed25519 key, address and signature`() {
        assertEquals("03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8",
            km.publicKeyBytesFromPrivateKey(key00to1f, AccountType.ED25519).toHex())
        assertEquals("pc1rj65g93q7lpdq0366vst22l7va9d26j3l2vr0em",
            km.addressFromPrivateKey(key00to1f, Network.MAINNET, AccountType.ED25519))
        assertEquals("1fc2c800499342d08242db9c3eb654027cb7b821e6af9ede56dfdb67e824f15b" +
            "ddb419d2db3fd5aaf3ef1a9ebb9a9deb749380f0d6a110cbe95319fe9f794305",
            km.sign(pactus, key00to1f, AccountType.ED25519).toHex())
    }

    @Test
    fun `bls key, address and signature`() {
        val pub = km.publicKeyBytesFromPrivateKey(key00to1f, AccountType.BLS)
        assertEquals(96, pub.size)
        assertEquals("pc1zcs9ezsmn6n7fc8jxzxks4n2lyw4ltzsd9wu6hw",
            km.addressFromPrivateKey(key00to1f, Network.MAINNET, AccountType.BLS))
        assertEquals("8bdda74336efdf43b428a3811d3d6867a19e20889c91261b02a6b950b130f5bb22621394667c27660bfed2a8719d9c52",
            km.sign(pactus, key00to1f, AccountType.BLS).toHex())
    }

    @Test
    fun `secp256k1 key, address and signature`() {
        assertEquals("036d6caac248af96f6afa7f904f550253a0f3ef3f5aa2fe6838a95b216691468e2",
            km.publicKeyBytesFromPrivateKey(key00to1f, AccountType.SECP256K1).toHex())
        assertEquals("pc1y90qakls8jlz9hyvdcsqsj0yj2lrqz26vqu7l0z",
            km.addressFromPrivateKey(key00to1f, Network.MAINNET, AccountType.SECP256K1))
        assertEquals("c86779676d217b04979434e5bd37eddd02b671e9a54b48d3a812c7862dcb5396" +
            "31bb5e8459fec007608f50ea5661e0a5215aac976705404cb4f36ee623e63199",
            km.sign(pactus, key00to1f, AccountType.SECP256K1).toHex())
    }

    @Test
    fun `accounts from the test mnemonic`() {
        fun address(type: AccountType, index: Int) = km.deriveAccount(mnemonic, Network.MAINNET, type, index).address
        assertEquals("pc1rcx9x55nfme5juwdgxd2ksjdcmhvmvkrygmxpa3", address(AccountType.ED25519, 0))
        assertEquals("pc1r7aynw9urvh66ktr3fte2gskjjnxzruflkgde94", address(AccountType.ED25519, 1))
        assertEquals("pc1z0m0vw8sjfgv7f2zgq2hfxutg8rwn7gpffhe8tf", address(AccountType.BLS, 0))
        assertEquals("pc1z4xuja689hg2434yhr32clhn97x6afw58a5n9ns", address(AccountType.BLS, 1))
        assertEquals("pc1zydjhrq06ngg6nwqs8n8jkyw6u58qlqc5cqqxht", address(AccountType.BLS, 7))
        assertTrue(address(AccountType.SECP256K1, 0).startsWith("pc1y"))
        assertEquals("m/12381'/21888'/2'/3", km.deriveAccount(mnemonic, Network.MAINNET, AccountType.BLS, 3).derivationPath)
        assertEquals("m/44'/21777'/4'/0'", km.deriveAccount(mnemonic, Network.TESTNET, AccountType.SECP256K1, 0).derivationPath)
    }

    @Test
    fun `pip-11 bls derivation`() {
        val seed = hex("000102030405060708090a0b0c0d0e0f")
        val h = HdKeys.HARDENED
        assertEquals("4f55e31ee1c4f58af0840fd3f5e635fd6c07eacd14283c45d7d43729003abb84", HdKeys.pip11Bls(seed, listOf()).toHex())
        assertEquals("555422bcbffd1d55eea6f87a924ba5d046bb60e2bffe2182daf78bab6a6e179f", HdKeys.pip11Bls(seed, listOf(h, 1)).toHex())
        assertEquals("2b01ef29730eb62c7114621d9d28ad77cf33f2434572a2bf9b73f1e502fea770",
            HdKeys.pip11Bls(seed, listOf(h, 1, 2 + h, 2, 1000000000)).toHex())
        assertEquals("41c9e07822b092a93fd6797396338c3ada4170cc81829fdfce6b5d34bd5e7ec7",
            HdKeys.blsKeyGen(hex("3141592653589793238462643383279502884197169399375105820974944592")).toHex())
    }

    @Test
    fun `bip32 secp256k1 derivation`() {
        val seed = hex("000102030405060708090a0b0c0d0e0f")
        val h = HdKeys.HARDENED
        assertEquals("e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35", HdKeys.bip32Secp256k1(seed, listOf()).toHex())
        assertEquals("cbce0d719ecf7431d88e6a89fa1483e02e35092af60c042b1df2ff59fa424dca",
            HdKeys.bip32Secp256k1(seed, listOf(h, 1, 2 + h)).toHex())
        assertEquals("471b76e389e528d6de6d816857e012c5455051cad6660850e58372a6c3e6e7c8",
            HdKeys.bip32Secp256k1(seed, listOf(h, 1, 2 + h, 2, 1000000000)).toHex())
    }

    @Test
    fun `bip39 mnemonics`() {
        assertTrue(Bip39.isValid(mnemonic))
        assertFalse(Bip39.isValid(mnemonic.replace("cactus", "abandon")))
        assertFalse(Bip39.isValid("not a mnemonic"))
        val generated = Bip39.generate(24)
        assertEquals(24, generated.split(" ").size)
        assertTrue(Bip39.isValid(generated))
        assertTrue(Bip39.isValid(Bip39.generate(12)))
    }

    @Test
    fun `secret key strings carry the key type`() {
        val (type, bytes) = PactusKeyManager.parsePrivateKey(
            "SECRET1YQQQSYQCYQ5RQWZQFPG9SCRGWPUGPZYSNZS23V9CCRYDPK8QARC0SPVXU8Z", AccountType.ED25519)
        assertEquals(AccountType.SECP256K1, type)
        assertEquals(key00to1f.toHex(), bytes.toHex())
        assertEquals(AccountType.BLS, PactusKeyManager.parsePrivateKey(
            "SECRET1PQQQSYQCYQ5RQWZQFPG9SCRGWPUGPZYSNZS23V9CCRYDPK8QARC0SEZYD4L", AccountType.ED25519).first)
        assertEquals(AccountType.BLS, PactusKeyManager.parsePrivateKey(key00to1f.toHex(), AccountType.BLS).first)
    }

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
