package es.upm.pokemonapp.data.decklocal;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "decks")
public class DeckEntity {
    @PrimaryKey(autoGenerate = true)
    public int deckId;

    @NonNull
    public String name;

    public long createdAt;

    public DeckEntity(@NonNull String name, long createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }
}

