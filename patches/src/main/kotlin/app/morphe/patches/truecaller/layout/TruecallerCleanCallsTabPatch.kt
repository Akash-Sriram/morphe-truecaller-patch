package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Removes frequent contacts avatars and filter chips from the main Calls tab.
 *
 * In Truecaller v26.39.6 Jetpack Compose architecture:
 * The Calls tab list is constructed in Lhd8;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
 * via LazyListScope (Ljyn;->n):
 *  1. "frequent_contacts_id": frequent contacts avatar row at the top of the call log.
 *  2. "call_log_filters": filter pills row ("All", "Missed", "Voicemail", "Not spam") in between.
 *
 * By replacing the invoke-static calls to Ljyn;->n for these two item keys with NOPs,
 * Compose LazyColumn cleanly omits both items with zero remaining whitespace or dead layout gaps,
 * rendering the clean call log list directly beneath the search bar.
 */
@Suppress("unused")
val truecallerCleanCallsTabPatch = bytecodePatch(
    name = "Truecaller — Clean Calls Tab",
    description = "Removes frequent contacts avatar row and filter chips from the Calls tab for a clean call log layout.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val hd8Class = mutableClassDefByOrNull("Lhd8;")
                ?: getAllClassesWithString("frequent_contacts_id").firstOrNull()?.let { mutableClassDefBy(it) }

            if (hd8Class != null) {
                for (method in hd8Class.methods) {
                    if (method.name == "invoke" && method.parameterTypes.size == 1 &&
                        method.returnType == "Ljava/lang/Object;"
                    ) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction) {
                                val ref = ins.reference.toString()
                                if (ref == "frequent_contacts_id" || ref == "call_log_filters") {
                                    // Locate the subsequent invoke-static {..., Ljyn;->n}
                                    for (j in i until minOf(i + 5, instrs.size)) {
                                        val nextIns = instrs[j]
                                        if (nextIns is ReferenceInstruction && nextIns.reference.toString().contains("Ljyn;->n")) {
                                            method.replaceInstruction(j, "nop")
                                            method.replaceInstruction(i, "nop")
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
