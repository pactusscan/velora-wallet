package com.andrutstudio.velora.presentation.components

import com.andrutstudio.velora.domain.model.Amount
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val pactusSymbols = DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = ','
    decimalSeparator = '.'
}

private val pactusFormatter = DecimalFormat("#,##0.#########", pactusSymbols)

/**
 * Formats Amount for display with PAC suffix.
 * Example: 1,234.56 PAC
 */
fun formatPac(amount: Amount): String {
    return "${pactusFormatter.format(amount.pac)} PAC"
}

/**
 * Formats Amount for input fields (no suffix, no thousands separator).
 * Example: 1234.56
 */
fun formatPacInput(amount: Amount): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        decimalSeparator = '.'
    }
    return DecimalFormat("0.#########", symbols).format(amount.pac)
}
