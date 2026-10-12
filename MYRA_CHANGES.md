# Myra – changes in this build

## 1. NouTube features in the Myra browser (YouTube)
- `browser/YouTubeEnhancer.kt` – strips ad data from YouTube API responses (fetch/XHR/initial data), removes ad items from feeds, hides Shorts, auto-skips leftover ads.
- Toggles: `YouTubeEnhancer.blockAds`, `YouTubeEnhancer.hideShorts`.

## 2. Background play fix
- `MyraWebView` (in `BrowserController.kt`) always reports a visible window, so the page never pauses when the app is backgrounded.
- `MyraMediaBridge`: page `<video>/<audio>` play/pause is reported to native, so the media foreground service + notification start when YOU play something (not only when the agent does).
- Auto-resume if the site pauses itself in the background, and auto-dismiss of "Continue watching?" prompts.
- `MainActivity` tells the browser when the app goes to background/foreground.

## 3. Status orb on the Voice screen
- `ui/components/CurlNoiseOrb.kt` – `MyraStatusOrb` = native state halo + WebGL Curl Noise Orb (+ native fallback orb if WebGL fails).
- `ui/components/MyraOrbState.kt` – OFF (grey) / CONNECTING (violet) / LISTENING (cyan-blue) / SPEAKING (pink) / MUTED (amber) / PAUSED (slate).
- `assets/orb/curl_noise_orb.html` – orb now takes `orbSetState(name)`, uses phone-safe shader precision and reports WebGL failure.
- `ui/VoiceScreen.kt` – coloured status chip + title ("Myra is OFF / Listening / Speaking …") driven by the same state.

## 4. Permanent conversation memory (saved to local files)
- `memory2/ConversationArchive.kt` – every message (voice + typed, yours + Myra's) is saved to
  `<app files>/conversations/YYYY-MM-DD.jsonl` (for recall) and one readable `Myra_Conversations.txt`.
  Hooked into `Memory2Database.saveConversationEntry()`, so no path can miss a message. "Clear chat" does NOT delete it.
- `GeminiLiveManager` now enables `inputAudioTranscription` + `outputAudioTranscription` (before this, spoken words were never transcribed, so voice chats were not saved).
- `BackgroundVoiceService` turns those transcripts into saved USER / MYRA entries (partial replies kept when you interrupt).
- On every connect, the last ~40 messages are injected into Myra's system instruction; new tool `recall_conversation`
  (query / days_ago / date / export) lets her search ALL older conversations ("kal kya baat hui thi?").
- Fixed: `getAllConversationEntries(limit)` returned the OLDEST rows; it now returns the newest `limit` rows.
- Settings -> Storage: "Conversation Memory" card (count, folder path, Export to Downloads/Myra, Delete archive).
