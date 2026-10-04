package app.morphe.patches.truecaller.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.OtpSmsApiSelectionFingerprint

/**
 * Forces SMS-based OTP verification instead of Google Play Services SmsRetriever.
 *
 * Essential for re-signed APKs where the signing certificate hash does not match
 * Truecaller's original Play Store signature hash, causing Google SmsRetriever to fail.
 *
 * Lineage: Joussflls10 / Paresh-Maheshwari / chirag127
 */
@Suppress("unused")
val truecallerGmsSignInBypassPatch = bytecodePatch(
    name = "Truecaller — GMS Sign-In Bypass",
    description = "Forces standard SMS-based OTP verification instead of GMS SmsRetriever, fixing login on re-signed APKs.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val googleFieldIndex = OtpSmsApiSelectionFingerprint.instructionMatches[0].index
            OtpSmsApiSelectionFingerprint.method.addInstructions(
                googleFieldIndex,
                """
                    sget-object v0, Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;->SMS:Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;
                    return-object v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }
    }
}
