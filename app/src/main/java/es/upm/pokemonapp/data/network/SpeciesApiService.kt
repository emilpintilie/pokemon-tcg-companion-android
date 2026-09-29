package es.upm.pokemonapp.data.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpeciesApiService {
    // Obtener información de una carta específica por su ID
    // Devolvemos ResponseBody para permitir parse manual cuando la respuesta
    // puede ser un objeto o un array JSON.
    @GET("cards/{cardId}")
    fun getCardInfo(@Path("cardId") cardId: String): Call<okhttp3.ResponseBody>

    // Obtener lista de todas las cartas (devuelve array)
    @GET("cards")
    fun getAllCards(): Call<List<CardSummary>>

    @GET("cards")
    fun getCardsFiltered(
        @Query("category") category: String? = null,
        @Query("types") type: String? = null,
        @Query("regulationMark") regulationMark: String? = null
    ): Call<List<CardSummary>>

    @GET("cards")
    fun getCardsPage(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("category") category: String? = null,
        @Query("types") type: String? = null
    ): Call<List<CardSummary>>

    // Buscar cartas por nombre (parámetro q)
    // La API devuelve un objeto con la propiedad `data: List<Card>`,
    // por eso usamos `CardSearchResponse` como tipo de respuesta.
    @GET("cards")
    fun searchCards(
        @Query("q") query: String,
        @Query("limit") limit: String = "1"
    ): Call<CardSearchResponse>

    // Mantener compatibilidad con métodos antiguos
    @GET("cards/{cardId}")
    fun getSpeciesInfo(@Path("cardId") cardId: String): Call<SpeciesDetails>

    @GET("cards")
    fun getAllSpeciesList(): Call<SpeciesResponse>

    // Obtener una carta usando limit/offset (se usa para obtener una carta aleatoria mediante offset)
    @GET("cards")
    fun getRandomCard(
        @Query("limit") limit: String = "1",
        @Query("offset") offset: String = "0"
    ): Call<CardSearchResponse>

    @GET("cards/{cardId}")
    fun getSpeciesMedia(
        @Path("cardId") cardId: String,
        @Query("limit") limit: String = "1"
    ): Call<SpeciesMedia>
}
