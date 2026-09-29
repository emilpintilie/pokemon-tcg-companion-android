package es.upm.pokemonapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import es.upm.pokemonapp.data.room.AppContainer
import es.upm.pokemonapp.data.room.AppDatabase
import es.upm.pokemonapp.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File


class OpenStreetMapActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "btaOpenStreetMapActivity"
    private lateinit var map: MapView
    private lateinit var marker: Marker
    private lateinit var drawerLayout: DrawerLayout

    //On stop
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: OpenStreetMapActivityStopped")
        LocationHelper.stopLocationUpdates(this)
    }
    //On create
    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting activity...")
        enableEdgeToEdge()
        setContentView(R.layout.activity_open_street_maps)

        //This way we sync the firebase database
        AppContainer.syncDatabase(applicationContext)

        //When the onLcoationChanged is executed on the LocationHelper, the method is called.
        LocationHelper.setCallback { location: Location ->
            onLocationChanged(location)
        }
        LocationHelper.startLocationUpdates(this)

        drawerLayout = findViewById(R.id.drawer_layout)
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }
        //Location View
        Configuration.getInstance().userAgentValue = "es.upm.pokemonapp"
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        val location = LocationHelper.getLastKnownLocation(this)
        val startPoint = if (location != null) {
            Log.d(
                TAG,
                "onCreate: Location[${location.altitude}][${location.latitude}][${location.longitude}]"
            )
            GeoPoint(location.latitude, location.longitude)
        } else {
            Log.d(TAG, "onCreate: Location is null, using default coordinates")
            GeoPoint(40.389683644051864, -3.627825356970311)
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        //Add actual position marker
        marker = Marker(map)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "My current location"
        map.overlays.add(marker)
        map.controller.setZoom(18.0)

        //Load coordinates from Room and show in map with another marker.
        loadDatabaseMarkers()



        // ButtomNavigationMenu
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
                currentNavigationId = R.id.navigation_map,
                context = this
            )
            drawerLayout.closeDrawers()
            true
        }

    }

    //Coordinates handling
    private fun onLocationChanged(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        map.controller.animateTo(geoPoint)
        marker.position = geoPoint
        map.invalidate()

        //Refresh firebaseDatabase
        AppContainer.syncDatabase(applicationContext)

        showToast("Location updated successfully.")
    }

    //TODO SACAR TODO ESTO FUERA DE LA CLASE
    //Function to get the pressing event, in order to press and hold
    private var pressStartingTime: Long =  SystemClock.uptimeMillis()
    //Variable controlling the amount of hold needed in order to register an add new marker event
    private val pressDurationThreesholdMin = 1500
    private val pressDurationThreesholdMax = 3000

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG,"Touch event detected: "+event)
        if(event.action.equals(MotionEvent.ACTION_UP)) {
            //If it detects a press we calculate the diff with the last press in order to know if is a long press, press and release...
            Log.d(TAG, "Up movement detected")
            val timeDiference = (SystemClock.uptimeMillis() - pressStartingTime).toInt()
            if(timeDiference >= pressDurationThreesholdMin && timeDiference < pressDurationThreesholdMax){
                val punto = map.projection.fromPixels(event.x.toInt(), event.y.toInt())
                map.controller.animateTo(punto)
                createNewMarker(GeoPoint(punto.latitude,punto.longitude),this   )
                Log.d(TAG,"long "+punto.longitude +" lat "+punto.latitude)
            }
        }else if (!event.action.equals(MotionEvent.ACTION_UP)){
            //Reset time in case we move or other action
            Log.d(TAG, "Other movement detected")
            pressStartingTime = SystemClock.uptimeMillis()
        }
        pressStartingTime = SystemClock.uptimeMillis()

        return super.onTouchEvent(event)
    }

    private fun createNewMarker(positionCoordinates: GeoPoint, context: Context){
        //Show the card register dialog
        showCardRegisterDialog(positionCoordinates){
            cardDataString ->
            val parts = cardDataString.split("|||")
            val cardId: String = parts.getOrNull(0) ?: ""
            val cardName: String = parts.getOrNull(1) ?: ""
            val cardPrice: String = parts.getOrNull(2) ?: "0.0"
            val status: String = parts.getOrNull(3) ?: "Available"
            val latitude: String = parts.getOrNull(4) ?: "0.0"
            val longitude: String = parts.getOrNull(5) ?: "0.0"

            //Create the marker
            val marker = Marker(map)
            marker.position = positionCoordinates
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Set icon based on status
            val iconRes = when(status) {
                "Sold" -> R.drawable.species_marker
                "Reserved" -> R.drawable.endagered_species_marker
                else -> R.drawable.species_marker
            }

            marker.icon = ContextCompat.getDrawable(context, iconRes)

            //Add metadata to marker.
            marker.title = cardName
            val description =  StringBuilder()
            description.append("Card Info: \n")
            description.append("🎴 Name: $cardName \n")
            description.append("💰 Price: €$cardPrice \n")
            description.append("📊 Status: $status \n")
            description.append("📍 Latitude: $latitude \n")
            description.append("📍 Longitude: $longitude")
            marker.subDescription = description.toString()

            //Add marker to map.
            map.overlays.add(marker)

            //force screen update
            map.controller.animateTo(positionCoordinates)

        }



    }


    @SuppressLint("SetTextI18n")
    private fun showCardRegisterDialog(positionCoordinates: GeoPoint, onDataSaved: (String) -> Unit){
        //Ask display of register_landmark.xml in dialog
        val cardDialog = LayoutInflater.from(this).inflate(R.layout.register_landmark, null)
        val builder = AlertDialog.Builder(this).setView(cardDialog).setTitle("Register Card").create()

        val cancelButton: Button = cardDialog.findViewById(R.id.cancelButton)
        val saveButton: Button = cardDialog.findViewById(R.id.saveButton)
        val cardIdEdit: EditText = cardDialog.findViewById(R.id.cardId)
        val cardNameEdit: EditText = cardDialog.findViewById(R.id.cardName)
        val userNameEdit: EditText = cardDialog.findViewById(R.id.userName)
        val userEmailEdit: EditText = cardDialog.findViewById(R.id.userEmail)
        val cardPriceEdit: EditText = cardDialog.findViewById(R.id.cardPrice)
        val statusSpinner: Spinner = cardDialog.findViewById(R.id.statusSpinner)
        val latitudeEdit: EditText = cardDialog.findViewById(R.id.latitude)
        val longitudeEdit: EditText = cardDialog.findViewById(R.id.longitude)

        //Set default text on editText box
        latitudeEdit.setText(positionCoordinates.latitude.toString())
        longitudeEdit.setText(positionCoordinates.longitude.toString())

        // Get current Firebase user and fill in name and email
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        userNameEdit.setText(currentUser?.displayName ?: "Unknown")
        userEmailEdit.setText(currentUser?.email ?: "unknown@email.com")
        userNameEdit.isEnabled = false
        userEmailEdit.isEnabled = false

        saveButton.setOnClickListener{
            val cardId: String = cardIdEdit.text.toString()
            val cardName: String = cardNameEdit.text.toString()
            val userName: String = userNameEdit.text.toString()
            val userEmail: String = userEmailEdit.text.toString()
            val cardPrice: String = cardPriceEdit.text.toString()
            val status: String = statusSpinner.selectedItem.toString()
            val latitude: String = latitudeEdit.text.toString()
            val longitude: String = longitudeEdit.text.toString()

            if(cardName.isEmpty() || cardId.isEmpty()) {
                showToast("Please fill in Card Name and Card ID")
                return@setOnClickListener
            }

            //Save on persistence
            val repository = AppContainer.coordinatesRepository
            lifecycleScope.launch {
                repository.saveCoordinates(
                    cardId = cardId,
                    cardName = cardName,
                    userName = userName,
                    userEmail = userEmail,
                    cardPrice = cardPrice.toDoubleOrNull() ?: 0.0,
                    status = status,
                    latitude = latitude.toDouble(),
                    longitude = longitude.toDouble(),
                    imageUrl = ""
                )
            }

            showToast("Card saved successfully.")
            Log.d(TAG,"Saved card: $cardName (ID: $cardId) at location $latitude, $longitude")
            //To return when saved button is pressed.
            onDataSaved("$cardId|||$cardName|||$cardPrice|||$status|||$latitude|||$longitude")
            builder.dismiss()
        }
        cancelButton.setOnClickListener{
            showToast("Card not saved")
            builder.dismiss()
        }
        builder.show()

        //Refresh firebaseDatabase
        AppContainer.syncDatabase(applicationContext)
    }

    //Save coordinates to file
    private fun saveMarkerToFile(species: String, type: String, latitude: String, longitude: String) {
        val fileName = "markers.csv"
        val file = File(filesDir, fileName)
        val timestamp = System.currentTimeMillis()
        file.appendText("$timestamp;$species;$type;$latitude;$longitude\n")
    }

    //Data class to pass the data
    data class RoomData(
        val cardId: String,
        val cardName: String,
        val cardPrice: Double,
        val status: String,
        val geoPoint: GeoPoint
    )

    //Read markers form database and put them in the map.
    private fun loadDatabaseMarkers(){
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO){
            val dbCoordinates = db.coordinatesDao().getAll()
            val roomData = dbCoordinates.map {
                RoomData(
                    cardId = it.cardId,
                    cardName = it.cardName,
                    cardPrice = it.cardPrice,
                    status = it.status,
                    geoPoint = GeoPoint(it.latitude, it.longitude)
                )
            }

            Log.d(TAG, "Obtained cards from Room: ${roomData}")
            withContext(Dispatchers.Main){
                addDatabaseMarkers(map, roomData, this@OpenStreetMapActivity)
            }
        }
    }

    //Add markers to map.
    private fun addDatabaseMarkers(map: MapView, roomData: List<RoomData>, context: Context){
        for(data in roomData){
            val marker = Marker(map)
            marker.position = data.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            Log.d(TAG,"Creating marker for card: ${data.cardName}")

            //Set icon based on status
            val iconRes = when(data.status) {
                "Sold" -> R.drawable.species_marker
                "Reserved" -> R.drawable.endagered_species_marker
                else -> R.drawable.species_marker
            }

            marker.icon = ContextCompat.getDrawable(context, iconRes)

            //Add metadata to marker.
            marker.title = data.cardName
            val description =  StringBuilder()
            description.append("Card Details: \n")
            description.append("🎴 ID: ${data.cardId} \n")
            description.append("💰 Price: €${data.cardPrice} \n")
            description.append("📊 Status: ${data.status} \n")
            description.append("📍 Latitude: ${data.geoPoint.latitude} \n")
            description.append("📍 Longitude: ${data.geoPoint.longitude}")

            marker.subDescription = description.toString()

            map.overlays.add(marker)

            Log.d(TAG,"Marker created for $data")
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

}


