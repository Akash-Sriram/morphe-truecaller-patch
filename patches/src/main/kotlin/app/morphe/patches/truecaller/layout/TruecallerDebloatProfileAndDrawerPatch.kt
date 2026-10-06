package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Strips non-core bloat services from Users Home & Drawer menu:
 *  - Cyber & Fraud Insurance (HDFC ERGO)
 *  - Government Services Directory
 *  - Viral Referral & Invite Friends prompts
 *  - Smartwatch (Android Watch) companion link
 */
@Suppress("unused")
val truecallerDebloatProfileAndDrawerPatch = bytecodePatch(
    name = "Truecaller — Debloat Profile & Drawer Services",
    description = "Removes Fraud Insurance, Government Services, Invite Friends, and Android Watch from profile menu.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Hook MenuItem.access$getOtherItems$cp() to return only Group 0
        // (SETTINGS, CHANGE_APP_THEME, HELP, CONNECT_WITH_US)
        // discarding Group 1 (GOVERNMENT_SERVICES, INVITE_FRIENDS, ANDROID_WATCH)
        try {
            val menuItemClass = mutableClassDefBy("Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;")
            for (method in menuItemClass.methods) {
                if (method.name == "access${'$'}getOtherItems${'$'}cp" && method.returnType == "Ljava/util/List;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;->otherItems:Ljava/util/List;
                                const/4 v1, 0x0
                                invoke-interface {v0, v1}, Ljava/util/List;->get(I)Ljava/lang/Object;
                                move-result-object v0
                                invoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }

                // 2. Hook MenuItem.access$getPremiumItems$cp() to return only indices [0, 3)
                // (WHO_VIEWED_MY_PROFILE, WHO_SEARCHED_FOR_ME, CONTACT_REQUESTS)
                // discarding index 3 (FRAUD_INSURANCE)
                if (method.name == "access${'$'}getPremiumItems${'$'}cp" && method.returnType == "Ljava/util/List;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;->premiumItems:Ljava/util/List;
                                const/4 v1, 0x0
                                const/4 v2, 0x3
                                invoke-interface {v0, v1, v2}, Ljava/util/List;->subList(II)Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
