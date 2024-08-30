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

    private var isAddingMarker = false // Flag per gestire l'aggiunta di marker
    private val markerLocations = mutableListOf<LatLng>() // Lista per memorizzare le coordinate dei marker

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1 // Costante per la richiesta di permessi di localizzazione
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initializePlacesClient() // Inizializza Places API
        initializeUIButton()     // Inizializza i pulsanti della UI
        initializeMaps()         // Inizializza la mappa
    }

    private fun initializePlacesClient() {
        // Configura Places API
        Places.initialize(applicationContext, getString(R.string.google_api_key))
        placesClient = Places.createClient(this)
    }

    private fun initializeUIButton() {
        // Riferimenti agli elementi UI
        buttonAddMarker = findViewById(R.id.buttonAddMarker)
        buttonConfirm = findViewById(R.id.buttonConfirm)

        // Listener per aggiungere/rimuovere modalità di aggiunta marker
        buttonAddMarker.setOnClickListener {
            isAddingMarker = !isAddingMarker
            if (isAddingMarker) {
                buttonAddMarker.setImageResource(R.drawable.ic_marker_active) // Cambia icona
                Toast.makeText(this, "Clicca sulla mappa per aggiungere un marker", Toast.LENGTH_SHORT).show()
            } else {
                buttonAddMarker.setImageResource(R.drawable.ic_add_marker) // Reset icona
                Toast.makeText(this, "Aggiunta marker annullata", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener per confermare l'aggiunta di marker
        buttonConfirm.setOnClickListener {
            if (markerLocations.isEmpty()) {
                Toast.makeText(this, "Nessun marker aggiunto.", Toast.LENGTH_SHORT).show()
            } else {
                // Prepara le coordinate per il ritorno
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
        // Inizializza la mappa con SupportMapFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Listener per il click sulla mappa per aggiungere un marker
        mMap.setOnMapClickListener { latLng ->
            if (isAddingMarker) {
                mMap.addMarker(MarkerOptions().position(latLng).title("Nuovo Marker"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                markerLocations.add(latLng) // Aggiungi le coordinate alla lista

                Toast.makeText(
                    this,
                    "Marker aggiunto a: Lat=${latLng.latitude}, Lng=${latLng.longitude}",
                    Toast.LENGTH_SHORT
                ).show()

                // Reset del flag e icona
                isAddingMarker = false
                buttonAddMarker.setImageResource(R.drawable.ic_add_marker)
            }
        }

        // Controlla i permessi di localizzazione
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Richiede i permessi di localizzazione
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Abilita le funzionalità di localizzazione
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
                    // Permesso concesso, abilita le funzionalità di localizzazione
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    // Permesso negato, mostra un messaggio
                    Toast.makeText(
                        this,
                        "Permesso di localizzazione negato. Impossibile accedere alla posizione.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}
