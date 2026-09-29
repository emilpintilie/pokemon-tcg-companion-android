package es.upm.pokemonapp.data.decklocal;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "deck_cards",
    primaryKeys = {"deckId", "cardId"},
    foreignKeys = {
        @ForeignKey(
            entity = DeckEntity.class,
            parentColumns = "deckId",
            childColumns = "deckId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {@Index(value = {"deckId"})}
)
public class DeckCardEntity {
    public int deckId;

    @NonNull
    public String cardId;

    @NonNull
    public String cardName;

    public int quantity;

    @NonNull
    public String imageUrl;

    public DeckCardEntity(int deckId, @NonNull String cardId, @NonNull String cardName, int quantity, @NonNull String imageUrl) {
        this.deckId = deckId;
        this.cardId = cardId;
        this.cardName = cardName;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }
}

