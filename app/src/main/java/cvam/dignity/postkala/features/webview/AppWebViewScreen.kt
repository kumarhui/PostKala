package cvam.dignity.postkala.features.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Enterprise desktop user-agent for India Post / SAP portals
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var defaultUserAgent by remember { mutableStateOf<String?>(null) }
    var isDesktopMode by remember { mutableStateOf(false) }

    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title.uppercase(),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick Zoom Out
                    IconButton(onClick = {
                        webViewInstance?.zoomOut()
                    }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }

                    // Quick Zoom In
                    IconButton(onClick = {
                        webViewInstance?.zoomIn()
                    }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }

                    // Desktop / Mobile Mode Toggle
                    IconButton(onClick = {
                        webViewInstance?.let { wv ->
                            isDesktopMode = !isDesktopMode
                            val currentUrl = wv.url ?: url

                            wv.settings.apply {
                                userAgentString = if (isDesktopMode) {
                                    DESKTOP_USER_AGENT
                                } else {
                                    defaultUserAgent
                                }
                                useWideViewPort = isDesktopMode
                                loadWithOverviewMode = isDesktopMode
                            }

                            // Re-request url to trigger desktop content negotiation
                            wv.loadUrl(currentUrl)
                        }
                    }) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.DesktopMac,
                            contentDescription = if (isDesktopMode) "Mobile Site" else "Desktop Site",
                            tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }

                    IconButton(onClick = {
                        val currentUrl = webViewInstance?.url ?: url
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && pageProgress < 1f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            if (defaultUserAgent == null) {
                                defaultUserAgent = settings.userAgentString
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true

                                // Viewport & Zoom Configuration
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false

                                // Reduce base text zoom so overflow buttons do not drop off container boundaries
                                textZoom = 90

                                allowFileAccess = true
                                allowContentAccess = true
                                javaScriptCanOpenWindowsAutomatically = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val reqUri = request?.url ?: return false
                                    val scheme = reqUri.scheme?.lowercase()

                                    if (scheme != "http" && scheme != "https") {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, reqUri)
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (_: Exception) {
                                            return true
                                        }
                                    }
                                    return false
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    handler?.proceed()
                                }

                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    super.onPageFinished(view, finishedUrl)
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true

                                    // Override viewport meta tags that block zooming and force mobile scaling
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var meta = document.querySelector('meta[name="viewport"]');
                                            if (meta) {
                                                meta.setAttribute('content', 'width=1024, initial-scale=0.75, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes');
                                            } else {
                                                var newMeta = document.createElement('meta');
                                                newMeta.name = 'viewport';
                                                newMeta.content = 'width=1024, initial-scale=0.75, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes';
                                                document.getElementsByTagName('head')[0].appendChild(newMeta);
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                    canGoBack = view?.canGoBack() == true
                                }
                            }

                            setDownloadListener { downloadUrl, _, _, _, _ ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {}
                            }

                            loadUrl(url)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
        }
    }
}