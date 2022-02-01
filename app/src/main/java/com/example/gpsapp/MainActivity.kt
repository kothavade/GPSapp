package com.example.gpsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.gpsapp.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import java.util.*

private lateinit var fusedLocationClient: FusedLocationProviderClient
private lateinit var locationRequest: LocationRequest
private lateinit var locationCallback: LocationCallback

private lateinit var binding: ActivityMainBinding


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("Tag", "oncreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(binding.root.context)
        getLocationUpdates()

    }
    private fun getLocationUpdates()
    {
        Log.d("Tag", "Get location updates")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(binding.root.context!!)
        Log.d("Tag", "Location Request")

        locationRequest = LocationRequest()
        locationRequest.interval = 500
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("Tag", "callback")

                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    val location =
                        locationResult.lastLocation
                    binding.coord.text = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    binding.textView.text = Geocoder(binding.root.context, Locale.US).getFromLocation(location.latitude,location.longitude,1)[0].getAddressLine(0)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        Log.d("Tag", "start updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()!!
        )
    }

    private fun stopLocationUpdates() {
        Log.d("Tag", "stop updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }
}
