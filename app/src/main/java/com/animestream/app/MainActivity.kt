package com.animestream.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, HTML, "text/html", "UTF-8", null)
        }
        setContentView(webView)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    companion object {
        const val HTML = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;padding:20px}
.container{max-width:600px;margin:0 auto}
h1{color:#fff;text-align:center;font-size:32px;margin-bottom:30px;text-shadow:0 2px 10px rgba(0,0,0,0.3);animation:fadeIn 0.5s}
.links{display:grid;gap:12px}
a{display:flex;align-items:center;padding:18px 20px;background:rgba(255,255,255,0.95);color:#333;text-decoration:none;border-radius:12px;font-size:16px;font-weight:500;box-shadow:0 4px 15px rgba(0,0,0,0.1);transition:all 0.3s;position:relative;overflow:hidden}
a:before{content:'';position:absolute;left:0;top:0;height:100%;width:4px;background:linear-gradient(135deg,#667eea,#764ba2);transition:width 0.3s}
a:active{transform:scale(0.98);box-shadow:0 2px 8px rgba(0,0,0,0.15)}
a:active:before{width:100%}
a:active{color:#fff}
.icon{width:24px;height:24px;margin-right:12px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:14px;font-weight:bold;flex-shrink:0}
@keyframes fadeIn{from{opacity:0;transform:translateY(-20px)}to{opacity:1;transform:translateY(0)}}
.links a{animation:slideIn 0.5s forwards;opacity:0}
.links a:nth-child(1){animation-delay:0.1s}
.links a:nth-child(2){animation-delay:0.15s}
.links a:nth-child(3){animation-delay:0.2s}
.links a:nth-child(4){animation-delay:0.25s}
.links a:nth-child(5){animation-delay:0.3s}
.links a:nth-child(6){animation-delay:0.35s}
.links a:nth-child(7){animation-delay:0.4s}
.links a:nth-child(8){animation-delay:0.45s}
.links a:nth-child(9){animation-delay:0.5s}
.links a:nth-child(10){animation-delay:0.55s}
@keyframes slideIn{to{opacity:1;transform:translateX(0)}from{opacity:0;transform:translateX(-20px)}}
</style>
</head>
<body>
<div class="container">
<h1>🎬 Anime Sites</h1>
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
