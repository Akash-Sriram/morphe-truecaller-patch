package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Suppresses Default Phone App and Default SMS App nag banners and cards across Truecaller:
 *  - Messages tab SMS spam protection filter banner (Lsh30;)
 *  - Settings main screen top "Set Truecaller as default phone app" card (Lyg20;, Lxg20;)
 *  - Calls Settings "Troubleshoot calling / Set Truecaller as default phone app" card (Lsfn;, Laf60;)
 */
@Suppress("unused")
val truecallerSuppressDefaultAppNagsPatch = bytecodePatch(
    name = "Truecaller — Suppress Default App Nags",
    description = "Suppresses default phone app and SMS app nag cards and banners in Messages feed, Settings main screen, and Calls troubleshooting.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Messages Tab SMS Filter Banner: Lsh30;-><init>(ZII)V
        // Force p1 (isVisible) to false (0x0) before storing into this.a
        try {
            val sh30Class = mutableClassDefBy("Lsh30;")
            for (method in sh30Class.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 3 &&
                    method.parameterTypes[0] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p1, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Settings Main Screen Top Banner State: Lyg20;-><init>(ZL...;)V
        // Force p1 (isVisible) to false (0x0) before storing into this.a
        try {
            val yg20Class = mutableClassDefBy("Lyg20;")
            for (method in yg20Class.methods) {
                if (method.name == "<init>" && method.parameterTypes.size >= 2 &&
                    method.parameterTypes[0] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p1, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Settings Banner Helper: Lxg20;->a()Lcom/truecaller/settings/impl/ui/categories/banner/BannerType;
        // Unconditionally return BannerType.NONE
        try {
            val xg20Class = mutableClassDefBy("Lxg20;")
            for (method in xg20Class.methods) {
                if (method.name == "a" &&
                    method.returnType == "Lcom/truecaller/settings/impl/ui/categories/banner/BannerType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(1)
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/settings/impl/ui/categories/banner/BannerType;->NONE:Lcom/truecaller/settings/impl/ui/categories/banner/BannerType;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Troubleshoot Option Resolver: Lsfn;->E(Ljava/util/Set;)Ljava/util/Set;
        // Unconditionally return empty set so 0 troubleshoot issues are ever detected
        try {
            val sfnClass = mutableClassDefBy("Lsfn;")
            for (method in sfnClass.methods) {
                if (method.name == "E" && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == "Ljava/util/Set;" &&
                    method.returnType == "Ljava/util/Set;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(1)
                        method.addInstructions(
                            0,
                            """
                                invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 5. Troubleshoot UI Binder: Laf60;->y1()V
        // Set root ConstraintLayout (ghk.a) to View.GONE (8) and return immediately
        try {
            val af60Class = mutableClassDefBy("Laf60;")
            for (method in af60Class.methods) {
                if (method.name == "y1" && method.parameterTypes.isEmpty()) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                iget-object v0, p0, Lqa5;->b:Ljava/lang/Object;
                                check-cast v0, Lue60;
                                if-eqz v0, :cond_suppress_troubleshoot
                                invoke-virtual {v0}, Lue60;->W4()Lghk;
                                move-result-object v0
                                if-eqz v0, :cond_suppress_troubleshoot
                                iget-object v0, v0, Lghk;->a:Landroidx/constraintlayout/widget/ConstraintLayout;
                                if-eqz v0, :cond_suppress_troubleshoot
                                const/16 v1, 0x8
                                invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                                :cond_suppress_troubleshoot
                                return-void
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 6. Messages Tab Non-DMA Nag Banners & Spacer: Lbo3;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
        // Strip out the entire non-DMA banner & spacer branch (idx 685 to 731)
        // so that non-dma-big-banner, promo-banner, and no_sms_permission-alert are never added to LazyColumn
        try {
            val bo3Class = mutableClassDefBy("Lbo3;")
            for (method in bo3Class.methods) {
                if (method.name == "invoke" && method.parameterTypes.size == 1 &&
                    method.returnType == "Ljava/lang/Object;"
                ) {
                    val instrs = method.instructions.toList()
                    var bannerCheckIdx = -1
                    var filterTypeIdx = -1
                    for (i in instrs.indices) {
                        val ins = instrs[i]
                        if (ins is ReferenceInstruction) {
                            val ref = ins.reference.toString()
                            if (ref.contains("Lk2d;->b:Lcom/truecaller/messaging/messaginglist/v2/model/NonDmaBannerType;")) {
                                bannerCheckIdx = i
                            } else if (bannerCheckIdx != -1 && filterTypeIdx == -1 && ref.contains("NON_DMA_INBOX")) {
                                filterTypeIdx = i
                                break
                            }
                        }
                    }
                    if (bannerCheckIdx != -1 && filterTypeIdx > bannerCheckIdx) {
                        val start = bannerCheckIdx - 1
                        val count = filterTypeIdx - start
                        method.removeInstructions(start, count)
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
