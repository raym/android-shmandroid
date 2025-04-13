package com.lightship.safestring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keystorevault.SecureStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorageService = SecureStorageService(application)

    private val _stringList = MutableLiveData<List<String>>()
    val stringList: LiveData<List<String>> = _stringList

    private val _currentString = MutableLiveData<Pair<String, String?>>()
    val currentString: LiveData<Pair<String, String?>> = _currentString

    fun loadStringList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val names = secureStorageService.getStoredStringNames()
                _stringList.postValue(names)
            }
        }
    }

    fun saveString(name: String, value: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                secureStorageService.encryptAndSave(name, value)
                loadStringList()
            }
        }
    }

    fun loadString(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val value = secureStorageService.retrieveDecrypted(name)
                _currentString.postValue(Pair(name, value))
            }
        }
    }

    fun deleteString(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                secureStorageService.deleteString(name)
                loadStringList()
            }
        }
    }
}