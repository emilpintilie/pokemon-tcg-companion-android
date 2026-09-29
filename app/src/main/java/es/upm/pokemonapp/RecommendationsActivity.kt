package es.upm.pokemonapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import es.upm.pokemonapp.data.network.SpeciesRepository
import es.upm.pokemonapp.config.LMStudioConfig
import es.upm.pokemonapp.utils.NavigationUtils
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class RecommendationsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "RecommendationsActivity"
    private lateinit var drawerLayout: DrawerLayout
    private val speciesRepository = SpeciesRepository()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Starting recommendations activity.")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recommendations)

        setupUI()

        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configure Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Bottom Navigation Menu
        val navView: NavigationView = findViewById(R.id.nav_view)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            val dLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            dLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { item ->
            NavigationUtils.navViewClick(
                item = item,
                currentNavigationId = R.id.navigation_recommendations,
                context = this
            )
            val dLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            dLayout.closeDrawers()
            true
        }

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

        val mainView = findViewById<ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            ).top
            v.updatePadding(top = top)
            insets
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        NavigationUtils.navViewClick(
            item = item,
            currentNavigationId = R.id.navigation_home,
            context = this
        )
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupUI() {
        // Server URL Configuration
        val serverUrlEdit: EditText = findViewById(R.id.serverUrlEdit)
        val saveServerUrlBtn: Button = findViewById(R.id.saveServerUrlBtn)
        val currentUrlText: TextView = findViewById(R.id.currentUrlText)

        // Initialize with current URL
        val currentUrl = "http://10.0.2.2:1234/"
        serverUrlEdit.setText(currentUrl)
        currentUrlText.text = "Current: $currentUrl"

        saveServerUrlBtn.setOnClickListener {
            val newUrl = serverUrlEdit.text.toString().trim()
            if (newUrl.isNotBlank()) {
                val finalUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
                currentUrlText.text = "Current: $finalUrl"
                Toast.makeText(this, "URL updated to: $finalUrl", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Server URL updated to: $finalUrl")
                // Note: The actual connection uses the hardcoded URL in LMStudioRemoteDataSource
                // For dynamic URL changes, you would need to recreate the data source instance
            }
        }

        // Card Recommendations Section
        val cardTypeSpinner: Spinner = findViewById(R.id.cardTypeSpinner)
        val getCardRecommendationsBtn: Button = findViewById(R.id.getCardRecommendationsBtn)
        val cardRecommendationsText: TextView = findViewById(R.id.cardRecommendationsText)

        val cardTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.card_types,
            android.R.layout.simple_spinner_item
        )
        cardTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cardTypeSpinner.adapter = cardTypeAdapter

        getCardRecommendationsBtn.setOnClickListener {
            val selectedType = cardTypeSpinner.selectedItem.toString()
            getCardRecommendations(selectedType, cardRecommendationsText)
        }

        // Deck Recommendations Section
        val deckTypeSpinner: Spinner = findViewById(R.id.deckTypeSpinner)
        val deckFormatSpinner: Spinner = findViewById(R.id.deckFormatSpinner)
        val playerLevelSpinner: Spinner = findViewById(R.id.playerLevelSpinner)
        val getDeckRecommendationsBtn: Button = findViewById(R.id.getDeckRecommendationsBtn)
        val deckRecommendationsText: TextView = findViewById(R.id.deckRecommendationsText)

        val deckTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.deck_types,
            android.R.layout.simple_spinner_item
        )
        deckTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deckTypeSpinner.adapter = deckTypeAdapter

        val formatAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.deck_formats,
            android.R.layout.simple_spinner_item
        )
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deckFormatSpinner.adapter = formatAdapter

        val playerLevelAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.player_levels,
            android.R.layout.simple_spinner_item
        )
        playerLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerLevelSpinner.adapter = playerLevelAdapter

        getDeckRecommendationsBtn.setOnClickListener {
            val selectedDeckType = deckTypeSpinner.selectedItem.toString()
            val selectedFormat = deckFormatSpinner.selectedItem.toString()
            val selectedLevel = playerLevelSpinner.selectedItem.toString()
            getDeckRecommendations(selectedDeckType, selectedFormat, selectedLevel, deckRecommendationsText)
        }

        // Strategy Advice Section
        val strategyCardsEdit: EditText = findViewById(R.id.strategyCardsEdit)
        val opponentTypeSpinner: Spinner = findViewById(R.id.opponentTypeSpinner)
        val getStrategyAdviceBtn: Button = findViewById(R.id.getStrategyAdviceBtn)
        val strategyAdviceText: TextView = findViewById(R.id.strategyAdviceText)

        val opponentTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.opponent_types,
            android.R.layout.simple_spinner_item
        )
        opponentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        opponentTypeSpinner.adapter = opponentTypeAdapter

        getStrategyAdviceBtn.setOnClickListener {
            val cardsText = strategyCardsEdit.text.toString()
            if (cardsText.isBlank()) {
                Toast.makeText(this, "Please enter your cards", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cards = cardsText.split(",").map { it.trim() }
            val selectedOpponent = opponentTypeSpinner.selectedItem.toString()
            getStrategyAdvice(cards, selectedOpponent, strategyAdviceText)
        }
    }

    private fun getCardRecommendations(cardType: String, textView: TextView) {
        textView.text = "Loading recommendations..."
        Toast.makeText(this, "Connecting to LM Studio...", Toast.LENGTH_SHORT).show()

        speciesRepository.getCardRecommendations(
            cardType = cardType,
            currentCards = emptyList(),
            onSuccess = { recommendations ->
                runOnUiThread {
                    textView.text = recommendations
                    Toast.makeText(this, "✅ Recommendations received", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    val errorMessage = "❌ Error: ${error.message}\n\nMake sure LM Studio is running at http://localhost:11434 with the google/gemma-4-e4b model."
                    textView.text = errorMessage
                    Log.e(TAG, "Error getting card recommendations", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun getDeckRecommendations(deckType: String, format: String, playerLevel: String, textView: TextView) {
        textView.text = "Loading deck recommendations..."
        Toast.makeText(this, "Connecting to LM Studio...", Toast.LENGTH_SHORT).show()

        speciesRepository.getDeckRecommendations(
            deckType = deckType,
            format = format,
            playerLevel = playerLevel,
            onSuccess = { recommendations ->
                runOnUiThread {
                    textView.text = recommendations
                    Toast.makeText(this, "✅ Deck recommendations received", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    val errorMessage = "❌ Error: ${error.message}\n\nMake sure LM Studio is running at http://localhost:11434 with the google/gemma-4-e4b model."
                    textView.text = errorMessage
                    Log.e(TAG, "Error getting deck recommendations", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun getStrategyAdvice(cards: List<String>, opponentType: String, textView: TextView) {
        textView.text = "Loading strategy advice..."
        Toast.makeText(this, "Connecting to LM Studio...", Toast.LENGTH_SHORT).show()

        speciesRepository.getStrategyAdvice(
            currentCards = cards,
            opponentType = opponentType,
            onSuccess = { advice ->
                runOnUiThread {
                    textView.text = advice
                    Toast.makeText(this, "✅ Strategy advice received", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    val errorMessage = "❌ Error: ${error.message}\n\nMake sure LM Studio is running at http://localhost:11434 with the google/gemma-4-e4b model."
                    textView.text = errorMessage
                    Log.e(TAG, "Error getting strategy advice", error)
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}





