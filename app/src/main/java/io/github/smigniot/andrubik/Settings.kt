package io.github.smigniot.andrubik

import android.content.Context
import androidx.core.content.edit

/** Thin wrapper over SharedPreferences for the sync settings. */
class Settings(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("andrubik_settings", Context.MODE_PRIVATE)

    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC, false)
        set(v) = prefs.edit { putBoolean(KEY_SYNC, v) }

    var serverUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(v) = prefs.edit { putString(KEY_URL, v) }

    var username: String
        get() = prefs.getString(KEY_USER, "").orEmpty()
        set(v) = prefs.edit { putString(KEY_USER, v) }

    var password: String
        get() = prefs.getString(KEY_PASS, "").orEmpty()
        set(v) = prefs.edit { putString(KEY_PASS, v) }

    private companion object {
        const val KEY_SYNC = "sync_enabled"
        const val KEY_URL = "server_url"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
    }
}
