package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

private const val RETURN_VOID = "return-void"
private const val RETURN_FALSE = "const/4 v0, 0x0\nreturn v0"
private const val RETURN_NULL = "const/4 v0, 0x0\nreturn-object v0"
private const val RETURN_UNIT = "sget-object p0, Lkotlin/Unit;->a:Lkotlin/Unit;\nreturn-object p0"

/**
 * Startup Speed, RAM & Battery Optimization Patch:
 * 1. CleverTap SDK neutralization: Hooks Lhqa; (CleverTapManager) and Lzpa; (CleverTapAPIWrapper)
 *    so CleverTap SQLite databases (clevertap_*.db), background profiling threads, and event tracking are eliminated.
 * 2. Freshchat SDK neutralization: Hooks Lrqk; and Freshchat so Freshchat is reported permanently disabled,
 *    stopping background service polling and token registration.
 */
@Suppress("unused")
val truecallerStartupOptimizationPatch = bytecodePatch(
    name = "Truecaller — Startup Speed, RAM & Battery Optimization",
    description = "Strips CleverTap and Freshchat SDK initialization, preventing background SQLite databases, telemetry threads, and idle wakeups.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. CleverTap Manager Neutralization (Lhqa;)
        try {
            val hqaClass = mutableClassDefByOrNull("Lhqa;")
                ?: mutableClassDefByOrNull { c ->
                    c.interfaces.any { it.contains("CleverTapManager") }
                }

            if (hqaClass != null) {
                for (method in hqaClass.methods) {
                    if (method.name in listOf(
                            "initWithoutActivityLifeCycleCallBacks",
                            "onUserLogin",
                            "push",
                            "updateProfile",
                            "updatePushRegistrationId",
                            "mayBeProcessNotificationExtras",
                        ) && method.returnType == "V"
                    ) {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_VOID)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 1b. CleverTap Coroutine Worker Wrapper (Lzpa;)
        try {
            val zpaClass = mutableClassDefByOrNull("Lzpa;")
            if (zpaClass != null) {
                for (method in zpaClass.methods) {
                    if (method.returnType == "Ljava/lang/Object;" && method.parameterTypes.any { it == "Lqrc;" }) {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_UNIT)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Freshchat Manager Neutralization (Lrqk;)
        try {
            val rqkClass = mutableClassDefByOrNull("Lrqk;")
            if (rqkClass != null) {
                for (method in rqkClass.methods) {
                    if (method.name == "b" && method.returnType == "Z") {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_FALSE)
                        }
                    } else if (method.name == "a" && method.returnType.contains("Freshchat")) {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_NULL)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2b. Direct Freshchat SDK Neutralization (if present in build)
        try {
            val freshchatClass = mutableClassDefByOrNull("Lcom/freshchat/consumer/sdk/Freshchat;")
            if (freshchatClass != null) {
                for (method in freshchatClass.methods) {
                    if (method.name in listOf("init", "handleFcmMessage") && method.returnType == "V") {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_VOID)
                        }
                    } else if (method.name == "getInstance" && method.returnType.contains("Freshchat")) {
                        method.implementation?.let {
                            method.addInstructions(0, RETURN_NULL)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
