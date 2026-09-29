package es.upm.pokemonapp.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpeciesRemoteDataSource {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.tcgdex.net/v2/en/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val service by lazy {
        retrofit.create(SpeciesApiService::class.java)
    }

    fun fetchSpeciesInfo(
        speciesName: String,
        onSuccess: (SpeciesDetails) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Primero intentamos obtener la carta directamente por ID
        val call = service.getCardInfo(speciesName)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val bodyString = try { response.body()?.string() } catch (_: Exception) { null }
                    if (bodyString != null) {
                        try {
                            val gson = Gson()
                            val jsonElem = JsonParser.parseString(bodyString)
                            if (jsonElem.isJsonObject) {
                                val card = gson.fromJson(jsonElem, Card::class.java)
                                onSuccess(card)
                                return
                            } else if (jsonElem.isJsonArray) {
                                val arr = jsonElem.asJsonArray
                                if (arr.size() > 0) {
                                    val card = gson.fromJson(arr[0], Card::class.java)
                                    onSuccess(card)
                                    return
                                }
                            }
                            // Si no es objeto ni array con elementos, fallback a búsqueda por nombre
                            searchCardByName(speciesName, onSuccess, onError)
                        } catch (_: Exception) {
                            // Si falla el parseo, intentamos la búsqueda por nombre
                            searchCardByName(speciesName, onSuccess, onError)
                        }
                    } else {
                        searchCardByName(speciesName, onSuccess, onError)
                    }
                } else {
                    // Si no encuentra por ID, intenta buscar por nombre
                    searchCardByName(speciesName, onSuccess, onError)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Si falla, intenta buscar por nombre
                searchCardByName(speciesName, onSuccess, onError)
            }
        })
    }

    private fun searchCardByName(
        cardName: String,
        onSuccess: (SpeciesDetails) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val searchCall = service.searchCards(cardName, "1")
        searchCall.enqueue(object : Callback<CardSearchResponse> {
            override fun onResponse(
                call: Call<CardSearchResponse>,
                response: Response<CardSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val cardList = response.body()?.data
                    if (!cardList.isNullOrEmpty()) {
                        onSuccess(cardList[0])
                    } else {
                        onError(Exception("No card found with name: $cardName"))
                    }
                } else {
                    onError(Exception("Response error: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<CardSearchResponse>, t: Throwable) {
                onError(t)
            }
        })
    }

    fun fetchRandomSpecies(
        onSuccess: (Species) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val randomOffset = (0 until 10000).random()
        val allCardsCall = service.getAllCards()

        Log.d(
            "SpeciesRemoteDataSource",
            "Fetching all cards to pick a random card, seed: $randomOffset"
        )

        allCardsCall.enqueue(object : Callback<List<CardSummary>> {
            override fun onResponse(
                call: Call<List<CardSummary>>,
                response: Response<List<CardSummary>>
            ) {
                if (response.isSuccessful) {
                    val cardList = response.body()

                    if (!cardList.isNullOrEmpty()) {
                        val index = randomOffset % cardList.size
                        val randomCard = cardList[index]

                        Log.d(
                            "SpeciesRemoteDataSource",
                            "Random card selected: ID=${randomCard.id}, Name=${randomCard.name}"
                        )

                        val cardDetailsCall = service.getSpeciesInfo(randomCard.id)

                        cardDetailsCall.enqueue(object : Callback<SpeciesDetails> {
                            override fun onResponse(
                                call: Call<SpeciesDetails>,
                                response: Response<SpeciesDetails>
                            ) {
                                if (response.isSuccessful) {
                                    val fullCard = response.body()

                                    if (fullCard != null) {
                                        Log.d(
                                            "SpeciesRemoteDataSource",
                                            "Full card loaded: ID=${fullCard.id}, Name=${fullCard.name}"
                                        )

                                        onSuccess(fullCard)
                                    } else {
                                        Log.w(
                                            "SpeciesRemoteDataSource",
                                            "Full card response was empty."
                                        )
                                        onError(Exception("Full card response was empty"))
                                    }
                                } else {
                                    Log.e(
                                        "SpeciesRemoteDataSource",
                                        "Card details response error: ${response.code()}"
                                    )
                                    onError(Exception("Card details response error: ${response.code()}"))
                                }
                            }

                            override fun onFailure(call: Call<SpeciesDetails>, t: Throwable) {
                                Log.e(
                                    "SpeciesRemoteDataSource",
                                    "Error fetching full card details.",
                                    t
                                )
                                onError(t)
                            }
                        })
                    } else {
                        onError(Exception("No cards found"))
                    }
                } else {
                    onError(Exception("Response error: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<List<CardSummary>>, t: Throwable) {
                onError(t)
            }
        })
    }

    fun fetchSpeciesMedia(
        cardId: String,
        onSuccess: (SpeciesMedia) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val call = service.getSpeciesMedia(cardId, "1")
        call.enqueue(object : Callback<SpeciesMedia> {
            override fun onResponse(
                call: Call<SpeciesMedia>,
                response: Response<SpeciesMedia>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it) }
                        ?: onError(Exception("Null response body"))
                } else {
                    onError(Exception("Response error: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<SpeciesMedia>, t: Throwable) {
                onError(t)
            }
        })
    }
}
