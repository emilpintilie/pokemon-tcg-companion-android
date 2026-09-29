package es.upm.pokemonapp.data.decklocal;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class DeckWithCards {
    @Embedded
    public DeckEntity deck;

    @Relation(parentColumn = "deckId", entityColumn = "deckId")
    public List<DeckCardEntity> cards;
}

