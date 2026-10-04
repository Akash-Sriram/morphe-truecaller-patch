package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.AssistantFeatureFingerprint

/**
 * Removes the AI Assistant tab from the bottom navigation bar.
 *
 * Lineage: Joussflls10 / Paresh-Maheshwari / chirag127
 */
@Suppress("unused")
val truecallerHideAssistantTabPatch = bytecodePatch(
    name = "Truecaller — Hide Assistant Tab",
    description = "Hides the AI Assistant tab from the bottom navigation bar.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            AssistantFeatureFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }
    }
}
