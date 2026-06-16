package org.jetbrains.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val KEYCHAIN_SERVICE = "org.jetbrains.realworld.auth"
private const val TOKEN_ACCOUNT = "auth-token"

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage = remember { IosKeychainSecureTokenStorage() }

@OptIn(ExperimentalForeignApi::class)
private class IosKeychainSecureTokenStorage : SecureTokenStorage {
    override suspend fun readToken(): String? = memScoped {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to TOKEN_ACCOUNT,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
        if (status == errSecSuccess) {
            val data = result.value as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        } else {
            null
        }
    }

    override suspend fun saveToken(token: String) {
        deleteExistingToken()
        val tokenBytes = token.encodeToByteArray()
        val tokenData = tokenBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = tokenBytes.size.toULong())
        }
        val attributes = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to TOKEN_ACCOUNT,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecValueData to tokenData,
        )
        val status = SecItemAdd(attributes as CFDictionaryRef, null)
        check(status == errSecSuccess) { "Unable to save token in Keychain (status=$status)" }
    }

    override suspend fun clearToken() {
        deleteExistingToken()
    }

    private fun deleteExistingToken() {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KEYCHAIN_SERVICE,
            kSecAttrAccount to TOKEN_ACCOUNT,
        )
        val status = SecItemDelete(query as CFDictionaryRef)
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Unable to delete token from Keychain (status=$status)"
        }
    }
}
