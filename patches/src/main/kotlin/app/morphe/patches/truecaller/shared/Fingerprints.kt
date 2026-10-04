package app.morphe.patches.truecaller.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object Fingerprints {

    // ============================================================================
    // Layout & Navigation Fingerprints
    // ============================================================================

    // AI Assistant Tab
    val AssistantFeatureFingerprint = Fingerprint(
        name = "b",
        returnType = "Z",
        parameters = listOf(),
        strings = listOf("featuresAssistantCheck", "assistantButton"),
    )

    // Family Protection Button
    val FamilyProtectFeatureFingerprint = Fingerprint(
        name = "b",
        returnType = "Z",
        parameters = listOf(),
        strings = listOf("familyProtectCheck", "familyProtectionButton"),
    )

    // Scams Tab
    val ScamFeedEnabledFingerprint = Fingerprint(
        name = "b",
        returnType = "Z",
        parameters = listOf(),
        strings = listOf("scamFeedCheck", "scamFeedButton"),
    )

    // Premium Purchase Supported / Bottom bar tab
    val PremiumPurchaseSupportedFingerprint = Fingerprint(
        name = "<init>",
        returnType = "V",
        strings = listOf("premiumPurchaseSupportedCheck", "premiumButton", "callsButton"),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IGET_OBJECT,
                definingClass = "Lqn2/a;",
                name = "d",
            ),
        ),
    )

    // Settings category item: Lfh2/m0; CategoriesSettingsState
    val CategoriesSettingsStateFingerprint = Fingerprint(
        returnType = "Ljava/lang/String;",
        filters = listOf(
            string("CategoriesSettingsState(isPremiumItemVisible="),
        ),
    )

    // Settings screen premium row visibility
    val SettingsPremiumVisibilityFingerprint = Fingerprint(
        definingClass = "Lgh2/d;",
        name = "invoke",
        returnType = "V",
    )

    // Jetpack Compose "Premium member" section on profile/user details
    val PremiumBlockComposeFingerprint = Fingerprint(
        returnType = "V",
        parameters = listOf(
            "Lcom/truecaller/usershome/presentaion/ui/components/featureditemsection/baz;",
            "Lkotlin/jvm/functions/Function0;",
            "Landroidx/compose/ui/b;",
            "Ll2/k;",
            "I",
        ),
        filters = listOf(
            string("premium_block"),
        ),
    )

    // ============================================================================
    // Core In-App Update Fingerprints
    // ============================================================================

    val InAppUpdateManagerClassFingerprint = Fingerprint(
        strings = listOf("playAppUpdateManager", "configsInventory"),
    )

    val ShouldTriggerUpdateFingerprint = Fingerprint(
        classFingerprint = InAppUpdateManagerClassFingerprint,
        returnType = "Z",
        parameters = listOf("Lcom/truecaller/inappupdate/UpdateTrigger;"),
    )

    // ============================================================================
    // Telemetry & First-Party Tracking Fingerprints
    // ============================================================================

    val AppStartTrackerFingerprint = Fingerprint(
        definingClass = "Lcom/truecaller/analytics/technical/AppStartTracker;",
        name = "enableTracking",
        returnType = "V",
    )

    // Broad AppStartTracker class reference (for version-drift resilience)
    val AppStartTrackerClassFingerprint = Fingerprint(
        definingClass = "Lcom/truecaller/analytics/technical/AppStartTracker;",
    )

    // ============================================================================
    // GMS Sign-In Bypass Fingerprint
    // ============================================================================

    val OtpSmsApiSelectionFingerprint = Fingerprint(
        returnType = "Ljava/lang/Object;",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        parameters = listOf(),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                definingClass = "Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;",
                name = "GOOGLE",
            ),
            fieldAccess(
                opcode = Opcode.SGET_OBJECT,
                definingClass = "Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;",
                name = "SMS",
            ),
        ),
    )

    // ============================================================================
    // Third-Party SDK & Analytics Fingerprints
    // ============================================================================

    val CleverTapFactoryFingerprint = Fingerprint(
        returnType = "L",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
        parameters = listOf(
            "Landroid/content/Context;",
            "Lcom/clevertap/android/sdk/CleverTapInstanceConfig;",
            "L",
        ),
    )

    val CleverTapPushEventFingerprint = Fingerprint(
        definingClass = "Lkr0/k;",
        name = "push",
        returnType = "V",
        parameters = listOf("Ljava/lang/String;"),
    )

    val CleverTapPushEventWithPropsFingerprint = Fingerprint(
        definingClass = "Lkr0/k;",
        name = "push",
        returnType = "V",
        parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
    )

    val VungleInitFingerprint = Fingerprint(
        returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
        parameters = listOf(
            "Landroid/content/Context;",
            "Ljava/lang/String;",
            "L",
        ),
        custom = { _, classDef -> classDef.type.startsWith("Lcom/vungle/ads/") },
    )

    val MintegralInitFingerprint = Fingerprint(
        returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC),
        parameters = listOf("Ljava/util/Map;", "Landroid/content/Context;"),
        custom = { _, classDef -> classDef.interfaces.contains("Lcom/mbridge/msdk/MBridgeSDK;") },
    )

    val PresenceStreamFingerprint = Fingerprint(
        returnType = "V",
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        parameters = listOf(),
        custom = { method, classDef ->
            classDef.interfaces.contains("Lcom/truecaller/presence/PresenceManager;") &&
                method.implementation != null &&
                method.implementation!!.registerCount >= 4
        },
    )

    // ============================================================================
    // Premium & Gold Tier Fingerprints
    // ============================================================================

    val IsPremiumFingerprint = Fingerprint(
        returnType = "Z",
        parameters = listOf(),
        filters = listOf(string("isPremiumExpired")),
    )

    val GetPremiumTierFingerprint = Fingerprint(
        returnType = "Lcom/truecaller/premium/data/tier/PremiumTierType;",
        parameters = listOf(),
        filters = listOf(string("premiumLevel")),
    )

    val PremiumFeatureManagerClassFingerprint = Fingerprint(
        returnType = "V",
        strings = listOf("premiumFeaturesInventory", "qaPremiumFeatureHelper"),
    )

    val IsFeatureAvailableFingerprint = Fingerprint(
        classFingerprint = PremiumFeatureManagerClassFingerprint,
        returnType = "Z",
        parameters = listOf("Lcom/truecaller/premium/data/feature/PremiumFeature;", "Z"),
    )

    val SavePremiumStateFingerprint = Fingerprint(
        returnType = "Ljava/lang/Object;",
        parameters = listOf("Lzz1/z;", "Ll33/a;"),
        filters = listOf(
            methodCall(definingClass = "Lzz1/o1;", name = "X"),
            methodCall(definingClass = "Lzz1/o1;", name = "i"),
        ),
    )

    val SubscriptionNotificationWorkerFingerprint = Fingerprint(
        definingClass = "Lcom/truecaller/premium/PremiumSubscriptionStatusNotificationWorker;",
        name = "doWork",
        returnType = "Ljava/lang/Object;",
    )

    val AttributesDTOToStringFingerprint = Fingerprint(
        returnType = "Ljava/lang/String;",
        filters = listOf(string("AttributesDTO(isPremium=")),
    )

    val PremiumStateConstructorFingerprint = Fingerprint(
        name = "<init>",
        returnType = "V",
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IPUT_OBJECT,
                type = "Lcom/truecaller/premium/data/tier/PremiumTierType;",
            ),
        ),
    )

    // ============================================================================
    // In-App Ads & Paywall Fingerprints
    // ============================================================================

    val AfterCallMaybeUpdateAdFingerprint = Fingerprint(
        definingClass = "Lcom/truecaller/acs/ui/baz;",
        name = "Rh",
        returnType = "V",
        parameters = listOf("Z"),
    )

    val NeoAcsMaybeUpdateAdFingerprint = Fingerprint(
        definingClass = "Ltw1/f;",
        name = "Th",
        returnType = "V",
        parameters = listOf("Z"),
    )

    val ShouldShowAdsPrefsFingerprint = Fingerprint(
        returnType = "Z",
        parameters = listOf(),
        filters = listOf(string("shouldShowAds")),
    )

    val FullScreenPaywallOnCreateFingerprint = Fingerprint(
        returnType = "V",
        name = "onCreate",
        parameters = listOf("Landroid/os/Bundle;"),
        custom = { _, classDef ->
            classDef.type.contains("FullScreenPaywall") || classDef.type.contains("PaywallActivity")
        },
    )

    val PremiumNavDrawerItemViewFingerprint = Fingerprint(
        name = "onAttachedToWindow",
        returnType = "V",
        parameters = listOf(),
        custom = { _, classDef -> classDef.type.contains("PremiumNavDrawerItemView") },
    )

    val AdsBazFingerprint = Fingerprint(
        definingClass = "Loi1/baz;",
    )
}
