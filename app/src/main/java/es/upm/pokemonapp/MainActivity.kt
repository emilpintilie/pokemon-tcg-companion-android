package es.upm.pokemonapp
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.content.Intent
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import es.upm.pokemonapp.data.network.SpeciesRepository
import es.upm.pokemonapp.utils.NavigationUtils
import es.upm.pokemonapp.auth.FirebaseAuthHelper
import es.upm.pokemonapp.auth.AuthActivity
import es.upm.pokemonapp.utils.goToActivityWithoutReturn
import es.upm.pokemonapp.utils.loadFromUrl
import es.upm.pokemonapp.utils.showToast


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "btaMainActivity"

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var drawerLayout: DrawerLayout
    private val speciesRepository = SpeciesRepository()

    //On create
    @SuppressLint("SetTextI18n", "UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Starting main activity.")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Login
        checkAuthentication()

        setContentView(R.layout.activity_main)
        setupUI()
        updateAppTheme()

        //Random species
        showRandomSpecies()

        val randomButton: Button = findViewById(R.id.randomButton)
        randomButton.setOnClickListener {
            showRandomSpecies()
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Toolbar
        // Configure Toolbar
       // val toolbar: Toolbar = findViewById(R.id.toolbar)
       // setSupportActionBar(toolbar)

        // ButtomNavigationMenu
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            val dLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            dLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { item ->
            NavigationUtils.navViewClick(
                item = item,
                currentNavigationId = R.id.navigation_home,
                context = this
            )
            val dLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            dLayout.closeDrawers()
            true
        }

        /* Check if the user identifier is already saved
        val userIdentifier = authHelper.getCurrentUser()?.uid
        if (userIdentifier == null) {
            // If not, ask for it
            showUserIdentifierDialog()
        } else {
            // If yes, use it or show it
            showToast("User ID: $userIdentifier")
            changeUserIDdisplay(userIdentifier)
        }
        */

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

    private fun setupUI() {
        val mainTextView = findViewById<TextView>(R.id.speciesTextView)
        val appName = getString(R.string.app_name)
        val welcomeMessage = getString(R.string.welcome_message, appName)
        mainTextView.text = welcomeMessage
    }

    //Auth
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            showToast("Welcome ${user?.displayName ?: "!"}")
        } else {

            showToast("Authentication failed.")
            finish()
        }
    }

    private fun checkAuthentication() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, AuthActivity::class.java)
            loginLauncher.launch(intent)
        } else {
            showToast("Welcome " + FirebaseAuth.getInstance().currentUser?.displayName + "!")
        }
    }

    private fun performLogout() {
        FirebaseAuthHelper.getInstance().logout(this) {
            goToActivityWithoutReturn(this, AuthActivity::class.java)
        }
    }

    //Toolbar/drawer
    //Inflate views, add the design of the toolbarMenu/drawer into the main_activity design.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    //Manage activity change from the toolbarMenu.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            R.id.action_recommendations -> {
                val intent = Intent(this, RecommendationsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    //Bottom menu
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        NavigationUtils.navViewClick(
            item = item,
            currentNavigationId = R.id.navigation_home,
            context = this
        )
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    //Change app theme according to last state from sharedPreferences
    private fun updateAppTheme() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val darkModeEnabled = sharedPreferences.getBoolean("theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun showRandomSpecies() {
        speciesRepository.getRandomSpecies(
            onSuccess = { randomCard ->
                showCardInfo(randomCard)
            },
            onError = { error ->
                Log.e(TAG, "Error fetching card: ${error.message}")
                showToast("Error fetching card: ${error.message}")
            }
        )
    }

    private fun showCardInfo(card: es.upm.pokemonapp.data.network.Card) {
        val cardText = StringBuilder()

        fun safeText(value: String?): String {
            return if (value.isNullOrBlank()) "Not available" else value
        }

        val cardName = safeText(card.name)
        val cardId = safeText(card.id)
        val localId = safeText(card.localId)

        Log.d(TAG, "Card loaded: ID=$cardId, Name=$cardName, Image URL=${card.image}")

        cardText.append("Name: $cardName\n")
        cardText.append("ID: $cardId\n")
        cardText.append("Local ID: $localId\n")
        cardText.append("Category: ${safeText(card.category)}\n")
        cardText.append("Rarity: ${safeText(card.rarity)}\n")
        cardText.append("HP: ${card.hp ?: "Not available"}\n")

        card.types?.let { types ->
            if (types.isNotEmpty()) {
                cardText.append("Types: ${types.joinToString(", ")}\n")
            }
        }

        card.stage?.let { stage ->
            if (!stage.isNullOrBlank()) {
                cardText.append("Stage: $stage\n")
            }
        }

        card.evolveFrom?.let { evolveFrom ->
            if (!evolveFrom.isNullOrBlank()) {
                cardText.append("Evolves from: $evolveFrom\n")
            }
        }

        card.dexId?.let { dexIds ->
            if (dexIds.isNotEmpty()) {
                cardText.append("Dex ID: ${dexIds.joinToString(", ")}\n")
            }
        }

        card.retreat?.let { retreat ->
            cardText.append("Retreat cost: $retreat\n")
        }

        card.regulationMark?.let { regulationMark ->
            if (!regulationMark.isNullOrBlank()) {
                cardText.append("Regulation mark: $regulationMark\n")
            }
        }

        card.legal?.let { legal ->
            cardText.append("Standard legal: ${if (legal.standard) "Yes" else "No"}\n")
            cardText.append("Expanded legal: ${if (legal.expanded) "Yes" else "No"}\n")
        }

        card.illustrator?.let { illustrator ->
            if (!illustrator.isNullOrBlank()) {
                cardText.append("Illustrator: $illustrator\n")
            }
        }

        card.description?.let { description ->
            if (!description.isNullOrBlank()) {
                cardText.append("\nDescription:\n$description\n")
            }
        }

        card.attacks?.let { attacks ->
            if (attacks.isNotEmpty()) {
                cardText.append("\nAttacks:\n")

                attacks.forEach { attack ->
                    cardText.append("- ${safeText(attack.name)}")

                    attack.damage?.let { damage ->
                        cardText.append(" | Damage: $damage")
                    }

                    val cost = attack.cost
                    if (cost != null && cost.isNotEmpty()) {
                        cardText.append(" | Cost: ${cost.joinToString(", ")}")
                    }

                    val effect = attack.effect
                    if (!effect.isNullOrBlank()) {
                        cardText.append("\n  Effect: $effect")
                    }

                    cardText.append("\n")
                }
            }
        }

        card.weaknesses?.let { weaknesses ->
            if (weaknesses.isNotEmpty()) {
                cardText.append("\nWeaknesses:\n")

                weaknesses.forEach { weakness ->
                    cardText.append("- ${safeText(weakness.type)} ${safeText(weakness.value)}\n")
                }
            }
        }

        card.updated?.let { updated ->
            if (!updated.isNullOrBlank()) {
                cardText.append("\nUpdated: $updated\n")
            }
        }

        val speciesTextView: TextView = findViewById(R.id.speciesTextView)
        speciesTextView.text = cardText.toString()

        val speciesImageView: ImageView = findViewById(R.id.speciesImageView)
        val image = card.image

        if (!image.isNullOrBlank()) {
            val imageUrl = when {
                image.endsWith("/high.png") -> image
                image.endsWith(".png") || image.endsWith(".jpg") -> image
                else -> "$image/high.png"
            }

            Log.d(TAG, "Original image: $image")
            Log.d(TAG, "Loading image from URL: $imageUrl")

            speciesImageView.loadFromUrl(imageUrl)
        } else {
            Log.w(TAG, "Image URL is empty for card: $cardId")
            showToast("Image not available for this card")
            speciesImageView.setImageResource(R.mipmap.image_not_found)
        }
    }
}