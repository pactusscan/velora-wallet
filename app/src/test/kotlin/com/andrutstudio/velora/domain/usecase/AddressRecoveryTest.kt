package com.andrutstudio.velora.domain.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressRecoveryTest {

    private fun recover(activeIndexes: Set<Int>): Int = runBlocking {
        AddressRecovery.discoverRecoveredCount { index -> index in activeIndexes }
    }

    @Test
    fun `fresh wallet with no activity recovers one account`() {
        assertEquals(1, recover(emptySet()))
    }

    @Test
    fun `only first address active recovers one account`() {
        assertEquals(1, recover(setOf(0)))
    }

    @Test
    fun `consecutive active addresses are all recovered`() {
        assertEquals(5, recover(setOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun `recovered count reaches the last active address`() {
        // Index 3 active with gaps before it: recover indexes 0..3.
        assertEquals(4, recover(setOf(0, 3)))
    }

    @Test
    fun `inactive addresses before the first active one are recovered too`() {
        assertEquals(3, recover(setOf(2)))
    }

    @Test
    fun `address within the gap limit is found`() {
        // Active at 0, then 7 inactive (1..7), active at 8.
        assertEquals(9, recover(setOf(0, 8)))
    }

    @Test
    fun `address beyond the gap limit is not found`() {
        // Per PIP-41 counters (inactive_count resets to 1 after an active
        // address), scanning stops before reaching index 9.
        assertEquals(1, recover(setOf(0, 9)))
    }

    @Test
    fun `scan from genesis tolerates up to eight leading gaps`() {
        // No active address yet; indexes 0..8 are scanned before breaking, so
        // an active address at index 8 is still found.
        assertEquals(9, recover(setOf(8)))
    }
}
