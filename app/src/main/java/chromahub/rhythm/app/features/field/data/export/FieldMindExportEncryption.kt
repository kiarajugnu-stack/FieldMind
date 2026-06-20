package fieldmind.research.app.features.field.data.export

import android.util.Base64
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption for .fieldmind backup packages.
 *
 * Uses PBKDF2-HMAC-SHA256 key derivation (100k iterations) so the user's
 * password is stretched into a strong 256-bit AES key before encryption.
 *
 * Encrypted format (stored as bytes):
 *   [salt: 16 bytes][iv: 12 bytes][ciphertext: variable][tag: 16 bytes]
 *
 * Decryption reverses the process: read salt + iv, derive key, decrypt.
 */
object FieldMindExportEncryption {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 100_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12  // GCM standard nonce length
    private const val TAG_LENGTH = 128  // bits

    /**
     * Encrypt a file with the given password.
     * Returns the encrypted file (same path as input, .encrypted extension added).
     *
     * @param inputFile The .fieldmind package to encrypt
     * @param password  User-provided password
     * @return Encrypted file
     * @throws IllegalArgumentException if password is blank
     */
    fun encryptFile(inputFile: File, password: String): File {
        require(password.isNotBlank()) { "Password cannot be blank" }

        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(TAG_LENGTH, iv))

        val inputBytes = inputFile.readBytes()
        val ciphertext = cipher.doFinal(inputBytes)

        val outputFile = File(inputFile.parentFile, "${inputFile.nameWithoutExtension}.encrypted")

        // Pack: [salt][iv][ciphertext with tag]
        outputFile.writeBytes(salt + iv + ciphertext)

        return outputFile
    }

    /**
     * Decrypt a file with the given password.
     * Returns the decrypted file content as a byte array.
     *
     * @param encryptedFile The .encrypted file to decrypt
     * @param password      User-provided password
     * @return Decrypted file content bytes
     * @throws IllegalArgumentException if password is blank or decryption fails
     */
    fun decryptToBytes(encryptedFile: File, password: String): ByteArray {
        require(password.isNotBlank()) { "Password cannot be blank" }

        val data = encryptedFile.readBytes()
        require(data.size > SALT_LENGTH + IV_LENGTH) { "File is too small to be a valid encrypted archive" }

        val salt = data.copyOfRange(0, SALT_LENGTH)
        val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)

        val key = deriveKey(password, salt)

        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(TAG_LENGTH, iv))
            return cipher.doFinal(ciphertext)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw IllegalArgumentException("Incorrect password or corrupted file", e)
        } catch (e: Exception) {
            throw IllegalArgumentException("Decryption failed: ${e.localizedMessage}", e)
        }
    }

    /**
     * Decrypt an encrypted file and write it to a temp .fieldmind file.
     *
     * @param encryptedFile The .encrypted file to decrypt
     * @param password      User-provided password
     * @param outputDir     Directory to write the decrypted file to
     * @return Decrypted .fieldmind file
     */
    fun decryptToFile(encryptedFile: File, password: String, outputDir: File): File {
        val decryptedBytes = decryptToBytes(encryptedFile, password)
        val outputFile = File(outputDir, encryptedFile.nameWithoutExtension + ".fieldmind")
        outputFile.writeBytes(decryptedBytes)
        return outputFile
    }

    /**
     * Check if a file appears to be an encrypted archive (has .encrypted extension).
     */
    fun isEncryptedFile(fileName: String): Boolean =
        fileName.endsWith(".encrypted")

    // ── Password strength meter ──

    data class PasswordStrength(
        val score: Int,          // 0-4
        val label: String,       // "Weak", "Fair", "Good", "Strong", "Very Strong"
        val color: Long          // ARGB color integer
    ) {
        companion object {
            fun evaluate(password: String): PasswordStrength {
                val score = calculateScore(password)
                return when {
                    score < 2 -> PasswordStrength(score, "Weak", 0xFFE53935)
                    score < 3 -> PasswordStrength(score, "Fair", 0xFFFB8C00)
                    score < 4 -> PasswordStrength(score, "Good", 0xFF43A047)
                    score < 5 -> PasswordStrength(score, "Strong", 0xFF2E7D32)
                    else -> PasswordStrength(score, "Very Strong", 0xFF1B5E20)
                }
            }

            private fun calculateScore(password: String): Int {
                if (password.length < 4) return 0
                var score = 0
                if (password.length >= 8) score++
                if (password.length >= 12) score++
                if (password.any { it.isUpperCase() }) score++
                if (password.any { it.isLowerCase() }) score++
                if (password.any { it.isDigit() }) score++
                if (password.any { !it.isLetterOrDigit() }) score++
                return score.coerceIn(0, 5)
            }
        }
    }

    // ── Key derivation ──

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        return factory.generateSecret(spec).encoded
    }
}
