package com.example.gpsapp


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpsapp.databinding.ActivityMainBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


@SuppressLint("StaticFieldLeak")
private lateinit var binding: ActivityMainBinding
private var totalDistance = 0.0
var locationManager: LocationManager? = null
var locationChangedTime: Long = 0L
var gmap: GoogleMap? = null
var track = true
var currentLocation: LatLng? = null
var totalElapsedTime: Long = 0
private var locationTimeMap = mutableMapOf<LatLng, Long>()


class MainActivity : AppCompatActivity(), LocationListener, OnMapReadyCallback,GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnCameraMoveStartedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("Tag", "oncreate")

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0x1)
        }
        else {
            Log.d("Tag", "Permission check granted")
            Toast.makeText(this,"Permission already granted!", Toast.LENGTH_SHORT).show()
            binding.mapView.onCreate(null)
            binding.mapView.getMapAsync(this)
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,0F,this)

        }

        lifecycleScope.launch {
            while (true) {
                if(currentLocation!=null) {
                    //val prevTime = locationTimeMap[currentLocation
                    val currentElapsedTime = SystemClock.elapsedRealtime() - locationChangedTime
                    totalElapsedTime = currentElapsedTime + locationTimeMap[currentLocation]!!

                    //locationTimeMap[currentLocation!!] = time
                    var minutes = TimeUnit.MILLISECONDS.toMinutes(currentElapsedTime)
                    var seconds = TimeUnit.MILLISECONDS.toSeconds(currentElapsedTime) - (minutes*60)
                    binding.time.text = "Current Location Time:    $minutes min, ${seconds}s"

                    minutes = TimeUnit.MILLISECONDS.toMinutes(totalElapsedTime)
                    seconds = TimeUnit.MILLISECONDS.toSeconds(totalElapsedTime) - (minutes*60)
                    binding.totalTime.text = "Total Location Time:      $minutes min, ${seconds}s"

                    val maxTime = locationTimeMap.maxByOrNull { it.value }?.value
                    if (maxTime==null || totalElapsedTime >= maxTime){
                        binding.favTime.text = "Favorite Location Time:   $minutes min, ${seconds}s"
                        binding.favLocation.text = binding.location.text
                    }
                }
                delay(100)
            }
        }


    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Tag", "permission result")
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Tag", "result true")
            binding.mapView.onCreate(null)
            binding.mapView.getMapAsync(this)
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,0F,this)

        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("Tag", "map ready")
        binding.mapView.onResume()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnCameraMoveStartedListener(this)
        map.isMyLocationEnabled = true
        gmap = map
    }

    override fun onLocationChanged(location: Location) {
        try{
            Log.d("Tag", "loc change")
            binding.progressBar.visibility=View.GONE
            locationChangedTime = SystemClock.elapsedRealtime()
            val previousLocation = currentLocation
            currentLocation = LatLng(location.latitude,location.longitude)
            if (locationTimeMap.containsKey(previousLocation)) {
                Log.d("Tag", 34.toString())
                locationTimeMap[previousLocation!!] = totalElapsedTime
            }
            if (!locationTimeMap.containsKey(currentLocation)) {
                locationTimeMap[currentLocation!!] = 0
            }

            val coordinates = currentLocation?.latitude?.shorten(4).toString() + ", " + currentLocation?.longitude?.shorten(4).toString()
            if (previousLocation!=null)
                totalDistance += SphericalUtil.computeDistanceBetween(currentLocation, previousLocation)
            val address = Geocoder(binding.root.context, Locale.US)
                .getFromLocation(currentLocation!!.latitude, currentLocation!!.longitude, 1)[0]
                .getAddressLine(0).substringBeforeLast(",")
            val miles: Double = (totalDistance * 0.000621371)
            val milesInt: Int = miles.toInt()
            val feet: Double = (miles-milesInt) * 5280

            binding.coord.text = coordinates
            binding.location.text = address
            binding.distance.text = "Total Distance: $milesInt mi, ${feet.shorten(2)} ft."

            if(track)
                gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 18f), 100, null)

            val favoriteLocationTime = locationTimeMap.maxByOrNull { it.value }
            if (favoriteLocationTime!=null){
                val minutes = TimeUnit.MILLISECONDS.toMinutes(favoriteLocationTime.value)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(favoriteLocationTime.value) - (minutes*60)
                val favoriteLocationAddress = Geocoder(binding.root.context, Locale.US)
                    .getFromLocation(favoriteLocationTime.key.latitude, favoriteLocationTime.key.longitude, 1)[0]
                    .getAddressLine(0).substringBeforeLast(",")
                binding.favTime.text = "Favorite Location Time:   $minutes min, ${seconds}s"
                binding.favLocation.text = favoriteLocationAddress
            }
        } catch (e: Exception){
            Log.d("Tag", e.toString())
        }
    }
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    private fun Double.shorten(decimals: Int): Double{ return String.format("%.$decimals" + "f", this).replace(',', '.').toDouble() }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
    override fun onResume() {
        super.onResume()
        Log.d("Tag", "resume")
        if(gmap!=null)
            binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("Tag", "pause")
        if(gmap!=null)
            binding.mapView.onPause()
    }

    override fun onMyLocationButtonClick(): Boolean {
        Log.d("button", "mylocationbuttonclick")
        track = true
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 18f), 500, null)
        return false
    }

    override fun onCameraMoveStarted(reason: Int) {
        Log.d("button", "camera, int: $reason")
        if (reason == REASON_GESTURE)
            track = false
    }
}
