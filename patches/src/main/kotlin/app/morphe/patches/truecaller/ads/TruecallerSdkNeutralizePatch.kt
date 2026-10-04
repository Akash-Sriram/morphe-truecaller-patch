package app.morphe.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.CleverTapFactoryFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.MintegralInitFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.PresenceStreamFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.VungleInitFingerprint

private const val RETURN_VOID = "return-void"
private const val RETURN_FALSE = "const/4 v0, 0x0\nreturn v0"
private const val RETURN_TRUE = "const/4 v0, 0x1\nreturn v0"
private const val RETURN_NULL = "const/4 v0, 0x0\nreturn-object v0"

/**
 * Neutralizes 15 embedded third-party ad networks, profiling engines, and tracking SDKs.
 * Each entry is wrapped in an isolated try/catch so missing SDKs in certain builds fail-soft.
 *
 * Lineage: Joussflls10 / Paresh-Maheshwari / chirag127
 */
@Suppress("unused")
val truecallerSdkNeutralizePatch = bytecodePatch(
    name = "Truecaller — Neutralize Third-Party SDKs",
    description = "No-ops all embedded third-party ad networks, telemetry, and tracking SDK initializers (AdMob, Meta, Unity, Amazon, Criteo, AdRouter, Clarity, CleverTap, AppsFlyer, Moloco, HMS, Freshchat, InMobi, Appnext, Vungle, Fyber, Mintegral, IronSource, HyBid, Presence stream, MLKit).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Microsoft Clarity — return null
        try {
            mutableClassDefBy("Lcom/microsoft/clarity/Clarity;").methods
                .first { it.name == "initialize" && it.parameterTypes.size == 2 }
                .addInstructions(0, RETURN_NULL)
        } catch (_: Exception) { /* SDK absent */ }

        // 2. CleverTap — return null
        try {
            CleverTapFactoryFingerprint.method.addInstructions(0, RETURN_NULL)
        } catch (_: PatchException) { /* SDK absent */ }
        try {
            val cleverTapClass = mutableClassDefBy("Lcom/clevertap/android/sdk/CleverTapAPI;")
            for (method in cleverTapClass.methods) {
                if (method.name in listOf("getDefaultInstance", "createInstance") && method.returnType == "Lcom/clevertap/android/sdk/CleverTapAPI;") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_NULL)
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. AppsFlyer — return void
        try {
            mutableClassDefBy("Lcom/appsflyer/internal/AFa1ySDK;").methods
                .first { it.name == "start" && it.parameterTypes.size == 1 && it.parameterTypes[0] == "Landroid/content/Context;" }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* fail-soft */ }
        try {
            val appsFlyerClass = mutableClassDefBy("Lcom/appsflyer/AppsFlyerLib;")
            for (method in appsFlyerClass.methods) {
                if (method.name in listOf("start", "logEvent", "sendPushNotificationData")) {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Moloco — return void
        try {
            mutableClassDefBy("Lcom/moloco/sdk/publisher/Moloco;").methods
                .first { it.name == "initialize" && it.parameterTypes.size == 2 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 5. Huawei HMS InitProvider — return true
        try {
            mutableClassDefBy("Lcom/huawei/hms/aaid/InitProvider;").methods
                .first { it.name == "onCreate" && it.returnType == "Z" }
                .addInstructions(0, RETURN_TRUE)
        } catch (_: Exception) { /* SDK absent */ }

        // 6. Freshchat — return false
        try {
            mutableClassDefBy("Lcom/freshchat/consumer/sdk/Freshchat;").methods
                .first { it.name == "init" && it.returnType == "Z" }
                .addInstructions(0, RETURN_FALSE)
        } catch (_: Exception) { /* SDK absent */ }

        // 7. InMobi — return void
        try {
            mutableClassDefBy("Lcom/inmobi/sdk/InMobiSdk;").methods
                .first { it.name == "init" && it.parameterTypes.size == 4 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 8. Appnext — return void
        try {
            mutableClassDefBy("Lcom/appnext/nexdk/AppnextSDK;").methods
                .first { it.name == "initialize" && it.parameterTypes.size == 1 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 9. Vungle — return void
        try {
            VungleInitFingerprint.method.addInstructions(0, RETURN_VOID)
        } catch (_: PatchException) { /* SDK absent */ }

        // 10. Fyber — return void
        try {
            mutableClassDefBy("Lcom/fyber/inneractive/sdk/external/InneractiveAdManager;").methods
                .first { it.name == "initialize" && it.parameterTypes.size == 3 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 11. Mintegral — return void
        try {
            MintegralInitFingerprint.method.addInstructions(0, RETURN_VOID)
        } catch (_: PatchException) { /* SDK absent */ }

        // 12. IronSource — return void
        try {
            mutableClassDefBy("Lcom/ironsource/mediationsdk/IronSource;").methods
                .first { it.name == "init" && it.parameterTypes.size == 4 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 13. PubNative HyBid — return void
        try {
            mutableClassDefBy("Lnet/pubnative/lite/sdk/HyBid;").methods
                .first { it.name == "initialize" && it.parameterTypes.size == 3 }
                .addInstructions(0, RETURN_VOID)
        } catch (_: Exception) { /* SDK absent */ }

        // 14. Presence gRPC stream — return void
        try {
            PresenceStreamFingerprint.method.addInstructions(0, RETURN_VOID)
        } catch (_: PatchException) { /* SDK absent */ }

        // 15. Google ML Kit — return empty list
        try {
            mutableClassDefBy("Lcom/google/mlkit/common/internal/CommonComponentRegistrar;").methods
                .first { it.name == "getComponents" && it.returnType == "Ljava/util/List;" }
                .addInstructions(
                    0,
                    """
                        new-instance v0, Ljava/util/ArrayList;
                        invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                        return-object v0
                    """,
                )
        } catch (_: Exception) { /* SDK absent */ }

        // 16. Google Mobile Ads (AdMob) — return void
        try {
            val mobileAdsClass = mutableClassDefBy("Lcom/google/android/gms/ads/MobileAds;")
            for (method in mobileAdsClass.methods) {
                if (method.name == "initialize" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }

        // 17. Meta / Facebook Audience Network — return void
        try {
            val fbAdsClass = mutableClassDefBy("Lcom/facebook/ads/AudienceNetworkAds;")
            for (method in fbAdsClass.methods) {
                if (method.name == "initialize" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                } else if (method.name == "isInAdsProcess" && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_FALSE)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }

        // 18. Unity Ads & Unity Services — return void / false
        try {
            val unityAdsClass = mutableClassDefBy("Lcom/unity3d/ads/UnityAds;")
            for (method in unityAdsClass.methods) {
                if (method.name == "initialize" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                } else if (method.name in listOf("isInitialized", "isSupported") && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_FALSE)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }
        try {
            val unityServicesClass = mutableClassDefBy("Lcom/unity3d/services/UnityServices;")
            for (method in unityServicesClass.methods) {
                if (method.name == "initialize" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                } else if (method.name in listOf("isInitialized", "isSupported") && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_FALSE)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }
        for (adapterClassDef in listOf(
            "Lcom/google/ads/mediation/unity/UnityAdapter;",
            "Lcom/google/ads/mediation/unity/UnityMediationAdapter;",
        )) {
            try {
                val adapterClass = mutableClassDefBy(adapterClassDef)
                for (method in adapterClass.methods) {
                    if (method.name == "initialize" && method.returnType == "V") {
                        if (method.implementation != null) {
                            method.addInstructions(0, RETURN_VOID)
                        }
                    }
                }
            } catch (_: Exception) { /* SDK absent */ }
        }

        // 19. Amazon Ads (AdRegistration) — return null / false / void
        try {
            val amazonAdsClass = mutableClassDefBy("Lcom/amazon/device/ads/AdRegistration;")
            for (method in amazonAdsClass.methods) {
                if (method.name == "getInstance" && method.returnType == "Lcom/amazon/device/ads/AdRegistration;") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_NULL)
                    }
                } else if (method.name == "isInitialized" && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_FALSE)
                    }
                } else if (method.name in listOf("enableLogging", "enableTesting", "useGeoLocation") && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }

        // 20. Criteo Ads Adapter — return void
        try {
            val criteoClass = mutableClassDefBy("Lcom/criteo/mediation/google/CriteoAdapter;")
            for (method in criteoClass.methods) {
                if (method.name in listOf("requestBannerAd", "requestInterstitialAd", "requestNativeAd") && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }

        // 21. Truecaller AdRouter Mediation Adapter — return void
        try {
            val routerClass = mutableClassDefBy("Lcom/truecaller/ads/mediation/google/AdRouterMediationAdapter;")
            for (method in routerClass.methods) {
                if (method.name in listOf("initialize", "loadBannerAd", "loadInterstitialAd", "loadNativeAd") && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, RETURN_VOID)
                    }
                }
            }
        } catch (_: Exception) { /* SDK absent */ }
    }
}
