package com.antigravity.remote.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class AntigravityChromeClient(
    private val onProgressChangedCallback: (Int) -> Unit,
    private val onLaunchFileChooser: (Intent, ValueCallback<Array<Uri>>) -> Unit,
    private val onOpenPopupWindow: (WebView) -> Unit,
    private val onClosePopupWindow: () -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedCallback(newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        val intent = try {
            fileChooserParams?.createIntent() ?: createDefaultFileIntent()
        } catch (_: Exception) {
            createDefaultFileIntent()
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        onLaunchFileChooser(intent, filePathCallback)
        return true
    }

    private fun createDefaultFileIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val context = view?.context ?: return false
        val popupWebView = WebView(context)
        WebViewConfigurator.configure(popupWebView, isDesktopMode = false)

        popupWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false
        }

        popupWebView.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
                onClosePopupWindow()
            }
        }

        onOpenPopupWindow(popupWebView)

        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        transport.webView = popupWebView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        onClosePopupWindow()
    }
}
