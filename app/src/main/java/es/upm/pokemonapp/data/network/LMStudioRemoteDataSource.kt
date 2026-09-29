package es.upm.pokemonapp.data.network

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import es.upm.pokemonapp.config.LMStudioConfig
import java.util.concurrent.TimeUnit

class LMStudioRemoteDataSource(
    private val lmStudioUrl: String = "http://10.0.2.2:1234/"  // Android emulator host mapping
) {

    private val retrofit by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(LMStudioConfig.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(LMStudioConfig.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(LMStudioConfig.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        Retrofit.Builder()
            .baseUrl(lmStudioUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val service by lazy {
        retrofit.create(LMStudioService::class.java)
    }

    fun getCardRecommendations(
        cardType: String,
        currentCards: List<String>,
        budget: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val prompt = buildCardRecommendationPrompt(cardType, currentCards, budget)
        val request = LMStudioRequest(
            input = prompt,
            max_output_tokens = LMStudioConfig.MAX_TOKENS_CARD_RECOMMENDATIONS
        )

        service.generateRecommendation(request).enqueue(object : Callback<LMStudioResponse> {
            override fun onResponse(call: Call<LMStudioResponse>, response: Response<LMStudioResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                        ?.output
                        ?.firstOrNull { it.type == "message" }
                        ?.content
                        ?: "No response"
                    Log.d("LMStudioRemoteDataSource", "Card recommendation received: ${result.take(100)}...")
                    onSuccess(result)
                } else {
                    val error = Exception("Response error: ${response.code()} - ${response.message()}")
                    Log.e("LMStudioRemoteDataSource", error.message ?: "Unknown error")
                    onError(error)
                }
            }

            override fun onFailure(call: Call<LMStudioResponse>, t: Throwable) {
                Log.e("LMStudioRemoteDataSource", "Error fetching card recommendations", t)
                onError(t)
            }
        })
    }

    fun getDeckRecommendations(
        deckType: String,
        format: String = "Standard",
        playerLevel: String = "Intermediate",
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val prompt = buildDeckRecommendationPrompt(deckType, format, playerLevel)
        val request = LMStudioRequest(
            input = prompt,
            max_output_tokens = LMStudioConfig.MAX_TOKENS_DECK_RECOMMENDATIONS
        )

        service.generateRecommendation(request).enqueue(object : Callback<LMStudioResponse> {
            override fun onResponse(call: Call<LMStudioResponse>, response: Response<LMStudioResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                        ?.output
                        ?.firstOrNull { it.type == "message" }
                        ?.content
                        ?: "No response"
                    Log.d("LMStudioRemoteDataSource", "Deck recommendation received: ${result.take(100)}...")
                    onSuccess(result)
                } else {
                    val error = Exception("Response error: ${response.code()} - ${response.message()}")
                    Log.e("LMStudioRemoteDataSource", error.message ?: "Unknown error")
                    onError(error)
                }
            }

            override fun onFailure(call: Call<LMStudioResponse>, t: Throwable) {
                Log.e("LMStudioRemoteDataSource", "Error fetching deck recommendations", t)
                onError(t)
            }
        })
    }

    fun getStrategyAdvice(
        currentCards: List<String>,
        opponentType: String,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val prompt = buildStrategyAdvicePrompt(currentCards, opponentType)
        val request = LMStudioRequestStrat(
            input = prompt,
            max_output_tokens = LMStudioConfig.MAX_TOKENS_STRATEGY_ADVICE
        )

        service.generateRecommendationStrat(request).enqueue(object : Callback<LMStudioResponse> {
            override fun onResponse(call: Call<LMStudioResponse>, response: Response<LMStudioResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                        ?.output
                        ?.firstOrNull { it.type == "message" }
                        ?.content
                        ?: "No response"
                    Log.d("LMStudioRemoteDataSource", "Strategy advice received: ${result.take(100)}...")
                    onSuccess(result)
                } else {
                    val error = Exception("Response error: ${response.code()} - ${response.message()}")
                    Log.e("LMStudioRemoteDataSource", error.message ?: "Unknown error")
                    onError(error)
                }
            }

            override fun onFailure(call: Call<LMStudioResponse>, t: Throwable) {
                Log.e("LMStudioRemoteDataSource", "Error fetching strategy advice", t)
                onError(t)
            }
        })
    }

    private fun buildCardRecommendationPrompt(
        cardType: String,
        currentCards: List<String>,
        budget: String?
    ): String {
        val cardsStr = currentCards.joinToString(", ")
        val budgetStr = budget?.let { "\nBudget: $it" } ?: ""

        return """Please recommend 5 Pokémon Trading Card Game cards that would be great additions to a deck.

Type/Theme: $cardType
Current cards in collection: $cardsStr$budgetStr

For each card recommendation, provide:
1. Card name
2. Why it's recommended
3. What type of strategy it supports

Format your response clearly with numbered recommendations."""
    }

    private fun buildDeckRecommendationPrompt(
        deckType: String,
        format: String,
        playerLevel: String
    ): String {
        return """Please recommend a complete Pokémon Trading Card Game deck for me.

Deck Type/Strategy: $deckType
Format: $format (Standard, Expanded, or Unlimited)
Player Level: $playerLevel

Please provide:
1. Deck name
2. Brief description
3. Main Pokémon cards (at least 3-5 key cards)
4. Support cards (Trainers, Supporters, Items)
5. Overall strategy of the deck
6. Estimated win rate / competitiveness level
7. Difficulty to pilot (Easy/Medium/Hard)

Make sure the deck composition is balanced and follows Pokémon TCG rules."""
    }

    private fun buildStrategyAdvicePrompt(
        currentCards: List<String>,
        opponentType: String
    ): String {
        val cardsStr = currentCards.joinToString(", ")

        return """I have a Pokémon Trading Card Game deck with these key cards: $cardsStr

I'm going to face a player using a $opponentType deck.

Please provide strategic advice:
1. What are the strengths of my deck against this matchup?
2. What are the weaknesses?
3. Which of my cards should I prioritize playing?
4. What tech cards or adjustments would help me win?
5. General strategy and win conditions

Be specific and tactical with your recommendations."""
    }
}







