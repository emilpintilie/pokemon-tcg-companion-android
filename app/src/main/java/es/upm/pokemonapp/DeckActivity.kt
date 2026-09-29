package es.upm.pokemonapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import es.upm.pokemonapp.data.decklocal.DeckDatabase
import es.upm.pokemonapp.data.decklocal.DeckWithCards
import es.upm.pokemonapp.utils.NavigationUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DeckActivity : AppCompatActivity() {

    private lateinit var deckListContainer: View
    private lateinit var deckDetailContainer: View
    private lateinit var listDecks: ListView
    private lateinit var buttonCreateDeck: Button
    private lateinit var buttonBackToDecks: Button
    private lateinit var buttonAddCards: Button
    private lateinit var tvDeckDetailTitle: TextView
    private lateinit var tvDeckDetailSubtitle: TextView
    private lateinit var recyclerDeckCards: RecyclerView

    private lateinit var deckAdapter: ArrayAdapter<DeckSummary>
    private lateinit var deckCardAdapter: DeckCardAdapter
    private val deckSummaries = mutableListOf<DeckSummary>()
    private val currentDeckCards = mutableListOf<DeckCardUi>()
    private lateinit var deckDatabase: DeckDatabase
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentDeckId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { view, insets ->
            val cutout = insets.displayCutout
            if (cutout != null) {
                // Insets seguros para no pisar cámara/notch
                val top = cutout.safeInsetTop
                val left = cutout.safeInsetLeft
                val right = cutout.safeInsetRight
                view.setPadding(left, top, right, 0)
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.updatePadding(top = topInset)
            insets
        }

        deckDatabase = DeckDatabase.getInstance(applicationContext)

        deckListContainer = findViewById(R.id.deckListContainer)
        deckDetailContainer = findViewById(R.id.deckDetailContainer)
        listDecks = findViewById(R.id.listDecks)
        buttonCreateDeck = findViewById(R.id.buttonCreateDeck)
        buttonBackToDecks = findViewById(R.id.buttonBackToDecks)
        buttonAddCards = findViewById(R.id.buttonAddCards)
        tvDeckDetailTitle = findViewById(R.id.tvDeckDetailTitle)
        tvDeckDetailSubtitle = findViewById(R.id.tvDeckDetailSubtitle)
        recyclerDeckCards = findViewById(R.id.recyclerDeckCards)

        deckAdapter = object : ArrayAdapter<DeckSummary>(
            this,
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            deckSummaries
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                val title: TextView = view.findViewById(android.R.id.text1)
                val subtitle: TextView = view.findViewById(android.R.id.text2)
                val item = getItem(position)
                if (item != null) {
                    title.text = item.name
                    subtitle.text = getString(R.string.deck_summary_cards, item.totalCards)
                }
                return view
            }
        }
        listDecks.adapter = deckAdapter
        listDecks.setOnItemClickListener { _, _, position, _ ->
            val summary = deckSummaries[position]
            openDeckDetail(summary.deckId)
        }

        buttonCreateDeck.setOnClickListener {
            startActivity(Intent(this, DeckBuilderActivity::class.java))
        }
        buttonBackToDecks.setOnClickListener { showDeckList() }
        buttonAddCards.setOnClickListener {
            if (currentDeckId >= 0) {
                Intent(this, DeckBuilderActivity::class.java).apply {
                    putExtra(DeckBuilderActivity.EXTRA_DECK_ID, currentDeckId)
                    startActivity(this)
                }
            }
        }

        recyclerDeckCards.layoutManager = GridLayoutManager(this, 2)
        deckCardAdapter = DeckCardAdapter()
        recyclerDeckCards.adapter = deckCardAdapter

        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { item ->
            NavigationUtils.navViewClick(
                item = item,
                currentNavigationId = R.id.navigation_deck,
                context = this
            )
            drawerLayout.closeDrawers()
            true
        }

        showDeckList()
    }

    override fun onResume() {
        super.onResume()
        if (currentDeckId >= 0 && deckDetailContainer.isVisible) {
            loadDeckDetail(currentDeckId)
        } else {
            loadDecks()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdown()
    }

    private fun loadDecks() {
        ioExecutor.execute {
            val decks: List<DeckWithCards> = deckDatabase.deckDao().getAllDecksWithCards()
            val rows = mutableListOf<DeckSummary>()
            for (deckWithCards in decks) {
                val deck = deckWithCards.deck ?: continue
                var cardCount = 0
                deckWithCards.cards?.forEach { card ->
                    cardCount += maxOf(1, card.quantity)
                }
                rows.add(DeckSummary(deck.deckId, deck.name, cardCount))
            }

            runOnUiThread {
                deckSummaries.clear()
                deckSummaries.addAll(rows)
                deckAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun openDeckDetail(deckId: Int) {
        currentDeckId = deckId
        loadDeckDetail(deckId)
    }

    private fun loadDeckDetail(deckId: Int) {
        ioExecutor.execute {
            val deckWithCards = deckDatabase.deckDao().getDeckWithCardsById(deckId)
            if (deckWithCards?.deck == null) {
                runOnUiThread { showDeckList() }
                return@execute
            }

            val cards = mutableListOf<DeckCardUi>()
            var totalCards = 0
            deckWithCards.cards?.forEach { card ->
                val quantity = maxOf(1, card.quantity)
                totalCards += quantity
                cards.add(DeckCardUi(card.deckId, card.cardId, card.cardName, quantity, card.imageUrl))
            }

            runOnUiThread {
                currentDeckCards.clear()
                currentDeckCards.addAll(cards)
                deckCardAdapter.notifyDataSetChanged()
                tvDeckDetailTitle.text = deckWithCards.deck.name
                tvDeckDetailSubtitle.text = getString(R.string.deck_detail_cards_count, totalCards)
                showDeckDetail()
            }
        }
    }

    private fun showDeckList() {
        deckDetailContainer.visibility = View.GONE
        deckListContainer.visibility = View.VISIBLE
        currentDeckId = -1
        loadDecks()
    }

    private fun showDeckDetail() {
        deckListContainer.visibility = View.GONE
        deckDetailContainer.visibility = View.VISIBLE
    }

    private fun adjustCardQuantity(card: DeckCardUi, delta: Int) {
        if (currentDeckId < 0) return

        ioExecutor.execute {
            val newQuantity = card.quantity + delta
            if (newQuantity <= 0) {
                deckDatabase.deckDao().deleteDeckCard(currentDeckId, card.cardId)
            } else {
                deckDatabase.deckDao().updateCardQuantity(currentDeckId, card.cardId, newQuantity)
            }
            loadDeckDetail(currentDeckId)
            loadDecks()
        }
    }

    private data class DeckSummary(
        val deckId: Int,
        val name: String,
        val totalCards: Int
    )

    private data class DeckCardUi(
        val deckId: Int,
        val cardId: String,
        val cardName: String,
        val quantity: Int,
        val imageUrl: String
    ) {
        val resolvedImageUrl: String = imageUrl.takeIf { it.isNotBlank() }
            ?: "https://assets.tcgdex.net/en/$cardId/high.png"
    }

    private inner class DeckCardAdapter : RecyclerView.Adapter<DeckCardAdapter.CardViewHolder>() {

        override fun getItemCount(): Int = currentDeckCards.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deck_card_detail, parent, false)
            return CardViewHolder(view)
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = currentDeckCards[position]
            holder.textName.text = "${card.cardName} x${card.quantity}"
            holder.textQuantity.text = getString(R.string.deck_card_quantity_label, card.quantity)
            Glide.with(this@DeckActivity)
                .load(card.resolvedImageUrl)
                .placeholder(R.mipmap.image_not_found)
                .error(R.mipmap.image_not_found)
                .into(holder.imageCard)

            holder.buttonPlus.setOnClickListener { adjustCardQuantity(card, +1) }
            holder.buttonMinus.setOnClickListener { adjustCardQuantity(card, -1) }
        }

        inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardSlot: View = itemView.findViewById(R.id.cardSlotDetail)
            val imageCard: android.widget.ImageView = itemView.findViewById(R.id.imageCardDetail)
            val textName: TextView = itemView.findViewById(R.id.textCardDetailName)
            val textQuantity: TextView = itemView.findViewById(R.id.textCardDetailQuantity)
            val buttonMinus: Button = itemView.findViewById(R.id.buttonMinus)
            val buttonPlus: Button = itemView.findViewById(R.id.buttonPlus)
        }
    }
}


