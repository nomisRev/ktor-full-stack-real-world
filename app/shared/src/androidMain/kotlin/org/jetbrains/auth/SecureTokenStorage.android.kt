package org.jetbrains.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

private const val SECURE_PREFS_NAME = "realworld_secure_auth"
private const val TOKEN_KEY = "auth_token"
private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val KEY_ALIAS = "realworld_auth_token_key"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val AES_KEY_SIZE_BITS = 256
private const val GCM_TAG_SIZE_BITS = 128
private const val GCM_IV_SIZE_BYTES = 12

private val Context.secureTokenDataStore by preferencesDataStore(name = SECURE_PREFS_NAME)
private val authTokenPreferenceKey = stringPreferencesKey(TOKEN_KEY)

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidSecureTokenStorage(context) }
}

private class AndroidSecureTokenStorage(context: Context) : SecureTokenStorage {
    private val dataStore = context.secureTokenDataStore
    private val cipher = AndroidTokenCipher

    override suspend fun readToken(): String? = withContext(Dispatchers.IO) {
        val encryptedToken = dataStore.data
            .map { preferences -> preferences[authTokenPreferenceKey] }
            .first()
            ?: return@withContext null

        try {
            cipher.decrypt(encryptedToken)
        } catch (e: Exception) {
            when (e) {
                is GeneralSecurityException,
                is IllegalArgumentException -> {
                    dataStore.edit { preferences -> preferences.remove(authTokenPreferenceKey) }
                    null
                }

                else -> throw e
            }
        }
    }

    override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            val encryptedToken = cipher.encrypt(token)
            dataStore.edit { preferences ->
                preferences[authTokenPreferenceKey] = encryptedToken
            }
        }
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences.remove(authTokenPreferenceKey)
            }
        }
    }
}

private object AndroidTokenCipher {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    fun encrypt(token: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedToken = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encryptedToken
        return Base64.encode(payload)
    }

    fun decrypt(encryptedToken: String): String {
        val payload = Base64.decode(encryptedToken)
        require(payload.size > GCM_IV_SIZE_BYTES) { "Encrypted token payload is too short." }

        val iv = payload.copyOfRange(0, GCM_IV_SIZE_BYTES)
        val cipherText = payload.copyOfRange(GCM_IV_SIZE_BYTES, payload.size)

        return Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        }.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { entry ->
            return entry.secretKey
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }
}
