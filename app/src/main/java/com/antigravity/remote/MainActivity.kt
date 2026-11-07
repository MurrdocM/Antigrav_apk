package com.antigravity.remote

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.antigravity.remote.data.PreferencesManager
import com.antigravity.remote.ui.AntigravityScreen
import com.antigravity.remote.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileUploadCallback ?: return@registerForActivityResult
        fileUploadCallback = null

        val data = result.data
        val clipData = data?.clipData
        val results: Array<Uri>? = when {
            result.resultCode != RESULT_OK -> null
            clipData != null && clipData.itemCount > 0 -> {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }
            data?.data != null -> arrayOf(data.data!!)
            else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        }
        callback.onReceiveValue(results)
    }

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(applicationContext)
        requestMediaPermissionsIfNeeded()

        setContent {
            AntigravityTheme {
                AntigravityScreen(
                    preferencesManager = preferencesManager,
                    onLaunchFilePicker = { intent, callback ->
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = callback
                        try {
                            fileChooserLauncher.launch(intent)
                        } catch (_: Exception) {
                            fileUploadCallback?.onReceiveValue(null)
                            fileUploadCallback = null
                        }
                    },
                    onExitApp = { finish() }
                )
            }
        }
    }

    private fun requestMediaPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            runtimePermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onStop() {
        super.onStop()
        CookieManager.getInstance().flush()
    }
}
