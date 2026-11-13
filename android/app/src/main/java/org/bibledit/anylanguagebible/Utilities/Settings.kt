package org.bibledit.anylanguagebible.Utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

object Settings {

    object Key {
        const val VOLUME_ID = "VolumeId"
    }

    private lateinit var prefs: SharedPreferences

    // Call this once at app start, before using any other function.
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
        volumeIdState = prefs.getInt(Key.VOLUME_ID, 0)
    }

    // Returns 0 if no value has been stored yet.
    fun getVolumeId(): Int = prefs.getInt(Key.VOLUME_ID, 0)

    fun setVolumeId(value: Int) {
        prefs.edit { putInt(Key.VOLUME_ID, value) }
        volumeIdState = value
    }

    // In a composable, read this to get the Android version of Swift @AppStorage:
    // the UI recomposes automatically when setVolumeId() changes the value.
    //     val volumeId = Settings.volumeIdState
    var volumeIdState by mutableIntStateOf(0)
        private set
}
