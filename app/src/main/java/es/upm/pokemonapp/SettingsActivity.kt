package es.upm.pokemonapp
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.pm.PackageManager
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import es.upm.pokemonapp.auth.AuthActivity
import es.upm.pokemonapp.auth.FirebaseAuthHelper
import es.upm.pokemonapp.utils.LocationHelper
import es.upm.pokemonapp.utils.NavigationUtils
import es.upm.pokemonapp.utils.goToActivityWithoutReturn
import es.upm.pokemonapp.utils.showToast

import android.Manifest


@SuppressLint("UseSwitchCompatOrMaterialCode","SetTextI18n")
class SettingsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var locationSwitch: Switch
    private lateinit var darkModeSwitch: Switch
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        LocationHelper.setCallback { location: Location ->
            onLocationChanged(location)
        }

        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val darkModeEnabled = sharedPreferences.getBoolean("theme", false)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        darkModeSwitch.isChecked = darkModeEnabled
        darkModeSwitch.text = if (darkModeEnabled) "Disable dark mode" else "Enable dark mode"
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            darkModeSwitch.text = if (isChecked) "Disable dark mode" else "Enable dark mode"
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            sharedPreferences.edit().putBoolean("theme", isChecked).apply()
        }

        val locationEnabled = LocationHelper.isLocationEnabled(this)
        locationSwitch = findViewById(R.id.locationSwitch)
        locationSwitch.isChecked = locationEnabled
        locationSwitch.text = if (locationEnabled) "Location enabled" else "Location disabled"

        val locationText: TextView = findViewById(R.id.locationText)

        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            locationSwitch.text = if (isChecked) "Location enabled" else "Location disabled"
            locationText.text = if (isChecked) "\uD83D\uDCCD" else ""

            if (isChecked) {
                if (hasLocationPermissions()) {
                    LocationHelper.enableLocationUpdates(this)
                    LocationHelper.startLocationUpdates(this)
                } else {
                    requestLocationPermissions()
                }
            } else {
                LocationHelper.disableLocationUpdates(this)
            }
        }

        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
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
                currentNavigationId = R.id.navigation_settings,
                context = this
            )
            drawerLayout.closeDrawers()
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }

        val userIdentifierEditText: EditText = findViewById(R.id.userIdentifier)
        val userIdentifier = FirebaseAuthHelper.getInstance().getCurrentUser()?.displayName.toString()
        userIdentifierEditText.setText(userIdentifier)

        val editIdentifier: Button = findViewById(R.id.editIdentifier)
        editIdentifier.setOnClickListener{
            val newUserName = userIdentifierEditText.text.toString()
            changeUserName(newUserName)
        }

        val buttonLogOut: Button = findViewById(R.id.logOutButton)
        buttonLogOut.setOnClickListener{
            performLogout()
        }

        if (locationSwitch.isChecked || darkModeSwitch.isChecked){
            LocationHelper.startLocationUpdates(this)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        NavigationUtils.navViewClick(
            item = item,
            currentNavigationId = R.id.navigation_settings,
            context = this
        )
        return true
    }

    private fun onLocationChanged(location: Location) {
        val textView: TextView = findViewById(R.id.locationText)
        val formattedText = StringBuilder()
        formattedText.append("\uD83C\uDF0D Current Location \n\n")
        formattedText.append("\uD83D\uDCCC Latitude: ${location.latitude}\n")
        formattedText.append("\uD83D\uDCCC Longitude: ${location.longitude}")
        textView.text = formattedText.toString()
        showToast("Location updated successfully.")
    }

    private fun performLogout() {
        FirebaseAuthHelper.getInstance().logout(this) {
            goToActivityWithoutReturn(this, AuthActivity::class.java)
        }
        Log.d(TAG, "User: ${FirebaseAuthHelper.getInstance().getCurrentUser()?.displayName.toString()} performed logout")
    }

    private fun changeUserName(displayName: String){
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("User edited successfully")
                } else {
                    showToast("Error: ${task.exception?.localizedMessage}")
  }
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LocationHelper.startLocationUpdates(this)
            } else {
                showToast("Ubicación desactivada. Necesitas conceder permisos de ubicación para usar esta función.")
            }
        }
    }
}