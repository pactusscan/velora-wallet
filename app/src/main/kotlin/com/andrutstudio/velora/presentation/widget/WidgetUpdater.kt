package com.andrutstudio.velora.presentation.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun update(totalBalanceNanoPac: Long, walletName: String, networkName: String) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(BalanceWidget::class.java)
            for (id in ids) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { current ->
                    current.toMutablePreferences().apply {
                        this[PREF_BALANCE] = totalBalanceNanoPac
                        this[PREF_NAME] = walletName
                        this[PREF_NETWORK] = networkName
                        this[PREF_UPDATED_AT] = System.currentTimeMillis()
                    }
                }
                BalanceWidget().update(context, id)
            }
        } catch (_: Exception) {
            // Widget not placed on home screen — no-op
        }
    }

    companion object {
        val PREF_BALANCE = longPreferencesKey("w_balance")
        val PREF_NAME = stringPreferencesKey("w_name")
        val PREF_NETWORK = stringPreferencesKey("w_network")
        val PREF_UPDATED_AT = longPreferencesKey("w_updated_at")
    }
}
