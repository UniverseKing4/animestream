package com.animestream.app

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Bundle
import java.io.ByteArrayInputStream

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var container: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var currentDomain: String? = null
    
    private val allowedDomains = mapOf(
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
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    
                    if (url.startsWith("intent://") || url.startsWith("market://") || 
                        url.startsWith("android-app://")) {
                        return true
                    }
                    
                    if (isAdUrl(url)) {
                        return true
                    }
                    
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val domain = extractDomain(url)
                        if (currentDomain != null && domain != currentDomain) {
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
            
            loadDataWithBaseURL(null, HTML, "text/html", "UTF-8", null)
        }
        
        container.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
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

    companion object {
        const val HTML = """
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
.loader{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:rgba(0,0,0,0.9);padding:24px 32px;border-radius:16px;display:none;z-index:1000;backdrop-filter:blur(10px)}
.loader.show{display:block;animation:fadeIn 0.2s}
.loader-text{color:#fff;font-size:14px;font-weight:600;text-align:center;margin-bottom:12px}
.spinner{width:40px;height:40px;margin:0 auto;border:3px solid #333;border-top-color:#667eea;border-radius:50%;animation:spin 0.8s linear infinite}
@keyframes fadeIn{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:translateY(0)}}
@keyframes spin{to{transform:rotate(360deg)}}
.links a{animation:slideIn 0.3s ease forwards;opacity:0}
.links a:nth-child(1){animation-delay:0.05s}
.links a:nth-child(2){animation-delay:0.1s}
.links a:nth-child(3){animation-delay:0.15s}
.links a:nth-child(4){animation-delay:0.2s}
.links a:nth-child(5){animation-delay:0.25s}
.links a:nth-child(6){animation-delay:0.3s}
.links a:nth-child(7){animation-delay:0.35s}
.links a:nth-child(8){animation-delay:0.4s}
.links a:nth-child(9){animation-delay:0.45s}
.links a:nth-child(10){animation-delay:0.5s}
@keyframes slideIn{to{opacity:1;transform:translateX(0)}from{opacity:0;transform:translateX(-10px)}}
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
<a href="#" onclick="navigate('https://9animetv.to/home','9animetv.to',this);return false"><span class="icon">9</span>9Anime TV</a>
<a href="#" onclick="navigate('https://anikai.to/home','anikai.to',this);return false"><span class="icon">A</span>Anikai</a>
<a href="#" onclick="navigate('https://www.animeparadise.moe/','animeparadise.moe',this);return false"><span class="icon">P</span>Anime Paradise</a>
<a href="#" onclick="navigate('https://animepahe.si/','animepahe.si',this);return false"><span class="icon">A</span>AnimePahe</a>
<a href="#" onclick="navigate('https://animotvslash.org/','animotvslash.org',this);return false"><span class="icon">T</span>AnimoTV Slash</a>
<a href="#" onclick="navigate('https://aniwatchtv.to/home','aniwatchtv.to',this);return false"><span class="icon">W</span>AniWatch TV</a>
<a href="#" onclick="navigate('https://gogoanimes.cv/','gogoanimes.cv',this);return false"><span class="icon">G</span>GogoAnimes</a>
<a href="#" onclick="navigate('https://hianime.to/home','hianime.to',this);return false"><span class="icon">H</span>HiAnime</a>
<a href="#" onclick="navigate('https://hianimes.se/home','hianimes.se',this);return false"><span class="icon">H</span>HiAnimes</a>
<a href="#" onclick="navigate('https://miruro.su/','miruro.su',this);return false"><span class="icon">M</span>Miruro</a>
</div>
</div>
</body>
</html>
"""
    }
}
