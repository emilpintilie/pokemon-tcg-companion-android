# Project documentation (current state)

Date: 2026-05-26
Last update: 2026-05-26T00:00:00Z

This document summarizes the main screens, utilities, data layers, and recent changes in `mssde-pokemon-app`. It is meant as high-level documentation for developers working on the project.

---

## Index
- Activities (screens)
- Utilities and extensions
- Persistence layer (Room)
- Network layer (Retrofit)
- Authentication (Firebase)
- Build notes and recent changes

---

## 1) Activities (screens)

- `app/src/main/java/es/upm/pokemonapp/MainActivity.kt`
  - Main entry screen. Checks authentication and shows a random species from the remote repository.
  - Uses `SpeciesRepository.getRandomSpecies()`.
  - Handles drawer/toolbar and bottom navigation through `NavigationUtils`.
  - Redirects unauthenticated users to `AuthActivity`.

- `app/src/main/java/es/upm/pokemonapp/OpenStreetMapActivity.kt`
  - Map screen (osmdroid). Shows the current user location and markers stored in Room / Firebase.
  - Long press creates a new marker (card registration flow).
  - Uses `LocationHelper` and `AppContainer.syncDatabase()`.

- `app/src/main/java/es/upm/pokemonapp/SpeciesActivity.kt`
  - Room-backed list of saved records (cards). Lets the user inspect each entry on an embedded map and edit/delete it.
  - Uses an internal `CoordinatesAdapter`.
  - Loads card details and images when available.

- `app/src/main/java/es/upm/pokemonapp/SettingsActivity.kt`
  - Settings screen for dark mode and location updates.
  - Persists preferences in `SharedPreferences`.
  - Allows editing the Firebase profile display name.

- `app/src/main/java/es/upm/pokemonapp/DeckActivity.kt`
  - Deck overview screen.
  - Lists local decks and opens a detail view for each deck.
  - Shows card images, quantities, and `+/-` buttons to edit copies.
  - Opens `DeckBuilderActivity` for deck editing/creation.
  - Reads decks from the local Room database (`deck_local_database`) only.

- `app/src/main/java/es/upm/pokemonapp/DeckBuilderActivity.kt`
  - Deck creation screen with a vertical two-column grid.
  - Loads cards from the TCGdex API and uses `assets.tcgdex.net` images.
  - Includes search by name or ID.
  - Prompts the user to choose a deck format before creation:
    - Standard: regulation marks `H`, `I`, and `J`.
    - Expanded: all cards.
  - Supports category and type filters.
  - Renders cards progressively while scrolling.
  - Allows repeated cards and stores `quantity` and `imageUrl` in Room.

---

## 2) Utilities and extensions

- `app/src/main/java/es/upm/pokemonapp/utils/NavigationUtils.kt`
  - Centralizes bottom navigation behavior.
  - Avoids reopening the same activity and shows a message when the user is already on that screen.

- `app/src/main/java/es/upm/pokemonapp/utils/LocationHelper.kt`
  - Singleton that implements `LocationListener` and manages `LocationManager`.
  - Provides `enableLocationUpdates`, `disableLocationUpdates`, `startLocationUpdates`, `stopLocationUpdates`, `getLastKnownLocation`, and callback registration.

- `app/src/main/java/es/upm/pokemonapp/utils/Extensions.kt`
  - Common helpers:
    - `Context.showToast(message: String)`
    - `ImageView.loadFromUrl(url: String?)`
    - `goToActivityWithoutReturn(fromActivity, destination)`

---

## 3) Persistence layer (Room)

- `app/src/main/java/es/upm/pokemonapp/data/room/CoordinatesEntity.kt`
  - `@Entity(tableName = "coordinates")` with: timestamp (PK), cardId, cardName, sellerCode, userName, userEmail, cardPrice, status, latitude, longitude, imageUrl.

- `app/src/main/java/es/upm/pokemonapp/data/room/ICoordinatesDao.kt`
  - DAO methods for insert, read, count, delete, get by timestamp, and update.

- `app/src/main/java/es/upm/pokemonapp/data/room/CoordinatesRepository.kt`
  - High-level API for saving and querying coordinates.

