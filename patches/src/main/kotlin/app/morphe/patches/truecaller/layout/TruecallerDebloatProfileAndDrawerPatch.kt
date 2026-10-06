package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

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
        // 1. Hook MenuItem.access$getOtherItems$cp() to return an empty list
        //    All options (Settings, Theme, Help, Backup) are either in the toolbar, in SettingsActivity, or non-functional.
        try {
            val menuItemClass = mutableClassDefBy("Lcom/truecaller/usershome/presentaion/ui/components/menuitems/MenuItem;")
            for (method in menuItemClass.methods) {
                if (method.name == "access${'$'}getOtherItems${'$'}cp" && method.returnType == "Ljava/util/List;") {
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

        // 4. Remove "Get verified" button from the profile screen.
        //    In Lvp70;->D the action list is built: Lbp (Edit profile) is always added, then
        //    `if (ri70.h) add(Lcp(...))` adds Get verified. Force the read of Lri70;->h:Z to false.
        try {
            val vp70Class = mutableClassDefBy("Lvp70;")
            for (method in vp70Class.methods) {
                if (method.name != "D" || method.implementation == null) continue
                val instrs = method.instructions.toList()
                for (i in instrs.indices) {
                    val ins = instrs[i]
                    if (ins.opcode == Opcode.IGET_BOOLEAN && ins is ReferenceInstruction &&
                        ins.reference.toString() == "Lri70;->h:Z"
                    ) {
                        val reg = (ins as TwoRegisterInstruction).registerA
                        method.addInstructions(i + 1, "const/4 v$reg, 0x0")
                        break
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
