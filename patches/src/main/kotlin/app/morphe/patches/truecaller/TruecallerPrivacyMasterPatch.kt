package app.morphe.patches.truecaller

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.ads.truecallerAdRemovalPatch
import app.morphe.patches.truecaller.ads.truecallerPromotionalNeutralizePatch
import app.morphe.patches.truecaller.ads.truecallerSdkNeutralizePatch
import app.morphe.patches.truecaller.layout.truecallerHideAssistantTabPatch
import app.morphe.patches.truecaller.layout.truecallerHideFamilyProtectionPatch
import app.morphe.patches.truecaller.layout.truecallerHideScamsTabPatch
import app.morphe.patches.truecaller.misc.truecallerDisableUpdateNagPatch
import app.morphe.patches.truecaller.misc.truecallerGmsSignInBypassPatch
import app.morphe.patches.truecaller.privacy.truecallerBypassPermissionGatePatch
import app.morphe.patches.truecaller.privacy.truecallerDisableTelemetryPatch
import app.morphe.patches.truecaller.privacy.truecallerWorkerNeutralizePatch
import app.morphe.patches.truecaller.privacy.truecallerNetworkBlackholePatch
import app.morphe.patches.truecaller.privacy.truecallerStoragePrivacyPatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Truecaller Minimal Privacy & Debloat Bundle.
 * Strips away telemetry, background surveillance workers, ad SDKs, UI bloat,
 * and permission nag gates, leaving strictly core caller identification intact.
 */
@Suppress("unused")
val truecallerPrivacyMasterPatch = bytecodePatch(
    name = "Truecaller — Core Privacy & Minimalist Bundle",
    description = "Enables complete telemetry suppression, background worker neutralization, ad removal, 15 SDK killers, bloat tab removal, permission nag gate bypass, and SMS OTP login.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    dependsOn(
        truecallerDisableTelemetryPatch,
        truecallerWorkerNeutralizePatch,
        truecallerSdkNeutralizePatch,
        truecallerAdRemovalPatch,
        truecallerGmsSignInBypassPatch,
        truecallerDisableUpdateNagPatch,
        truecallerHideAssistantTabPatch,
        truecallerHideFamilyProtectionPatch,
        truecallerHideScamsTabPatch,
        truecallerBypassPermissionGatePatch,
        truecallerNetworkBlackholePatch,
        truecallerPromotionalNeutralizePatch,
        truecallerStoragePrivacyPatch,
    )

    execute {
        // Sub-patches execute their own transformations
    }
}
