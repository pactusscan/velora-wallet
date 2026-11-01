package com.andrutstudio.velora.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorRulesTest {

    @Test
    fun `root must be 32 to 64 bytes of hex`() {
        assertEquals("ab".repeat(32), AnchorRules.normalizeRoot(" ${"AB".repeat(32)} "))
        assertEquals("ab".repeat(64), AnchorRules.normalizeRoot("ab".repeat(64)))
        assertNull(AnchorRules.normalizeRoot("ab".repeat(31)))
        assertNull(AnchorRules.normalizeRoot("ab".repeat(65)))
        assertNull(AnchorRules.normalizeRoot("zz".repeat(32)))
        assertNull(AnchorRules.normalizeRoot("a".repeat(65)))
    }

    @Test
    fun `limits count bytes, not characters`() {
        assertTrue(AnchorRules.isValidUri("a".repeat(128)))
        assertFalse(AnchorRules.isValidUri("a".repeat(129)))
        assertFalse(AnchorRules.isValidMemo("é".repeat(33)))
    }

    @Test
    fun `a new anchor locks the minimum and a replacement locks nothing more`() {
        assertEquals(1_000_000_000, AnchorRules.deposit(null, hasAnchor = false).nanoPac)
        assertEquals(0, AnchorRules.deposit(null, hasAnchor = true).nanoPac)
        assertEquals(5, AnchorRules.deposit(Amount.fromNanoPac(5), hasAnchor = true).nanoPac)
    }
}
