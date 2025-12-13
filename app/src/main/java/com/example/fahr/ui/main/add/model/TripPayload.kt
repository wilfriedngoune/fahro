package com.example.fahr.ui.main.add.model

import java.io.Serializable

data class TripPayload(
    val departureDate: String,
    val departureTime: String,
    val departureAddress: String,
    val arrivalAddress: String,
    val stops: List<String>,
    val price: Double           // ✅ nouveau champ
) : Serializable
