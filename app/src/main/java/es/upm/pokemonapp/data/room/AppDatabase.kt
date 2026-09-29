package es.upm.pokemonapp.data.room
import android.content.Context
import es.upm.pokemonapp.data.room.CoordinatesEntity
import es.upm.pokemonapp.data.room.ICoordinatesDao
import es.upm.pokemonapp.data.room.CoordinatesRepository
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

@Database(entities = [es.upm.pokemonapp.data.room.CoordinatesEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coordinatesDao(): ICoordinatesDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coordinates_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object AppContainer {
    private lateinit var appDatabase: AppDatabase

    fun syncDatabase(context: Context) {
        appDatabase = AppDatabase.getDatabase(context)

        // Sincronización de Firebase a Room
        syncFirebaseToRoom()

        // Subir datos de Room a Firebase
        uploadRoomDataToFirebase()
    }

    private fun syncFirebaseToRoom() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("coordinates")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dao = appDatabase.coordinatesDao()
                CoroutineScope(Dispatchers.IO).launch {
                    for (child in snapshot.children) {
                        val coordinates = child.getValue(es.upm.pokemonapp.data.room.CoordinatesEntity::class.java)
                        if (coordinates != null) {
                            val exists = dao.getCoordinateByTimestamp(coordinates.timestamp)
                            if (exists == null) {
                                dao.insert(coordinates)
                            }
                        }
                    }
                    Log.e("RealtimeSync", "All OK")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeSync", "Error al sincronizar: ${error.message}")
            }
        })
    }

    private fun uploadRoomDataToFirebase() {
        //To conect to specific DB
        // val database = FirebaseDatabase.getInstance("https://mad-environment-app-default-rtdb.europe-west1.firebasedatabase.app")
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("coordinates")

        CoroutineScope(Dispatchers.IO).launch {
            // Obtener los datos de la base de datos Room
            val coordinatesList = appDatabase.coordinatesDao().getAll()

            for (coordinates in coordinatesList) {
                // Subir cada coordenada a Firebase
                ref.child(coordinates.timestamp.toString()).setValue(coordinates)
                    .addOnSuccessListener {
                        Log.e("FirebaseUpload", "Datos subidos correctamente: ${coordinates.timestamp}")
                    }
                    .addOnFailureListener { error ->
                        Log.e("FirebaseUpload", "Error al subir datos: ${error.message}")
                    }
            }
        }
    }

    val coordinatesRepository: es.upm.pokemonapp.data.room.CoordinatesRepository by lazy {
        es.upm.pokemonapp.data.room.CoordinatesRepository(appDatabase.coordinatesDao())
    }
}