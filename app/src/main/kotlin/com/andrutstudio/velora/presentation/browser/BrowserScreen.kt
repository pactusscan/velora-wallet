package com.andrutstudio.velora.presentation.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.local.db.BookmarkEntity
import com.andrutstudio.velora.data.local.db.BrowserHistoryEntity
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandPurple
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val webView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Aktifkan akselerasi hardware
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setGeolocationEnabled(true)
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Chrome Android features
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Mimic Chrome Android User Agent
                val defaultUa = WebSettings.getDefaultUserAgent(context)
                userAgentString = "$defaultUa VeloraWallet/1.0"
            }
            
            // Cookies
            android.webkit.CookieManager.getInstance().setAcceptCookie(true)
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            
            // Critical for touch interaction
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    val bridge = remember {
        PactusJsBridge(
            onGetAccounts = { viewModel.getAccountsJson() },
            onGetNetwork = { viewModel.getNetworkName() },
            onSignRequest = { id, to, amount, fee, memo ->
                viewModel.onSignRequest(id, to, amount, fee, memo)
            },
        )
    }

    DisposableEffect(webView) {
        webView.addJavascriptInterface(bridge, "PactusAndroid")
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                viewModel.onProgressChanged(newProgress)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url?.let { viewModel.onPageStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val title = view?.title.orEmpty()
                url?.let { viewModel.onPageFinished(it, title) }
                view?.evaluateJavascript(PactusJsBridge.JS_BRIDGE, null)
                viewModel.onNavStateChanged(
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                )
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url.scheme ?: return false
                
                if (scheme != "http" && scheme != "https") {
                    viewModel.onExternalLinkRequested(url)
                    return true
                }
                return false
            }
        }
        onDispose {
            webView.removeJavascriptInterface("PactusAndroid")
            webView.destroy()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserViewModel.Effect.LoadUrl -> webView.loadUrl(effect.url)
                is BrowserViewModel.Effect.GoBack -> webView.goBack()
                is BrowserViewModel.Effect.GoForward -> webView.goForward()
                is BrowserViewModel.Effect.Reload -> webView.reload()
                is BrowserViewModel.Effect.OpenExternal -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                }
                is BrowserViewModel.Effect.ResolveSign -> {
                    webView.evaluateJavascript("window.pactus._resolve('${effect.requestId}','${effect.txId}')", null)
                }
                is BrowserViewModel.Effect.RejectSign -> {
                    val escaped = effect.error.replace("\\", "\\\\").replace("'", "\\'")
                    webView.evaluateJavascript("window.pactus._reject('${effect.requestId}','$escaped')", null)
                }
                is BrowserViewModel.Effect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    if (state.pendingSignRequest != null) {
        SignRequestSheet(
            request = state.pendingSignRequest!!,
            isSigning = state.isSigning,
            onConfirm = viewModel::onConfirmSign,
            onCancel = viewModel::onCancelSign,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BrowserTopBar(
                urlBarText = state.urlBarText,
                isLoading = state.isLoading,
                loadingProgress = state.loadingProgress,
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                isBookmarked = state.isBookmarked,
                onNavigateBack = onNavigateBack,
                onUrlChange = viewModel::onUrlBarChanged,
                onUrlSubmit = viewModel::onUrlSubmitted,
                onBack = viewModel::onBackClicked,
                onForward = viewModel::onForwardClicked,
                onReload = viewModel::onReloadClicked,
                onToggleBookmark = viewModel::onToggleBookmark,
                onFocusChange = { /* No-op */ }
            )
        },
        bottomBar = {
            MainBottomNavigation(
                navController = navController,
                currentRoute = Screen.Browser.withUrl(state.urlBarText)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )

            if (state.url == "about:blank") {
                BrowserHome(onUrlSubmit = viewModel::onUrlSubmitted)
            }
        }
    }
}

