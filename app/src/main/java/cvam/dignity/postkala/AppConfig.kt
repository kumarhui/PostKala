package cvam.dignity.postkala

/**
 * PRODUCTION SECURITY: Ensures test ads are NEVER shown when IS_TEST_MODE is false.
 */
object AdsConfig {
    // SET TO FALSE FOR PLAY STORE RELEASE
    const val IS_TEST_MODE = false

    // Official Google Test IDs (Safe for development)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    // YOUR REAL PRODUCTION IDs (Paste them here)
    private const val PROD_BANNER = "YOUR_REAL_BANNER_UNIT_ID"
    private const val PROD_INTERSTITIAL = "YOUR_REAL_INTERSTITIAL_UNIT_ID"

    val bannerId: String
        get() = if (IS_TEST_MODE) TEST_BANNER else PROD_BANNER

    val interstitialId: String
        get() = if (IS_TEST_MODE) TEST_INTERSTITIAL else PROD_INTERSTITIAL
}