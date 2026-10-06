package app.morphe.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Suppresses promotional upsell banners, "upgrade to be invisible" incognito bottom sheets,
 * social proof upgrade cards, full-screen paywalls, and all full-screen ad/offerwall activities.
 */
@Suppress("unused")
val truecallerPromotionalNeutralizePatch = bytecodePatch(
    name = "Truecaller — Suppress Promotional Upsells & Paywalls",
    description = "Neutralizes promotional banners, incognito mode upsells, and paywall/ad activities across the app.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Incognito / Ghost Mode Promotional Bottom Sheet (cdn.a)
        try {
            val cdnClass = mutableClassDefBy("Lcdn;")
            for (method in cdnClass.methods) {
                if (method.name == "a" && method.parameterTypes.size == 5) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                if-eqz p5, :cond_null
                                invoke-interface {p5}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;
                                :cond_null
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Call Log Multi-Ad Promotional Banner View (kf8)
        try {
            val kf8Class = mutableClassDefBy("Lkf8;")
            for (method in kf8Class.methods) {
                if (method.name == "onAttachedToWindow") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/16 v0, 0x8
                                invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Friend Upgraded Social Proof Promotional View
        try {
            val friendPromoClass = mutableClassDefBy("Lcom/truecaller/premium/ui/friendpromo/PremiumFriendUpgradedPromoView;")
            for (method in friendPromoClass.methods) {
                if (method.name == "onAttachedToWindow") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/16 v0, 0x8
                                invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Full-Screen Ad Activities Immediate Finish
        val fullScreenActivitiesToFinish = listOf(
            "Lcom/truecaller/ads/adsrouter/ui/interstitial/AdRouterInterstitialActivity;",
            "Lcom/truecaller/ads/adsrouter/ui/offers/AdOffersActivity;",
            "Lcom/truecaller/ads/rewarded/WhoViewedMeAdActivity;",
            "Lcom/truecaller/ads/ui/CustomNativeVideoAdActivity;",
            "Lcom/truecaller/ads/postclickexperience/common/ui/PostClickExperienceActivity;",
            "Lcom/truecaller/ads/postclickexperience/type/htmlpage/HtmlPageActivity;",
            "Lcom/truecaller/ads/postclickexperience/type/nativevideo/NativeVideoActivity;"
        )

        for (paywallClassDef in fullScreenActivitiesToFinish) {
            try {
                val clazz = mutableClassDefBy(paywallClassDef)
                for (method in clazz.methods) {
                    if (method.name == "onResume") {
                        if (method.implementation != null) {
                            method.addInstructions(
                                0,
                                """
                                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                                """.trimIndent(),
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }
    }
}
