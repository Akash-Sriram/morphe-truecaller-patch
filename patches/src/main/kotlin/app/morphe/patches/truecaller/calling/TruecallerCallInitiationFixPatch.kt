package app.morphe.patches.truecaller.calling

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Fixes outgoing call initiation and default dialer redirection across Truecaller:
 * 1. Neutralizes the Voice Revamp / Calling Limits interception gate (Lkpp;->a)
 *    which otherwise aborts calls and triggers an onboarding bottom sheet (0x7f0a03e6).
 * 2. Removes the invalid package restriction ("com.android.server.telecom") in Lmhn;->a,
 *    allowing Android 15 to cleanly dispatch the call intent to the system default dialer.
 */
@Suppress("unused")
val truecallerCallInitiationFixPatch = bytecodePatch(
    name = "Truecaller — Fix Call Initiation and Dialer Redirection",
    description = "Fixes outgoing call initiation from dialpad and call log by neutralizing call interception overlays and restoring default dialer intent redirection.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Force Lkpp;->a(CallOptions, Continuation) to unconditionally return Boolean.FALSE
        try {
            val kppClass = mutableClassDefByOrNull("Lkpp;")
                ?: mutableClassDefByOrNull { c ->
                    c.methods.any { m ->
                        m.name == "a" && m.parameterTypes.any { it.contains("InitiateCallHelper\$CallOptions") }
                    }
                }

            if (kppClass != null) {
                for (method in kppClass.methods) {
                    if (method.name == "a" && method.parameterTypes.any { it.contains("InitiateCallHelper\$CallOptions") }) {
                        if (method.implementation != null) {
                            method.ensureRegisters(1)
                            method.addInstructions(
                                0,
                                """
                                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                                    return-object v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Remove hardcoded "com.android.server.telecom" package restriction from Lmhn;->a
        try {
            val mhnClass = mutableClassDefByOrNull("Lmhn;")
                ?: getAllClassesWithString("com.android.server.telecom").firstOrNull()?.let { mutableClassDefBy(it) }

            if (mhnClass != null) {
                for (method in mhnClass.methods) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        var telecomStrIdx = -1
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction) {
                                val ref = ins.reference.toString()
                                if (ref == "com.android.server.telecom") {
                                    telecomStrIdx = i
                                } else if (telecomStrIdx != -1 && ref.contains("setPackage")) {
                                    method.replaceInstruction(i, "nop")
                                    method.replaceInstruction(telecomStrIdx, "nop")
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
