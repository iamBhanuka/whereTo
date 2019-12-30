package com.bhanuka.whereto

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationProvider
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bhanuka.whereto.data.remote.DirectionAPIClient
import com.bhanuka.whereto.data.responses.DirectionApiResponse
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit


class MapsActivity() : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var mMap: GoogleMap? = null
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private lateinit var mLastLocation: Location
    private var mMarker: Marker? = null
    private var mplace: Place? = null

    //location

    lateinit var fusedLocationProvider: LocationProvider
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private const val My_PERMISSION_CODE: Int = 1000
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Places.initialize(getApplicationContext(), "AIzaSyCVluAGL43uqSqE0Z5BDcUEMPKnlQbgO28");

        fusedLocationProviderClient = FusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //request permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest();
                buildLocationcallback();

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                );
            }
        } else {
            buildLocationRequest();
            buildLocationcallback();

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            );
        }

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            val AUTOCOMPLETE_REQUEST_CODE = 1;

// Set the fields to specify which types of place data to
// return after the user has made a selection.
            val fields =
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

// Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields
            )
                .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

        }


    }

    private fun getGeoContext(): GeoApiContext = GeoApiContext.Builder()
        .queryRateLimit(3)
        .apiKey(getString(R.string.googleMapApiKey))
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.SECONDS)
        .build()


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        print("here?1")
        super.onActivityResult(requestCode, resultCode, data)
        print("here?1")
        if (requestCode == 1) {
            print("here?2")
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!);
                mplace = place
                val markerOptions = MarkerOptions()
                    .position(place.latLng!!)
                    .title("Fuck")

                print("here?3")
                print("${mMarker == null}")
                print("${place.latLng}")
                Log.d("SEX", "lol")


                if (mMap != null) {
                    mMarker = mMap?.addMarker(markerOptions)

                    getDirection(TravelMode.DRIVING, mMarker!!.position, place.latLng!!)
                    getDirection(TravelMode.WALKING, mMarker!!.position, place.latLng!!)
                    //move camera

                    mMap?.moveCamera(CameraUpdateFactory.newLatLng(place.latLng))
                    mMap?.animateCamera(CameraUpdateFactory.zoomTo(11f))
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                val status = Autocomplete.getStatusFromIntent(data!!);
                Log.i("sex", status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    private fun getDirection(mode: TravelMode, origin: LatLng, destination: LatLng) {

        print("${origin.latitude},${origin.longitude}")
        print("${destination.latitude},${destination.longitude}")

        val result = DirectionAPIClient.invoke().getDirections(
            "${origin.latitude},${origin.longitude}",
            "${destination.latitude},${destination.longitude}",
            mode.name.toLowerCase(),
            getString(R.string.googleMapApiKey)
        )

        print(result)

//        addPolyline(result)
    }

    private fun addPolyline(result: DirectionApiResponse) {
        val decodedPath =
            PolylineEncoding.decode(result.routes[0].overview_polyline.points).map {
                LatLng(it.lat, it.lng)
            }
        mMap?.addPolyline(PolylineOptions().addAll(decodedPath))
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 500
        locationRequest.smallestDisplacement = 10f
    }

    private fun buildLocationcallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation = p0!!.locations.get(p0?.locations.size - 1)

                Log.e("callback","i am callback")

                if (mMarker != null) {
                    mMarker!!.remove()
                }


                val latitude = mLastLocation.latitude
                val longitude = mLastLocation.longitude

                val latLng = LatLng(latitude, longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("I'm Hear")

                if (mMap != null) {
                    mMarker = mMap?.addMarker(markerOptions)

                    //move camera

                    mMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    mMap?.animateCamera(CameraUpdateFactory.zoomTo(11f))
                }

                if (mplace != null) {
                    val destLat = mplace!!.latLng!!.latitude
                    val destLong = mplace!!.latLng!!.longitude
                    if (destLat - latitude == 0.1234567 && destLong - longitude == 0.1234567) {
                        Log.e("callback","Fucking")
                    } else {
                        Log.e("callback","pussy")
                    }
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), My_PERMISSION_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), My_PERMISSION_CODE
                )
            }
            return false
        } else {
            return true
        }
    }


    //override OnRequestpermissionResults

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            My_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (checkLocationPermission()) {

                            buildLocationRequest();
                            buildLocationcallback();

                            fusedLocationProviderClient =
                                LocationServices.getFusedLocationProviderClient(this);
                            fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                            );
                            mMap?.isMyLocationEnabled = true
                        }
                    } else {
                        Toast.makeText(this, "Permission Denid", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        println("sex");
        mMap = googleMap

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap?.isMyLocationEnabled = true
            }
        } else {
            mMap?.isMyLocationEnabled = true
        }
        mMap?.uiSettings?.isZoomControlsEnabled = true
    }


}
