package com.andrutstudio.velora.presentation.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.local.db.BookmarkEntity
import com.andrutstudio.velora.data.local.db.BrowserHistoryEntity
import com.andrutstudio.velora.presentation.theme.BrandPurple
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import java.net.URI
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private const val RECENT_ON_HOME = 3

// ── Start page sections ──────────────────────────────────────────────────────

/** Bookmarks row and the latest visits, shown on the browser start page above the ecosystem cards. */
@Composable
fun HomeSavedSections(
    bookmarks: List<BookmarkEntity>,
    history: List<BrowserHistoryEntity>,
    onOpen: (String) -> Unit,
    onSeeAll: (LibraryTab) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (bookmarks.isNotEmpty()) {
            SectionHeader(stringResource(R.string.browser_bookmarks_header), stringResource(R.string.browser_see_all)) {
                onSeeAll(LibraryTab.BOOKMARKS)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bookmarks.forEach { bookmark ->
                    BookmarkCard(bookmark) { onOpen(bookmark.url) }
                }
            }
        }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.browser_recent), stringResource(R.string.browser_history_header)) {
                onSeeAll(LibraryTab.HISTORY)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            ) {
                history.take(RECENT_ON_HOME).forEachIndexed { i, entry ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SavedRow(title = entry.title, url = entry.url, onClick = { onOpen(entry.url) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clickable(onClick = onAction)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = BrandTeal)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BookmarkCard(bookmark: BookmarkEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SiteInitial(bookmark.title, bookmark.url, size = 40)
        Text(
            bookmark.title.ifBlank { hostOf(bookmark.url) },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Bookmarks and history sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySheet(
    tab: LibraryTab,
    bookmarks: List<BookmarkEntity>,
    history: List<BrowserHistoryEntity>,
    onTabChange: (LibraryTab) -> Unit,
    onOpen: (String) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LibraryContent(tab, bookmarks, history, onTabChange, onOpen, onDeleteBookmark, onDeleteHistory, onClearHistory)
    }
}

@Composable
private fun LibraryContent(
    tab: LibraryTab,
    bookmarks: List<BookmarkEntity>,
    history: List<BrowserHistoryEntity>,
    onTabChange: (LibraryTab) -> Unit,
    onOpen: (String) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val today = stringResource(R.string.browser_history_today)
    val yesterday = stringResource(R.string.browser_history_yesterday)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabSwitch(tab, onTabChange)
        when (tab) {
            LibraryTab.BOOKMARKS -> if (bookmarks.isEmpty()) {
                EmptyNote(stringResource(R.string.browser_bookmarks_empty), stringResource(R.string.browser_bookmarks_empty_hint))
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        SavedRow(
                            title = bookmark.title,
                            url = bookmark.url,
                            onClick = { onOpen(bookmark.url) },
                            deleteLabel = stringResource(R.string.browser_delete_bookmark, bookmark.title.ifBlank { hostOf(bookmark.url) }),
                            onDelete = { onDeleteBookmark(bookmark) },
                        )
                    }
                }
            }
            LibraryTab.HISTORY -> if (history.isEmpty()) {
                EmptyNote(stringResource(R.string.browser_history_empty), stringResource(R.string.browser_history_empty_hint))
            } else {
                val groups = history.groupBy { dayLabel(it.visitedAt, today, yesterday) }
                LazyColumn(Modifier.weight(1f)) {
                    groups.forEach { (day, entries) ->
                        item(key = "day-$day") {
                            Text(
                                day,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(entries, key = { it.id }) { entry ->
                            SavedRow(
                                title = entry.title,
                                url = entry.url,
                                time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.visitedAt)),
                                onClick = { onOpen(entry.url) },
                                deleteLabel = stringResource(R.string.browser_delete_history, hostOf(entry.url)),
                                onDelete = { onDeleteHistory(entry.url) },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Text(stringResource(R.string.browser_clear_history))
                }
            }
        }
    }
}

@Composable
private fun TabSwitch(tab: LibraryTab, onTabChange: (LibraryTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            LibraryTab.BOOKMARKS to R.string.browser_bookmarks_header,
            LibraryTab.HISTORY to R.string.browser_history_header,
        ).forEach { (value, label) ->
            val active = value == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, if (active) BrandPurple else Color.Transparent, RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(8.dp))
                    .selectable(selected = active, role = Role.Tab, onClick = { onTabChange(value) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedRow(
    title: String,
    url: String,
    onClick: () -> Unit,
    time: String? = null,
    deleteLabel: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SiteInitial(title, url, size = 36)
        Column(Modifier.weight(1f)) {
            Text(title.ifBlank { hostOf(url) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                displayUrl(url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        time?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = deleteLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** First letter of the site in a rounded square, so rows read without loading favicons. */
@Composable
private fun SiteInitial(title: String, url: String, size: Int) {
    val letter = (hostOf(url).removePrefix("www.").firstOrNull() ?: title.firstOrNull() ?: '?').uppercaseChar()
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(BrandTeal.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.toString(), color = BrandTeal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyNote(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: url

private fun displayUrl(url: String): String = url.removePrefix("https://").removePrefix("http://").removeSuffix("/")

private fun dayLabel(millis: Long, today: String, yesterday: String): String {
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay(day, now) -> today
        sameDay(day, now.apply { add(Calendar.DAY_OF_YEAR, -1) }) -> yesterday
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewBookmarks = listOf(
    BookmarkEntity(1, "https://play.pactusscan.com", "Pactus Play"),
    BookmarkEntity(2, "https://pactus.org", "Pactus", isPreset = true),
    BookmarkEntity(3, "https://pactusscan.com", "Pactus Explorer", isPreset = true),
    BookmarkEntity(4, "https://wrapto.app", "Wrapto", isPreset = true),
)

private val previewHistory = System.currentTimeMillis().let { now ->
    listOf(
        BrowserHistoryEntity(1, "https://pactusscan.com/validators", "Validators | PactusScan", now - 60_000),
        BrowserHistoryEntity(2, "https://docs.pactus.org/", "Pactus Docs", now - 3_600_000),
        BrowserHistoryEntity(3, "https://play.pactusscan.com/", "Pactus Play", now - 26 * 3_600_000L),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeSavedSectionsPreview() {
    VeloraTheme {
        Box(Modifier.padding(16.dp)) {
            HomeSavedSections(previewBookmarks, previewHistory, onOpen = {}, onSeeAll = {})
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun LibraryHistoryPreview() {
    VeloraTheme {
        LibraryContent(LibraryTab.HISTORY, previewBookmarks, previewHistory, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun LibraryBookmarksEmptyPreview() {
    VeloraTheme {
        LibraryContent(LibraryTab.BOOKMARKS, emptyList(), previewHistory, {}, {}, {}, {}, {})
    }
}
