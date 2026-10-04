package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Bypasses Truecaller's mandatory permission nag gate (RequiredPermissionsActivityV2),
 * allowing the application to operate strictly as an incoming caller ID lookup engine
 * without requiring READ_CONTACTS, READ_CALL_LOG, or READ_SMS.
 */
@Suppress("unused")
val truecallerBypassPermissionGatePatch = bytecodePatch(
    name = "Truecaller — Bypass Permission Gate",
    description = "Bypasses the mandatory permission nag screen, allowing the app to run without contacts, call logs, or SMS permissions.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Force Lpnv.g([Ljava/lang/String;)Z to always return true (1)
        try {
            val pnvClass = mutableClassDefBy("Lpnv;")
            for (method in pnvClass.methods) {
                if (method.name == "g" && method.parameterTypes.size == 1 && method.parameterTypes[0] == "[Ljava/lang/String;") {
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

        // 2. Force Llyn.g([Ljava/lang/String;)Z to always return true (1)
        try {
            val lynClass = mutableClassDefBy("Llyn;")
            for (method in lynClass.methods) {
                if (method.name == "g" && method.parameterTypes.size == 1 && method.parameterTypes[0] == "[Ljava/lang/String;") {
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

        // 3. Force Lvsz.B([Ljava/lang/String;)Lbnv; to return Granted (Lzmv;->a)
        try {
            val vSzClass = mutableClassDefBy("Lvsz;")
            for (method in vSzClass.methods) {
                if (method.name == "B" && method.parameterTypes.size == 1 && method.parameterTypes[0] == "[Ljava/lang/String;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lzmv;->a:Lzmv;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
