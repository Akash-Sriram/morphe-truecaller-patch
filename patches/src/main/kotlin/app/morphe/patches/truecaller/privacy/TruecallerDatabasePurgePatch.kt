package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Enforces SQLite Storage & RAM Purge:
 * Permanently deletes dead and obsolete database files from /data/data/com.truecaller/databases/
 * on application startup using native Context.deleteDatabase(String):
 *  - surveysDb (Feedback & marketing surveys)
 *  - assistant_campaigns_db (Assistant promotions)
 *  - enterprise_callkit_db (Enterprise business calling)
 *  - family_group (Family protection tracking)
 *  - freshchat.db (Customer support chat SDK)
 *  - exoplayer_internal.db (Video ad caching)
 *  - analytics.db (Local telemetry logging)
 *  - google_app_measurement_local.db (Firebase tracking)
 *  - tags.db (Contact tagging)
 *  - performance_monitoring (SDK metric profiling)
 *  - contact_request (Contact exchange)
 *  - bizDynamicContactDatabase (Business ads)
 *  - telecom_operator_data_db (Telco profiling)
 *  - work_actions (Internal WorkManager queue)
 *  - com.google.android.datatransport.events (DataTransport queue)
 *
 * This frees several megabytes of storage, eliminates SQLite WAL write-locks,
 * and recovers file descriptors and native memory pools.
 */
@Suppress("unused")
val truecallerDatabasePurgePatch = bytecodePatch(
    name = "Truecaller — SQLite Storage & RAM Purge",
    description = "Deletes obsolete database files on startup, preventing dead database engines from consuming RAM and flash storage.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val appClass = mutableClassDefByOrNull("Lcom/truecaller/TruecallerApp;")
            if (appClass != null) {
                for (method in appClass.methods) {
                    if (method.name == "onCreate" && method.returnType == "V" && method.parameterTypes.isEmpty()) {
                        method.implementation?.let {
                            val deadDatabases = listOf(
                                "surveysDb",
                                "assistant_campaigns_db",
                                "enterprise_callkit_db",
                                "family_group",
                                "freshchat.db",
                                "exoplayer_internal.db",
                                "analytics.db",
                                "google_app_measurement_local.db",
                                "tags.db",
                                "performance_monitoring",
                                "contact_request",
                                "bizDynamicContactDatabase",
                                "telecom_operator_data_db",
                                "work_actions",
                                "com.google.android.datatransport.events",
                            )

                            val purgeInstructions = buildString {
                                for (db in deadDatabases) {
                                    appendLine("const-string v0, \"$db\"")
                                    appendLine("invoke-virtual {p0, v0}, Landroid/content/Context;->deleteDatabase(Ljava/lang/String;)Z")
                                }
                            }.trimIndent()

                            method.addInstructions(0, purgeInstructions)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