- `app/src/main/java/es/upm/pokemonapp/data/room/AppDatabase.kt` and `AppContainer`
  - Room database and Firebase sync container.
  - Supports sync from Firebase to Room and Room to Firebase.

- Local deck persistence (no Firebase sync):
  - `app/src/main/java/es/upm/pokemonapp/data/decklocal/DeckEntity.java`
  - `app/src/main/java/es/upm/pokemonapp/data/decklocal/DeckCardEntity.java`
  - `app/src/main/java/es/upm/pokemonapp/data/decklocal/DeckWithCards.java`
  - `app/src/main/java/es/upm/pokemonapp/data/decklocal/DeckDao.java`
  - `app/src/main/java/es/upm/pokemonapp/data/decklocal/DeckDatabase.java`
  - `DeckCardEntity` stores `quantity` and `imageUrl`.

---

## 4) Network layer (Retrofit)

- `app/src/main/java/es/upm/pokemonapp/data/network/SpeciesApiService.kt`
  - Retrofit interface for `https://api.tcgdex.net/v2/en/`.
  - Important methods: `getCardInfo`, `getAllCards`, `getCardsFiltered`, `searchCards`, `getSpeciesInfo`, `getAllSpeciesList`, `getRandomCard`, `getSpeciesMedia`.

- `app/src/main/java/es/upm/pokemonapp/data/network/SpeciesResponse.kt`
  - Data models for the API response: `Card`, `CardSummary`, `CardSet`, `Variants`, `Attack`, `Weakness`, `Pricing`, etc.
  - Includes type aliases for compatibility (`Species`, `SpeciesDetails`, ...).

- `app/src/main/java/es/upm/pokemonapp/data/network/SpeciesRemoteDataSource.kt`
  - Retrofit-based implementation for API calls.
  - Tries to fetch by ID first and falls back to name search when needed.
  - `fetchRandomSpecies` selects a random card from the list and then loads its full details.

- `app/src/main/java/es/upm/pokemonapp/data/network/SpeciesRepository.kt`
  - Thin wrapper exposing `getSpeciesInfo`, `getRandomSpecies`, and `getSpeciesMedia`.

---

## 5) Authentication (Firebase)

- `app/src/main/java/es/upm/pokemonapp/auth/AuthActivity.kt`
  - Entry point for authentication flows.

- `app/src/main/java/es/upm/pokemonapp/auth/LoginActivity.kt`
  - Email/password login via `FirebaseAuth`.

- `app/src/main/java/es/upm/pokemonapp/auth/RegisterActivity.kt`
  - User registration with `FirebaseAuth.createUserWithEmailAndPassword`.

- `app/src/main/java/es/upm/pokemonapp/auth/RecoverPasswordActivityCustom.kt`
  - Password recovery via `sendPasswordResetEmail`.

- `app/src/main/java/es/upm/pokemonapp/auth/FirebaseAuthHelper.kt`
  - Small helper around `FirebaseAuth` and `AuthUI.signOut`.

---

## 6) Build notes and recent changes

- Relevant dependencies in `app/build.gradle.kts`: AppCompat, Material, Room, Retrofit, Glide, Firebase Auth, Firebase Database, Firebase UI, osmdroid, etc.

- Current changes in the repository:
  - `DeckActivity` and `DeckBuilderActivity` migrated to Kotlin (`DeckActivity.kt`, `DeckBuilderActivity.kt`).
  - Legacy Java files remain only as backup and are not part of the active code path.
  - `DeckBuilderActivity` now uses `CardSummary` for the TCGdex list responses.
  - Image loading was fixed using the API image URL and an `id/localId` fallback.
  - Standard deck filtering is applied from the API with `regulationMark=H,I,J`.
  - Build validation was confirmed with `./gradlew assembleDebug` using Android Studio's JBR JDK 21.

---

## 7) Recommendations and next steps

- Install or configure JDK 21 locally if you need to build outside Android Studio's JBR.
- Improve runtime permission handling for location if needed.
- Add unit/integration tests if you want more robustness.

---

## Changelog

- 2026-05-17: Initial project documentation created.
- 2026-05-26: Documentation updated for Kotlin migration, deck module changes, card summary model, image loading fixes, and standard/expanded deck formats.
