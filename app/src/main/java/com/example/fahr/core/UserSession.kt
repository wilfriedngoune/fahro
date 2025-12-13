package com.example.fahr.core

import android.content.Context

object UserSession {

    private const val PREFS_NAME = "fahr_user_prefs"
    private const val KEY_USER_ID = "current_user_id"

    fun setCurrentUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getCurrentUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }
}
