package com.andrutstudio.velora.data.local

import com.andrutstudio.velora.data.local.model.VaultAccount
import com.andrutstudio.velora.data.local.model.nextDerivationIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class NextDerivationIndexTest {

    private fun accounts(vararg indexes: Int, type: String = "ED25519") = indexes.map {
        VaultAccount(address = "a$it", label = "", type = type, derivationPath = "", derivationIndex = it)
    }

    @Test
    fun `continues after the last account when there is no gap`() {
        assertEquals(3, accounts(0, 1, 2).nextDerivationIndex("ED25519"))
    }

    @Test
    fun `fills the gap a deleted account left`() {
        assertEquals(1, accounts(0, 2).nextDerivationIndex("ED25519"))
        assertEquals(0, accounts(1, 2).nextDerivationIndex("ED25519"))
    }

    @Test
    fun `counts each account type on its own`() {
        val mixed = accounts(0, 1) + accounts(0, type = "BLS")
        assertEquals(1, mixed.nextDerivationIndex("BLS"))
    }
}
