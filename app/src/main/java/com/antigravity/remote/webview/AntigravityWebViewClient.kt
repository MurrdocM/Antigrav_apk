package com.antigravity.remote.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class AntigravityWebViewClient(
    private val onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()
        view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme == "http" || scheme == "https") {
            return false
        }

        val context = view?.context ?: return true
        if (scheme == "intent") {
            try {
                val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrEmpty()) {
                        view.loadUrl(fallbackUrl)
                    }
                }
            } catch (_: Exception) {
            }
            return true
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val failingUrl = request.url?.toString().orEmpty()
            val errorDesc = error?.description?.toString().orEmpty()
            val html = """
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { background:#0D0E12; color:#E2E2E9; font-family:sans-serif; display:flex; flex-direction:column; align-items:center; justify-content:center; height:90vh; text-align:center; padding:20px; }
                        h2 { color:#7C4DFF; margin-bottom:8px; }
                        p { color:#90929C; font-size:14px; word-break:break-all; }
                        button { margin-top:20px; padding:12px 24px; background:#7C4DFF; color:#FFF; border:none; border-radius:8px; font-size:15px; font-weight:bold; }
                    </style>
                </head>
                <body>
                    <h2>Немає з'єднання</h2>
                    <p>$errorDesc</p>
                    <p>$failingUrl</p>
                    <button onclick="location.replace('$failingUrl')">Повторити</button>
                </body>
                </html>
            """.trimIndent()
            view?.loadDataWithBaseURL(failingUrl, html, "text/html", "UTF-8", failingUrl)
        }
    }
}
