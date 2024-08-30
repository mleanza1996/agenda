package com.agenda.Activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ViewMarkerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap // Istanza della mappa di Google
    private lateinit var buttonAddMarker: FloatingActionButton // Pulsante per aggiungere marker (nascosto)
    private lateinit var buttonConfirm: Button // Pulsante di conferma (nascosto)

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1 // Codice richiesta permessi di localizzazione
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        hideButton()     // Nasconde i pulsanti che non sono necessari in questa activity
        initializeMaps() // Inizializza la mappa
    }

    private fun hideButton() {
        // Nasconde i pulsanti per aggiungere e confermare i marker
        buttonAddMarker = findViewById(R.id.buttonAddMarker)
        buttonConfirm = findViewById(R.id.buttonConfirm)

        buttonAddMarker.visibility = View.GONE
        buttonConfirm.visibility = View.GONE
    }

    private fun initializeMaps() {
        // Inizializza la mappa usando SupportMapFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
            // Abilita le funzionalità di localizzazione se i permessi sono stati concessi
            mMap.isMyLocationEnabled = true
        }

        // Recupera le informazioni sui marker dall'Intent
        val markerInfosString = intent.getStringExtra("MARKER_INFOS")
        markerInfosString?.let {
            // Converte la stringa delle coordinate in una lista di LatLng
            val markerInfos = it.split(";").map { coord ->
                val (lat, lng) = coord.split(",").map { it.toDouble() }
                LatLng(lat, lng)
            }
            addMarkersToMap(markerInfos) // Aggiunge i marker alla mappa
        }
    }

    private fun addMarkersToMap(markerInfos: List<LatLng>) {
        // Aggiunge i marker alla mappa e muove la fotocamera
        for (latLng in markerInfos) {
            mMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Se il permesso è concesso, abilita le funzionalità di localizzazione
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Gestire il caso in cui i permessi siano ancora mancanti
                    return
                }
                mMap.isMyLocationEnabled = true
            } else {
                // Mostra un messaggio se il permesso è negato
                Toast.makeText(
                    this,
                    "Permesso di localizzazione negato. Impossibile accedere alla posizione.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}