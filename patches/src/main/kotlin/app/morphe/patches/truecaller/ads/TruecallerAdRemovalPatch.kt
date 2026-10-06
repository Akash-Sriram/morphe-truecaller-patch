package app.morphe.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Permanently neutralizes all in-app advertising, ad downloading,
 * and ad presentation components:
 *  1. Central Feature Source of Truth (Lmyw; - PremiumFeatureManager):
 *     - Forces `b()Ljava/lang/Boolean;` (shouldShowAds) to always return `Boolean.FALSE`.
 *     - Forces `c(PremiumFeature, boolean)Z` to return `true` for `NO_ADS`.
 *  2. UI Ad Container Collapsing:
 *     - Forces `setVisibility(View.GONE)` across AdsContainer, AdsContainerLight,
 *       AdsSwitchView, MultiAdContainer, AcsMultiAdContainer, DetailsOverlayAdView,
 *       and AdRouterSuggestedAppsView.
 *     - Collapses onMeasure in AdsContainer to 0x0 dimensions.
 *  3. MultiAdContainer item count forced to 0.
 */
@Suppress("unused")
val truecallerAdRemovalPatch = bytecodePatch(
    name = "Truecaller — Remove Ads",
    description = "Neutralizes all internal and external ads, blocks ad downloading at the source of truth, and collapses ad UI containers.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Central Source of Truth: Lmyw; (PremiumFeatureManager)
        try {
            val mywClass = mutableClassDefBy("Lmyw;")
            for (method in mywClass.methods) {
                if (method.implementation == null) continue

                // b() -> Boolean (shouldShowAds)
                if (method.name == "b" && method.returnType == "Ljava/lang/Boolean;") {
                    method.addInstructions(
                        0,
                        """
                            sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                            return-object v0
                        """.trimIndent(),
                    )
                }

                // c(PremiumFeature, boolean) -> boolean (isFeatureAvailable)
                if (method.name == "c" && method.returnType == "Z" && method.parameterTypes.size == 2) {
                    method.addInstructions(
                        0,
                        """
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->NO_ADS:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-ne p1, v0, :cond_check_orig
                            const/4 v0, 0x1
                            return v0
                            :cond_check_orig
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Collapse AdsContainer onMeasure to 0x0 and set GONE
        try {
            val adsContainer = mutableClassDefBy("Lcom/truecaller/ads/ui/AdsContainer;")
            for (method in adsContainer.methods) {
                if (method.name == "onMeasure" && method.implementation != null) {
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            invoke-virtual {p0, v0, v0}, Landroid/view/View;->setMeasuredDimension(II)V
                            const/16 v0, 0x8
                            invoke-virtual {p0, v0}, Landroid/view/View;->setVisibility(I)V
                            return-void
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. MultiAdContainer: force getAdItemCount() and getItemCount() to 0
        try {
            val multiAdContainer = mutableClassDefBy("Lcom/truecaller/ads/mutliad/ui/container/MultiAdContainer;")
            for (method in multiAdContainer.methods) {
                if (method.name in listOf("getAdItemCount", "getItemCount") && method.returnType == "I" && method.implementation != null) {
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Force View.GONE on ad containers in onAttachedToWindow
        val adViewClasses = listOf(
            "Lcom/truecaller/ads/ui/AdsContainer;",
            "Lcom/truecaller/ads/ui/AdsContainerLight;",
            "Lcom/truecaller/ads/ui/AdsSwitchView;",
            "Lcom/truecaller/ads/adsrouter/ui/suggestedapps/AdRouterSuggestedAppsView;",
            "Lcom/truecaller/ads/mutliad/ui/container/MultiAdContainer;",
            "Lcom/truecaller/ads/mutliad/placement/acs/view/AcsMultiAdContainer;",
            "Lcom/truecaller/details_view/ui/ads/DetailsOverlayAdView;",
            "Lkf8;",
            "Lcom/truecaller/premium/ui/friendpromo/PremiumFriendUpgradedPromoView;"
        )

        for (clazzName in adViewClasses) {
            try {
                val clazz = mutableClassDefBy(clazzName)
                for (method in clazz.methods) {
                    if (method.name == "onAttachedToWindow" && method.implementation != null) {
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
            } catch (_: Exception) { /* fail-soft */ }
        }
    }
}
