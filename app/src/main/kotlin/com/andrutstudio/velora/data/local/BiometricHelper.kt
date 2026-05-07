package com.andrutstudio.velora.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "biometric_vault_key"

    fun isBiometricEnrolled(): Boolean {
        return androidx.biometric.BiometricManager.from(context)
            .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun getOrCreateKey(): SecretKey {
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    fun encryptAndStorePassword(cipher: Cipher, password: String) {
        val encrypted = cipher.doFinal(password.toByteArray())
        val iv = cipher.iv

        prefs.edit()
            .putString("bio_enc_pwd", Base64.getEncoder().encodeToString(encrypted))
            .putString("bio_iv", Base64.getEncoder().encodeToString(iv))
            .apply()
    }

    fun clearStoredPassword() {
        prefs.edit().remove("bio_enc_pwd").remove("bio_iv").apply()
        keyStore.deleteEntry(keyAlias)
    }

    fun getDecryptCipher(): Cipher? {
        val encPwd = prefs.getString("bio_enc_pwd", null) ?: return null
        val iv = prefs.getString("bio_iv", null) ?: return null
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = keyStore.getKey(keyAlias, null) as? SecretKey ?: return null
        val spec = GCMParameterSpec(128, Base64.getDecoder().decode(iv))
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }

    fun decryptPassword(cipher: Cipher): String {
        val encPwd = prefs.getString("bio_enc_pwd", null) ?: return ""
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encPwd))
        return String(decryptedBytes)
    }

    fun showBiometricPromptForEncryption(
        activity: FragmentActivity,
        passwordToStore: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            getEncryptCipher()
        } catch (e: Exception) {
            onError("Failed to initialize biometric: ${e.message}")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric")
            .setSubtitle("Authenticate to securely store your password")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authenticatedCipher = result.cryptoObject?.cipher ?: run {
                    onError("Failed to get authenticated cipher")
                    return
                }
                try {
                    encryptAndStorePassword(authenticatedCipher, passwordToStore)
                    onSuccess()
                } catch (e: Exception) {
                    onError("Failed to store password: ${e.message}")
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        })

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun showBiometricPromptForDecryption(
        activity: FragmentActivity,
        title: String = "Unlock Wallet",
        subtitle: String = "Authenticate to proceed",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            getDecryptCipher() ?: run {
                onError("Biometric unlock not set up")
                return
            }
        } catch (e: Exception) {
            onError("Biometric session expired or changed")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val decryptedCipher = result.cryptoObject?.cipher ?: run {
                    onError("Failed to get decrypted cipher")
                    return
                }
                try {
                    val password = decryptPassword(decryptedCipher)
                    onSuccess(password)
                } catch (e: Exception) {
                    onError("Failed to decrypt: ${e.message}")
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        })

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
