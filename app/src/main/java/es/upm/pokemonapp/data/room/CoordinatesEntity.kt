package es.upm.pokemonapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coordinates")
data class CoordinatesEntity(
    @PrimaryKey val timestamp: Long = 0L,
    val cardId: String = "",
    val cardName: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val cardPrice: Double = 0.0,
    val status: String = "Available",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = ""
)

// Mantener compatibilidad con código anterior
typealias CardEntity = CoordinatesEntity
