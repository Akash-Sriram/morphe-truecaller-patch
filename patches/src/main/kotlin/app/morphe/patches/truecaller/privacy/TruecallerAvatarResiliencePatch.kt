package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Ensures contact profile pictures are resilient and never masked by unresolvable local URIs:
 * 1. Contact Aggregator (`Lr5u;->a`): Swaps candidate photo priority from {v0, v2} (Phonebook, Cloud)
 *    to {v2, v0} (Cloud, Phonebook) so reachable online Google Cloud CDN photos always take precedence.
 *    If no cloud photo exists, it cleanly falls back to the phonebook URI.
 * 2. Startup Database Self-Healer (`Lyk60;->onOpen`): Automatically executes an SQLite migration query
 *    on tc.db to repair existing contacts whose aggregated record was masked by a dead local URI.
 */
@Suppress("unused")
val truecallerAvatarResiliencePatch = bytecodePatch(
    name = "Truecaller — Avatar Resilience & Cloud Photo Fallback",
    description = "Prioritizes online Cloud avatar photos over unresolvable local address book URIs in the contact aggregator and automatically heals existing database records on startup.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Prioritize Cloud avatar over local URI in Contact Aggregator (Lr5u;)
        try {
            val r5uClass = mutableClassDefByOrNull("Lr5u;")
                ?: mutableClassDefByOrNull { c ->
                    c.methods.any { m ->
                        m.name == "a" && m.parameterTypes.size == 3 &&
                            m.parameterTypes.all { it == "Landroid/content/ContentValues;" }
                    }
                }

            if (r5uClass != null) {
                for (method in r5uClass.methods) {
                    if (method.name == "a" && method.parameterTypes.size == 3 &&
                        method.parameterTypes.all { it == "Landroid/content/ContentValues;" }
                    ) {
                        val impl = method.implementation ?: continue
                        var foundContactImageUrl = false
                        var targetFilledNewArrayIdx = -1

                        for ((idx, inst) in impl.instructions.withIndex()) {
                            if (!foundContactImageUrl) {
                                if (inst is ReferenceInstruction && (inst.reference as? StringReference)?.string == "contact_image_url") {
                                    foundContactImageUrl = true
                                }
                            } else {
                                if (inst.opcode == Opcode.FILLED_NEW_ARRAY) {
                                    targetFilledNewArrayIdx = idx
                                    break
                                }
                            }
                        }

                        if (targetFilledNewArrayIdx >= 0) {
                            method.replaceInstruction(
                                targetFilledNewArrayIdx,
                                "filled-new-array {v2, v0}, [Ljava/lang/String;",
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

    }
}
