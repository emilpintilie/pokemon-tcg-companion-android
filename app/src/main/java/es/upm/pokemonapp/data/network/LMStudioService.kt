package es.upm.pokemonapp.data.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Modelos para LM Studio/Ollama
data class LMStudioRequest(
    val model: String = "google/gemma-4-e4b",
    val system_prompt: String = "Just answer with the name of a Pokémon, if requested a card, or the names of the Pokémon if requested a deck, nothing else.",
    val input: String,
    val stream: Boolean = false,
    val temperature: Double = 0.7,
    val max_output_tokens: Int = 1000
)

data class LMStudioRequestStrat(
    val model: String = "google/gemma-4-e4b",
    val system_prompt: String = "Answer all the questions, but be concise.",
    val input: String,
    val stream: Boolean = false,
    val temperature: Double = 0.7,
    val max_output_tokens: Int = 1000
)

data class LMStudioResponse(
    val model_instance_id: String,
    val output: List<LMStudioOutput>,
    val stats: LMStudioStats,
    val response_id: String
)

data class LMStudioOutput(
    val type: String,
    val content: String
)

data class LMStudioStats(
    val input_tokens: Int,
    val total_output_tokens: Int,
    val reasoning_output_tokens: Int,
    val tokens_per_second: Double,
    val time_to_first_token_seconds: Double,
    val model_load_time_seconds: Double
)

interface LMStudioService {
    @POST("/api/v1/chat")
    fun generateRecommendation(
        @Body request: LMStudioRequest
    ): Call<LMStudioResponse>

    @POST("/api/v1/chat")
    fun generateRecommendationStrat(
        @Body request: LMStudioRequestStrat
    ): Call<LMStudioResponse>
}

