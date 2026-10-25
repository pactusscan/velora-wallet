package com.andrutstudio.velora.presentation.components

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Text a ViewModel hands to the UI. ViewModels have no access to the current
 * locale, so they pass a string resource (resolved on screen, in the user's
 * language) or, for messages that come from the node or an exception, the raw
 * text as is.
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

/** The exception's own message when it has one (e.g. from the node), else a translated fallback. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText =
    message?.takeIf { it.isNotBlank() }?.let { UiText.Raw(it) } ?: UiText.Res(fallback)

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.toTypedArray())
    is UiText.Raw -> value
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Res -> context.getString(id, *args.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
    is UiText.Raw -> value
}
