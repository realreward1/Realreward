package com.example.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.firebase.auth.FirebaseUser
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

object AdminSecurity {
    private const val OWNER_EMAIL = "wahidzroman@gmail.com"
    private const val SALT = "rr_bd_owner_salt_v1:"
    private const val EXPECTED_DIGEST_HEX =
        "ea36829b87154c3f550b4e3d97a0c2d4d0e3b4834265125055db638ac60cc307"

    private const val CORE_SALT = "rr_bd_core_salt_v1:"
    private const val EXPECTED_CORE_DIGEST_HEX =
        "2f7a28f261eb39b3501c456a7f2723586656be576d2a63c441ebc7bb59538ac3"

    fun isVerifiedOwner(user: FirebaseUser?): Boolean {
        return user != null &&
            user.email.equals(OWNER_EMAIL, ignoreCase = true) &&
            user.isEmailVerified
    }

    fun verifyAdminPasscode(input: String): Boolean {
        val candidate = SALT + input.trim()
        val bytes = MessageDigest.getInstance("SHA-256").digest(candidate.toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(
            hex.toByteArray(Charsets.UTF_8),
            EXPECTED_DIGEST_HEX.toByteArray(Charsets.UTF_8)
        )
    }

    fun verifyCoreSettingsPassword(input: String): Boolean {
        val candidate = CORE_SALT + input.trim()
        val bytes = MessageDigest.getInstance("SHA-256").digest(candidate.toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(
            hex.toByteArray(Charsets.UTF_8),
            EXPECTED_CORE_DIGEST_HEX.toByteArray(Charsets.UTF_8)
        )
    }

    fun hashUserPassword(uid: String, rawPassword: String): String {
        val payload = "rr_user_pw_v1:$uid:${rawPassword.trim()}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyUserPassword(uid: String, rawPassword: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val expected = hashUserPassword(uid, rawPassword)
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    @SuppressLint("HardwareIds")
    fun getDeviceRegistryId(context: Context): String {
        val raw = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val sanitized = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return if (sanitized.length >= 4) "dev_${sanitized.take(96)}" else "dev_android_fallback"
    }

    fun sanitizeEmailKey(email: String): String {
        return email.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(120)
    }

    fun generateReferralCode(uid: String): String {
        val suffix = uid.filter { it.isLetterOrDigit() }.uppercase().take(4).ifBlank { "BD" }
        val num = (1000..9999).random()
        return "RR$suffix$num"
    }

    fun normalizeBdMobile(input: String): String {
        return input.trim().removePrefix("+88").removePrefix("88").replace(" ", "")
    }

    fun isValidBdMobile(input: String): Boolean {
        return Regex("^01[3-9]\\d{8}$").matches(normalizeBdMobile(input))
    }

    fun calculateWithdrawalFeeBdt(amountBdt: Double): Double {
        val clean = kotlin.math.round(amountBdt * 100.0) / 100.0
        if (clean <= 0.0) return 0.0
        if (clean <= 800.0) return 10.0
        if (clean <= 1000.0) return 20.0
        val fullThousands = kotlin.math.floor(clean / 1000.0).toLong()
        val remainder = clean - (fullThousands * 1000.0)
        val extra = when {
            remainder <= 0.0001 -> 0.0
            remainder <= 300.0 -> 5.0
            remainder <= 800.0 -> 10.0
            else -> 20.0
        }
        return (fullThousands * 20.0) + extra
    }

    fun vipBonusPercentForTier(vipTierRaw: String): Int {
        return when (vipTierRaw.trim().uppercase()) {
            "BRONZE" -> 2
            "SILVER" -> 4
            "GOLD" -> 7
            "DIAMOND" -> 10
            else -> 0
        }
    }

    fun calculateTaskRewardWithVip(baseRewardBdt: Double, vipTierRaw: String): Double {
        if (baseRewardBdt <= 0.0) return 0.0
        val bonusPct = vipBonusPercentForTier(vipTierRaw)
        if (bonusPct <= 0) return kotlin.math.round(baseRewardBdt * 100.0) / 100.0
        return kotlin.math.round((baseRewardBdt * (1.0 + (bonusPct / 100.0))) * 100.0) / 100.0
    }

    fun formatBdt(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "৳${formatter.format(amount)}"
    }
}
