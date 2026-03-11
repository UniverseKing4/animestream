package com.animestream.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var container: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var currentDomain: String? = null
    private var isInPipMode = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var pendingDownload: PendingDownload? = null
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingDownload?.let { downloadFile(it.url, it.userAgent, it.contentDisposition, it.mimeType) }
        } else {
            Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_LONG).show()
        }
        pendingDownload = null
    }
    
    data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String
    )
    
    private val defaultSites = mapOf(
        "9animetv.to" to "https://9animetv.to/home",
        "anikai.to" to "https://anikai.to/home",
        "animeparadise.moe" to "https://www.animeparadise.moe/",
        "animepahe.si" to "https://animepahe.si/",
        "animotvslash.org" to "https://animotvslash.org/",
        "aniwatchtv.to" to "https://aniwatchtv.to/home",
        "gogoanimes.cv" to "https://gogoanimes.cv/",
        "hianime.to" to "https://hianime.to/home",
        "hianimes.se" to "https://hianimes.se/home",
        "miruro.su" to "https://miruro.su/"
    )
    
    private val allowedDomains: MutableMap<String, String> by lazy {
        loadCustomSites().toMutableMap()
    }
    
    private val adHosts = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "googletagmanager.com", "facebook.net",
        "facebook.com/tr", "connect.facebook.net", "adservice", "advertising",
        "analytics", "tracker", "banner", "popup", "popunder", "ad.doubleclick.net",
        "static.doubleclick.net", "m.doubleclick.net", "mediavisor.doubleclick.net",
        "pagead2.googlesyndication.com", "adserver", "ads-twitter.com",
        "ads.linkedin.com", "ads.pinterest.com", "ads.reddit.com",
        "ads.youtube.com", "adservice.google", "afs.googlesyndication.com",
        "tpc.googlesyndication.com", "pagead.googlesyndication.com",
        "pagead.l.google.com", "partnerad.l.google.com", "video-ad-stats.googlesyndication.com",
        "www.googletagservices.com", "www.google-analytics.com", "ssl.google-analytics.com",
        "adclick", "adserver", "adtech", "adview", "advertising.com",
        "adsystem", "adnxs.com", "advertising.com", "adsrvr.org",
        "adform.net", "serving-sys.com", "criteo.com", "outbrain.com",
        "taboola.com", "revcontent.com", "mgid.com", "propellerads.com",
        "popcash.net", "popads.net", "exoclick.com", "juicyads.com",
        "trafficjunky.com", "ero-advertising.com", "exosrv.com",
        "tsyndicate.com", "clksite.com", "adcash.com", "hilltopads.net",
        "clickadu.com", "adsterra.com", "adk2x.com", "adk2.co",
        "amskiploomr.com", "ak.amskiploomr.com"
    )
    
    private val adPatterns = listOf(
        Regex(".*[/.]ad[sx]?[/.].*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]ad[sx]?\\d+[/.].*", RegexOption.IGNORE_CASE),
        Regex(".*[/_]ads?[/_].*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]advert.*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]banner.*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]popup.*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]click.*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]track.*", RegexOption.IGNORE_CASE),
        Regex(".*[/.]analytic.*", RegexOption.IGNORE_CASE)
    )
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        container = FrameLayout(this)
        setContentView(container)
        
        webView = WebView(this).apply {
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun addSite(domain: String, url: String) {
                    val prefs = getSharedPreferences("sites", Context.MODE_PRIVATE)
                    prefs.edit().putString(domain, url).apply()
                    allowedDomains[domain] = url
                }
                
                @android.webkit.JavascriptInterface
                fun removeSite(domain: String) {
                    if (defaultSites.containsKey(domain)) return
                    val prefs = getSharedPreferences("sites", Context.MODE_PRIVATE)
                    prefs.edit().remove(domain).apply()
                    allowedDomains.remove(domain)
                }
                
                @android.webkit.JavascriptInterface
                fun isDefaultSite(domain: String): Boolean {
                    return defaultSites.containsKey(domain)
                }
            }, "AndroidBridge")
            
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                handleDownload(url, userAgent, contentDisposition, mimeType)
            }
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    
                    // Block intent/market redirects
                    if (url.startsWith("intent://") || url.startsWith("market://") || 
                        url.startsWith("android-app://")) {
                        return true
                    }
                    
                    // Block ads
                    if (isAdUrl(url)) {
                        return true
                    }
                    
                    // Domain lock protection
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val domain = extractDomain(url)
                        if (currentDomain != null && domain != currentDomain && !allowedDomains.containsKey(domain)) {
                            Toast.makeText(this@MainActivity, "Blocked redirect to $domain", Toast.LENGTH_SHORT).show()
                            return true
                        }
                        return false
                    }
                    
                    return true
                }
                
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                    
                    if (isAdUrl(url)) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                    }
                    
                    return super.shouldInterceptRequest(view, request)
                }
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    
                    customView = view
                    customViewCallback = callback
                    
                    webView.visibility = View.GONE
                    container.addView(customView, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    
                    hideSystemUI()
                }
                
                override fun onHideCustomView() {
                    if (customView == null) return
                    
                    webView.visibility = View.VISIBLE
                    container.removeView(customView)
                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    
                    showSystemUI()
                }
            }
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                blockNetworkImage = false
                loadsImagesAutomatically = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
            }
            
            loadDataWithBaseURL(null, generateHTML(), "text/html", "UTF-8", null)
        }
        
        container.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                } else if (webView.url == "about:blank" || webView.url?.contains("data:text/html") == true) {
                    finish()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    webView.loadDataWithBaseURL(null, generateHTML(), "text/html", "UTF-8", null)
                }
            }
        })
        
        checkSiteStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            scope.cancel()
            webView.destroy()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (customView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }
    
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPipMode && customView != null) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // PiP not supported or failed
            }
        }
    }
    
    private fun handleDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need storage permission
            downloadFile(url, userAgent, contentDisposition, mimeType)
        } else {
            // Android 9 and below need storage permission
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    downloadFile(url, userAgent, contentDisposition, mimeType)
                }
                else -> {
                    pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType)
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    private fun downloadFile(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null) {
                    addRequestHeader("Cookie", cookies)
                }
                setDescription("Downloading $fileName")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                } else {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
            }
            
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Downloading $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadCustomSites(): Map<String, String> {
        val prefs = getSharedPreferences("sites", Context.MODE_PRIVATE)
        val sites = defaultSites.toMutableMap()
        prefs.all.forEach { (domain, url) ->
            if (url is String) sites[domain] = url
        }
        return sites
    }
    
    private fun checkSiteStatus() {
        scope.launch {
            allowedDomains.forEach { (domain, url) ->
                launch {
                    val isOnline = withContext(Dispatchers.IO) {
                        try {
                            val connection = URL(url).openConnection() as HttpURLConnection
                            connection.requestMethod = "HEAD"
                            connection.connectTimeout = 3000
                            connection.readTimeout = 3000
                            connection.instanceFollowRedirects = true
                            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                            connection.connect()
                            val code = connection.responseCode
                            connection.disconnect()
                            code in 200..399
                        } catch (e: Exception) {
                            false
                        }
                    }
                    updateSiteStatus(url, isOnline)
                }
            }
        }
    }
    
    private fun updateSiteStatus(url: String, isOnline: Boolean) {
        val js = """
            (function() {
                const link = document.querySelector('a[data-url="${url.replace("\"", "\\\"")}"]');
                if (link) {
                    const indicator = link.querySelector('.status');
                    if (indicator) {
                        indicator.className = 'status ${if (isOnline) "online" else "offline"}';
                    }
                }
            })();
        """
        runOnUiThread {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                // WebView might be destroyed
            }
        }
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }
    
    private fun isAdUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        if (adHosts.any { lowerUrl.contains(it) }) {
            return true
        }
        
        if (adPatterns.any { it.matches(url) }) {
            return true
        }
        
        return false
    }
    
    private fun generateHTML(): String {
        val siteLinks = allowedDomains.map { (domain, url) ->
            val name = domain.split(".")[0].replaceFirstChar { it.uppercase() }
            val initial = name.first().uppercase()
            val isDefault = defaultSites.containsKey(domain)
            val deleteBtn = if (!isDefault) """<button class="delete-btn" onclick="event.preventDefault();event.stopPropagation();deleteSite('$domain',this.parentElement);return false">×</button>""" else ""
            """<a href="#" data-url="$url" data-domain="$domain" onclick="navigate('$url','$domain',this);return false">
                <span class="icon">$initial</span>
                <span class="site-info">
                    <span class="site-name">$name</span>
                    <span class="status checking"></span>
                </span>
                $deleteBtn
            </a>"""
        }.joinToString("\n")
        
        return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@500;600;700&display=swap');
*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
body{font-family:'Inter',system-ui,-apple-system,sans-serif;background:#0f0f0f;min-height:100vh;padding:20px 16px 100px;overflow-x:hidden}
.container{max-width:480px;margin:0 auto}
h1{color:#fff;text-align:center;font-size:26px;font-weight:700;margin-bottom:6px;letter-spacing:-0.5px;animation:fadeIn 0.5s}
.subtitle{color:#888;text-align:center;font-size:13px;font-weight:500;margin-bottom:28px;animation:fadeIn 0.5s 0.1s backwards}
.links{display:flex;flex-direction:column;gap:8px}
a{display:flex;align-items:center;padding:14px 16px;background:#1a1a1a;color:#fff;text-decoration:none;border-radius:12px;font-size:15px;font-weight:600;border:1px solid #2a2a2a;transition:all 0.15s ease;position:relative;will-change:transform}
a:active{transform:scale(0.98);background:#222;border-color:#333}
a.loading{pointer-events:none;opacity:0.6}
.icon{width:36px;height:36px;margin-right:12px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:15px;font-weight:700;flex-shrink:0;transition:transform 0.3s}
a.loading .icon{animation:spin 1s linear infinite}
.site-info{flex:1;display:flex;align-items:center;justify-content:space-between}
.site-name{flex:1}
.status{width:8px;height:8px;border-radius:50%;background:#444;margin-left:8px}
.status.checking{animation:pulse 1.5s infinite}
.status.online{background:#0f0}
.status.offline{background:#f00}
.delete-btn{width:28px;height:28px;border-radius:50%;background:#f00;color:#fff;border:none;font-size:20px;line-height:1;cursor:pointer;display:flex;align-items:center;justify-content:center;margin-left:8px;flex-shrink:0}
.delete-btn:active{background:#c00}
.loader{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:rgba(0,0,0,0.9);padding:24px 32px;border-radius:16px;display:none;z-index:1000;backdrop-filter:blur(10px)}
.loader.show{display:block;animation:fadeIn 0.2s}
.loader-text{color:#fff;font-size:14px;font-weight:600;text-align:center;margin-bottom:12px}
.spinner{width:40px;height:40px;margin:0 auto;border:3px solid #333;border-top-color:#667eea;border-radius:50%;animation:spin 0.8s linear infinite}
.add-btn{position:fixed;bottom:24px;right:24px;width:56px;height:56px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:28px;box-shadow:0 4px 12px rgba(102,126,234,0.4);cursor:pointer;border:none}
.add-btn:active{transform:scale(0.95)}
.modal{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.8);display:none;align-items:center;justify-content:center;z-index:2000;padding:20px}
.modal.show{display:flex}
.modal-content{background:#1a1a1a;border-radius:16px;padding:24px;width:100%;max-width:400px;border:1px solid #2a2a2a}
.modal-title{color:#fff;font-size:20px;font-weight:700;margin-bottom:20px}
input{width:100%;padding:12px;background:#0f0f0f;border:1px solid #2a2a2a;border-radius:8px;color:#fff;font-size:14px;margin-bottom:12px;font-family:inherit}
input::placeholder{color:#666}
.btn-group{display:flex;gap:8px;margin-top:16px}
.btn{flex:1;padding:12px;border-radius:8px;border:none;font-weight:600;font-size:14px;cursor:pointer}
.btn-primary{background:linear-gradient(135deg,#667eea,#764ba2);color:#fff}
.btn-secondary{background:#2a2a2a;color:#fff}
.btn:active{opacity:0.8}
@keyframes fadeIn{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:translateY(0)}}
@keyframes spin{to{transform:rotate(360deg)}}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.3}}
.links a{animation:slideIn 0.3s ease forwards;opacity:0}
@keyframes slideIn{to{opacity:1;transform:translateX(0)}from{opacity:0;transform:translateX(-10px)}}
@keyframes slideOut{to{opacity:0;transform:translateX(-20px);height:0;margin:0;padding:0}from{opacity:1;transform:translateX(0)}}
</style>
<script>
let loader;
function navigate(url, domain, elem) {
    if(!loader) loader = document.getElementById('loader');
    elem.classList.add('loading');
    loader.classList.add('show');
    window.currentDomain = domain;
    setTimeout(() => window.location.href = url, 300);
}
function showAddModal() {
    document.getElementById('modal').classList.add('show');
}
function hideAddModal() {
    document.getElementById('modal').classList.remove('show');
    document.getElementById('siteName').value = '';
    document.getElementById('siteUrl').value = '';
}
function addSite() {
    const name = document.getElementById('siteName').value.trim();
    const url = document.getElementById('siteUrl').value.trim();
    if (!name || !url) {
        alert('Please fill all fields');
        return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        alert('URL must start with http:// or https://');
        return;
    }
    try {
        const domain = new URL(url).hostname;
        window.AndroidBridge.addSite(domain, url);
        hideAddModal();
        location.reload();
    } catch(e) {
        alert('Invalid URL');
    }
}
function deleteSite(domain, elem) {
    if (confirm('Remove this site?')) {
        window.AndroidBridge.removeSite(domain);
        elem.style.animation = 'slideOut 0.3s ease forwards';
        setTimeout(() => elem.remove(), 300);
    }
}
</script>
</head>
<body>
<div class="loader" id="loader">
<div class="loader-text">Loading...</div>
<div class="spinner"></div>
</div>
<div class="container">
<h1>🎬 Anime Sites</h1>
<p class="subtitle">Choose your streaming platform</p>
<div class="links">
$siteLinks
</div>
</div>
<button class="add-btn" onclick="showAddModal()">+</button>
<div class="modal" id="modal">
<div class="modal-content">
<div class="modal-title">Add Custom Site</div>
<input type="text" id="siteName" placeholder="Site Name (e.g., MyAnime)">
<input type="url" id="siteUrl" placeholder="URL (e.g., https://example.com)">
<div class="btn-group">
<button class="btn btn-secondary" onclick="hideAddModal()">Cancel</button>
<button class="btn btn-primary" onclick="addSite()">Add</button>
</div>
</div>
</div>
</body>
</html>
"""
    }

}
