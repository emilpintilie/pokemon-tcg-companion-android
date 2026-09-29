package es.upm.pokemonapp.data.network

data class CardRecommendation(
    val cardName: String,
    val reason: String,
    val type: String = "",
    val strategy: String = ""
)

data class DeckRecommendation(
    val deckName: String,
    val deckDescription: String,
    val mainCards: List<String>,
    val supportCards: List<String>,
    val strategy: String,
    val winRate: String = "Not specified",
    val difficulty: String = "Medium"
)

data class RecommendationsResponse(
    val cardRecommendations: List<CardRecommendation> = emptyList(),
    val deckRecommendations: List<DeckRecommendation> = emptyList(),
    val generalAdvice: String = ""
)

