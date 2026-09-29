package es.upm.pokemonapp.data.room

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoordinatesRepository(private val dao: ICoordinatesDao) {

    suspend fun saveCoordinates(
        cardId: String,
        cardName: String,
        userName: String,
        userEmail: String,
        cardPrice: Double,
        status: String,
        latitude: Double,
        longitude: Double,
        imageUrl: String
    ) {
        val entity = CoordinatesEntity(
            timestamp = System.currentTimeMillis(),
            cardId = cardId,
            cardName = cardName,
            userName = userName,
            userEmail = userEmail,
            cardPrice = cardPrice,
            status = status,
            latitude = latitude,
            longitude = longitude,
            imageUrl = imageUrl
        )
        dao.insert(entity)
    }

    // Método para compatibilidad con código anterior
    suspend fun saveCoordinatesLegacy(
        species: String,
        type: String,
        isEndangered: Boolean,
        latitude: Double,
        longitude: Double
    ) {
        val entity = CoordinatesEntity(
            timestamp = System.currentTimeMillis(),
            cardId = species,
            cardName = type,
            latitude = latitude,
            longitude = longitude
        )
        dao.insert(entity)
    }

    suspend fun getAllCoordinates(): List<CoordinatesEntity> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun deleteWithTimestamp(timestamp: Long) = withContext(Dispatchers.IO) {
        dao.deleteWithTimestamp(timestamp)
    }

    suspend fun updateByTimestamp(
        timestamp: Long,
        cardId: String,
        cardName: String,
        status: String
    ) = withContext(Dispatchers.IO) {
        dao.updateByTimestamp(timestamp, cardId, cardName, status)
    }

    suspend fun getFormattedCoordinates(): List<List<String>> = withContext(Dispatchers.IO) {
        getAllCoordinates().map { entity ->
            listOf(
                entity.timestamp.toString(),
                entity.latitude.toString(),
                entity.longitude.toString(),
                entity.cardName,
                entity.cardId,
                entity.status
            )
        }
    }
}