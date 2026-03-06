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
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;padding:24px 20px 120px}
.container{max-width:600px;margin:0 auto}
h1{color:#fff;text-align:center;font-size:28px;font-weight:700;margin-bottom:8px;text-shadow:0 2px 20px rgba(0,0,0,0.2);animation:fadeIn 0.6s;letter-spacing:-0.5px}
.subtitle{color:rgba(255,255,255,0.85);text-align:center;font-size:14px;font-weight:500;margin-bottom:32px;animation:fadeIn 0.6s 0.1s backwards}
.links{display:grid;gap:10px}
a{display:flex;align-items:center;padding:16px 18px;background:rgba(255,255,255,0.98);color:#1a1a1a;text-decoration:none;border-radius:16px;font-size:15px;font-weight:600;box-shadow:0 2px 12px rgba(0,0,0,0.08);transition:all 0.2s cubic-bezier(0.4,0,0.2,1);position:relative;overflow:hidden;backdrop-filter:blur(10px)}
a:before{content:'';position:absolute;left:0;top:0;height:100%;width:3px;background:linear-gradient(135deg,#667eea,#764ba2);transition:width 0.3s cubic-bezier(0.4,0,0.2,1)}
a:active{transform:scale(0.97);box-shadow:0 1px 6px rgba(0,0,0,0.12)}
a:active:before{width:100%;opacity:0.1}
.icon{width:40px;height:40px;margin-right:14px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:12px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:16px;font-weight:700;flex-shrink:0;box-shadow:0 2px 8px rgba(102,126,234,0.3)}
@keyframes fadeIn{from{opacity:0;transform:translateY(-10px)}to{opacity:1;transform:translateY(0)}}
.links a{animation:slideIn 0.4s cubic-bezier(0.4,0,0.2,1) forwards;opacity:0}
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
@keyframes slideIn{to{opacity:1;transform:translateX(0)}from{opacity:0;transform:translateX(-15px)}}
</style>
</head>
<body>
<div class="container">
<h1>🎬 Anime Sites</h1>
<p class="subtitle">Choose your streaming platform</p>
<div class="links">
<a href="https://anikai.to/home"><span class="icon">A</span>Anikai</a>
<a href="https://9animetv.to/home"><span class="icon">9</span>9Anime TV</a>
<a href="https://aniwatchtv.to/home"><span class="icon">W</span>AniWatch TV</a>
<a href="https://hianime.to/home"><span class="icon">H</span>HiAnime</a>
<a href="https://miruro.su/"><span class="icon">M</span>Miruro</a>
<a href="https://animotvslash.org/"><span class="icon">T</span>AnimoTV Slash</a>
<a href="https://www.animeparadise.moe/"><span class="icon">P</span>Anime Paradise</a>
<a href="https://hianimes.se/"><span class="icon">H</span>HiAnimes</a>
<a href="https://gogoanimes.cv/"><span class="icon">G</span>GogoAnimes</a>
<a href="https://animepahe.si/"><span class="icon">A</span>AnimePahe</a>
</div>
</div>
</body>
</html>
"""
    }
}
