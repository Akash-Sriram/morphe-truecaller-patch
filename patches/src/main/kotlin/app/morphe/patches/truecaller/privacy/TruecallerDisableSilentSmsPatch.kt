package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Permanently neutralizes covert Mobile-Originated (MO) Silent SMS verification.
 * Truecaller attempts to silently dispatch an outgoing SMS containing "Truecaller <uuid>"
 * to carrier gateway numbers without explicit user confirmation.
 *
 * This patch inspects the outgoing message payload in the primary Multi-SIM SMS dispatcher
 * (Llz20; - standard Android 5.1+ SubscriptionManager / SmsManager wrapper).
 * If the payload starts with "Truecaller " or "truecaller ", transmission is aborted
 * immediately, preventing cellular radio usage and carrier SMS charges.
 * Genuine user-composed messages are unaffected.
 */
@Suppress("unused")
val truecallerDisableSilentSmsPatch = bytecodePatch(
    name = "Truecaller — Disable Silent SMS Verification",
    description = "Neutralizes covert Mobile-Originated (MO) verification SMS messages by intercepting Truecaller verification nonces before reaching SmsManager.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // Target modern AOSP Multi-SIM dispatcher
        val targetDispatchers = listOf(
            "Llz20;"
        )

        // Inspection guard for Llz20;->B:
        // p0 = SmsManager, p1 = dest, p2 = sc, p3 = text, p4 = sent, p5 = delivery, p6 = String param
        val guardSnippet = """
            if-eqz p3, :cond_sms_pass
            const-string p6, "Truecaller "
            invoke-virtual {p3, p6}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result p6
            if-nez p6, :cond_drop_verification_sms
            const-string p6, "truecaller "
            invoke-virtual {p3, p6}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result p6
            if-nez p6, :cond_drop_verification_sms
            const-string p6, ""
            goto :cond_sms_pass

            :cond_drop_verification_sms
            const/4 p0, 0x0
            return p0

            :cond_sms_pass
        """.trimIndent()

        for (dispatcherClass in targetDispatchers) {
            try {
                val mutableClass = mutableClassDefBy(dispatcherClass)
                for (method in mutableClass.methods) {
                    if (method.implementation == null) continue

                    // Match method B(String, String, String, PendingIntent, PendingIntent, String)Z
                    if (method.name == "B" && method.returnType == "Z") {
                        var insertIndex = -1
                        for ((index, instruction) in method.instructions.withIndex()) {
                            val insStr = instruction.toString()
                            if (insStr.contains("sendTextMessage") || insStr.contains("4a0a")) {
                                insertIndex = index
                                break
                            }
                        }

                        if (insertIndex >= 0) {
                            method.addInstructions(insertIndex, guardSnippet)
                        } else {
                            method.addInstructions(0, guardSnippet)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fail-soft if class is absent
            }
        }
    }
}
