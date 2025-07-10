package com.sslythrrr.galeri.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.dao.SearchHistoryDao
import com.sslythrrr.galeri.data.entity.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val searchHistoryDao: SearchHistoryDao
    init {
        val db = AppDatabase.getInstance(application)
        searchHistoryDao = db.searchHistoryDao()
    }
    val searchHistory: StateFlow<List<SearchHistory>> = searchHistoryDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDao.clearHistory()
        }
    }
}