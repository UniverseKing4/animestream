# 📺 Anime Streaming Guide - v4.0.0

## ✅ WHAT'S WORKING NOW

### Video Playback
- **Demo Streams**: 100% working (Mux, Unified Streaming, Akamai)
- **Real Anime**: Integrated via Consumet API (GogoAnime + Zoro/HiAnime)
- **Player**: ExoPlayer with controls, quality selector, error handling

### How It Works
1. **Browse** → See trending/popular anime from AniList
2. **Select Anime** → View details, description, genres, episodes
3. **Click Episode** → Opens video player
4. **Auto-Play** → Video starts immediately

## 🔧 STREAMING ARCHITECTURE

### Multi-Source System
```
Episode Click
    ↓
Parse Episode ID (anime-title-episode-X)
    ↓
Search GogoAnime API → Find anime → Get episode → Extract M3U8
    ↓ (if fails)
Search Zoro/HiAnime API → Find anime → Get episode → Extract M3U8
    ↓ (if fails)
Fallback to Demo Streams
    ↓
ExoPlayer plays video
```

### API Endpoints Used
1. **GogoAnime**: `https://api.consumet.org/anime/gogoanime/{anime-slug}`
2. **Zoro/HiAnime**: `https://api.consumet.org/anime/zoro/{anime-slug}`
3. **Streaming**: `https://api.consumet.org/anime/{provider}/watch/{episode-id}`

### Episode ID Mapping
- **AniList Format**: `spy-x-family-episode-1`
- **Parsed**: `anime-slug = "spy-x-family"`, `episode-number = 1`
- **Process**: Search anime by slug → Find episode by number → Get streaming links

## 📱 USER EXPERIENCE

### What You'll See
- **Loading**: Spinner while fetching streams
- **Playing**: Video with controls (play/pause, seek, quality)
- **Error**: Retry button + helpful message
- **Demo Fallback**: If real sources fail, demo streams play automatically

### Quality Options
- Multiple sources with different qualities (1080p, 720p, 480p, etc.)
- Source selector button (gear icon) to switch between providers
- Auto-selects first available source

## 🚀 CURRENT STATUS

### ✅ Fully Implemented
- [x] Episode list with thumbnails
- [x] Click navigation to player
- [x] Multi-source streaming (GogoAnime + Zoro)
- [x] Smart episode ID parsing
- [x] Fallback demo streams
- [x] ExoPlayer integration
- [x] Quality selector
- [x] Error handling with retry
- [x] Loading states
- [x] Timeout handling (8s per API call)

### ⚠️ Known Limitations
- **Consumet API**: Public instance may be rate-limited or down
- **Episode Matching**: Depends on anime title matching between AniList and streaming providers
- **Demo Streams**: Shown when real anime sources unavailable

### 🔮 Future Improvements
- [ ] Self-hosted Consumet instance for reliability
- [ ] More streaming providers (AnimePahe, 9anime, etc.)
- [ ] Better episode ID mapping (use MAL ID, AniList ID)
- [ ] Subtitle support
- [ ] Download episodes
- [ ] Watch history
- [ ] Continue watching

## 🛠️ TROUBLESHOOTING

### "Unable to load video"
**Cause**: API timeout or no sources found  
**Solution**: Tap "Retry" button or wait for demo streams

### Demo streams playing instead of anime
**Cause**: Consumet API down or anime not found on streaming providers  
**Solution**: This is expected behavior - demo streams prove player works

### Episode not found
**Cause**: Episode ID doesn't match streaming provider format  
**Solution**: Future update will improve ID mapping

## 📊 TECHNICAL DETAILS

### Dependencies
- **Ktor**: HTTP client for API calls
- **ExoPlayer**: Video playback
- **Hilt**: Dependency injection
- **Jetpack Compose**: UI framework

### Timeouts
- API calls: 8000ms (8 seconds)
- Retry: Manual via button
- Fallback: Immediate to demo streams

### Logging
All streaming attempts logged with tag `AnimeAPI`:
- Search attempts
- Episode matching
- Source extraction
- Errors

### Error Handling
- Try GogoAnime → catch exception
- Try Zoro → catch exception
- Fallback to demo → always succeeds
- Show error UI with retry option

## 🎯 TESTING

### How to Test
1. Download `animestream-v4.0.0.apk` from GitHub Releases
2. Install on Android device
3. Open app → Browse anime
4. Click any anime → Scroll to episodes
5. Click episode → Video should play

### Expected Behavior
- **Best case**: Real anime plays from GogoAnime/Zoro
- **Fallback**: Demo stream plays (proves player works)
- **Worst case**: Error with retry button

## 📝 VERSION HISTORY

### v4.0.0 (Current)
- Multi-source streaming (GogoAnime + Zoro)
- Smart episode ID parsing
- Demo stream fallback
- Better error messages

### v3.1.0
- Fixed player controls
- Added demo streams

### v3.0.0
- Initial streaming implementation
- ExoPlayer integration

### v2.0.0
- AniList + Jikan API integration
- Browse/search functionality

## 🔗 RESOURCES

- **GitHub**: https://github.com/UniverseKing4/animestream
- **Consumet Docs**: https://docs.consumet.org
- **AniList API**: https://anilist.gitbook.io/anilist-apiv2-docs
- **ExoPlayer**: https://exoplayer.dev

---

**Note**: This app is for educational purposes. All anime content is sourced from third-party APIs. No content is hosted on our servers.
