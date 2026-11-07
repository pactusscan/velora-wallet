package com.andrutstudio.velora.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DelegationRulesTest {
    private val validator = "pc1p8fmz4l8prrth4zc35yqgxl8tm8e9uw0vg5a6ae"

    @Test
    fun `percent to share rounds down to a multiple of 32`() {
        assertEquals(200_000_000L, DelegationRules.percentToShare(20.0))
        assertEquals(700_000_000L, DelegationRules.percentToShare(70.0))
        assertEquals(0L, DelegationRules.percentToShare(12.3456789) % 32)
    }

    @Test
    fun `share must be 0 to 0_7 PAC and a multiple of 32`() {
        DelegationRules.checkShare(0)
        DelegationRules.checkShare(700_000_000L)
        assertThrows(IllegalArgumentException::class.java) { DelegationRules.checkShare(700_000_032L) }
        assertThrows(IllegalArgumentException::class.java) { DelegationRules.checkShare(33) }
    }

    @Test
    fun `validator addresses follow the network`() {
        assertTrue(DelegationRules.isValidatorAddress(validator, Network.MAINNET))
        assertFalse(DelegationRules.isValidatorAddress(validator, Network.TESTNET))
        assertTrue(DelegationRules.isValidatorAddress("tpc1pd0rk7e5uhu2kzuj2xatvq4pw2272g7rj7lar6n", Network.TESTNET))
        assertFalse(DelegationRules.isValidatorAddress("pc1ryhndnylqrtze0r8qne3tr37w3lled25f2j77cg", Network.MAINNET))
    }

    @Test
    fun `public keys follow the network`() {
        val key = "public1psuz3y3vtakgh5zyanxfrdr56aaftr9dgyglff7q7mdsx96g0sngnfyke290z7arpy733l4nuq67h7rekzt5u5c9tyuy7wskyft6el2tsg02zxyemkdgr40zg5j6c4dstx6mrc96z2h843wcypqmgheu56v59pxy7"
        assertTrue(DelegationRules.isPublicKeyFor(key, Network.MAINNET))
        assertFalse(DelegationRules.isPublicKeyFor(key, Network.TESTNET))
    }

    @Test
    fun `dApp bond parameters are checked`() {
        val ok = DelegationRules.parseDappBond(validator, "", "200000000", "9000000", "hub", Network.MAINNET)
        assertEquals(9_000_000L, ok.expiry)
        assertThrows(IllegalArgumentException::class.java) {
            DelegationRules.parseDappBond(validator, "", "33", "9000000", "", Network.MAINNET)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DelegationRules.parseDappBond(validator, "", "0", "0", "", Network.MAINNET)
        }
    }
}
