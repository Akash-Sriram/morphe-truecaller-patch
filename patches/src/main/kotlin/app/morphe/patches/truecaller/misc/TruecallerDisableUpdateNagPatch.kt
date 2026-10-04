package app.morphe.patches.truecaller.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.ShouldTriggerUpdateFingerprint

/**
 * Disables the in-app forced update check and update nag prompts.
 *
 * Lineage: Joussflls10 / Paresh-Maheshwari / chirag127
 */
@Suppress("unused")
val truecallerDisableUpdateNagPatch = bytecodePatch(
    name = "Truecaller — Disable Update Check",
    description = "Disables in-app update checks and update nag prompts.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            ShouldTriggerUpdateFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }
    }
}
