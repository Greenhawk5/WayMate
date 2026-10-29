package ir.alifaniani.waymate.data.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Production [SessionStorage] backed by hardware-protected Android Keystore and AES-256 GCM.
 *
 * Tokens and user data are encrypted at rest using a key that never leaves the hardware
 * security module (TEE / StrongBox). If the key is altered or corrupted, the session is
 * safely purged without crashing.
 */
class KeystoreSessionStorage(
    context: Context,
    private val keyAlias: String = KEY_ALIAS_DEFAULT
) : SessionStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyExists()
    }

    override fun saveSession(token: String, user: User, expiresAt: Long) {
        try {
            val encryptedToken = encrypt(token)
            val userPayload = "${user.id}|${user.username}|${user.role.name}|${user.callsign}"
            val encryptedUser = encrypt(userPayload)

            prefs.edit()
                .putString(KEY_TOKEN_CIPHER, encryptedToken)
                .putString(KEY_USER_CIPHER, encryptedUser)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply()
        } catch (e: Exception) {
            clearSession()
        }
    }

    override fun getToken(): String? {
        val encrypted = prefs.getString(KEY_TOKEN_CIPHER, null) ?: return null
        return try {
            decrypt(encrypted)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    override fun getUser(): User? {
        val encrypted = prefs.getString(KEY_USER_CIPHER, null) ?: return null
        return try {
            val decrypted = decrypt(encrypted) ?: return null
            val parts = decrypted.split("|")
            if (parts.size >= 4) {
                val role = try {
                    UserRole.valueOf(parts[2])
                } catch (e: Exception) {
                    UserRole.TEAM_LEAD
                }
                User(
                    id = parts[0],
                    username = parts[1],
                    role = role,
                    callsign = parts[3]
                )
            } else {
                null
            }
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    override fun getExpiresAt(): Long {
        return prefs.getLong(KEY_EXPIRES_AT, 0L)
    }

    override fun isSessionValid(): Boolean {
        val token = getToken() ?: return false
        if (token.isBlank()) return false

        val expiresAt = getExpiresAt()
        if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            clearSession()
            return false
        }
        return true
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN_CIPHER)
            .remove(KEY_USER_CIPHER)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // In unit-test JVM or unsupported Keystore environments
        }
    }

    private fun getSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey
    }

    private fun encrypt(plainText: String): String {
        val secretKey = getSecretKey() ?: throw IllegalStateException("Keystore key not found")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String? {
        val secretKey = getSecretKey() ?: return null
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        if (combined.size <= GCM_IV_LENGTH_BYTES) return null

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val cipherText = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val PREFS_NAME = "waymate_secure_session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_DEFAULT = "waymate_auth_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val KEY_TOKEN_CIPHER = "enc_jwt_token"
        private const val KEY_USER_CIPHER = "enc_user_payload"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_ms"
    }
}
