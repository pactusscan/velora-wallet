package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.Curve
import wallet.core.jni.HDWallet
import wallet.core.jni.Hash
import wallet.core.jni.PrivateKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PactusKeyManager @Inject constructor() {

    fun deriveAccount(
        mnemonic: String,
        network: Network,
        type: AccountType,
        index: Int,
    ): DerivedKey {
        val coinType = CoinType.PACTUS
        val path = derivationPath(type, network.coinType, index)

        return HDWallet(mnemonic, "").use { wallet ->
            val privateKey = wallet.getKey(coinType, path)
            val publicKey = privateKey.getPublicKeyEd25519()
            
            // Manual Pactus address derivation: bech32m(hrp, 0x03 + ripemd160(blake2b_256(pubKey)))
            val pubKeyBytes = publicKey.data()
            val hash256 = Hash.blake2b(pubKeyBytes, 32)
            val hash160 = Hash.ripemd(hash256)
            
            val address = Bech32m.encodeWithType(network.hrp, 0x03, hash160)

            DerivedKey(
                address = address,
                publicKeyBytes = pubKeyBytes,
                derivationPath = path,
                type = type,
            )
        }
    }

    fun sign(rawTxBytes: ByteArray, privateKeyBytes: ByteArray, type: AccountType): ByteArray {
        // TWC Android JNI does not expose BLS12-381, so all account types are
        // signed with Ed25519. Currently only AccountType.ED25519 is exposed in
        // the UI, matching the pactus-wallet web reference (m/44'/21888'/3'/index).
        return PrivateKey(privateKeyBytes).sign(rawTxBytes, Curve.ED25519)
    }

    fun derivePrivateKeyBytes(
        mnemonic: String,
        network: Network,
        type: AccountType,
        index: Int,
    ): ByteArray {
        val path = derivationPath(type, 21888, index)
        return HDWallet(mnemonic, "").use { wallet ->
            wallet.getKey(CoinType.PACTUS, path).data().copyOf()
        }
    }

    fun addressFromPrivateKey(
        privateKeyBytes: ByteArray,
        network: Network = Network.MAINNET,
        type: AccountType = AccountType.ED25519,
    ): String {
        val pubKeyBytes = publicKeyBytesFromPrivateKey(privateKeyBytes, type)
        val hash256 = Hash.blake2b(pubKeyBytes, 32)
        val hash160 = Hash.ripemd(hash256)
        
        val addrType = when(type) {
            AccountType.BLS -> 0x02.toByte()
            AccountType.ED25519 -> 0x03.toByte()
        }
        
        return Bech32m.encodeWithType(network.hrp, addrType, hash160)
    }

    fun publicKeyBytesFromPrivateKey(
        privateKeyBytes: ByteArray,
        type: AccountType = AccountType.ED25519,
    ): ByteArray {
        return when(type) {
            AccountType.BLS -> {
                // TWC Android JNI does not expose BLS12-381. 
                // This will need a separate BLS library or a TWC update.
                throw UnsupportedOperationException("BLS support is coming soon")
            }
            AccountType.ED25519 -> PrivateKey(privateKeyBytes).getPublicKeyEd25519().data()
        }
    }

    private fun derivationPath(type: AccountType, coinType: Int, index: Int): String = when (type) {
        AccountType.ED25519 -> "m/44'/$coinType'/3'/$index'"
        AccountType.BLS -> "m/12381'/$coinType'/2'/$index'"
    }

    private inline fun <T> HDWallet.use(block: (HDWallet) -> T): T = try {
        block(this)
    } finally {
        // TWC manages native HDWallet lifetime via finalizers — nothing to free here.
    }
}
