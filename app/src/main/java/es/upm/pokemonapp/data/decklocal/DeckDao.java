package es.upm.pokemonapp.data.decklocal;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface DeckDao {
    @Insert
    long insertDeck(DeckEntity deck);

    @Insert
    void insertDeckCards(List<DeckCardEntity> deckCards);

    @Transaction
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    List<DeckWithCards> getAllDecksWithCards();

    @Transaction
    @Query("SELECT * FROM decks WHERE deckId = :deckId LIMIT 1")
    DeckWithCards getDeckWithCardsById(int deckId);

    @Query("UPDATE decks SET name = :name, createdAt = :createdAt WHERE deckId = :deckId")
    void updateDeck(int deckId, String name, long createdAt);

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId")
    void deleteDeckCards(int deckId);

    @Query("UPDATE deck_cards SET quantity = :quantity WHERE deckId = :deckId AND cardId = :cardId")
    void updateCardQuantity(int deckId, String cardId, int quantity);

    @Query("DELETE FROM deck_cards WHERE deckId = :deckId AND cardId = :cardId")
    void deleteDeckCard(int deckId, String cardId);
}

