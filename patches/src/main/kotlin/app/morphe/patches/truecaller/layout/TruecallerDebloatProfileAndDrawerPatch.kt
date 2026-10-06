package app.morphe.patches.truecaller.layout

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Placeholder — the previous profile menu patch caused a VerifyError
 * (access$getOtherItems$cp has registers:1 but injected code needed v0+v1).
 * Kept as a no-op so the registered patch name stays valid.
 *
 * Root cause: Morphe's addInstructions(0, ...) inserts code at offset 0 without
 * enlarging the register frame. access$getOtherItems$cp() only allocates 1 register
 * (registers:1 = v0 only), so using v1 causes ART to reject the class entirely
 * with "register index out of range (1 >= 1)".
 */
@Suppress("unused")
val truecallerDebloatProfileAndDrawerPatch = bytecodePatch(
    name = "Truecaller — Debloat Profile & Drawer Services",
    description = "No-op: prior implementation caused VerifyError due to register allocation mismatch in access\$getOtherItems\$cp.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // Intentionally empty — profile menu is accessible again without crash.
        // The Fraud Insurance item remains; it can be addressed via a Compose-layer hook
        // once the correct mechanism for register widening is confirmed.
    }
}
