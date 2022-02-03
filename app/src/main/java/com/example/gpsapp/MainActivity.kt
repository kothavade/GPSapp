package com.example.gpsapp


import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.gpsapp.databinding.ActivityMainBinding
import java.util.*

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity(), LocationListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,100.toFloat(),this);

    }

//    private val locationListener: LocationListener =
//        LocationListener { location ->
//
//        }

    override fun onLocationChanged(location: Location) {
        binding.coord.text = "Latitude: ${location.latitude.shorten(5)}, Longitude: ${location.longitude.shorten(5)}"
        binding.textView.text = Geocoder(binding.root.context, Locale.US).getFromLocation(
            location.latitude,
            location.longitude,
            1
        )[0].getAddressLine(0)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    fun Double.shorten(decimals: Int): Double{ return String.format("%.$decimals" + "f", this).replace(',', '.').toDouble() }

}
