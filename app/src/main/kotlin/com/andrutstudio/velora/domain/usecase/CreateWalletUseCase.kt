package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

class CreateWalletUseCase @Inject constructor(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(
        name: String,
        mnemonic: String,
        password: String,
        isRestore: Boolean,
    ): Wallet = if (isRestore) {
        repository.restoreWallet(name, mnemonic, password)
    } else {
        repository.createWallet(name, mnemonic, password)
    }
}
