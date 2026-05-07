package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

class ValidateMnemonicUseCase @Inject constructor(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(mnemonic: String): Boolean =
        repository.validateMnemonic(mnemonic)
}
