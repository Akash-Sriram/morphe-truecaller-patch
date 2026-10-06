package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Unlocks Max Protection, Unlimited Spam List Updates, and Neighbor Spoofing & Number Series Prefix Blocking:
 * 1. PremiumFeature checker (Lmyw;, Lnpw;):
 *    - Unlocks EXTENDED_SPAM_BLOCKING, entitling the app to the Max Protection spam filtering tier
 *      and direct offline spam list database downloads without paywalls.
 * 2. Block Settings Presenter (Lfe6;):
 *    - Hooks fe6.l() to return true, signaling active extended protection to the block settings UI.
 * 3. Feature Toggle Manager (Ledo;):
 *    - Forces "featureNumberSeriesBlocking" -> true (enables wildcard/prefix series blocking engine).
 *    - Forces "featureBlockNeighbourSpoofingAsPremium" -> false (exposes neighbor spoofing as standard configurable protection).
 * 4. Block Settings Preferences (Lhrj;):
 *    - Defaults autoUpdateTopSpammers to Boolean.TRUE when unset.
 *    - Defaults neighbor spoofing matching digits to 6 when unset.
 */
@Suppress("unused")
val truecallerMaxSpamProtectionPatch = bytecodePatch(
    name = "Truecaller — Max Protection & Advanced Spam Blocking",
    description = "Enables Max Protection spam tier, unlimited offline spam list updates, Neighbor Spoofing detection, and Number Series Prefix Blocking.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Hook PremiumFeature checker in Lmyw; for EXTENDED_SPAM_BLOCKING, SPAM_BLOCKING & NO_ADS
        try {
            val mywClass = mutableClassDefBy("Lmyw;")
            for (method in mywClass.methods) {
                if (method.implementation == null) continue

                if (method.name == "c" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Lcom/truecaller/premium/data/feature/PremiumFeature;" &&
                    method.returnType == "Z"
                ) {
                    method.addInstructions(
                        0,
                        """
                            if-nez p1, :cond_c_pass
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->EXTENDED_SPAM_BLOCKING:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_c_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->SPAM_BLOCKING:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_c_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->NO_ADS:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_c_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->INCOGNITO_MODE:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_c_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->WHO_SEARCHED_FOR_ME:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_c_is_premium
                            goto :cond_c_pass
                            :cond_c_is_premium
                            const/4 v0, 0x1
                            return v0
                            :cond_c_pass
                            nop
                        """.trimIndent(),
                    )
                } else if ((method.name == "d" || method.name == "e") && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == "Lcom/truecaller/premium/data/feature/PremiumFeature;" &&
                    method.returnType == "Z"
                ) {
                    method.addInstructions(
                        0,
                        """
                            if-nez p1, :cond_de_pass
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->EXTENDED_SPAM_BLOCKING:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_de_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->SPAM_BLOCKING:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_de_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->NO_ADS:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_de_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->INCOGNITO_MODE:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_de_is_premium
                            sget-object v0, Lcom/truecaller/premium/data/feature/PremiumFeature;->WHO_SEARCHED_FOR_ME:Lcom/truecaller/premium/data/feature/PremiumFeature;
                            if-eq p1, v0, :cond_de_is_premium
                            goto :cond_de_pass
                            :cond_de_is_premium
                            const/4 v0, 0x1
                            return v0
                            :cond_de_pass
                            nop
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

// 2. Note: Lnpw;->t delegates directly to Lmyw;->c (no separate hook needed)

        // 3. Hook Block Settings Presenter Lfe6;->l()Z and Lfe6;->m()Z
        try {
            val fe6Class = mutableClassDefBy("Lfe6;")
            for (method in fe6Class.methods) {
                if ((method.name == "l" || method.name == "m") && method.parameterTypes.isEmpty() && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x1
                                return v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Hook Premium State Settings La6x;->I()Z (active premium status)
        try {
            val a6xClass = mutableClassDefBy("La6x;")
            for (method in a6xClass.methods) {
                if (method.name == "I" && method.parameterTypes.isEmpty() && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x1
                                return v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 5. Hook Number Series Database Entity List;-><init> (force is_premium = false)
        try {
            val listClass = mutableClassDefBy("List;")
            for (method in listClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 7 &&
                    method.parameterTypes[3] == "Z" && method.parameterTypes[6] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p4, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 6. Hook Number Series Domain Model Lzrt;-><init> (force isPremium = false)
        try {
            val zrtClass = mutableClassDefBy("Lzrt;")
            for (method in zrtClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 7 &&
                    method.parameterTypes[3] == "Z" && method.parameterTypes[6] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p4, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 7. Hook Number Series UI State Lzst;-><init> (force isVisible = true)
        try {
            val zstClass = mutableClassDefBy("Lzst;")
            for (method in zstClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[1] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p2, 0x1
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 8. Hook Spam List Outdated Checker Lzpz;->n()Z (always return false = up to date!)
        try {
            val zpzClass = mutableClassDefBy("Lzpz;")
            for (method in zpzClass.methods) {
                if (method.name == "n" && method.parameterTypes.isEmpty() && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x0
                                return v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 9. Hook Feature Toggle Manager Ledo;->a(Ljava/lang/String;Lcom/truecaller/featuretoggles/FeatureState;)Z
        try {
            val edoClass = mutableClassDefBy("Ledo;")
            for (method in edoClass.methods) {
                if (method.name == "a" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Ljava/lang/String;" &&
                    method.returnType == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_toggle_pass
                                const-string v0, "featureNumberSeriesBlocking"
                                invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                                move-result v0
                                if-eqz v0, :cond_check_spoof
                                const/4 v0, 0x1
                                return v0
                                :cond_check_spoof
                                const-string v0, "featureBlockNeighbourSpoofingAsPremium"
                                invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                                move-result v0
                                if-eqz v0, :cond_toggle_pass
                                const/4 v0, 0x0
                                return v0
                                :cond_toggle_pass
                                nop
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 10. Hook Block Settings Preferences Lhrj;
        try {
            val hrjClass = mutableClassDefBy("Lhrj;")
            for (method in hrjClass.methods) {
                if (method.implementation == null) continue

                // Auto update top spammers default: Boolean.TRUE if unset
                if (method.name == "b" && method.parameterTypes.isEmpty() && method.returnType == "Ljava/lang/Boolean;") {
                    method.addInstructions(
                        0,
                        """
                            iget-object v0, p0, Lhrj;->a:Landroid/content/SharedPreferences;
                            const-string v1, "filter_autoUpdateTopSpammers"
                            invoke-interface {v0, v1}, Landroid/content/SharedPreferences;->contains(Ljava/lang/String;)Z
                            move-result v0
                            if-nez v0, :cond_has_top_spammers
                            sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                            return-object v0
                            :cond_has_top_spammers
                        """.trimIndent(),
                    )
                }

                // Matching digits default: 6 if unset
                if (method.name == "a" && method.parameterTypes.isEmpty() && method.returnType == "Ljava/lang/Integer;") {
                    method.addInstructions(
                        0,
                        """
                            iget-object v0, p0, Lhrj;->a:Landroid/content/SharedPreferences;
                            const-string v1, "filter_filteringNeighbourSpoofingMatchingDigits"
                            invoke-interface {v0, v1}, Landroid/content/SharedPreferences;->contains(Ljava/lang/String;)Z
                            move-result v0
                            if-nez v0, :cond_has_matching_digits
                            const/4 v0, 0x6
                            invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                            move-result-object v0
                            return-object v0
                            :cond_has_matching_digits
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
