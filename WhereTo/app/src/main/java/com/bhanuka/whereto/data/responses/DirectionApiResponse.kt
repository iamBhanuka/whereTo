package com.bhanuka.whereto.data.responses

class DirectionApiResponse(
    val geocoded_waypoints: List<GeocodedWaypoint>,
    val routes: List<Route>,
    val status: String
)