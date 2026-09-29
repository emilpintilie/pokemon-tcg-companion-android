package es.upm.pokemonapp.config

import android.os.Build

/**
 * Configuration helper for LM Studio integration
 *
 * The app will automatically switch to the appropriate host when running on the
 * Android emulator (uses 10.0.2.2). For a physical device, replace the
 * LOCALHOST_URL value with your PC LAN IP (for example: http://192.168.1.42:1234/)
 */
object LMStudioConfig {

    // LM Studio addresses
    private const val LOCALHOST_URL = "http://localhost:1234/"            // when running on the PC
    private const val EMULATOR_HOST_URL = "http://10.0.2.2:1234/"         // Android emulator -> host machine
    private const val GENYMOTION_HOST_URL = "http://10.0.3.2:1234/"       // Genymotion

    // Public-facing default value (kept for compatibility)
    @Deprecated("Use getBaseUrl() instead which auto-detects emulator vs device")
    val LM_STUDIO_URL = getBaseUrl()

    // Model name to use
    const val MODEL_NAME = "gemma-4-e4b"

    // Request timeout in milliseconds
    const val REQUEST_TIMEOUT = 120000L // 2 minutes

    // Temperature parameter (0.0 = deterministic, 1.0 = creative)
    const val TEMPERATURE = 0.7

    // Maximum tokens for each response
    const val MAX_TOKENS_CARD_RECOMMENDATIONS = 1500
    const val MAX_TOKENS_DECK_RECOMMENDATIONS = 2000
    const val MAX_TOKENS_STRATEGY_ADVICE = 1500

    // Enable/disable LM Studio feature
    const val ENABLE_LM_STUDIO = true

    // Runtime override: if set (non-blank), this value will be used as the base URL
    // Allows changing the LM Studio server at runtime (e.g. enter 10.0.2.2:1234 from emulator)
    @JvmStatic
    var overrideUrl: String? = null

    /**
     * Returns the effective base URL to contact the LM Studio instance from the app.
     * - Emulator: 10.0.2.2 -> host machine
     * - Genymotion: 10.0.3.2
     * - Physical device: use LOCALHOST_URL replaced by your PC IP (edit constant above or
     *   change through runtime settings if you add that UI)
     */
    fun getBaseUrl(): String {
        // If runtime override provided, use it
        overrideUrl?.let { v ->
            if (v.isNotBlank()) return if (v.endsWith("/")) v else "$v/"
        }

        return when {
            isGenymotion() -> GENYMOTION_HOST_URL
            isProbablyEmulator() -> EMULATOR_HOST_URL
            else -> LOCALHOST_URL
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))
    }

    private fun isGenymotion(): Boolean {
        return Build.MANUFACTURER.contains("Genymotion") || Build.BRAND.contains("genymotion", true)
    }

}
