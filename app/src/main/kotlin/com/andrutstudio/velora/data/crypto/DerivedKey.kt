package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.AccountType

data class DerivedKey(
    val address: String,
    val publicKeyBytes: ByteArray,
    val derivationPath: String,
    val type: AccountType,
) {
    override fun equals(other: Any?) = other is DerivedKey && address == other.address
    override fun hashCode() = address.hashCode()
}
