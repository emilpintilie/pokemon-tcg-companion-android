package es.upm.pokemonapp.data.network

// Response para obtener una lista de cartas (búsqueda)
data class CardSearchResponse(
    val data: List<Card>
)

// Respuesta resumida de la API para listados de cartas
data class CardSummary(
    val id: String,
    val localId: String? = null,
    val name: String? = null,
    val image: String? = null
)

// Respuesta para obtener una sola carta con toda su información
data class Card(
    val category: String,
    val trainerType: String? = null,
    val id: String,
    val illustrator: String?,
    val image: String,
    val localId: String,
    val name: String,
    val rarity: String?,
    val set: CardSet,
    val variants: Variants?,
    val variants_detailed: List<VariantDetailed>?,
    val dexId: List<Int>?,
    val hp: Int?,
    val types: List<String>?,
    val evolveFrom: String?,
    val description: String?,
    val stage: String?,
    val attacks: List<Attack>?,
    val weaknesses: List<Weakness>?,
    val retreat: Int?,
    val regulationMark: String?,
    val legal: Legal?,
    val updated: String?,
    val pricing: Pricing?
)

data class CardSet(
    val cardCount: CardCount,
    val id: String,
    val logo: String,
    val name: String,
    val symbol: String
)

data class CardCount(
    val official: Int,
    val total: Int
)

data class Variants(
    val firstEdition: Boolean,
    val holo: Boolean,
    val normal: Boolean,
    val reverse: Boolean,
    val wPromo: Boolean
)

data class VariantDetailed(
    val type: String,
    val size: String,
    val variantId: String
)

data class Attack(
    val cost: List<String>,
    val name: String,
    val effect: String,
    val damage: Int?
)

data class Weakness(
    val type: String,
    val value: String
)

data class Legal(
    val standard: Boolean,
    val expanded: Boolean
)

data class Pricing(
    val cardmarket: CardMarketPricing?,
    val tcgplayer: Any?
)

data class CardMarketPricing(
    val updated: String,
    val unit: String,
    val idProduct: Int,
    val avg: Double,
    val low: Double,
    val trend: Double,
    val avg1: Double,
    val avg7: Double,
    val avg30: Double,
    val avg_holo: Double?,
    val low_holo: Double?,
    val trend_holo: Double?,
    val avg1_holo: Double?,
    val avg7_holo: Double?,
    val avg30_holo: Double?
)

// Para compatibilidad con código anterior
typealias SpeciesResponse = CardSearchResponse
typealias Species = Card
typealias SpeciesDetails = Card
typealias SpeciesMedia = CardSearchResponse






