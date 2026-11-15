package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keys of the three Pactus account types, byte-compatible with the Go node and
 * the Velora browser extension:
 * - ED25519 (pc1r): SLIP-0010, m/44'/{coin}'/3'/{index}', Ed25519 signatures.
 * - BLS (pc1z): PIP-11, m/12381'/{coin}'/2'/{index} (index not hardened),
 *   signature on G1 (48 B), public key on G2 (96 B), see [Bls12381].
 * - SECP256K1 (pc1y): BIP32, m/44'/{coin}'/4'/{index}', RFC6979 low-S ECDSA
 *   over Blake2b-256 of the message, signature r || s (64 B), public key 33 B.
 * Address = bech32m(hrp, type || ripemd160(blake2b_256(publicKey))).
 */
@Singleton
class PactusKeyManager @Inject constructor() {

    fun deriveAccount(mnemonic: String, network: Network, type: AccountType, index: Int): DerivedKey {
        val privateKey = derivePrivateKeyBytes(mnemonic, network, type, index)
        try {
            val publicKey = publicKeyBytesFromPrivateKey(privateKey, type)
            return DerivedKey(
                address = addressFromPublicKey(publicKey, network, type),
                publicKeyBytes = publicKey,
                derivationPath = derivationPath(type, network.coinType, index),
                type = type,
            )
        } finally {
            privateKey.fill(0)
        }
    }

    fun derivePrivateKeyBytes(mnemonic: String, network: Network, type: AccountType, index: Int): ByteArray {
        val seed = Bip39.toSeed(mnemonic)
        try {
            val coin = network.coinType
            return when (type) {
                AccountType.ED25519 -> HdKeys.slip10Ed25519(
                    seed, listOf(HdKeys.hardened(44), HdKeys.hardened(coin), HdKeys.hardened(3), HdKeys.hardened(index)),
                )
                AccountType.BLS -> HdKeys.pip11Bls(
                    seed, listOf(HdKeys.hardened(12381), HdKeys.hardened(coin), HdKeys.hardened(2), index.toLong()),
                )
                AccountType.SECP256K1 -> HdKeys.bip32Secp256k1(
                    seed, listOf(HdKeys.hardened(44), HdKeys.hardened(coin), HdKeys.hardened(4), HdKeys.hardened(index)),
                )
            }
        } finally {
            seed.fill(0)
        }
    }

    fun sign(message: ByteArray, privateKeyBytes: ByteArray, type: AccountType): ByteArray = when (type) {
        AccountType.ED25519 -> Ed25519Signer().run {
            init(true, Ed25519PrivateKeyParameters(privateKeyBytes, 0))
            update(message, 0, message.size)
            generateSignature()
        }
        AccountType.BLS -> Bls12381.sign(message, privateKeyBytes)
        AccountType.SECP256K1 -> {
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            signer.init(true, ECPrivateKeyParameters(BigInteger(1, privateKeyBytes), SECP256K1))
            val (r, s) = signer.generateSignature(blake2b256(message))
            val lowS = if (s > SECP256K1.n.shiftRight(1)) SECP256K1.n.subtract(s) else s
            Bls12381.toBytes(r, 32) + Bls12381.toBytes(lowS, 32)
        }
    }

    fun publicKeyBytesFromPrivateKey(privateKeyBytes: ByteArray, type: AccountType): ByteArray = when (type) {
        AccountType.ED25519 -> Ed25519PrivateKeyParameters(privateKeyBytes, 0).generatePublicKey().encoded
        AccountType.BLS -> Bls12381.publicKey(privateKeyBytes)
        AccountType.SECP256K1 -> {
            val d = BigInteger(1, privateKeyBytes)
            require(d.signum() != 0 && d < SECP256K1.n) { "Invalid secp256k1 private key" }
            SECP256K1.g.multiply(d).normalize().getEncoded(true)
        }
    }

    fun addressFromPrivateKey(privateKeyBytes: ByteArray, network: Network, type: AccountType): String =
        addressFromPublicKey(publicKeyBytesFromPrivateKey(privateKeyBytes, type), network, type)

    fun addressFromPublicKey(publicKey: ByteArray, network: Network, type: AccountType): String =
        Bech32m.encodeWithType(network.hrp, type.addressType, ripemd160(blake2b256(publicKey)))

    private fun derivationPath(type: AccountType, coin: Int, index: Int): String = when (type) {
        AccountType.ED25519 -> "m/44'/$coin'/3'/$index'"
        AccountType.BLS -> "m/12381'/$coin'/2'/$index"
        AccountType.SECP256K1 -> "m/44'/$coin'/4'/$index'"
    }

    companion object {
        private val SECP256K1 = CustomNamedCurves.getByName("secp256k1").let {
            ECDomainParameters(it.curve, it.g, it.n, it.h)
        }

        fun blake2b256(data: ByteArray): ByteArray = Blake2bDigest(256).run {
            update(data, 0, data.size)
            ByteArray(32).also { doFinal(it, 0) }
        }

        fun ripemd160(data: ByteArray): ByteArray = RIPEMD160Digest().run {
            update(data, 0, data.size)
            ByteArray(20).also { doFinal(it, 0) }
        }

        /**
         * Parses a private key as typed or stored: a bech32m secret key
         * (SECRET1R… Ed25519, SECRET1P… BLS, SECRET1Y… Secp256k1, tsecret1… on
         * testnet) or raw hex, which carries no type and uses [hexType].
         */
        fun parsePrivateKey(input: String, hexType: AccountType): Pair<AccountType, ByteArray> {
            val trimmed = input.trim().removePrefix("0x")
            if (trimmed.length == 64 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                return hexType to trimmed.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            }
            val (hrp, signatureType, data) = Bech32m.decode(trimmed)
            require(hrp == "secret" || hrp == "tsecret") { "Unknown secret key prefix: $hrp" }
            require(data.size == 32) { "Invalid secret key length" }
            val type = AccountType.fromSignatureType(signatureType.toInt())
                ?: throw IllegalArgumentException("Unsupported secret key type: $signatureType")
            return type to data
        }
    }
}
