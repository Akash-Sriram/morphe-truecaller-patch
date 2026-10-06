package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Unlocks Incognito / Ghost Mode (Silent Profile Searching):
 * 1. Profile View Decision Engine (Lph90;):
 *    - Hooks ph90.g() -> true (renders Incognito toggle card in settings UI).
 *    - Hooks ph90.f() -> true (enables feature toggle).
 *    - Causes ph90.m() to evaluate Incognito as active, which makes GenerateProfileViewWorker
 *      completely skip reporting the profile view to KnownEndpoints.PROFILE_VIEW.
 * 2. Preference Resolver (Lve90;):
 *    - Hooks ve90.i() -> true (enables search incognito mode).
 */
@Suppress("unused")
val truecallerIncognitoModePatch = bytecodePatch(
    name = "Truecaller — Incognito & Silent Profile Searching",
    description = "Enables Incognito Mode, suppresses profile view notifications to searched users, unlocks silent profile viewing, and bypasses paywalls.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Hook Who Viewed Me & Incognito Controller Lph90;
        try {
            val ph90Class = mutableClassDefBy("Lph90;")
            for (method in ph90Class.methods) {
                if (method.implementation == null) continue

                // Hook ph90.g()Z -> true (renders incognito toggle switch in UI)
                if (method.name == "g" && method.parameterTypes.isEmpty() && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x1
                            return v0
                        """.trimIndent(),
                    )
                }

                // Hook ph90.f()Z -> true (enables Who Viewed Me feature toggle)
                if (method.name == "f" && method.parameterTypes.isEmpty() && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x1
                            return v0
                        """.trimIndent(),
                    )
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Hook Preference Resolver Lve90;->i()Z -> true
        try {
            val ve90Class = mutableClassDefBy("Lve90;")
            for (method in ve90Class.methods) {
                if (method.name == "i" && method.parameterTypes.isEmpty() && method.returnType == "Z") {
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
