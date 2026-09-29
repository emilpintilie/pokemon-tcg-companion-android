# Quick Start Guide - LM Studio Integration

## 5-Minute Setup

### Step 1: Download and Install LM Studio
- Visit https://lmstudio.ai/
- Download for your operating system
- Install the application

### Step 2: Download the Model
1. Open LM Studio
2. Click "Library"
3. Search for "gemma-4-e4b"
4. Click "Download" (takes 10-20 minutes, ~8GB)

### Step 3: Start the Server
1. Go to "Local Server" tab
2. Select "google/gemma-4-e4b" model
3. Click "Start Server"
4. Verify: http://localhost:11434 shows a response

### Step 4: Run the App
1. Open Android Studio
2. Run the Pokémon TCG App
3. Tap the toolbar menu (≡) → "AI Recommendations"

### Step 5: Get Your First Recommendation
- Choose a card type
- Click "Get Card Recommendations"
- Wait 30-60 seconds for the first response
- Read the AI-generated recommendations!

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│           Android App (RecommendationsActivity)         │
│                                                          │
│  Card Recs │ Deck Recs │ Strategy Advice                 │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│          SpeciesRepository                              │
│  (Orchestrates all network requests)                    │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│       LMStudioRemoteDataSource                          │
│  (Connects to LM Studio via Retrofit)                   │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│    Your Computer's LM Studio (localhost:11434)          │
│                                                          │
│  Model: google/gemma-4-e4b (running locally)           │
└─────────────────────────────────────────────────────────┘
```

## File Structure

```
app/src/main/java/es/upm/pokemonapp/
├── RecommendationsActivity.kt          # Main UI for recommendations
├── config/
│   └── LMStudioConfig.kt              # Configuration settings
└── data/network/
    ├── LMStudioService.kt             # Retrofit API interface
    ├── LMStudioRemoteDataSource.kt     # Network calls & prompts
    ├── RecommendationResponse.kt       # Data models
    └── SpeciesRepository.kt            # (Updated with new methods)
```

## Key Classes Explained

### RecommendationsActivity
- Displays UI for three types of recommendations
- Handles user input from spinners and text fields
- Calls repository methods and displays results

### LMStudioService
- Retrofit interface for HTTP calls
- Defines the `/api/generate` endpoint
- Handles request/response serialization

### LMStudioRemoteDataSource
- Creates prompts for each recommendation type
- Makes HTTP calls to LM Studio
- Handles callbacks and error handling

### SpeciesRepository
- Unified interface for all data sources
- Routes requests to appropriate data source
- Now includes: `getCardRecommendations()`, `getDeckRecommendations()`, `getStrategyAdvice()`

## Network Flow Example: Card Recommendations

```
1. User selects "Rare Pokémon" from CardTypeSpinner
2. Clicks "Get Card Recommendations" button
   ↓
3. RecommendationsActivity calls:
   speciesRepository.getCardRecommendations("Rare Pokémon", ...)
   ↓
4. SpeciesRepository delegates to:
   lmStudioDataSource.getCardRecommendations(...)
   ↓
5. LMStudioRemoteDataSource:
   a) Builds prompt: buildCardRecommendationPrompt()
   b) Creates LMStudioRequest object
   c) Makes HTTP POST to /api/generate
   ↓
6. LM Studio (on your PC):
   a) Receives the prompt
   b) Processes with gemma-4-e4b model
   c) Returns generated recommendations
   ↓
7. Response comes back to:
   LMStudioRemoteDataSource.onResponse()
   ↓
8. Success callback in RecommendationsActivity:
   textView.text = recommendations
   ↓
9. User sees the AI recommendations on screen!
```

## Configuration

Edit `LMStudioConfig.kt` to customize:

```kotlin
const val LM_STUDIO_URL = "http://localhost:11434/"  // Change for remote connection
const val MODEL_NAME = "gemma-4-e4b"                  // Model name
const val TEMPERATURE = 0.7                           // 0=deterministic, 1=creative
const val MAX_TOKENS_CARD_RECOMMENDATIONS = 1500      // Response length
```

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| "Connection refused" | LM Studio not running | Start Local Server in LM Studio |
| "Timeout" | Network issue | Use correct IP address, check firewall |
| "Model not found" | Model not downloaded | Download in Library tab |
| App freezes | Slow response | Be patient, first request is slow |
| Wrong recommendations | Model needs tuning | Check temperature & max_tokens |

## Performance Expectations

| First Request | Subsequent Requests | Device Load |
|---|---|---|
| 30-60 seconds | 10-30 seconds | Moderate CPU/RAM usage |

## Next Steps

1. ✅ Try the basic recommendation features
2. 📊 Customize prompts in `LMStudioRemoteDataSource.kt`
3. 🔧 Adjust temperature & max_tokens for different results
4. 🌐 Change URL for different network setups
5. 📱 Deploy to physical device with proper IP configuration

## For Developers

### Adding a New Recommendation Type

1. Add method in `LMStudioRemoteDataSource.kt`:
```kotlin
fun getNewRecommendation(param: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
    val prompt = buildNewPrompt(param)
    val request = LMStudioRequest(prompt = prompt, max_tokens = 1500)
    service.generateRecommendation(request).enqueue(...)
}
```

2. Add to `SpeciesRepository.kt`:
```kotlin
fun getNewRecommendation(param: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
    lmStudioDataSource.getNewRecommendation(param, onSuccess, onError)
}
```

3. Add UI in `RecommendationsActivity.kt` and `activity_recommendations.xml`

## Support & Debugging

Enable verbose logging:
```kotlin
// In LMStudioRemoteDataSource.kt
Log.d("LMStudioRemoteDataSource", "Request: $prompt")
Log.d("LMStudioRemoteDataSource", "Response: ${result.take(500)}...")
```

View logs in Android Studio:
```
View → Tool Windows → Logcat
Filter: "LMStudioRemoteDataSource"
```

---

**Happy recommending! 🎴✨**
