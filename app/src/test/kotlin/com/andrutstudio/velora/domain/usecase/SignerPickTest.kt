package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test

class SignerPickTest {

    private val bls = Account("pc1zbls0", "BLS", AccountType.BLS, 0)
    private val ed0 = Account("pc1red0", "Account 1", AccountType.ED25519, 0)
    private val ed1 = Account("pc1red1", "Account 2", AccountType.ED25519, 1)
    private val accounts = listOf(bls, ed0, ed1)

    @Test
    fun `candidates keep only Ed25519 accounts in order`() {
        assertEquals(listOf(ed0, ed1), SignerPick.candidates(accounts))
    }

    @Test
    fun `requested Ed25519 address is found`() {
        assertEquals(2, SignerPick.requestedSignerIndex(accounts, "pc1red1"))
    }

    @Test
    fun `requested address is trimmed`() {
        assertEquals(1, SignerPick.requestedSignerIndex(accounts, "  pc1red0 "))
    }

    @Test
    fun `requested address not held is -1`() {
        assertEquals(-1, SignerPick.requestedSignerIndex(accounts, "pc1rother"))
    }

    @Test
    fun `requested BLS address cannot sign`() {
        assertEquals(-1, SignerPick.requestedSignerIndex(accounts, "pc1zbls0"))
    }

    @Test
    fun `blank request has no preference`() {
        assertEquals(-1, SignerPick.requestedSignerIndex(accounts, " "))
    }

    @Test
    fun `requested address wins over the sheet choice`() {
        assertEquals(1, SignerPick.resolveSignerIndex(accounts, "pc1red0", chosen = 2))
    }

    @Test
    fun `requested address not held never falls back to another key`() {
        assertEquals(-1, SignerPick.resolveSignerIndex(accounts, "pc1rother", chosen = 1))
    }

    @Test
    fun `chosen Ed25519 account is used without a request`() {
        assertEquals(2, SignerPick.resolveSignerIndex(accounts, "", chosen = 2))
    }

    @Test
    fun `chosen BLS or out of range account falls back to first Ed25519`() {
        assertEquals(1, SignerPick.resolveSignerIndex(accounts, "", chosen = 0))
        assertEquals(1, SignerPick.resolveSignerIndex(accounts, "", chosen = 9))
        assertEquals(1, SignerPick.resolveSignerIndex(accounts, "", chosen = null))
    }

    @Test
    fun `no Ed25519 account means nothing can sign`() {
        assertEquals(-1, SignerPick.resolveSignerIndex(listOf(bls), "", chosen = null))
    }
}
