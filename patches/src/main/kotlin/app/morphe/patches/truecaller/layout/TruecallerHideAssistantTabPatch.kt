package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.AssistantFeatureFingerprint

/**
 * Removes the AI Assistant and Premium tabs from the bottom navigation bar.
 *
 * In Truecaller v26.39.6, the non-core bottom bar tabs (Assistant, Premium, Scam Feed,
 * Family Protection) are gated by Lum6;.a:()Z in BottomBarManager (Lxm6;.g).
 * Forcing Lum6;.a:()Z to return false causes the manager to exclude these tabs,
 * leaving strictly the core caller-ID tabs: Calls, Messages, and Protection.
 *
 * Lineage: Joussflls10 / Paresh-Maheshwari / chirag127 / Akash-Sriram
 */
@Suppress("unused")
val truecallerHideAssistantTabPatch = bytecodePatch(
    name = "Truecaller — Hide Assistant Tab",
    description = "Hides the AI Assistant and Premium tabs from the bottom navigation bar.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Legacy fingerprint hook (fail-soft for older versions)
        try {
            AssistantFeatureFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }

        // 2. Truecaller v26.39.6 BottomBar Tab Filter (Lum6;.a:()Z)
        // Controls display of bloat tabs (Assistant, Premium, Scam Feed, Family Protection).
        // Returning false causes BottomBarManager (Lxm6;.g) to exclude them from the navigation bar.
        try {
            val um6Class = mutableClassDefBy("Lum6;")
            for (method in um6Class.methods) {
                if (method.name == "a" && method.returnType == "Z" && method.parameterTypes.isEmpty()) {
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
    }
}
