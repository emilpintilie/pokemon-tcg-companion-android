package es.upm.pokemonapp.data.room
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ICoordinatesDao {
    @Insert
    suspend fun insert(coordinates: CoordinatesEntity)

    @Query("SELECT * FROM coordinates")
    suspend fun getAll(): List<CoordinatesEntity>

    @Query("SELECT COUNT(*) FROM coordinates")
    fun getCount(): Int

    @Query("DELETE FROM coordinates WHERE timestamp = :timestamp")
    fun deleteWithTimestamp(timestamp: Long)

    @Query("SELECT * FROM coordinates WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getCoordinateByTimestamp(timestamp: Long): CoordinatesEntity?

    @Query("UPDATE coordinates SET cardId = :cardId, cardName = :cardName, status = :status WHERE timestamp = :timestamp")
    suspend fun updateByTimestamp(
        timestamp: Long,
        cardId: String,
        cardName: String,
        status: String
    )
}
