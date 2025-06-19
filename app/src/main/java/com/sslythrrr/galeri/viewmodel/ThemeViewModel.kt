package com.sslythrrr.galeri.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class ThemeViewModel(private val context: Context) : ViewModel() {

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val isDarkTheme: StateFlow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_THEME] == false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[IS_DARK_THEME] = !isDark
            }
        }
    }
}