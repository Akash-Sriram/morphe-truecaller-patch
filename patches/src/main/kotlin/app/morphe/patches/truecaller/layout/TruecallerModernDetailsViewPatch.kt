package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Enforces the modern Compose DetailsView UI across all contacts and search results.
 *
 * In Truecaller v26.39.6, the app contains two details screens:
 *  1. Modern Compose UI: com.truecaller.detailsview.presentation.ui.screen.DetailsViewActivity
 *  2. Legacy XML UI: com.truecaller.details_view.ui.DetailsViewActivityLegacy (via Liif;)
 *
 * When offline or when server-driven A/B experiment rollout flags are suppressed,
 * Truecaller defaults to Liif; (the legacy screen with rectangular bordered buttons).
 *
 * This patch intercepts Liif;->a(...) and builds the Intent directly for DetailsViewActivity,
 * using move-object/from16 to safely load high parameter registers (v17/v18) into 4-bit registers
 * (v0..v3), ensuring 100% ART dex verification compliance.
 */
@Suppress("unused")
val truecallerModernDetailsViewPatch = bytecodePatch(
    name = "Truecaller — Modern Details View",
    description = "Forces the modern polished Jetpack Compose contact details screen with circular action buttons instead of the legacy fallback UI.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val iifClass = mutableClassDefBy("Liif;")
            for (method in iifClass.methods) {
                if (method.name == "a" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Landroid/content/Context;" &&
                    method.returnType == "Landroid/content/Intent;"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                move-object/from16 v0, p1
                                move-object/from16 v1, p2
                                new-instance v2, Landroid/content/Intent;
                                const-class v3, Lcom/truecaller/detailsview/presentation/ui/screen/DetailsViewActivity;
                                invoke-direct {v2, v0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                                const-string v3, "DetailsViewExtrasV2"
                                invoke-virtual {v2, v3, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Parcelable;)Landroid/content/Intent;
                                return-object v2
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Hook A/B Test Variant Checker Leoa;->a(Lirb;)Z to return true.
        // In Truecaller Compose DetailsView, action buttons are built by Lxa20;->o.
        // WhatsApp button in the top action row is gated by Lirb;->b(), which delegates
        // to Leoa;->a(Lirb;). Forcing it to true places the WhatsApp button in the top circular row.
        try {
            val eoaClass = mutableClassDefBy("Leoa;")
            for (method in eoaClass.methods) {
                if (method.name == "a" && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == "Lirb;" &&
                    method.returnType == "Z"
                ) {
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
    }
}
