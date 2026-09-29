package es.upm.pokemonapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import es.upm.pokemonapp.data.decklocal.DeckCardEntity
import es.upm.pokemonapp.data.decklocal.DeckDatabase
import es.upm.pokemonapp.data.decklocal.DeckEntity
import es.upm.pokemonapp.data.network.CardSummary
import es.upm.pokemonapp.data.network.SpeciesApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DeckBuilderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DECK_ID = "extra_deck_id"
        private const val PRELOAD_THRESHOLD = 10
        private const val INITIAL_VISIBLE_CARDS = 48
        private const val VISIBLE_CARDS_STEP = 24
    }

    private lateinit var editDeckName: EditText
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvActiveFilters: TextView
    private lateinit var searchCardBar: SearchView
    private lateinit var buttonFilters: Button
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var buttonSaveDeck: Button
    private lateinit var buttonCancelDeck: Button

    private val allCards = mutableListOf<CardItem>()
    private val filteredCards = mutableListOf<CardItem>()
    private val selectedCardCounts = HashMap<String, Int>()

    private lateinit var gridAdapter: DeckGridAdapter
    private lateinit var deckDatabase: DeckDatabase
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.tcgdex.net/v2/en/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val speciesApiService: SpeciesApiService = retrofit.create(SpeciesApiService::class.java)
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var editingDeckId: Int = -1
    private var currentSearchQuery = ""
    private var selectedType = ""
    private var selectedCategory = ""
    private var currentDeckFormat = "standard"
    private var currentApiCategory: String? = null
    private var currentApiType: String? = null
    private var isLoadingCards = false
    private var hasMoreCards = true
    private var cardsQueryToken = 0L
    private var visibleCardLimit = INITIAL_VISIBLE_CARDS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_deck_builder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val cutout = insets.displayCutout
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (cutout != null) {
                // Insets seguros para no pisar cámara/notch
                val top = maxOf(cutout.safeInsetTop, systemBars.top)
                val left = maxOf(cutout.safeInsetLeft, systemBars.left)
                val right = maxOf(cutout.safeInsetRight, systemBars.right)
                view.setPadding(left, top, right, view.paddingBottom)
            } else {
                view.updatePadding(
                    left = systemBars.left,
                    top = systemBars.top,
                    right = systemBars.right,
                    bottom = view.paddingBottom
                )
            }
            insets
        }

        deckDatabase = DeckDatabase.getInstance(applicationContext)
        editingDeckId = intent.getIntExtra(EXTRA_DECK_ID, -1)

        editDeckName = findViewById(R.id.editDeckName)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tvActiveFilters = findViewById(R.id.tvActiveFilters)
        searchCardBar = findViewById(R.id.searchCardBar)
        buttonFilters = findViewById(R.id.buttonFilters)
        cardsRecyclerView = findViewById(R.id.cardsRecyclerView)
        buttonSaveDeck = findViewById(R.id.buttonSaveDeck)
        buttonCancelDeck = findViewById(R.id.buttonCancelDeck)

        gridAdapter = DeckGridAdapter()
        val gridLayoutManager = GridLayoutManager(this, 2)
        cardsRecyclerView.layoutManager = gridLayoutManager
        cardsRecyclerView.adapter = gridAdapter
        cardsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val lastVisible = gridLayoutManager.findLastVisibleItemPosition()
                val totalItems = gridAdapter.itemCount
                if (!isLoadingCards && hasMoreCards && totalItems > 0 && lastVisible >= totalItems - PRELOAD_THRESHOLD) {
                    increaseVisibleCardLimit()
                }
            }
        })

        searchCardBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                visibleCardLimit = INITIAL_VISIBLE_CARDS
                applyFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                visibleCardLimit = INITIAL_VISIBLE_CARDS
                applyFilter()
                return true
            }
        })

        buttonFilters.setOnClickListener { showFilterDialog() }
        buttonCancelDeck.setOnClickListener { finish() }
        buttonSaveDeck.setOnClickListener { saveDeck() }

        if (editingDeckId >= 0) {
            buttonSaveDeck.setText(R.string.update_deck)
            loadExistingDeckState(editingDeckId)
        }

        if (editingDeckId < 0) {
            showDeckFormatDialog()
        } else {
            loadCardsFromApi(null, null)
        }

        updateSelectedCounter()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }

    private fun loadCardsFromApi(apiCategory: String?, apiType: String?) {
        cardsQueryToken++
        val requestToken = cardsQueryToken
        currentApiCategory = apiCategory?.takeIf { it.isNotBlank() }
        currentApiType = apiType?.takeIf { it.isNotBlank() }
        isLoadingCards = false
        hasMoreCards = false
        visibleCardLimit = INITIAL_VISIBLE_CARDS

        allCards.clear()
        filteredCards.clear()
        gridAdapter.notifyDataSetChanged()

        val regulationMark = if (currentDeckFormat == "standard") "H,I,J" else null
        val cardsCall = speciesApiService.getCardsFiltered(currentApiCategory, currentApiType, regulationMark)

        cardsCall.enqueue(object : Callback<List<CardSummary>> {
            override fun onResponse(call: Call<List<CardSummary>>, response: Response<List<CardSummary>>) {
                if (requestToken != cardsQueryToken) {
                    isLoadingCards = false
                    return
                }

                isLoadingCards = false
                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(this@DeckBuilderActivity, "Error loading cards", Toast.LENGTH_LONG).show()
                    return
                }

                allCards.clear()
                Log.d("DeckBuilder", "Loading cards for format: $currentDeckFormat")
                response.body().orEmpty().forEach { card ->
                    buildCardItem(card)?.let { allCards.add(it) }
                }
                Log.d("DeckBuilder", "Loaded ${allCards.size} cards in format: $currentDeckFormat")
                applyFilter()
            }

            override fun onFailure(call: Call<List<CardSummary>>, t: Throwable) {
                if (requestToken != cardsQueryToken) {
                    isLoadingCards = false
                    return
                }

                isLoadingCards = false
                Toast.makeText(this@DeckBuilderActivity, "Error loading cards: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveDeck() {
        val deckName = editDeckName.text.toString().trim()
        if (deckName.isEmpty()) {
            editDeckName.error = "Deck name is required"
            return
        }

        if (getTotalSelectedCards() == 0) {
            Toast.makeText(this, "Select at least one card", Toast.LENGTH_SHORT).show()
            return
        }

        ioExecutor.execute {
            val deckId = if (editingDeckId >= 0) {
                editingDeckId.toLong()
            } else {
                deckDatabase.deckDao().insertDeck(DeckEntity(deckName, System.currentTimeMillis()))
            }

            if (editingDeckId >= 0) {
                deckDatabase.deckDao().updateDeck(editingDeckId, deckName, System.currentTimeMillis())
                deckDatabase.deckDao().deleteDeckCards(editingDeckId)
            }

            val deckCards = mutableListOf<DeckCardEntity>()
            for (card in allCards) {
                val quantity = selectedCardCounts[card.cardId] ?: 0
                if (quantity > 0) {
                    deckCards.add(DeckCardEntity(deckId.toInt(), card.cardId, card.cardName, quantity, card.imageUrl))
                }
            }

            deckDatabase.deckDao().insertDeckCards(deckCards)

            runOnUiThread {
                Toast.makeText(
                    this,
                    if (editingDeckId >= 0) "Deck updated locally" else "Deck saved locally",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun loadExistingDeckState(deckId: Int) {
        ioExecutor.execute {
            val deckWithCards = deckDatabase.deckDao().getDeckWithCardsById(deckId)
            if (deckWithCards?.deck == null) return@execute

            val initialCounts = HashMap<String, Int>()
            deckWithCards.cards?.forEach { card ->
                initialCounts[card.cardId] = maxOf(1, card.quantity)
            }

            runOnUiThread {
                editDeckName.setText(deckWithCards.deck.name)
                selectedCardCounts.clear()
                selectedCardCounts.putAll(initialCounts)
                updateSelectedCounter()
                gridAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun addCard(card: CardItem) {
        val current = selectedCardCounts[card.cardId] ?: 0
        selectedCardCounts[card.cardId] = current + 1
        updateSelectedCounter()
        gridAdapter.notifyDataSetChanged()
    }

    private fun removeCard(card: CardItem) {
        val current = selectedCardCounts[card.cardId] ?: 0
        if (current <= 1) {
            selectedCardCounts.remove(card.cardId)
        } else {
            selectedCardCounts[card.cardId] = current - 1
        }
        updateSelectedCounter()
        gridAdapter.notifyDataSetChanged()
    }

    private fun getTotalSelectedCards(): Int = selectedCardCounts.values.sum()

    private fun updateSelectedCounter() {
        tvSelectedCount.text = getString(R.string.selected_cards_count, getTotalSelectedCards())
    }

    private fun applyFilter() {
        val normalized = currentSearchQuery.trim().lowercase(Locale.ROOT)
        val matchingCards = mutableListOf<CardItem>()
        for (card in allCards) {
            if (normalized.isNotEmpty()) {
                val name = card.cardName.lowercase(Locale.ROOT)
                val id = card.cardId.lowercase(Locale.ROOT)
                if (!(name.contains(normalized) || id.contains(normalized))) continue
            }
            matchingCards.add(card)
        }

        filteredCards.clear()
        val visibleCount = minOf(visibleCardLimit, matchingCards.size)
        filteredCards.addAll(matchingCards.take(visibleCount))
        hasMoreCards = visibleCount < matchingCards.size

        updateActiveFiltersSummary()
        gridAdapter.notifyDataSetChanged()
    }

    private fun increaseVisibleCardLimit() {
        val previousLimit = visibleCardLimit
        visibleCardLimit += VISIBLE_CARDS_STEP
        if (visibleCardLimit != previousLimit) {
            applyFilter()
        }
    }

    private fun getApiCategory(ui: String?): String {
        if (ui.isNullOrBlank()) return ""
        val lower = ui.trim().lowercase(Locale.ROOT)
        if (lower.contains("pok")) return "Pokemon"
        if (lower.contains("entren") || lower.contains("trainer") || lower.contains("obj") || lower.contains("item") || lower.contains("tool") || lower.contains("stad")) {
            return "Trainer"
        }
        if (lower.contains("energ")) return "Energy"
        return ""
    }

    private fun getApiType(ui: String?): String {
        if (ui.isNullOrBlank()) return ""
        return when (ui.trim().lowercase(Locale.ROOT)) {
            "agua" -> "Water"
            "fuego" -> "Fire"
            "planta" -> "Grass"
            "electrico", "eléctrico" -> "Lightning"
            "psiquico", "psíquico" -> "Psychic"
            "lucha" -> "Fighting"
            "siniestro" -> "Darkness"
            "metal" -> "Metal"
            "dragon", "dragón" -> "Dragon"
            "incoloro" -> "Colorless"
            "hada" -> "Fairy"
            else -> ui
        }
    }

    private fun updateActiveFiltersSummary() {
        val summary = buildString {
            if (selectedType.isEmpty() && selectedCategory.isEmpty()) {
                append(getString(R.string.no_filters_applied))
            } else {
                if (selectedType.isNotEmpty()) {
                    append(getString(R.string.filter_type)).append(": ").append(selectedType)
                }
                if (selectedCategory.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append(getString(R.string.filter_category)).append(": ").append(selectedCategory)
                }
            }
        }
        tvActiveFilters.text = summary
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_card_filters, null)
        val spinnerType: android.widget.Spinner = dialogView.findViewById(R.id.spinnerType)
        val spinnerCategory: android.widget.Spinner = dialogView.findViewById(R.id.spinnerCategory)

        val typeOptions = listOf(
            getString(R.string.all_items),
            "Agua",
            "Fuego",
            "Planta",
            "Electrico",
            "Psiquico",
            "Lucha",
            "Siniestro",
            "Metal",
            "Dragon",
            "Incoloro",
            "Hada"
        )

        val categoryOptions = listOf(
            getString(R.string.all_items),
            getString(R.string.category_pokemon),
            getString(R.string.category_trainer),
            getString(R.string.category_energy)
        )

        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)

        setSpinnerSelection(spinnerType, selectedType.ifBlank { getString(R.string.all_items) })
        setSpinnerSelection(spinnerCategory, selectedCategory.ifBlank { getString(R.string.all_items) })

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.filters)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_filters) { _, _ ->
                selectedType = spinnerType.selectedItem?.toString().orEmpty()
                selectedCategory = spinnerCategory.selectedItem?.toString().orEmpty()
                normalizeFilterValues()
                val apiCategory = getApiCategory(selectedCategory)
                val apiType = getApiType(selectedType)
                loadCardsFromApi(apiCategory, apiType)
            }
            .setNeutralButton(R.string.clear_filters) { _, _ ->
                selectedType = ""
                selectedCategory = ""
                loadCardsFromApi(null, null)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun normalizeFilterValues() {
        if (selectedType == getString(R.string.all_items)) selectedType = ""
        if (selectedCategory == getString(R.string.all_items)) selectedCategory = ""
    }

    private fun setSpinnerSelection(spinner: android.widget.Spinner, value: String) {
        if (value.isBlank()) return
        val adapter = spinner.adapter as? ArrayAdapter<*> ?: return
        for (index in 0 until adapter.count) {
            if (value == adapter.getItem(index)?.toString()) {
                spinner.setSelection(index)
                break
            }
        }
    }

    private fun resolveImageUrl(image: String?, cardId: String, localId: String): String {
        image?.trim()?.takeIf { it.isNotEmpty() }?.let { trimmed ->
            val lower = trimmed.lowercase(Locale.ROOT)
            return when {
                lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith("/high.png") || lower.endsWith("/high.webp") -> trimmed
                lower.endsWith("/high") -> "$trimmed.png"
                else -> "$trimmed/high.png"
            }
        }

        val safeCardId = cardId.trim()
        val safeLocalId = localId.trim()
        if (safeCardId.contains("-")) {
            val setId = safeCardId.substringBeforeLast('-')
            val setPrefix = setId.replace(Regex("[0-9.]+$"), "")
            if (setPrefix.isNotBlank() && setId.isNotBlank() && safeLocalId.isNotBlank()) {
                return "https://assets.tcgdex.net/en/$setPrefix/$setId/$safeLocalId/high.png"
            }
        }
        return "https://assets.tcgdex.net/en/$safeCardId/high.png"
    }

    private fun buildCardItem(card: CardSummary): CardItem? {
        if (card.id.trim().isEmpty()) return null

        val name = card.name?.takeIf { it.isNotBlank() } ?: card.id
        val localId = card.localId.orEmpty()
        val image = resolveImageUrl(card.image, card.id, localId)
        val expansion = ""
        val expansionCode = ""
        val category = ""
        val types = mutableListOf<String>()
        val description = ""
        val trainerType = ""

        return CardItem(card.id, localId, name, image, expansion, expansionCode, category, trainerType, description, types)
    }

    private data class CardItem(
        val cardId: String,
        val localId: String,
        val cardName: String,
        val imageUrl: String,
        val expansion: String,
        val expansionCode: String,
        val category: String,
        val trainerType: String,
        val description: String,
        val types: List<String>
    )

    private inner class DeckGridAdapter : RecyclerView.Adapter<DeckGridAdapter.CardViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_card_grid, parent, false)
            return CardViewHolder(view)
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = filteredCards[position]
            bindCard(holder, card)
        }

        override fun getItemCount(): Int = filteredCards.size

        private fun bindCard(holder: CardViewHolder, card: CardItem) {
            val quantity = selectedCardCounts[card.cardId] ?: 0
            val title = buildString {
                append(card.cardName).append(" (").append(card.cardId).append(")")
                if (quantity > 0) append(" x").append(quantity)
            }
            holder.textCardName.text = title

            Glide.with(this@DeckBuilderActivity)
                .load(card.imageUrl)
                .placeholder(R.mipmap.image_not_found)
                .error(R.mipmap.image_not_found)
                .into(holder.imageCard)

            val selected = quantity > 0
            holder.cardSlot.setBackgroundResource(
                if (selected) R.drawable.deck_card_selected_bg else R.drawable.deck_card_unselected_bg
            )
            holder.textCardState.text = if (selected) {
                getString(R.string.added_with_count, quantity)
            } else {
                getString(R.string.tap_to_add)
            }

            holder.cardSlot.setOnClickListener { addCard(card) }
            holder.cardSlot.setOnLongClickListener {
                if (quantity > 0) {
                    removeCard(card)
                    true
                } else {
                    false
                }
            }
        }

        inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardSlot: View = itemView.findViewById(R.id.cardSlot)
            val imageCard: ImageView = itemView.findViewById(R.id.imageCard)
            val textCardName: TextView = itemView.findViewById(R.id.textCardName)
            val textCardState: TextView = itemView.findViewById(R.id.textCardState)
        }
    }

    private fun showDeckFormatDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_deck_format, null)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radioGroupFormat)

        if (currentDeckFormat == "standard") {
            radioGroup.check(R.id.radioStandard)
        } else {
            radioGroup.check(R.id.radioExpanded)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.select_deck_format)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                currentDeckFormat = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioExpanded -> "expanded"
                    else -> "standard"
                }
                loadCardsFromApi(null, null)
            }
            .show()
    }
}


