package es.upm.pokemonapp.data.decklocal;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DeckEntity.class, DeckCardEntity.class}, version = 3, exportSchema = false)
public abstract class DeckDatabase extends RoomDatabase {

    public abstract DeckDao deckDao();

    private static volatile DeckDatabase INSTANCE;

    public static DeckDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DeckDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        DeckDatabase.class,
                        "deck_local_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}

