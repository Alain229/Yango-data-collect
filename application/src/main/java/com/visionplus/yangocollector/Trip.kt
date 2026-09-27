package com.visionplus.yangocollector

data class Trip(
    val id: Long = 0,
    val horodatageCapture: Long = System.currentTimeMillis(),
    val dateCourse: String?,
    val adressesBrutes: String,
    val distanceKm: Double?,
    val dureeMin: Int?,
    val revenuFcfa: Int?,
    val nomPassager: String?,
    val texteBrutOcr: String
)
