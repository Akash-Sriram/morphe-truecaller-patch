package app.morphe.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.AdsBazFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.AfterCallMaybeUpdateAdFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.NeoAcsMaybeUpdateAdFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.ShouldShowAdsPrefsFingerprint

/**
 * Removes in-app advertisements across the after-call screen, caller ID overlay,
 * and internal ad presentation services.
 *
 * Lineage:
 *  - Primary: bufferk / chirag127
 *  - Secondary/Alternative: binarymend (v26.12.5 Loi1/baz fallback)
 */
@Suppress("unused")
val truecallerAdRemovalPatch = bytecodePatch(
    name = "Truecaller — Remove Ads",
    description = "Removes ads from the after-call screen, caller-ID overlay, and suppresses ad feature toggles.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. After-Call Screen ad update site (Classic ACS)
        try {
            AfterCallMaybeUpdateAdFingerprint.method.addInstructions(0, "return-void")
        } catch (_: PatchException) { /* fail-soft */ }

        // 2. Neo ACS (New UI after-call screen) ad update site
        try {
            NeoAcsMaybeUpdateAdFingerprint.method.addInstructions(0, "return-void")
        } catch (_: PatchException) { /* fail-soft */ }

        // 3. SharedPreferences shouldShowAds source of truth
        try {
            ShouldShowAdsPrefsFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }

        // 4. Fallback ad feature toggles (binarymend v26.12.5)
        try {
            val adsClass = classDefBy(AdsBazFingerprint.definingClass!!)
            val mutableAdsClass = AdsBazFingerprint.match(adsClass).classDef
            for (method in mutableAdsClass.methods) {
                if (method.returnType == "Z" && method.implementation != null) {
                    method.removeInstructions(0, method.instructions.count())
                    method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                }
            }
        } catch (_: Exception) { /* fail-soft if class changed */ }
    }
}
