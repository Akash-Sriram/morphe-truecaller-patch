package app.morphe.patches.truecaller.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation

/**
 * Grows the method's register count to [needed] if it is currently smaller.
 *
 * `MutableMethodImplementation.registerCount` is a `private final int`. Growing
 * it is required when injecting smali code that uses scratch registers beyond
 * the method's original `.registers N`.
 */
fun MutableMethod.ensureRegisters(needed: Int) {
    val impl = implementation ?: return
    if (impl.registerCount >= needed) return
    val field = MutableMethodImplementation::class.java.declaredFields
        .firstOrNull { it.type == Int::class.javaPrimitiveType }
        ?.apply { isAccessible = true }
        ?: throw PatchException(
            "MutableMethodImplementation has no int field (registerCount). " +
                "dexlib2 internal layout changed?",
        )
    field.setInt(impl, needed)
}
