package com.animestream.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, HTML, "text/html", "UTF-8", null)
        }
        setContentView(webView)
    }

    companion object {
        const val HTML = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
body{margin:0;padding:20px;font-family:Arial,sans-serif;background:#1a1a1a;color:#fff}
h1{text-align:center}
.links{max-width:600px;margin:0 auto}
a{display:block;padding:15px;margin:10px 0;background:#2a2a2a;color:#fff;text-decoration:none;border-radius:5px}
a:active{background:#3a3a3a}
</style>
</head>
<body>
<h1>Anime Sites</h1>
<div class="links">
<a href="https://anikai.to/home">Anikai</a>
<a href="https://9animetv.to/home">9Anime TV</a>
<a href="https://aniwatchtv.to/home">AniWatch TV</a>
<a href="https://hianime.to/home">HiAnime</a>
<a href="https://miruro.su/">Miruro</a>
<a href="https://animotvslash.org/">AnimoTV Slash</a>
<a href="https://www.animeparadise.moe/">Anime Paradise</a>
<a href="https://hianimes.se/">HiAnimes</a>
<a href="https://gogoanimes.cv/">GogoAnimes</a>
<a href="https://animepahe.si/">AnimePahe</a>
</div>
</body>
</html>
"""
    }
}
