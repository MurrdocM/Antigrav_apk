package com.antigravity.remote.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.remote.data.PreferencesManager
import com.antigravity.remote.ui.theme.BgDark
import com.antigravity.remote.ui.theme.PrimaryAccent
import com.antigravity.remote.ui.theme.SurfaceDark
import com.antigravity.remote.ui.theme.TextPrimary
import com.antigravity.remote.webview.AntigravityChromeClient
import com.antigravity.remote.webview.AntigravityWebViewClient
import com.antigravity.remote.webview.WebViewConfigurator
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AntigravityScreen(
    preferencesManager: PreferencesManager,
    onLaunchFilePicker: (Intent, ValueCallback<Array<Uri>>) -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedUrl by preferencesManager.currentUrl.collectAsState(initial = PreferencesManager.DEFAULT_URL)
    val isDesktopMode by preferencesManager.isDesktopMode.collectAsState(initial = false)
    val urlHistory by preferencesManager.urlHistory.collectAsState(initial = listOf(PreferencesManager.DEFAULT_URL))

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val isKeyboardVisible = WindowInsets.isImeVisible

    BackHandler(enabled = true) {
        when {
            popupWebView != null -> {
                popupWebView?.destroy()
                popupWebView = null
            }
            webViewInstance?.canGoBack() == true -> {
                webViewInstance?.goBack()
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    onExitApp()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "Натисніть ще раз для виходу", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .imePadding()
    ) {
        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryAccent,
                trackColor = SurfaceDark
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        WebViewConfigurator.configure(this, isDesktopMode)
                        webViewClient = AntigravityWebViewClient { back, forward ->
                            canGoBack = back
                            canGoForward = forward
                        }
                        webChromeClient = AntigravityChromeClient(
                            onProgressChangedCallback = { progress -> loadProgress = progress },
                            onLaunchFileChooser = onLaunchFilePicker,
                            onOpenPopupWindow = { childView -> popupWebView = childView },
                            onClosePopupWindow = {
                                popupWebView?.destroy()
                                popupWebView = null
                            }
                        )
                        loadUrl(savedUrl)
                        webViewInstance = this
                    }
                },
                update = { view ->
                    WebViewConfigurator.applyDesktopMode(view, isDesktopMode)
                }
            )

            popupWebView?.let { childWebView ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark)
                ) {
                    Surface(
                        color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Авторизація",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    childWebView.destroy()
                                    popupWebView = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        factory = {
                            (childWebView.parent as? ViewGroup)?.removeView(childWebView)
                            childWebView
                        }
                    )
                }
            }
        }

        AnimatedVisibility(visible = !isKeyboardVisible && popupWebView == null) {
            BottomControlBar(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isDesktopMode = isDesktopMode,
                onBackClick = { webViewInstance?.goBack() },
                onForwardClick = { webViewInstance?.goForward() },
                onHomeClick = {
                    scope.launch {
                        preferencesManager.resetToDefault()
                        webViewInstance?.loadUrl(PreferencesManager.DEFAULT_URL)
                    }
                },
                onReloadClick = { webViewInstance?.reload() },
                onToggleDesktopMode = {
                    scope.launch {
                        val nextMode = !isDesktopMode
                        preferencesManager.setDesktopMode(nextMode)
                        webViewInstance?.let {
                            WebViewConfigurator.applyDesktopMode(it, nextMode)
                            it.reload()
                        }
                    }
                },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    }

    if (showSettingsDialog) {
        UrlSettingsDialog(
            currentUrl = webViewInstance?.url ?: savedUrl,
            urlHistory = urlHistory,
            onDismiss = { showSettingsDialog = false },
            onApplyUrl = { newUrl ->
                showSettingsDialog = false
                scope.launch {
                    preferencesManager.setCurrentUrl(newUrl)
                    webViewInstance?.loadUrl(newUrl)
                }
            },
            onResetDefault = {
                showSettingsDialog = false
                scope.launch {
                    preferencesManager.resetToDefault()
                    webViewInstance?.loadUrl(PreferencesManager.DEFAULT_URL)
                }
            },
            onClearSession = {
                showSettingsDialog = false
                WebViewConfigurator.clearSessionAndCache(webViewInstance)
                Toast.makeText(context, "Кеш та сесію очищено", Toast.LENGTH_SHORT).show()
                webViewInstance?.reload()
            }
        )
    }
}
