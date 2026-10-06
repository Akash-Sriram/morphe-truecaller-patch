package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters

/**
 * Strips non-core bloat services and upsells from Users Home & Drawer menu:
 *  - Cyber & Fraud Insurance (HDFC ERGO)
 *  - Who viewed my profile & Who searched for me paywall traps
 *  - Contact requests social trap
 *  - Government Services Directory
 *  - Viral Referral & Invite Friends prompts
 *  - Smartwatch (Android Watch) companion link
 *  - Connect with us social redirects
 *  - Featured promotional cards / Premium upsell banners
 *
 * Keeps:
 *  - Your Truecaller stats (preserved via gcy.c / UserStats intact)
 *  - Settings, Backup, Change app theme, and Help.
 */
@Suppress("unused")
val truecallerDebloatProfileAndDrawerPatch = bytecodePatch(
    name = "Truecaller — Debloat Profile & Drawer Services",
    description = "Removes Fraud Insurance, Premium upsell banners, Government Services, Invite Friends, and Android Watch from profile menu while preserving stats.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Hook MenuItem.access$getOtherItems$cp() to return only:
        //    Group 0: SETTINGS, BACKUP, CHANGE_APP_THEME (indices 0, 1, 2)
        //    Group 1: HELP (index 2 of Group 1)
        //    Removing: ANDROID_WATCH, GOVERNMENT_SERVICES, INVITE_FRIENDS, CONNECT_WITH_US
        try {
            val menuItemClass = mutableClassDefBy("Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;")
            for (method in menuItemClass.methods) {
                if (method.name == "access${'$'}getOtherItems${'$'}cp" && method.returnType == "Ljava/util/List;") {
                    if (method.implementation != null) {
                        method.ensureRegisters(6)
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;->otherItems:Ljava/util/List;
                                const/4 v1, 0x0
                                invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
                                move-result-object v1
                                check-cast v1, Ljava/util/List;
                                const/4 v2, 0x0
                                const/4 v3, 0x3
                                invoke-interface {v1, v2, v3}, Ljava/util/List;->subList(II)Ljava/util/List;
                                move-result-object v1
                                const/4 v2, 0x1
                                invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;
                                move-result-object v2
                                check-cast v2, Ljava/util/List;
                                const/4 v3, 0x2
                                const/4 v4, 0x3
                                invoke-interface {v2, v3, v4}, Ljava/util/List;->subList(II)Ljava/util/List;
                                move-result-object v2
                                const/4 v0, 0x2
                                new-array v0, v0, [Ljava/util/List;
                                const/4 v3, 0x0
                                aput-object v1, v0, v3
                                const/4 v3, 0x1
                                aput-object v2, v0, v3
                                invoke-static {v0}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }

                // 2. Hook MenuItem.access$getPremiumItems$cp() to return an empty list
                //    Removing: WHO_VIEWED_MY_PROFILE, WHO_SEARCHED_FOR_ME, CONTACT_REQUESTS, FRAUD_INSURANCE
                if (method.name == "access${'$'}getPremiumItems${'$'}cp" && method.returnType == "Ljava/util/List;") {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Suppress featured promotional cards / Premium banners in UsersHome:
        //    Lg6y;.f(Lkcj;Lkotlin/jvm/functions/Function0;Lads;Lbzk;I)V
        //    Ldem;.m(Lkcj;Lkotlin/jvm/functions/Function0;Lads;Lbzk;I)V
        try {
            val g6yClass = mutableClassDefBy("Lg6y;")
            for (method in g6yClass.methods) {
                if (method.name == "f" && method.parameterTypes.size == 5 &&
                    method.parameterTypes[0] == "Lkcj;" && method.returnType == "V"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        try {
            val demClass = mutableClassDefBy("Ldem;")
            for (method in demClass.methods) {
                if (method.name == "m" && method.parameterTypes.size == 5 &&
                    method.parameterTypes[0] == "Lkcj;" && method.returnType == "V"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
