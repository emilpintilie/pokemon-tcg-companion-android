package es.upm.pokemonapp.utils

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat

object LocationHelper : LocationListener{
    private lateinit var locationManager: LocationManager
    private var callback: ((Location) -> Unit)? = null
    private var locationEnabled = false

    fun enableLocationUpdates(context: Context): Boolean{
        if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            writeEnabledLocationSharedPreferences(context)
            Log.d(TAG,"Location updates enabled on LocationHelper")
            return true
        }else{
            Log.d(TAG,"Location permissions not granded")
            return false
        }
    }
    fun disableLocationUpdates(context: Context): Boolean{
        if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            writeDisabledLocationSharedPreferences(context)
            Log.d(TAG,"Location updates disabled on LocationHelper")
            return true
        }else{
            Log.d(TAG,"Location permissions not granded")
            return false
        }
    }

    fun startLocationUpdates(context: Context): Boolean {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (isLocationEnabled(context) && (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
            writeEnabledLocationSharedPreferences(context)
            Log.d(TAG,"Location updates started on LocationHelper")
            return true
        }else{
            Log.d(TAG,"Location updates not started on LocationHelper")
            return false
        }
    }

    fun stopLocationUpdates(context: Context) {
        locationManager.removeUpdates(this)
        writeDisabledLocationSharedPreferences(context)
        Log.d(TAG,"Location updates stopped from LocationHelper")
    }

    fun getLastKnownLocation(context: Context): Location? {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            null
        }
    }

    fun setCallback(cb: (Location) -> Unit){
        callback = cb
        Log.d(TAG,"Callback setup from LocationHelper")
    }

    override fun onLocationChanged(location: Location) {
        //Notify classes that the locationIsChanged
        Log.d(TAG,"Location updated from LocationHelper")
        callback?.invoke(location)
    }

    fun isLocationEnabled(context: Context) : Boolean{
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        locationEnabled = sharedPreferences.getBoolean("location", false)
        Log.d(TAG, "IsLocationEnabled: $locationEnabled")
        return locationEnabled
    }

    private fun writeEnabledLocationSharedPreferences(context: Context){
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("location", true).apply()
        Log.d(TAG, "IsLocationEnabled wrote to AppPreferences")
    }

    private fun writeDisabledLocationSharedPreferences(context: Context){
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("location", false).apply()
        Log.d(TAG, "IsLocationDisabled wrote to AppPreferences")

    }

}

