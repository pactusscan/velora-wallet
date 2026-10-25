package com.andrutstudio.velora.presentation.components

import com.andrutstudio.velora.R
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `exception message is shown as is`() {
        val text = IllegalStateException("invalid transaction: insufficient fee").toUiText(R.string.error_transaction_failed)
        assertEquals(UiText.Raw("invalid transaction: insufficient fee"), text)
    }

    @Test
    fun `missing or blank message falls back to the translated text`() {
        assertEquals(UiText.Res(R.string.error_transaction_failed), RuntimeException().toUiText(R.string.error_transaction_failed))
        assertEquals(UiText.Res(R.string.error_transaction_failed), RuntimeException(" ").toUiText(R.string.error_transaction_failed))
    }

    @Test
    fun `uiText keeps format arguments`() {
        assertEquals(UiText.Res(R.string.settings_wallet_renamed, listOf("Savings")), uiText(R.string.settings_wallet_renamed, "Savings"))
    }
}