@Composable
private fun BrowserHome(onUrlSubmit: (String) -> Unit) {
    val ecosystemLinks = listOf(
        EcosystemLink("Pactus Website", "https://pactus.org", "The official website of Pactus blockchain."),
        EcosystemLink("Pactus Scan", "https://pactusscan.com", "The primary block explorer for the Pactus network."),
        EcosystemLink("Pactus Documentation", "https://docs.pactus.org", "Comprehensive guides and technical documentation."),
        EcosystemLink("Pactus GitHub", "https://github.com/pactus-project/pactus", "Source code and development activity."),
        EcosystemLink("Pactus PIPS", "https://pips.pactus.org", "Pactus Improvement Proposals and standards.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pactus_logo),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = Color.Unspecified
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            stringResource(R.string.browser_ecosystem_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.browser_ecosystem_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))

        ecosystemLinks.forEach { link ->
            EcosystemCard(link) { onUrlSubmit(link.url) }
            Spacer(Modifier.height(12.dp))
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

private data class EcosystemLink(val title: String, val url: String, val description: String)

@Composable
private fun EcosystemCard(link: EcosystemLink, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        link.url.contains("github") -> Icons.Rounded.Code
                        link.url.contains("docs") || link.url.contains("pips") -> Icons.Rounded.Description
                        link.url.contains("scan") -> Icons.Rounded.Timeline
                        else -> Icons.Rounded.Public
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    link.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    link.url.removePrefix("https://").removePrefix("http://"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserTopBar(
    urlBarText: String,
    isLoading: Boolean,
    loadingProgress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    onNavigateBack: () -> Unit,
    onUrlChange: (String) -> Unit,
    onUrlSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.browser_close),
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.browser_go_back),
                        modifier = Modifier.size(18.dp),
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.browser_go_forward),
                        modifier = Modifier.size(18.dp),
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }

                Spacer(Modifier.width(4.dp))

                val urlBarBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                val urlBarTextStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                )
                BasicTextField(
                    value = urlBarText,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChange(it.isFocused) }
                        .border(1.dp, urlBarBorderColor, MaterialTheme.shapes.extraLarge),
                    singleLine = true,
                    textStyle = urlBarTextStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = {
                        onUrlSubmit(urlBarText)
                        focusManager.clearFocus()
                    }),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (urlBarText.isEmpty()) {
                                Text(
                                    stringResource(R.string.browser_search_placeholder),
                                    style = urlBarTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onReload,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                        contentDescription = if (isLoading) stringResource(R.string.browser_stop) else stringResource(R.string.browser_reload),
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = stringResource(R.string.browser_bookmark),
                        modifier = Modifier.size(18.dp),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignRequestSheet(
    request: BrowserViewModel.PendingSignRequest,
    isSigning: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.browser_sign_title), style = MaterialTheme.typography.titleLarge)

            SignRow(stringResource(R.string.browser_sign_from), request.fromAddress)
            SignRow(stringResource(R.string.browser_sign_to), request.to)
            SignRow(stringResource(R.string.browser_sign_amount), "%.6f PAC".format(Amount.fromNanoPac(request.amountNanoPac).pac))
            SignRow(stringResource(R.string.browser_sign_fee), "%.6f PAC".format(Amount.fromNanoPac(request.feeNanoPac).pac))
            if (request.memo.isNotBlank()) {
                SignRow(stringResource(R.string.browser_sign_memo), request.memo)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSigning,
                ) { Text(stringResource(R.string.action_reject)) }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = !isSigning,
                ) {
                    if (isSigning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowserTopBarPreview() {
    VeloraTheme {
        BrowserTopBar(
            urlBarText = "https://pactus.org",
            isLoading = false,
            loadingProgress = 100,
            canGoBack = true,
            canGoForward = false,
            isBookmarked = true,
            onNavigateBack = {},
            onUrlChange = {},
            onUrlSubmit = {},
            onBack = {},
            onForward = {},
            onReload = {},
            onToggleBookmark = {},
            onFocusChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignRowPreview() {
    VeloraTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SignRow("To", "pc1rmv39cmjlexample27l5jg5wv67am5hy2velora")
            SignRow("Amount", "10.000000 PAC")
            SignRow("Fee", "0.000100 PAC")
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun BrowserHomePreview() {
    VeloraTheme {
        BrowserHome(onUrlSubmit = {})
    }
}
