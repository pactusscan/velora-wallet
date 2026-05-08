package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

class GenerateMnemonicUseCase @Inject constructor(
    private val repository: WalletRepository,
) {
    suspend operator fun invoke(wordCount: Int = 12): String =
        repository.generateMnemonic(wordCount)
}
