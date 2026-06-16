package org.jetbrains.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREF_NODE = "org/jetbrains/realworld/auth"
private const val TOKEN_KEY = "auth_token"
private const val KEY_ALIAS = "realworld-auth-token"
private const val KEYSTORE_TYPE = "JCEKS"
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val IV_LENGTH_BYTES = 12

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage = remember { DesktopSecureTokenStorage() }

private class DesktopSecureTokenStorage : SecureTokenStorage {
    private val preferences: Preferences = Preferences.userRoot().node(PREF_NODE)
    private val secretKey: SecretKey by lazy { loadOrCreateSecretKey() }

    override suspend fun readToken(): String? {
        val encoded = preferences.get(TOKEN_KEY, null) ?: return null
        return decrypt(encoded)
    }

    override suspend fun saveToken(token: String) {
        preferences.put(TOKEN_KEY, encrypt(token))
        preferences.flush()
    }

    override suspend fun clearToken() {
        preferences.remove(TOKEN_KEY)
        preferences.flush()
    }

    private fun encrypt(token: String): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    private fun decrypt(encoded: String): String? = runCatching {
        val payload = Base64.getDecoder().decode(encoded)
        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.doFinal(ciphertext).decodeToString()
    }.getOrNull()

    private fun loadOrCreateSecretKey(): SecretKey {
        val keyStorePath = keyStorePath()
        Files.createDirectories(keyStorePath.parent)

        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        val password = keyStorePassword()
        if (Files.exists(keyStorePath)) {
            Files.newInputStream(keyStorePath).use { keyStore.load(it, password) }
        } else {
            keyStore.load(null, password)
        }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val generatedKey = keyGenerator.generateKey()
            keyStore.setEntry(
                KEY_ALIAS,
                KeyStore.SecretKeyEntry(generatedKey),
                KeyStore.PasswordProtection(password),
            )
            Files.newOutputStream(keyStorePath).use { keyStore.store(it, password) }
            makeOwnerReadableOnly(keyStorePath)
        }

        return keyStore.getKey(KEY_ALIAS, password) as SecretKey
    }

    private fun keyStorePath(): Path = Paths.get(
        System.getProperty("user.home"),
        ".realworld",
        "auth-token.jceks",
    )

    private fun keyStorePassword(): CharArray = "realworld-local-token-keystore".toCharArray()

    private fun makeOwnerReadableOnly(path: Path) {
        runCatching {
            val file = path.toFile()
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setReadable(true, true)
            file.setWritable(true, true)
        }
    }
}
