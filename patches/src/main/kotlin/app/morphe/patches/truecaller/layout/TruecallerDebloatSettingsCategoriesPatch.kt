package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Strips non-core bloat categories from Truecaller Settings:
 *  - AI Call Assistant, screening rules, and digital voice cloning (SETTINGS_CALL_ASSISTANT)
 *  - Premium subscription store & upsell hub (SETTINGS_PREMIUM)
 *  - Android Watch / Wear OS companion guide (SETTINGS_WATCH)
 *  - Carrier voicemail integration (SETTINGS_VOICEMAIL)
 */
@Suppress("unused")
val truecallerDebloatSettingsCategoriesPatch = bytecodePatch(
    name = "Truecaller — Debloat Settings Categories",
    description = "Removes AI Call Assistant, Premium Store, Wear OS Watch, and Voicemail categories from Settings.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. CategoriesSettingsState (Ll4a;): Force all bloat category booleans to false
        // Signature: Ll4a;.<init>:(ZZZZZZLs1y;)V
        // p1: isPremiumItemVisible
        // p2: isWatchItemVisible
        // p3: isCallAssistantItemVisible
        // p4: isVoicemailItemVisible
        try {
            val l4aClass = mutableClassDefBy("Ll4a;")
            for (method in l4aClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size >= 4) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p1, 0x0
                                const/4 p2, 0x0
                                const/4 p3, 0x0
                                const/4 p4, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Suppress "Home Shortcuts" from General Settings
        // Signature: Lkux;->z(Lx6a;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lgf50;Ljava/lang/Integer;Lkotlin/jvm/functions/Function1;I)V
        try {
            val kuxClass = mutableClassDefBy("Lkux;")
            for (method in kuxClass.methods) {
                if (method.name == "z" && method.parameterTypes.size == 6 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Shortcuts${'$'}Companion;
                                if-eqz v0, :cond_shortcuts_pass
                                return-void
                                :cond_shortcuts_pass
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
