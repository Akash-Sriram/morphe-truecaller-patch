package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Enforces Zero-Wakeup Deep Sleep & Battery Lockdown:
 * 1. SystemJobService (Landroidx/work/impl/background/systemjob/SystemJobService;):
 *    Hooks onStartJob(JobParameters) to return false (0) immediately, telling the OS
 *    JobScheduler that the job is already finished, instantly dropping CPU wakelocks.
 * 2. AlarmReceiver (Lcom/truecaller/service/AlarmReceiver;):
 *    Hooks onReceive(Context, Intent) to return void immediately, neutralizing all
 *    RTC_WAKEUP alarms and periodic wakeup broadcasts.
 *
 * Incoming and anticipated calls remain 100% functional because incoming cellular calls
 * are handled exclusively by Android Telecom Binder IPC via TruecallerCallScreeningService
 * (android.telecom.CallScreeningService) and InCallUIService (android.telecom.InCallService).
 */
@Suppress("unused")
val truecallerDeepSleepLockdownPatch = bytecodePatch(
    name = "Truecaller — Zero-Wakeup Deep Sleep & Battery Lockdown",
    description = "Neutralizes background CPU wakeups from JobScheduler and AlarmManager while preserving real-time incoming call recognition.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Lockdown SystemJobService (JobScheduler entry point)
        try {
            val systemJobServiceClass = mutableClassDefByOrNull("Landroidx/work/impl/background/systemjob/SystemJobService;")
            if (systemJobServiceClass != null) {
                for (method in systemJobServiceClass.methods) {
                    if (method.name == "onStartJob" && method.returnType == "Z") {
                        method.implementation?.let {
                            method.addInstructions(
                                0,
                                """
                                    const/4 v0, 0x0
                                    return v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Lockdown AlarmReceiver (AlarmManager RTC_WAKEUP entry point)
        try {
            val alarmReceiverClass = mutableClassDefByOrNull("Lcom/truecaller/service/AlarmReceiver;")
            if (alarmReceiverClass != null) {
                for (method in alarmReceiverClass.methods) {
                    if (method.name == "onReceive" && method.returnType == "V") {
                        method.implementation?.let {
                            method.addInstructions(0, "return-void")
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
