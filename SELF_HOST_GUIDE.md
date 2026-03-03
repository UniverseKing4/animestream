# Self-Hosting Guide for Real Anime Streaming

## THE REALITY

**ALL public anime streaming APIs are broken/dead in March 2026:**
- aniwatch-api.vercel.app → Returns empty results
- api.consumet.org → Down/archived
- All public instances → Broken

## THE ONLY SOLUTION

**Deploy your OWN aniwatch-api instance:**

### Option 1: Railway (Recommended)
1. Fork: https://github.com/ghoshRitesh12/aniwatch-api
2. Go to: https://railway.app
3. Click "New Project" → "Deploy from GitHub"
4. Select your forked repo
5. Railway auto-detects Node.js
6. Get your URL: `https://your-app.railway.app`

### Option 2: Render
1. Fork: https://github.com/ghoshRitesh12/aniwatch-api
2. Go to: https://render.com
3. Click "New Web Service"
4. Connect GitHub repo
5. Build: `npm install`
6. Start: `npm start`
7. Get URL: `https://your-app.onrender.com`

### Option 3: Vercel
1. Fork: https://github.com/ghoshRitesh12/aniwatch-api
2. Go to: https://vercel.com
3. Import project
4. Deploy
5. Get URL: `https://your-app.vercel.app`

## Update Android App

Once deployed, update `AnimeApiClient.kt`:

```kotlin
private val aniwatchBaseUrl = "https://YOUR-INSTANCE.railway.app"
```

## Cost
- Railway: Free tier (500 hours/month)
- Render: Free tier (750 hours/month)
- Vercel: Free tier (100GB bandwidth)

## Legal Notice
Self-hosting anime scraping APIs operates in a legal gray area. Use at your own risk.
