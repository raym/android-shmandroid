package com.example.keystorevault

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageService(context: Context) {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    private val preferences: SharedPreferences = context.getSharedPreferences("SecureStringPrefs", Context.MODE_PRIVATE)
    private val ivPreferences: SharedPreferences = context.getSharedPreferences("IVPrefs", Context.MODE_PRIVATE)

    init {
        keyStore.load(null)
    }

    companion object {
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$PADDING"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }

    private fun getOrCreateKey(keyName: String): SecretKey {
        if (!keyStore.containsAlias(keyName)) {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(false) // True if you want key access to require authentication
                .build()
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        return keyStore.getKey(keyName, null) as SecretKey
    }

    fun encryptAndSave(stringName: String, stringValue: String) {
        try {
            val keyAlias = "key_$stringName"
            val secretKey = getOrCreateKey(keyAlias)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(stringValue.toByteArray())

            // Store IV for decryption later
            ivPreferences.edit().putString(keyAlias, Base64.encodeToString(iv, Base64.DEFAULT)).apply()

            // Store encrypted value
            val encryptedValue = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            preferences.edit().putString(stringName, encryptedValue).apply()
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error encrypting string", e)
            throw e
        }
    }

    fun retrieveDecrypted(stringName: String): String? {
        try {
            val keyAlias = "key_$stringName"
            val encryptedValue = preferences.getString(stringName, null) ?: return null
            val ivString = ivPreferences.getString(keyAlias, null) ?: return null

            val secretKey = getOrCreateKey(keyAlias)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivString, Base64.DEFAULT)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))
            val decodedValue = Base64.decode(encryptedValue, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedValue)

            return String(decryptedBytes)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error decrypting string", e)
            return null
        }
    }

    fun getStoredStringNames(): List<String> {
        return preferences.all.keys.toList()
    }

    fun deleteString(stringName: String) {
        val keyAlias = "key_$stringName"
        try {
            // Delete from preferences
            preferences.edit().remove(stringName).apply()
            ivPreferences.edit().remove(keyAlias).apply()

            // Delete the key from the keystore
            keyStore.deleteEntry(keyAlias)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Error deleting string", e)
        }
    }
}