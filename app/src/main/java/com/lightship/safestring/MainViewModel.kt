package com.lightship.safestring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val secureStorageService = SecureStorageService(application)
    
    private val _stringList = MutableStateFlow<List<String>>(emptyList())
    val stringList: StateFlow<List<String>> = _stringList.asStateFlow()
    
    private val _currentString = MutableStateFlow<Pair<String, String?>?>(null)
    val currentString: StateFlow<Pair<String, String?>?> = _currentString.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadStringList() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val names = secureStorageService.getStoredStringNames()
                _stringList.value = names
            }
            _isLoading.value = false
        }
    }
    
    fun saveString(name: String, value: String) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                secureStorageService.encryptAndSave(name, value)
                loadStringList()
            }
            _isLoading.value = false
        }
    }
    
    fun loadString(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val value = secureStorageService.retrieveDecrypted(name)
                _currentString.value = Pair(name, value)
            }
            _isLoading.value = false
        }
    }
    
    fun deleteString(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                secureStorageService.deleteString(name)
                loadStringList()
            }
            _isLoading.value = false
        }
    }
    
    fun clearCurrentString() {
        _currentString.value = null
    }
}