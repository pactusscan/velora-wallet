package com.andrutstudio.velora.presentation.anchor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.AnchorInfo
import com.andrutstudio.velora.domain.model.AnchorRequest
import com.andrutstudio.velora.domain.model.AnchorRules
import com.andrutstudio.velora.presentation.components.TxErrorBox
import com.andrutstudio.velora.presentation.components.asString
import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.home.BiometricEntryPoint
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun AnchorScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnchorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricHelper = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java)
            .biometricHelper()
    }
    val sent = stringResource(R.string.anchor_sent)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AnchorViewModel.Effect.Sent -> snackbarHostState.showSnackbar(sent)
                AnchorViewModel.Effect.RequestBiometricUnlock -> if (activity != null) {
                    biometricHelper.showBiometricPromptForDecryption(
                        activity = activity,
                        onSuccess = viewModel::onBiometricUnlockSuccess,
                        onError = {},
                    )
                }
            }
        }
    }

    AnchorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::load,
        onStartPublish = viewModel::onStartPublish,
        onCancelPublish = viewModel::onCancelPublish,
        onRootHashChange = viewModel::onRootHashChange,
        onManifestUriChange = viewModel::onManifestUriChange,
        onAnchorTypeChange = viewModel::onAnchorTypeChange,
        onMemoChange = viewModel::onMemoChange,
        onReviewPublish = viewModel::onReviewPublish,
        onReviewDelete = viewModel::onReviewDelete,
    )

    state.pending?.let { request ->
        ConfirmAnchorSheet(
            state = state,
            request = request,
            onDismiss = viewModel::onDismissConfirm,
            onPasswordChange = viewModel::onPasswordChange,
            onConfirm = viewModel::onConfirm,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnchorScreenContent(
    state: AnchorViewModel.State,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onStartPublish: () -> Unit,
    onCancelPublish: () -> Unit,
    onRootHashChange: (String) -> Unit,
    onManifestUriChange: (String) -> Unit,
    onAnchorTypeChange: (Int) -> Unit,
    onMemoChange: (String) -> Unit,
    onReviewPublish: () -> Unit,
    onReviewDelete: () -> Unit,
) {
    val publishing = state.mode == AnchorViewModel.Mode.PUBLISH
    BackHandler(enabled = publishing, onBack = onCancelPublish)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (publishing) R.string.anchor_publish_title else R.string.anchor_title))
                        state.account?.let {
                            Text(
                                text = it.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (publishing) onCancelPublish else onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                publishing -> PublishForm(
                    state = state,
                    onRootHashChange = onRootHashChange,
                    onManifestUriChange = onManifestUriChange,
                    onAnchorTypeChange = onAnchorTypeChange,
                    onMemoChange = onMemoChange,
                    onReview = onReviewPublish,
                )
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
                state.loadError -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.anchor_error_load),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.anchor_retry)) }
                }
                else -> {
                    val anchor = state.anchor
                    if (anchor == null) EmptyAnchor() else AnchorDetails(anchor)
                    state.formError?.let { TxErrorBox(message = it.asString()) }
                    if (state.canSign) {
                        Actions(
                            hasAnchor = anchor != null,
                            isPreparing = state.isPreparing,
                            onPublish = onStartPublish,
                            onDelete = onReviewDelete,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.anchor_watch_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnchor() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.Anchor,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.anchor_none_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.anchor_none_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AnchorDetails(anchor: AnchorInfo) {
    val clipboard = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                DetailRow(
                    label = stringResource(R.string.anchor_root_hash),
                    value = anchor.rootHash,
                    mono = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(anchor.rootHash)) }) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            // Written by whoever published the anchor: shown, never opened
            if (anchor.manifestUri.isNotEmpty()) {
                DetailRow(stringResource(R.string.anchor_manifest_uri), anchor.manifestUri)
            }
            DetailRow(stringResource(R.string.anchor_type), anchorTypeName(anchor.anchorType))
            DetailRow(stringResource(R.string.anchor_locked), formatPac(anchor.lockedDeposit))
            DetailRow(
                stringResource(R.string.anchor_updated),
                DateFormat.getDateTimeInstance().format(Date(anchor.updatedTime * 1000)),
            )
        }
    }
}

