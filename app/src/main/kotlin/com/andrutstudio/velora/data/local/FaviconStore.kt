package com.andrutstudio.velora.data.local

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/** A saved favicon; [stamp] changes when the file is replaced, so the UI reloads it. */
data class FaviconRef(val path: String, val stamp: Long)

/**
 * Favicons the dApp browser received from WebView, one small PNG per host in the app's
 * private files, so bookmarks and history show the site's own icon after a visit.
 */
@Singleton
class FaviconStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dir = File(context.filesDir, "favicons")

    private val _icons = MutableStateFlow(
        dir.listFiles { f -> f.extension == "png" }.orEmpty()
            .associate { it.nameWithoutExtension to FaviconRef(it.path, it.lastModified()) },
    )
    val icons: StateFlow<Map<String, FaviconRef>> = _icons.asStateFlow()

    suspend fun save(url: String, icon: Bitmap) = withContext(Dispatchers.IO) {
        val host = hostKey(url) ?: return@withContext
        dir.mkdirs()
        val file = File(dir, "$host.png")
        val longest = maxOf(icon.width, icon.height)
        val scaled = if (longest > MAX_SIZE) {
            val ratio = MAX_SIZE.toFloat() / longest
            Bitmap.createScaledBitmap(icon, (icon.width * ratio).toInt().coerceAtLeast(1), (icon.height * ratio).toInt().coerceAtLeast(1), true)
        } else {
            icon
        }
        runCatching { file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            .onSuccess { _icons.update { it + (host to FaviconRef(file.path, file.lastModified())) } }
    }

    /** Drops the icons of hosts no longer used by any bookmark or history entry. */
    suspend fun keepOnly(urls: Collection<String>) = withContext(Dispatchers.IO) {
        val keep = urls.mapNotNull(::hostKey).toSet()
        val drop = _icons.value.keys - keep
        drop.forEach { File(dir, "$it.png").delete() }
        _icons.update { it - drop }
    }

    companion object {
        private const val MAX_SIZE = 96

        /** Lower-case host, safe as a file name; null for non-web URLs. */
        fun hostKey(url: String): String? {
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (uri.scheme != "https" && uri.scheme != "http") return null
            val host = uri.host?.lowercase() ?: return null
            return host.takeIf { it.matches(Regex("[a-z0-9.-]+")) }
        }
    }
}
