package es.upm.pokemonapp.data.network

import es.upm.pokemonapp.data.network.SpeciesRemoteDataSource
import es.upm.pokemonapp.data.network.SpeciesDetails
import es.upm.pokemonapp.data.network.Species
import es.upm.pokemonapp.data.network.SpeciesMedia

class SpeciesRepository {

    private val remoteDataSource = SpeciesRemoteDataSource()
    private val lmStudioDataSource = LMStudioRemoteDataSource()

    fun getSpeciesInfo(
        speciesName: String,
        onSuccess: (SpeciesDetails) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        remoteDataSource.fetchSpeciesInfo(speciesName, onSuccess, onError)
    }

    fun getRandomSpecies(
        onSuccess: (Species) -> Unit,
        onError: (Throwable) -> Unit
    ){
        remoteDataSource.fetchRandomSpecies(onSuccess, onError)
    }

    fun getSpeciesMedia(
        cardId: String,
        onSuccess: (SpeciesMedia) -> Unit,
        onError: (Throwable) -> Unit
    ){
        remoteDataSource.fetchSpeciesMedia(cardId, onSuccess, onError)
    }

    // New LM Studio methods for card and deck recommendations
    fun getCardRecommendations(
        cardType: String,
        currentCards: List<String>,
        budget: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        lmStudioDataSource.getCardRecommendations(cardType, currentCards, budget, onSuccess, onError)
    }

    fun getDeckRecommendations(
        deckType: String,
        format: String = "Standard",
        playerLevel: String = "Intermediate",
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        lmStudioDataSource.getDeckRecommendations(deckType, format, playerLevel, onSuccess, onError)
    }

    fun getStrategyAdvice(
        currentCards: List<String>,
        opponentType: String,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        lmStudioDataSource.getStrategyAdvice(currentCards, opponentType, onSuccess, onError)
    }
}

