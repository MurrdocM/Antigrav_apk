package com.antigravity.remote.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object WebViewConfigurator {

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, isDesktopMode: Boolean) {
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        applyDesktopMode(webView, isDesktopMode)
        webView.addJavascriptInterface(BlobDownloader(webView.context), "AntigravityBlobDownloader")
        setupDownloadListener(webView)
    }

    fun applyDesktopMode(webView: WebView, isDesktop: Boolean) {
        val defaultUa = WebSettings.getDefaultUserAgent(webView.context)
        val cleanMobileUa = defaultUa
            .replace("; wv", "")
            .replace(Regex("Version/\\d+\\.\\d+\\s+"), "")

        webView.settings.apply {
            userAgentString = if (isDesktop) DESKTOP_USER_AGENT else cleanMobileUa
            useWideViewPort = isDesktop
            loadWithOverviewMode = isDesktop
        }
    }

    private fun setupDownloadListener(webView: WebView) {
        val context = webView.context
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("blob:") || url.startsWith("data:")) {
                val js = """
                    (function() {
                        fetch('$url')
                            .then(r => r.blob())
                            .then(blob => {
                                var reader = new FileReader();
                                reader.onloadend = function() {
                                    window.AntigravityBlobDownloader.saveBase64(
                                        reader.result,
                                        '${URLUtil.guessFileName(url, contentDisposition, mimeType)}'
                                    );
                                };
                                reader.readAsDataURL(blob);
                            });
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js, null)
                return@setDownloadListener
            }

            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val cookies = CookieManager.getInstance().getCookie(url)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    if (!cookies.isNullOrEmpty()) addRequestHeader("cookie", cookies)
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(fileName)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(context, "Завантаження: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Помилка завантаження: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearSessionAndCache(webView: WebView?) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.clearFormData()
    }

    class BlobDownloader(private val context: Context) {
        @JavascriptInterface
        fun saveBase64(base64Data: String, fileName: String) {
            try {
                val cleanBase64 = base64Data.substringAfter("base64,")
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName.ifEmpty { "artifact_${System.currentTimeMillis()}.bin" })
                FileOutputStream(file).use { it.write(bytes) }
                Toast.makeText(context, "Збережено в Downloads: ${file.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Помилка збереження файлу", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
