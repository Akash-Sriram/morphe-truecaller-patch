package app.morphe.patches.truecaller.calling

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Fixes Call Log Deletion & Synchronization:
 *
 * Problem:
 * 1. Single/Selected Deletion (`Lqa;->y`): Hardcodes `"type IN (1,2,3) "` for local SQLite history deletion.
 *    Rejected calls (`type = 5`) and Blocked calls (`type = 6`) are silently filtered out by SQLite,
 *    causing deletions to affect 0 rows and leaving call entries permanently stuck in the Calls tab.
 * 2. Clear All Calls (`Lke8;->invokeSuspend` case 5): Also hardcodes `"type IN (1,2,3) "`, leaving rejected
 *    and blocked calls behind.
 * 3. Call Log Synchronization (`Ltx40;` and `Lvx40;`): Queries only `"type IN (1,2,3)"`, preventing
 *    rejected/blocked calls from ever syncing deletions or updates with the Android OS call log.
 * 4. Orphan Linking (`Lke8;->invokeSuspend`): Filters on `"type in (1,2,3)"` when linking calls without
 *    call_log_id, causing rejected calls to keep `call_log_id = NULL` forever.
 *
 * Solution:
 * Widens all `"type IN (1,2,3)"` filters across all history operations to `"type IN (1,2,3,4,5,6,7,8,9)"`,
 * allowing all call record types (incoming, outgoing, missed, voicemail, rejected, blocked, externally answered)
 * to be cleanly deleted, linked, and synchronized.
 */
@Suppress("unused")
val truecallerCallLogDeleteFixPatch = bytecodePatch(
    name = "Truecaller — Fix Call Log Deletion & Synchronization",
    description = "Fixes call record deletion in Calls tab and system call log sync by supporting all call types (including rejected and blocked calls).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val candidateClasses = setOf(
                "Lqa;", "Lke8;", "Lle8;", "La68;", "Lje8;", "Ln78;", "Ltqf;", "Ltx40;", "Lvx40;", "Lsf;", "Lmie;"
            )

            val matchedClasses = candidateClasses.mapNotNull { mutableClassDefByOrNull(it) }
                .toMutableList()

            // Dynamically discover any other classes referencing "type IN (1,2,3)"
            val dynamicClasses = (getAllClassesWithString("type IN (1,2,3)") + getAllClassesWithString("type in (1,2,3)"))
                .distinctBy { it.type }
                .mapNotNull { mutableClassDefByOrNull(it.type) }

            val allClasses = (matchedClasses + dynamicClasses).distinctBy { it.type }

            for (clazz in allClasses) {
                for (method in clazz.methods) {
                    val impl = method.implementation ?: continue
                    val instrs = impl.instructions.toList()

                    for ((idx, inst) in instrs.withIndex()) {
                        if (inst is ReferenceInstruction && inst is OneRegisterInstruction) {
                            val strRef = inst.reference as? StringReference ?: continue
                            val originalStr = strRef.string
                            if (originalStr.contains("type IN (1,2,3)", ignoreCase = true) ||
                                originalStr.contains("type in (1,2,3)", ignoreCase = true)
                            ) {
                                val newStr = originalStr
                                    .replace("type IN (1,2,3)", "type IN (1,2,3,4,5,6,7,8,9)", ignoreCase = true)
                                    .replace("type in (1,2,3)", "type in (1,2,3,4,5,6,7,8,9)", ignoreCase = true)

                                val escaped = newStr.replace("\n", "\\n").replace("\"", "\\\"")
                                val reg = inst.registerA
                                method.replaceInstruction(idx, "const-string v$reg, \"$escaped\"")
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
