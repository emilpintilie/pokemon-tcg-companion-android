package es.upm.pokemonapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HeaderViewListAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import es.upm.pokemonapp.data.network.SpeciesDetails
import es.upm.pokemonapp.data.network.SpeciesRepository
import es.upm.pokemonapp.data.room.AppContainer
import es.upm.pokemonapp.utils.NavigationUtils
import es.upm.pokemonapp.utils.loadFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class  SpeciesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val TAG = "btaSecondActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var listView: ListView
    private val speciesRepository = SpeciesRepository()
    private var activityName: String =
        "Fetching card description from trading card web \n[🚧 Under Development 🚧]"

    //On create
    @SuppressLint("MissingInflatedId", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: The activity $activityName is being created.")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_species)

        val userAgent =
            "MADEnvironmentApp/1.0 (Android; MADEnvironmentApp - Map usage)"  // For a specific error on logs.  No more pixeled map
        Configuration.getInstance().userAgentValue = userAgent

        // Set up the ListView
        listView = findViewById(R.id.lvCoordinates)
        val headerView = layoutInflater.inflate(R.layout.species_listview_header, null)
        listView.addHeaderView(headerView, null, false)

        // Fetch data from Room database and creating the adapter
        AppContainer.syncDatabase(applicationContext)
        val repository = AppContainer.coordinatesRepository
        lifecycleScope.launch {
            val dbCoordinates = repository.getAllCoordinates()
            val roomCoordinates = dbCoordinates.map {
                listOf(
                    it.timestamp.toString(),
                    it.latitude.toString(),
                    it.longitude.toString(),
                    it.cardName,
                    it.cardId,
                    it.status
                )
            }
            Log.d(TAG, "Room obtained data: $roomCoordinates")

            // Instanciate the adapter
            val adapter = CoordinatesAdapter(this@SpeciesActivity, roomCoordinates)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val adjustedPosition = position - 1
                if (adjustedPosition >= 0 && adjustedPosition < roomCoordinates.size) {
                    val item = roomCoordinates[adjustedPosition]
                    showSpeciesWindow(item)
                }
            }
        }

        // ButtonNavigationMenu
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
                currentNavigationId = R.id.navigation_list,
                context = this
            )
            val dLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            dLayout.closeDrawers()
            true
        }

        val drawerLayoutView = findViewById<DrawerLayout>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayoutView) { view, insets ->
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

    // This is for lateral menu
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        NavigationUtils.navViewClick(
            item = item,
            currentNavigationId = R.id.navigation_list,
            context = this
        )
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showSpeciesWindow(speciesData: List<String>) {
        val speciesMapView = findViewById<View>(R.id.species_map_include)
        val overlayView = findViewById<View>(R.id.overlay)
        val mapView = speciesMapView.findViewById<MapView>(R.id.map)
        val repository = AppContainer.coordinatesRepository

        speciesMapView.visibility = View.VISIBLE
        overlayView.visibility = View.VISIBLE

        mapView.setTileSource(TileSourceFactory.MAPNIK)

        val timestamp = speciesData[0]
        val latitude = speciesData[1].toDoubleOrNull() ?: 0.0
        val longitude = speciesData[2].toDoubleOrNull() ?: 0.0
        val cardName = speciesData[3]
        val cardId = speciesData[4]
        val status = speciesData[5]

        val geoPoint = GeoPoint(latitude, longitude)

        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(geoPoint)
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "$cardName\n$cardId"
        mapView.overlays.add(marker)
        mapView.invalidate()

        //Fetch card information
        Log.d(TAG, "showCardMap: $cardName")
        showCardInfo(cardId)

        val returnButton = speciesMapView.findViewById<Button>(R.id.buttonReturn)
        returnButton.setOnClickListener {
            speciesMapView.visibility = View.GONE
            overlayView.visibility = View.GONE
            mapView.onPause()
        }
        mapView.onResume()

        val buttonModify = speciesMapView.findViewById<Button>(R.id.buttonModify)
        buttonModify.setOnClickListener {

            showCardEditDialog(cardName, cardId, status, geoPoint)
            { savedData ->
                val parts = savedData.split("|||")
                lifecycleScope.launch {
                    repository.updateByTimestamp(
                        timestamp.toLong(),
                        parts[0],
                        parts[1],
                        parts[2]
                    )
                }
                speciesMapView.visibility = View.GONE
                overlayView.visibility = View.GONE

                refreshData()

                Toast.makeText(
                    this@SpeciesActivity,
                    "✅ Card data modified",
                    Toast.LENGTH_LONG
                ).show()

            }
        }

        val buttonDelete = speciesMapView.findViewById<Button>(R.id.buttonDelete)
        buttonDelete.setOnClickListener {

            AlertDialog.Builder(this@SpeciesActivity).apply {
                setTitle("Confirm delete")
                setMessage("Are you sure you want to delete this card record?")

                setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {

                        repository.deleteWithTimestamp(timestamp.toLong())
                        speciesMapView.visibility = View.GONE
                        overlayView.visibility = View.GONE

                        refreshData()

                        Toast.makeText(
                            this@SpeciesActivity,
                            "✅ Card deleted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }

                create().show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCardEditDialog(cardName: String, cardId: String, status: String, positionCoordinates: GeoPoint, onDataSaved: (String) -> Unit){
        //Ask display of register_landmark.xml in dialog
        val cardDialog = LayoutInflater.from(this).inflate(R.layout.register_landmark, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this).setView(cardDialog).setTitle("Edit Card").create()

        val cardImageView = cardDialog.findViewById<ImageView>(R.id.cardImageView)
        val cancelButton: Button = cardDialog.findViewById(R.id.cancelButton)
        val saveButton: Button = cardDialog.findViewById(R.id.saveButton)
        val cardIdEdit: EditText = cardDialog.findViewById(R.id.cardId)
        val cardNameEdit: EditText = cardDialog.findViewById(R.id.cardName)
        val userNameEdit: EditText = cardDialog.findViewById(R.id.userName)
        val userEmailEdit: EditText = cardDialog.findViewById(R.id.userEmail)
        val statusSpinner: Spinner = cardDialog.findViewById(R.id.statusSpinner)
        val latitudeEdit: EditText = cardDialog.findViewById(R.id.latitude)
        val longitudeEdit: EditText = cardDialog.findViewById(R.id.longitude)

        //Set default text
        cardIdEdit.setText(cardId)
        cardNameEdit.setText(cardName)
        latitudeEdit.setText(positionCoordinates.latitude.toString())
        longitudeEdit.setText(positionCoordinates.longitude.toString())
        latitudeEdit.isEnabled = false
        longitudeEdit.isEnabled = false
        userNameEdit.isEnabled = false
        userEmailEdit.isEnabled = false

        fetchAndLoadCardImage(cardId, cardImageView)

        // Set status spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.card_status_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = adapter
        val statusIndex = adapter.getPosition(status)
        statusSpinner.setSelection(statusIndex)

        saveButton.setOnClickListener{
            val updatedCardId: String = cardIdEdit.text.toString()
            val updatedCardName: String = cardNameEdit.text.toString()
            val updatedStatus: String = statusSpinner.selectedItem.toString()

            Log.d(TAG,"Updated card: $updatedCardName")
            //To return when saved button is pressed.
            onDataSaved("$updatedCardId|||$updatedCardName|||$updatedStatus")
            builder.dismiss()
        }
        cancelButton.setOnClickListener{
            Toast.makeText(this, "Nothing saved", Toast.LENGTH_LONG).show()
            builder.dismiss()
        }
        builder.show()
    }


    private fun refreshData() {
        val repository = AppContainer.coordinatesRepository
        lifecycleScope.launch {
            try {
                val roomCoordinates = repository.getFormattedCoordinates()

                withContext(Dispatchers.Main) {
                    val adapter: ListAdapter? = listView.adapter
                    when (adapter) {
                        is HeaderViewListAdapter -> {
                            (adapter.wrappedAdapter as? CoordinatesAdapter)?.apply {
                                clear()
                                addAll(roomCoordinates)
                                notifyDataSetChanged()
                            }
                        }

                        is CoordinatesAdapter -> {
                            adapter.apply {
                                clear()
                                addAll(roomCoordinates)
                                notifyDataSetChanged()
                            }
                        }

                        else -> {
                            Log.e("SpeciesActivity", "Error: Adapter is not of type CoordinatesAdapter")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SpeciesActivity", "Error fetching data", e)
            }
        }
    }



    private fun fetchAndLoadCardImage(cardId: String, imageView: ImageView) {
        speciesRepository.getSpeciesInfo(
            cardId,
            onSuccess = { cardResponse ->
                renderCardImage(cardResponse, imageView)
            },
            onError = { error ->
                Log.e(TAG, "Error fetching image for card $cardId: ${error.message}")
                imageView.setImageResource(R.mipmap.image_not_found)
            }
        )
    }

    private fun renderCardImage(cardResponse: SpeciesDetails, imageView: ImageView) {
        try {
            val img = cardResponse.image
            if (img.isNotEmpty()) {
                val imageUrl = when {
                    img.endsWith("/high.png") -> img
                    img.endsWith(".png") || img.endsWith(".jpg") -> img
                    else -> "$img/high.png"
                }
                Log.d(TAG, "SpeciesActivity: Loading image $imageUrl for card ${cardResponse.id}")
                imageView.loadFromUrl(imageUrl)
            } else {
                imageView.setImageResource(R.mipmap.image_not_found)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading card image: ${e.message}")
            imageView.setImageResource(R.mipmap.image_not_found)
        }
    }

    private class CoordinatesAdapter(
        context: Context,
        private val coordinatesList: List<List<String>>
    ) :
        ArrayAdapter<List<String>>(context, R.layout.species_listview_item, coordinatesList) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.species_listview_item, parent, false)
            val cardNameTextView: TextView = view.findViewById(R.id.tvSpecies)
            val cardIdTextView: TextView = view.findViewById(R.id.tvType)
            val latitudeTextView: TextView = view.findViewById(R.id.tvLatitude)
            val longitudeTextView: TextView = view.findViewById(R.id.tvLongitude)
            val statusTextView: TextView = view.findViewById(R.id.tvEndangered)

            val item = coordinatesList[position]
            try {

                latitudeTextView.text = formatCoordinate(item[1].toDouble())
                longitudeTextView.text = formatCoordinate(item[2].toDouble())
                cardNameTextView.text = item[3]
                cardIdTextView.text = item[4]

                //Special condition to change background color based on status
                val status = item[5]
                when(status) {
                    "Sold" -> {
                        statusTextView.text = status
                        statusTextView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.pokemon_red_dark
                            )
                        )
                        statusTextView.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.pokemon_background
                            )
                        )
                    }
                    "Reserved" -> {
                        statusTextView.text = status
                        statusTextView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.pokemon_red_dark
                            )
                        )
                        statusTextView.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.pokemon_background
                            )
                        )
                    }
                    else -> {
                        statusTextView.text = status
                        statusTextView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.transparent
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("CoordinatesAdapter", "Error parsing coordinates: ${e.message}")
            }
            return view
        }

        @SuppressLint("DefaultLocale")
        private fun formatCoordinate(value: Double): String {
            return String.format("%.4f", value)
        }
    }

    private fun showCardInfo(cardId: String) {
        speciesRepository.getSpeciesInfo(
            cardId,
            onSuccess = { cardResponse ->
                val formattedText = formatCardInfo(cardResponse)
                updateUI(formattedText)
                findViewById<ImageView>(R.id.speciesImageView)?.let { imageView ->
                    renderCardImage(cardResponse, imageView)
                }
            },
            onError = { error ->
                Log.e(TAG, "Error fetching card: ${error.message}")
                Toast.makeText(this, "Error fetching card: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun formatCardInfo(cardResponse: SpeciesDetails): String {
        val name = cardResponse.name
        val cardId = cardResponse.id
        val localId = cardResponse.localId
        val category = cardResponse.category
        val setInfo = cardResponse.set.name
        val setId = cardResponse.set.id
        val rarity = cardResponse.rarity ?: "Not available"
        val illustrator = cardResponse.illustrator ?: "Not available"
        val description = cardResponse.description ?: "No description available"
        val updated = cardResponse.updated ?: "Not available"

        val cardCount = cardResponse.set.cardCount

        val cardNumber = if (cardCount != null) {
            "$localId/${cardCount.official}"
        } else {
            localId
        }

        val setTotalCards = if (cardCount != null) {
            "${cardCount.official} official / ${cardCount.total} total"
        } else {
            "Not available"
        }

        val variants = cardResponse.variants
        val availableVariants = mutableListOf<String>()

        if (variants?.normal == true) availableVariants.add("Normal")
        if (variants?.reverse == true) availableVariants.add("Reverse")
        if (variants?.holo == true) availableVariants.add("Holo")
        if (variants?.firstEdition == true) availableVariants.add("First Edition")
        if (variants?.wPromo == true) availableVariants.add("Promo")

        val variantsText = availableVariants
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: "Not available"

        val detailedVariantsText = cardResponse.variants_detailed
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { variant ->
                "  - Type: ${variant.type}\n    Size: ${variant.size}\n    ID: ${variant.variantId}"
            } ?: "Not available"

        val typesText = cardResponse.types
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: "Not available"

        val dexText = cardResponse.dexId
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: "Not available"

        val stage = cardResponse.stage ?: "Not available"
        val evolvesFrom = cardResponse.evolveFrom ?: "Not available"
        val hp = cardResponse.hp?.toString() ?: "Not available"
        val retreat = cardResponse.retreat?.toString() ?: "Not available"
        val regulationMark = cardResponse.regulationMark ?: "Not available"

        val legalText = cardResponse.legal?.let { legal ->
            buildString {
                appendLine("Standard: ${if (legal.standard) "Legal" else "Not legal"}")
                appendLine("Expanded: ${if (legal.expanded) "Legal" else "Not legal"}")
            }.trimEnd()
        } ?: "Not available"

        val weaknessesText = cardResponse.weaknesses
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { weakness ->
                "  - ${weakness.type}: ${weakness.value}"
            } ?: "Not available"

        val attacksText = cardResponse.attacks
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n") { attack ->
                val cost = if (attack.cost.isNotEmpty()) {
                    attack.cost.joinToString(", ")
                } else {
                    "No cost"
                }

                val damage = attack.damage?.toString() ?: "No damage"

                val effect = if (attack.effect.isNotBlank()) {
                    attack.effect
                } else {
                    "No effect"
                }

                buildString {
                    appendLine(attack.name)
                    appendLine("  Cost: $cost")
                    appendLine("  Damage: $damage")
                    appendLine("  Effect: $effect")
                }.trimEnd()
            } ?: "Not available"

        val priceText = cardResponse.pricing?.cardmarket?.let { price ->
            buildString {
                appendLine("Cardmarket")
                appendLine("  Product ID: ${price.idProduct}")
                appendLine("  Currency: ${price.unit}")
                appendLine("  Average: ${price.avg} ${price.unit}")
                appendLine("  Lowest: ${price.low} ${price.unit}")
                appendLine("  Trend: ${price.trend} ${price.unit}")
                appendLine("  Average 1 day: ${price.avg1} ${price.unit}")
                appendLine("  Average 7 days: ${price.avg7} ${price.unit}")
                appendLine("  Average 30 days: ${price.avg30} ${price.unit}")

                price.avg_holo?.let { appendLine("  Average holo: $it ${price.unit}") }
                price.low_holo?.let { appendLine("  Lowest holo: $it ${price.unit}") }
                price.trend_holo?.let { appendLine("  Trend holo: $it ${price.unit}") }
                price.avg1_holo?.let { appendLine("  Average holo 1 day: $it ${price.unit}") }
                price.avg7_holo?.let { appendLine("  Average holo 7 days: $it ${price.unit}") }
                price.avg30_holo?.let { appendLine("  Average holo 30 days: $it ${price.unit}") }

                appendLine("  Price updated: ${price.updated}")
            }.trimEnd()
        } ?: buildString {
            appendLine("Cardmarket")
            appendLine("  Not available")
            appendLine()
            appendLine("Note")
            appendLine("  This card was loaded without pricing data.")
            appendLine("  Check that this screen receives the full card detail response, not only the search/list response.")
            appendLine("  Card ID used: $cardId")
        }.trimEnd()

        return buildString {
            appendLine("Card name:")
            appendLine("  $name")
            appendLine()

            appendLine("Card ID:")
            appendLine("  $cardId")
            appendLine()

            appendLine("Local ID:")
            appendLine("  $localId")
            appendLine()

            appendLine("Category:")
            appendLine("  $category")
            appendLine()

            appendLine("Set:")
            appendLine("  $setInfo")
            appendLine()

            appendLine("Set ID:")
            appendLine("  $setId")
            appendLine()

            appendLine("Card number:")
            appendLine("  $cardNumber")
            appendLine()

            appendLine("Set card count:")
            appendLine("  $setTotalCards")
            appendLine()

            appendLine("Rarity:")
            appendLine("  $rarity")
            appendLine()

            appendLine("Variants:")
            appendLine("  $variantsText")
            appendLine()

            appendLine("Detailed variants:")
            appendLine(detailedVariantsText.prependIndent("  "))
            appendLine()

            appendLine("Illustrator:")
            appendLine("  $illustrator")
            appendLine()

            appendLine("Market price:")
            appendLine(priceText.prependIndent("  "))
            appendLine()

            appendLine("Gameplay data:")
            appendLine("  HP: $hp")
            appendLine("  Type: $typesText")
            appendLine("  Stage: $stage")
            appendLine("  Evolves from: $evolvesFrom")
            appendLine("  Pokédex ID: $dexText")
            appendLine("  Retreat cost: $retreat")
            appendLine("  Regulation mark: $regulationMark")
            appendLine()

            appendLine("Legality:")
            appendLine(legalText.prependIndent("  "))
            appendLine()

            appendLine("Weaknesses:")
            appendLine(weaknessesText.prependIndent("  "))
            appendLine()

            appendLine("Attacks:")
            appendLine(attacksText.prependIndent("  "))
            appendLine()

            appendLine("Description:")
            appendLine("  $description")
            appendLine()

            appendLine("Card data updated:")
            appendLine("  $updated")
        }.trimEnd()
    }    private fun updateUI(text: String) {
        val speciesTextView: TextView = findViewById(R.id.speciesTextView)
        speciesTextView.text = text
    }



}
