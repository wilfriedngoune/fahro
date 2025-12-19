package com.example.fahr.ui.main.profile.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val car: String = "",
    val address: String = "",
    val description: String = "",
    val rating: Double = 0.0,
    val balance: Double = 0.0,
    val avatarResName: String = "wilfried",
    val verified: Boolean = false
)
