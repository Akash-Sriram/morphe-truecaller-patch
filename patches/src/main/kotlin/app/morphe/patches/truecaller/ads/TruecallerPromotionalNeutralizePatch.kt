package app.morphe.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Suppresses promotional upsell banners, "upgrade to be invisible" incognito bottom sheets,
 * social proof upgrade cards, and full-screen paywalls.
 */
@Suppress("unused")
val truecallerPromotionalNeutralizePatch = bytecodePatch(
    name = "Truecaller — Suppress Promotional Upsells & Paywalls",
    description = "Neutralizes promotional banners, incognito mode upsells, and paywall prompts across the dialer and contact views.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Incognito / Ghost Mode Promotional Bottom Sheet (cdn.a)
        // Suppress the promotional upsell bottom sheet (Ladn;) while executing
        // the navigation lambda callback (p5) so search results expand to full profile
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

        // 4. Paywall Screen Immediate Finish (FullScreenPaywallActivity, UpgradePathPaywallActivity, NavDrawerPaywallActivity)
        for (paywallClassDef in listOf(
            "Lcom/truecaller/premium/FullScreenPaywallActivity;",
            "Lcom/truecaller/premium/UpgradePathPaywallActivity;",
            "Lcom/truecaller/premium/NavDrawerPaywallActivity;",
        )) {
            try {
                val clazz = mutableClassDefBy(paywallClassDef)
                for (method in clazz.methods) {
                    if (method.name == "onResume") {
                        if (method.implementation != null) {
                            method.addInstructions(
                                0,
                                """
                                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                                    return-void
                                """.trimIndent(),
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }
    }
}
