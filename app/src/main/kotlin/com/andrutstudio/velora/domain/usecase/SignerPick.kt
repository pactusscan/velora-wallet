package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType

/**
 * Chooses which account signs a dApp message. A dApp may name the address it
 * needs a signature from (for example to prove ownership of that address);
 * otherwise the user picks in the approval sheet, defaulting to the first
 * account. Only Ed25519 accounts can sign messages.
 *
 * Mirrors signerPick.ts in the Velora browser extension.
 */
object SignerPick {

    /** Accounts that can sign a message, in wallet order. */
    fun candidates(accounts: List<Account>): List<Account> =
        accounts.filter { it.type == AccountType.ED25519 }

    /**
     * Index of the account a dApp asked for, or -1 when the wallet does not
     * hold it as an Ed25519 account. A blank request means "no preference".
     */
    fun requestedSignerIndex(accounts: List<Account>, requested: String): Int {
        val want = requested.trim()
        if (want.isEmpty()) return -1
        return accounts.indexOfFirst { it.address == want && it.type == AccountType.ED25519 }
    }

    /**
     * Final signer index. A requested address always wins over the sheet's
     * choice, so a page is never answered with a different key than the one it
     * showed the user. Returns -1 when nothing can sign.
     */
    fun resolveSignerIndex(accounts: List<Account>, requested: String, chosen: Int?): Int {
        if (requested.isNotBlank()) return requestedSignerIndex(accounts, requested)
        if (chosen != null && accounts.getOrNull(chosen)?.type == AccountType.ED25519) return chosen
        return accounts.indexOfFirst { it.type == AccountType.ED25519 }
    }
}
