package com.joel.videodownloader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var addressInput: EditText
    private lateinit var downloadBar: View
    private lateinit var detectedTitle: TextView
    private lateinit var detectedStatus: TextView
    private lateinit var pageProgress: ProgressBar
    private var currentPageUrl: String? = null
    private var detectedMediaUrl: String? = null

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        addressInput = findViewById(R.id.addressInput)
        downloadBar = findViewById(R.id.downloadBar)
        detectedTitle = findViewById(R.id.detectedTitle)
        detectedStatus = findViewById(R.id.detectedStatus)
        pageProgress = findViewById(R.id.pageProgress)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString.replace("; wv", "")
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(false)
        }
        webView.addJavascriptInterface(MediaBridge(), "JoelMediaBridge")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                pageProgress.progress = newProgress
                pageProgress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) detectedTitle.text = title
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return if (uri.scheme == "http" || uri.scheme == "https") false else {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                currentPageUrl = url
                detectedMediaUrl = null
                downloadBar.visibility = View.GONE
                addressInput.setText(url.orEmpty())
            }

            override fun onPageFinished(view: WebView, url: String) {
                currentPageUrl = url
                addressInput.setText(url)
                injectPlaybackDetector(view)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (looksLikeMedia(url)) detectedMediaUrl = url
                return super.shouldInterceptRequest(view, request)
            }
        }

        addressInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                loadAddress(addressInput.text.toString())
                true
            } else false
        }
        findViewById<View>(R.id.backButton).setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        findViewById<View>(R.id.forwardButton).setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        findViewById<View>(R.id.refreshButton).setOnClickListener { webView.reload() }
        findViewById<View>(R.id.updateEngineButton).setOnClickListener { button ->
            button.isEnabled = false
            Toast.makeText(this, "Updating downloader engine…", Toast.LENGTH_SHORT).show()
            Thread {
                val result = EngineUpdater.updateNightly(this, force = true)
                runOnUiThread {
                    button.isEnabled = true
                    Toast.makeText(this, result.fold({ it }, { "Update failed: ${it.cause?.message ?: it.message}" }), Toast.LENGTH_LONG).show()
                }
            }.start()
        }
        findViewById<View>(R.id.downloadButton).setOnClickListener { openDownloadSheet() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val shared = intent.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        loadAddress(shared ?: Prefs.lastUrl(this) ?: HOME)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val shared = intent.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)
        if (!shared.isNullOrBlank()) loadAddress(shared)
    }

    private fun loadAddress(raw: String) {
        val text = raw.trim()
        val url = when {
            UrlTools.isWebUrl(text) -> text
            text.contains('.') && !text.contains(' ') -> "https://$text"
            else -> "https://www.google.com/search?q=${Uri.encode(text)}"
        }
        webView.loadUrl(url)
    }

    private fun openDownloadSheet() {
        val pageUrl = currentPageUrl
        if (pageUrl.isNullOrBlank() || !UrlTools.isWebUrl(pageUrl)) {
            Toast.makeText(this, "No downloadable page is open", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setLastUrl(this, pageUrl)
        val cookies = CookieManager.getInstance().getCookie(pageUrl)
        DownloadSheetActivity.open(this, pageUrl, cookies, detectedMediaUrl)
    }

    private fun injectPlaybackDetector(view: WebView) {
        view.evaluateJavascript(JS_DETECTOR, null)
    }

    private inner class MediaBridge {
        @JavascriptInterface
        fun onPlaybackStarted(source: String?, title: String?) {
            runOnUiThread {
                source?.takeIf { it.startsWith("http") }?.let { detectedMediaUrl = it }
                detectedTitle.text = title?.takeIf { it.isNotBlank() } ?: webView.title ?: "Video detected"
                detectedStatus.text = "Playback detected · tap Download"
                downloadBar.visibility = View.VISIBLE
            }
        }
    }

    private fun looksLikeMedia(url: String): Boolean {
        val clean = url.substringBefore('?').lowercase(Locale.US)
        return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".m3u8") ||
            clean.endsWith(".mpd") || clean.endsWith(".m4a") || clean.endsWith(".mp3")
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("JoelMediaBridge")
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val HOME = "https://www.google.com"
        private val JS_DETECTOR = """
            (function() {
              if (window.__joelDetectorInstalled) return;
              window.__joelDetectorInstalled = true;
              function notify(el) {
                try {
                  JoelMediaBridge.onPlaybackStarted(el.currentSrc || el.src || '', document.title || 'Video detected');
                } catch (e) {}
              }
              document.addEventListener('play', function(e) {
                if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) notify(e.target);
              }, true);
              document.querySelectorAll('video,audio').forEach(function(el) {
                el.addEventListener('play', function(){ notify(el); }, true);
                if (!el.paused) notify(el);
              });
              new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                  m.addedNodes.forEach(function(n) {
                    if (!n.querySelectorAll) return;
                    var list = [];
                    if (n.tagName === 'VIDEO' || n.tagName === 'AUDIO') list.push(n);
                    n.querySelectorAll('video,audio').forEach(function(x){ list.push(x); });
                    list.forEach(function(el){ el.addEventListener('play', function(){ notify(el); }, true); });
                  });
                });
              }).observe(document.documentElement, {childList:true, subtree:true});
            })();
        """.trimIndent()
    }
}
