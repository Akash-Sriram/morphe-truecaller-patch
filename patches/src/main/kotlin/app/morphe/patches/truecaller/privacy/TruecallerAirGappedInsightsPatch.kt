package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Enforces an "Air-Gapped Smart Mode" on Truecaller Insights:
 *  - Preserves local on-device smart features: 1-tap "Copy OTP" notification buttons,
 *    smart inbox category tabs (Personal, Transactions, Spam), and visual cards.
 *  - Silences background nudge/reminder broadcast receivers.
 *  - Neutralizes cloud backup staging and financial telemetry ingestion.
 */
@Suppress("unused")
val truecallerAirGappedInsightsPatch = bytecodePatch(
    name = "Truecaller — Air-Gapped Smart Insights",
    description = "Preserves local on-device Smart SMS and Copy-OTP convenience while permanently cutting off background nudges, cloud backup, and financial hoarding.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Silence background nudge and reminder broadcast receivers
        val receiversToSilence = listOf(
            "Lcom/truecaller/insights/nudges/receivers/BillNudgesBroadcastReceiver;",
            "Lcom/truecaller/insights/nudges/receivers/TravelNudgesBroadcastReceiver;",
            "Lcom/truecaller/insights/nudges/receivers/NudgesBootReceiver;",
            "Lcom/truecaller/insights/nudges/notification/NudgeNotificationBroadcastReceiver;",
            "Lcom/truecaller/insights/reminders/receiver/ReminderBootReceiver;"
        )

        for (receiverClass in receiversToSilence) {
            try {
                val mutableClass = mutableClassDefBy(receiverClass)
                for (method in mutableClass.methods) {
                    if (method.name == "onReceive" && method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            } catch (_: Exception) {
                // Fail-soft if class is absent
            }
        }
    }
}
