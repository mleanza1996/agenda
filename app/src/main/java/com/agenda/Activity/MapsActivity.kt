package com.agenda.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.agenda.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var buttonAddMarker: FloatingActionButton
    private lateinit var buttonConfirm: Button

    private var isAddingMarker = false // Flag for adding markers
    private val markerLocations = mutableListOf<LatLng>() // List to store marker coordinates

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initializePlacesClient()
        initializeUIButton()
        initializeMaps()
    }

    private fun initializePlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_api_key))
        placesClient = Places.createClient(this)
    }

    private fun initializeUIButton() {
        // Get references to UI elements
        buttonAddMarker = findViewById(R.id.buttonAddMarker)
        buttonConfirm = findViewById(R.id.buttonConfirm)

        buttonAddMarker.setOnClickListener {
            isAddingMarker = !isAddingMarker
            if (isAddingMarker) {
                buttonAddMarker.setImageResource(R.drawable.ic_marker_active) // Update icon if needed
                Toast.makeText(this, "Click on the map to add a marker", Toast.LENGTH_SHORT).show()
            } else {
                buttonAddMarker.setImageResource(R.drawable.ic_add_marker) // Reset icon
                Toast.makeText(this, "Marker addition canceled", Toast.LENGTH_SHORT).show()
            }
        }

        buttonConfirm.setOnClickListener {
            if (markerLocations.isEmpty()) {
                Toast.makeText(this, "No markers added.", Toast.LENGTH_SHORT).show()
            } else {

                val coordinates = markerLocations.joinToString(separator = ";") {
                    "${it.latitude},${it.longitude}"
                }

                val resultIntent = Intent().apply {
                    putExtra("marker_locations", coordinates)
                }

                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun initializeMaps() {
        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set up a click listener on the map
        mMap.setOnMapClickListener { latLng ->
            if (isAddingMarker) {
                // Add a marker at the clicked position
                mMap.addMarker(MarkerOptions().position(latLng).title("New Marker"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                // Save the latitude and longitude to the list
                markerLocations.add(latLng)

                // Notify the user
                Toast.makeText(
                    this,
                    "Marker added at: Lat=${latLng.latitude}, Lng=${latLng.longitude}",
                    Toast.LENGTH_SHORT
                ).show()

                // Reset the flag
                isAddingMarker = false
                buttonAddMarker.setImageResource(R.drawable.ic_add_marker) // Reset icon
            }
        }

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Enable location-related functionalities
            mMap.isMyLocationEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Location permission denied. Unable to access location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}
