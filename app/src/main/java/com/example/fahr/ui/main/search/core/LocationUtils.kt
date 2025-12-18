package com.example.fahr.core

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlin.math.*


object LocationUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Convertit une adresse texte en (lat, lng).
     * Retourne null si échec.
     */
    fun geocodeAddress(context: Context, address: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context)
            val results = geocoder.getFromLocationName(address, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                loc.latitude to loc.longitude
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "geocodeAddress failed for '$address': ${e.message}")
            null
        }
    }

    /**
     * Distance en km entre deux coordonnées (Haversine).
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) +
                sin(dLon / 2).pow(2.0) * cos(rLat1) * cos(rLat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Distance entre deux adresses (en km) ou null si on ne peut pas géocoder.
     */
    fun distanceBetweenAddresses(
        context: Context,
        fromAddress: String,
        toAddress: String
    ): Double? {
        val from = geocodeAddress(context, fromAddress) ?: return null
        val to = geocodeAddress(context, toAddress) ?: return null
        return distanceKm(from.first, from.second, to.first, to.second)
    }

    /**
     * Estimation de la durée en minutes pour une distance donnée.
     * (Approximation : 40 km/h en moyenne)
     */
    fun estimateTravelMinutes(distanceKm: Double): Int {
        val speedKmH = 40.0           // vitesse moyenne en ville
        val minutes = distanceKm / speedKmH * 60.0
        val minMinutes = 3.0          // minimum 3 minutes par segment
        return minutes.coerceAtLeast(minMinutes).roundToInt()
    }

    /**
     * Ajoute X minutes à une heure HH:mm -> renvoie HH:mm.
     */
    fun addMinutesToTime(baseTime: String, minutesToAdd: Int): String {
        val parts = baseTime.split(":")
        if (parts.size != 2) return baseTime
        val h = parts[0].toIntOrNull() ?: return baseTime
        val m = parts[1].toIntOrNull() ?: return baseTime

        val total = h * 60 + m + minutesToAdd
        val newH = ((total / 60) % 24 + 24) % 24
        val newM = (total % 60 + 60) % 60

        return String.format("%02d:%02d", newH, newM)
    }

    /**
     * Convertit HH:mm en minutes depuis 00:00.
     */
    fun timeToMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
