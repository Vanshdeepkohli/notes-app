package com.example.notes.SharedPreference

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class SharedPrefLiveData(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Boolean
) : LiveData<Boolean>() {

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, sharedKey ->
        if (sharedKey == key) {
            value = sharedPreferences.getBoolean(key, defaultValue)
        }
    }

    override fun onActive() {
        super.onActive()
        value = sharedPreferences.getBoolean(key, defaultValue)
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
