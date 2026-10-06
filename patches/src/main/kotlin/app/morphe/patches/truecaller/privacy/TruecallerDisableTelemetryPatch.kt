package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.Fingerprints.AppStartTrackerClassFingerprint
import app.morphe.patches.truecaller.shared.Fingerprints.AppStartTrackerFingerprint

/**
 * Disables Truecaller's internal technical AppStartTracker session telemetry,
 * neutralizes the batch log collector (Lljq), and suppresses analytics ingestion.
 */
@Suppress("unused")
val truecallerDisableTelemetryPatch = bytecodePatch(
    name = "Truecaller — Disable Telemetry",
    description = "Disables AppStartTracker, BatchLogClient (ljq), and technical telemetry checkpoints.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. AppStartTracker: make enableTracking() a no-op
        try {
            AppStartTrackerFingerprint.method.addInstructions(0, "return-void")
        } catch (_: PatchException) {
            try {
                val trackerClass = classDefBy(AppStartTrackerClassFingerprint.definingClass!!)
                val mutableClass = AppStartTrackerClassFingerprint.match(trackerClass).classDef
                for (method in mutableClass.methods) {
                    if (method.name.contains("log") || method.name == "enableTracking") {
                        if (method.implementation != null) {
                            method.removeInstructions(0, method.instructions.count())
                            method.addInstructions(0, "return-void")
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }

        // 2. BatchLogClient (Lljq;): force collection extraction to return empty list
        try {
            val ljqClass = mutableClassDefBy("Lljq;")
            for (method in ljqClass.methods) {
                if (method.name == "c" && method.returnType == "Ljava/util/Collection;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lqlh;->b:Lqlh;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Neutralize RetentionTracker & Analytics HeartBeat (Lucm;)
        try {
            val ucmClass = mutableClassDefBy("Lucm;")
            for (method in ucmClass.methods) {
                if (method.name in listOf("a", "b") && method.returnType != "V") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Neutralize Notification Interaction Telemetry Profiler (Lyi10;)
        try {
            val yi10Class = mutableClassDefBy("Lyi10;")
            for (method in yi10Class.methods) {
                if (method.name == "b" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                } else if (method.name == "a" && method.returnType == "Ljava/lang/String;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const-string v0, ""
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 5. Neutralize Sensor, Activity, Presence, and Unlock Surveillance Receivers
        val surveillanceReceivers = listOf(
            "Lcom/truecaller/familyprotect/domain/status/activity/receiver/UserActivityTransitionBroadcastReceiver;",
            "Lcom/truecaller/presence/domain/worker/PresenceSchedulerReceiver;",
            "Lcom/truecaller/util/ScreenUnlockedBroadcastReceiver;",
            "Lcom/truecaller/util/RingerModeChangedReceiver;",
            "Lcom/truecaller/stats/IntentChooserReceiver;",
            "Lcom/truecaller/analytics/ChosenComponentReceiverViewActionEvent;"
        )
        for (receiverClassDef in surveillanceReceivers) {
            try {
                val recvClass = mutableClassDefBy(receiverClassDef)
                for (method in recvClass.methods) {
                    if (method.name == "onReceive" && method.returnType == "V") {
                        if (method.implementation != null) {
                            method.addInstructions(0, "return-void")
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }

        // 6. Enforce One-Way Presence (Ghost Mode): neutralizes uploading own status
        //    Llbx;->U (SetPresence) returns empty protobuf success Lh920;->b()Lh920;
        //    Llbx;->T (SetLastSeen) returns empty protobuf success Ld920;->b()Ld920;
        //    Lybx;->h(AvailabilityTrigger, boolean) becomes return-void
        try {
            val lbxClass = mutableClassDefBy("Llbx;")
            for (method in lbxClass.methods) {
                if (method.name == "U" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                invoke-static {}, Lh920;->b()Lh920;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                } else if (method.name == "T" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                invoke-static {}, Ld920;->b()Ld920;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        try {
            val ybxClass = mutableClassDefBy("Lybx;")
            for (method in ybxClass.methods) {
                if (method.name == "h" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
