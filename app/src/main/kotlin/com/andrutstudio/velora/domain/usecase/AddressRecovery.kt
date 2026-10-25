package com.andrutstudio.velora.domain.usecase

/**
 * PIP-41 address recovery (https://pips.pactus.org/PIPs/pip-41).
 *
 * When restoring a wallet from a mnemonic, previously used addresses are
 * rediscovered by deriving addresses in order and checking each against the
 * blockchain. Scanning stops after [GAP_LIMIT] consecutive inactive addresses,
 * and every address up to the last active one is recovered.
 *
 * Invariant: the returned count equals (index of the last active address + 1),
 * and is never less than 1 — the first account is always restored.
 */
object AddressRecovery {

    const val GAP_LIMIT = 8

    /**
     * Runs the PIP-41 discovery loop. [isActive] reports whether the address at
     * a derivation index is indexed in the blockchain; it should throw on
     * transport failures so a network outage aborts discovery instead of being
     * mistaken for a series of inactive addresses.
     */
    suspend fun discoverRecoveredCount(isActive: suspend (index: Int) -> Boolean): Int {
        var recoveredCount = 1
        var inactiveCount = 0
        var index = 0
        while (true) {
            if (isActive(index)) {
                recoveredCount += inactiveCount
                inactiveCount = 1
            } else {
                inactiveCount += 1
                if (inactiveCount > GAP_LIMIT) break
            }
            index += 1
        }
        return recoveredCount
    }
}
