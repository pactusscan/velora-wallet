package com.andrutstudio.velora.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import javax.inject.Inject

class RefreshBalancesUseCase @Inject constructor(
    private val blockchainRepository: BlockchainRepository,
) {
    /** Fetches balances for all accounts in parallel. Returns address → Amount map. */
    suspend operator fun invoke(wallet: Wallet): Map<String, Amount> = coroutineScope {
        wallet.accounts
            .map { account ->
                async { account.address to blockchainRepository.getBalance(account.address) }
            }
            .awaitAll()
            .toMap()
    }
}
