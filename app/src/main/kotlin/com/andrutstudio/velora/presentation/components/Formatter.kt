package com.andrutstudio.velora.presentation.components

import com.andrutstudio.velora.domain.model.Amount
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.presentation.theme.VeloraTheme
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

@Preview(showBackground = true)
@Composable
private fun FormatterPreview() {
    val samples = listOf(
        Amount.fromPac(0.0),
        Amount.fromPac(1.23456789),
        Amount.fromPac(1234.56),
        Amount.fromPac(1000000.0)
    )
    VeloraTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("formatPac (Display):", style = MaterialTheme.typography.titleSmall)
            samples.forEach {
                Text(text = formatPac(it), style = MaterialTheme.typography.bodyMedium)
            }
            
            Text("\nformatPacInput (Editing):", style = MaterialTheme.typography.titleSmall)
            samples.forEach {
                Text(text = formatPacInput(it), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
