package com.example.locationinfo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    LocationScreen()
                }
            }
        }
    }
}

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionsLauncher.launch(permissions)
        } else {
            hasLocationPermission = true
        }
    }

    // States for user location, address, and custom markers.
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf("Fetching address...") }
    var customMarkers by remember { mutableStateOf(listOf<LatLng>()) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        userLocation = latLng
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            address = if (!addresses.isNullOrEmpty())
                                addresses[0].getAddressLine(0) ?: "Address not found"
                            else "Address not found"
                        } catch (e: Exception) {
                            address = "Error fetching address"
                        }
                    } else {
                        address = "Location not available"
                    }
                }
            } catch (e: SecurityException) {
                address = "Location permission error"
            }
        }
    }


    val cameraPositionState = rememberCameraPositionState {
        position = userLocation?.let {
            CameraPosition.fromLatLngZoom(it, 15f)
        } ?: CameraPosition.fromLatLngZoom(LatLng(37.4219999, -122.0862462), 15f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Display the fetched address.
        Text(text = "Address: $address", modifier = Modifier.padding(16.dp))
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                customMarkers = customMarkers + latLng
            }
        ) {
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Your Location"
                )
            }
            customMarkers.forEach { markerLatLng ->
                Marker(
                    state = MarkerState(position = markerLatLng),
                    title = "Custom Marker"
                )
            }
        }
    }
}