@Composable
private fun Actions(
    hasAnchor: Boolean,
    isPreparing: Boolean,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
) {
    Button(onClick = onPublish, modifier = Modifier.fillMaxWidth(), enabled = !isPreparing) {
        Text(stringResource(if (hasAnchor) R.string.anchor_replace_action else R.string.anchor_publish_action))
    }
    if (hasAnchor) {
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(), enabled = !isPreparing) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.anchor_delete_title))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishForm(
    state: AnchorViewModel.State,
    onRootHashChange: (String) -> Unit,
    onManifestUriChange: (String) -> Unit,
    onAnchorTypeChange: (Int) -> Unit,
    onMemoChange: (String) -> Unit,
    onReview: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var typeMenuOpen by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = state.rootHash,
        onValueChange = onRootHashChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.anchor_root_label)) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        isError = state.rootError != null,
        supportingText = state.rootError?.let { { Text(it.asString()) } },
        trailingIcon = {
            IconButton(onClick = { clipboard.getText()?.text?.let(onRootHashChange) }) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.anchor_paste))
            }
        },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = state.manifestUri,
        onValueChange = onManifestUriChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.anchor_uri_label)) },
        singleLine = true,
        isError = state.uriError != null,
        supportingText = state.uriError?.let { { Text(it.asString()) } },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
    )
    ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
        OutlinedTextField(
            value = anchorTypeName(state.anchorType),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text(stringResource(R.string.anchor_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
        )
        ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
            AnchorRules.KNOWN_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(anchorTypeName(type)) },
                    onClick = {
                        onAnchorTypeChange(type)
                        typeMenuOpen = false
                    },
                )
            }
        }
    }
    OutlinedTextField(
        value = state.memo,
        onValueChange = onMemoChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.stake_memo_label)) },
        placeholder = { Text(stringResource(R.string.stake_memo_placeholder)) },
        singleLine = true,
        isError = state.memoError != null,
        supportingText = state.memoError?.let { { Text(it.asString()) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.anchor_deposit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = formatPac(state.deposit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
    if (state.anchor != null) {
        Text(
            text = stringResource(R.string.anchor_replace_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    state.formError?.let { TxErrorBox(message = it.asString()) }
    Button(
        onClick = onReview,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.rootHash.isNotBlank() && !state.isPreparing,
    ) {
        if (state.isPreparing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.anchor_review))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmAnchorSheet(
    state: AnchorViewModel.State,
    request: AnchorRequest,
    onDismiss: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val useBiometric = state.isBiometricEnabled && state.password.isEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (request.isDelete) R.string.anchor_delete_title else R.string.anchor_publish_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (request.isDelete) {
                        Text(text = stringResource(R.string.anchor_delete_hint), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        DetailRow(stringResource(R.string.anchor_root_hash), request.rootHash, mono = true)
                        if (request.manifestUri.isNotEmpty()) {
                            DetailRow(stringResource(R.string.anchor_manifest_uri), request.manifestUri)
                        }
                        DetailRow(stringResource(R.string.anchor_type), anchorTypeName(request.anchorType))
                        DetailRow(stringResource(R.string.anchor_deposit), formatPac(request.deposit))
                        if (request.memo.isNotEmpty()) {
                            DetailRow(stringResource(R.string.browser_sign_memo), request.memo)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(stringResource(R.string.stake_fee_label), formatPac(request.fee))
                }
            }
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.backup_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it.asString()) } },
                enabled = !state.isSending,
            )
            state.sendError?.let { TxErrorBox(message = it.asString()) }
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSending && (state.password.isNotEmpty() || state.isBiometricEnabled),
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = if (useBiometric) Icons.Rounded.Fingerprint else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            when {
                                useBiometric -> R.string.stake_confirm_biometric
                                request.isDelete -> R.string.anchor_confirm_delete
                                else -> R.string.anchor_confirm_publish
                            },
                        ),
                    )
                }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !state.isSending) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
private fun anchorTypeName(type: Int): String = when (type) {
    0 -> stringResource(R.string.anchor_type_0)
    1 -> stringResource(R.string.anchor_type_1)
    2 -> stringResource(R.string.anchor_type_2)
    3 -> stringResource(R.string.anchor_type_3)
    255 -> stringResource(R.string.anchor_type_255)
    else -> type.toString()
}

private val previewAccount = Account(
    address = "tpc1rw03cn7xkd7jjl5ajjkwj67wwhsf7e252fu50nz",
    label = "Account 1",
    type = AccountType.ED25519,
    derivationIndex = 0,
)

private val previewAnchor = AnchorInfo(
    rootHash = "ab".repeat(32),
    manifestUri = "https://example.org/manifest.json",
    anchorType = 3,
    lockedDeposit = Amount.fromPac(1.0),
    updatedTime = 1_793_000_000,
)

@Composable
private fun Preview(state: AnchorViewModel.State) {
    VeloraTheme {
        AnchorScreenContent(
            state = state,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onRetry = {},
            onStartPublish = {},
            onCancelPublish = {},
            onRootHashChange = {},
            onManifestUriChange = {},
            onAnchorTypeChange = {},
            onMemoChange = {},
            onReviewPublish = {},
            onReviewDelete = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AnchorEmptyPreview() {
    Preview(AnchorViewModel.State(account = previewAccount, canSign = true, isLoading = false))
}

@Preview(showSystemUi = true)
@Composable
private fun AnchorActivePreview() {
    Preview(AnchorViewModel.State(account = previewAccount, canSign = true, isLoading = false, anchor = previewAnchor))
}

@Preview(showSystemUi = true)
@Composable
private fun AnchorPublishPreview() {
    Preview(
        AnchorViewModel.State(
            account = previewAccount,
            canSign = true,
            isLoading = false,
            anchor = previewAnchor,
            mode = AnchorViewModel.Mode.PUBLISH,
            rootHash = "ab".repeat(32),
        ),
    )
}
