package com.example.gpsapp


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


@SuppressLint("StaticFieldLeak")
private lateinit var binding: ActivityMainBinding
private var totalDistance = 0.0
private var locationList  = mutableListOf<Location>()
var locationManager: LocationManager? = null
var locationChangedTime: Long = 0L
var state = true
var lat = 0.0
var lon = 0.0
var latLng = LatLng(0.0, 0.0)
var gmap: GoogleMap? = null
var track = true


class MainActivity : AppCompatActivity(), LocationListener, OnMapReadyCallback,GoogleMap.OnMyLocationClickListener,GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnCameraMoveStartedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("Tag", "oncreate")
//        binding.mapView.onCreate(null)
//        binding.mapView.getMapAsync(this)

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
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5F,this)

        }

        lifecycleScope.launch {
            while (true) {
                if(state) {
                    val time = System.currentTimeMillis() - locationChangedTime
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
                    var seconds = TimeUnit.MILLISECONDS.toSeconds(time)
                    seconds -= minutes * 60
                    binding.textView3.text = "Time Elapsed: $minutes min, ${seconds}s"
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
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5F,this)

        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("Tag", "map ready")
        binding.mapView.onResume()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        map.setOnCameraMoveStartedListener(this)
        map.isMyLocationEnabled = true
        gmap = map
    }

    override fun onLocationChanged(location: Location) {
        try{
        Log.d("Tag", "loc change")
        binding.progressBar.visibility=View.GONE
        state = true
        locationChangedTime = System.currentTimeMillis()
        lat = location.latitude
        lon = location.longitude
        latLng = LatLng(lat,lon)
        if(track)
            gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f), 100, null)
        binding.coord.text = "Latitude: ${lat.shorten(5)}, Longitude: ${lon.shorten(5)}"
        binding.textView.text = Geocoder(binding.root.context, Locale.US).getFromLocation(
            lat,
            lon,
            1
        )[0].getAddressLine(0)
        locationList.add(location)
        Log.d("list", locationList.toString())
        if (locationList.size>1) {
            Log.d("list", locationList.lastIndex.toString())
            totalDistance += location.distanceTo(locationList[locationList.lastIndex - 1])
        }
        val miles = locationDistanceParse(totalDistance)[0].toInt()
        val feet = locationDistanceParse(totalDistance)[1].shorten(2)
        //binding.textView2.text = "Total Distance: $miles mi, $feet ft."
        binding.textView2.text = "Total Distance: ${totalDistance.shorten(5)} meters"
        } catch (e: Exception){
            Log.d("list", e.toString())
        }
    }
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    private fun Double.shorten(decimals: Int): Double{ return String.format("%.$decimals" + "f", this).replace(',', '.').toDouble() }
    private fun locationDistanceParse(meters: Double): Array<Double> {
        val miles: Double = (meters * 0.000621371)
        val milesInt: Int = miles.toInt()
        val feet: Double = (miles-milesInt) * 5280
        return arrayOf(miles,feet)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        state = false
        //Step 8 says to remove listener on rotation and relaunch, but this hurts app functionality as it restarts the initial wait
        //locationManager?.removeUpdates(this)
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

    override fun onMyLocationClick(location: Location) {

        Log.d("button", "onmylocationclick")

        //gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f), 500, null)
    }

    override fun onMyLocationButtonClick(): Boolean {
        Log.d("button", "mylocationbuttonclick")
        track = true
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f), 500, null)
        return false
    }

    override fun onCameraMoveStarted(reason: Int) {
        Log.d("button", "camera, int: $reason")
        if (reason == REASON_GESTURE)
            track = false
    }
}
