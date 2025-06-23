package com.sslythrrr.galeri.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ThemeViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val isDarkTheme: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_DARK_THEME] == true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[IS_DARK_THEME] = isDark
            }
        }
    }
}