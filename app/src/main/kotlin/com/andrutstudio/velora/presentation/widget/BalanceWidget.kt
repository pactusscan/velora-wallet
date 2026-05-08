package com.andrutstudio.velora.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.R
import com.andrutstudio.velora.MainActivity
import com.andrutstudio.velora.presentation.theme.Background
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.OnSurface
import com.andrutstudio.velora.presentation.theme.OnSurfaceVariant
import com.andrutstudio.velora.presentation.theme.SurfaceContainer

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    internal fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val balanceNanoPac = prefs[WidgetUpdater.PREF_BALANCE] ?: 0L
        val walletName = prefs[WidgetUpdater.PREF_NAME] ?: context.getString(R.string.app_name)
        val network = prefs[WidgetUpdater.PREF_NETWORK] ?: context.getString(R.string.network_mainnet)

        WidgetLayout(
            balanceNanoPac = balanceNanoPac,
            walletName = walletName,
            network = network,
        )
    }

    @Composable
    internal fun WidgetLayout(
        balanceNanoPac: Long,
        walletName: String,
        network: String
    ) {
        val balancePac = "%.4f PAC".format(balanceNanoPac / 1_000_000_000.0)
        val context = LocalContext.current

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(SurfaceContainer))
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Wallet name + network row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = walletName,
                        style = TextStyle(
                            color = ColorProvider(OnSurface),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = network,
                        style = TextStyle(
                            color = ColorProvider(BrandTeal),
                            fontSize = 11.sp,
                        ),
                    )
                }

                Spacer(GlanceModifier.height(8.dp))

                // Balance
                Text(
                    text = balancePac,
                    style = TextStyle(
                        color = ColorProvider(BrandTeal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                )

                Spacer(GlanceModifier.height(4.dp))

                Text(
                    text = context.getString(R.string.widget_tap_to_open),
                    style = TextStyle(
                        color = ColorProvider(OnSurfaceVariant),
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 100)
@Composable
private fun BalanceWidgetPreview() {
    BalanceWidget().apply {
        WidgetLayout(
            balanceNanoPac = 12500000000L,
            walletName = "Main Wallet",
            network = "Mainnet"
        )
    }
}
