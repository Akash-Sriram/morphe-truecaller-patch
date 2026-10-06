package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Surgically neutralizes background surveillance workers:
 *  - PhonebookSyncWorker (Contact address book harvesting)
 *  - CallHistoryFullSyncWorker (Full call log uploading)
 *  - BatteryStatusWorker & ActivityStatusWorker (Sensor & physical motion telemetry)
 *  - RingerModeListenerWorker (Volume/silent monitoring)
 *  - PostPresenceWorker & PostLastSeenWorker (Real-time online presence)
 *  - InsightsOneOffEnrichmentWorker (SMS transaction parsing)
 *  - VerifiedCampaignClickStreamWorker (In-app touch/click telemetry)
 */
@Suppress("unused")
val truecallerWorkerNeutralizePatch = bytecodePatch(
    name = "Truecaller — Neutralize Surveillance Workers",
    description = "Neutralizes background contact harvesting, call history sync, sensor logging, and presence workers by forcing immediate success results.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        val targetWorkers = listOf(
            "Lcom/truecaller/contact/phonebook/worker/PhonebookSyncWorker;",
            "Lcom/truecaller/callhistory/CallHistoryFullSyncWorker;",
            "Lcom/truecaller/familyprotect/domain/status/worker/BatteryStatusWorker;",
            "Lcom/truecaller/familyprotect/domain/status/worker/ActivityStatusWorker;",
            "Lcom/truecaller/presence/domain/worker/RingerModeListenerWorker;",
            "Lcom/truecaller/presence/domain/worker/PostPresenceWorker;",
            "Lcom/truecaller/presence/domain/worker/PostLastSeenWorker;",
            "Lcom/truecaller/insights/workers/InsightsOneOffEnrichmentWorker;",
            "Lcom/truecaller/insights/workers/InsightsReSyncWorker;",
            "Lcom/truecaller/insights/workers/InsightsRestoreSyncWorker;",
            "Lcom/truecaller/insights/workers/InsightsResyncEventLogWorker;",
            "Lcom/truecaller/insights/workers/InsightsSenderResolutionWorker;",
            "Lcom/truecaller/bizmon/banner/clickStream/VerifiedCampaignClickStreamWorker;",
            "Lcom/truecaller/enterpriseconfigurablecall/domain/clickstream/ConfigurableCallClickStreamWorker;",
            "Lcom/truecaller/surveys/data/PostSurveyAnswersWorker;",
            "Lcom/truecaller/bizmon/callSurvey/data/PostBizSurveyAnswersWorker;",
            "Lcom/truecaller/messaging/defaultsms/DmaAdsWorker;",
            "Lcom/truecaller/name_quality_feedback/internal/upload/UploadNameQualityFeedbackWorker;",
            "Lcom/truecaller/commentfeedback/workers/CommentFeedbackUploadWorker;",
            "Lcom/truecaller/commentfeedback/workers/VoteCommentWorker;",
            "Lcom/truecaller/verifiedcampaign/domain/internal/clickStream/VerifiedCampaignClickStreamWorker;",
            "Lcom/truecaller/whosearchedforme/ExactSearchLoggerWorker;",
            "Lcom/truecaller/whoviewedme/GenerateProfileViewWorker;",
            "Lcom/truecaller/whoviewedme/ReceiveProfileViewWorker;",
            "Lcom/truecaller/messaging/event_sender/RetryEventWorker;",
            "Lcom/truecaller/familyprotect/FamilyProtectFetchConfigWorker;",
            "Lcom/truecaller/familyprotect/domain/data/configuration/FamilyGroupInviteNudgeNotificationOneTimeWorker;",
            "Lcom/truecaller/familyprotect/domain/notification/FamilyGroupNotificationWorker;",
            "Lcom/truecaller/familyprotect/domain/transferblocklist/TransferBlockListWorker;",
            "Lcom/truecaller/scamfeed/ScamFeedNotificationWorker;",
            "Lcom/truecaller/rewardprogram/RewardProgramNotificationOneTimeWorker;",
            "Lcom/truecaller/backup/worker/BackupWorker;",
            "Lcom/truecaller/videocallerid/worker/VideoCallerIdCachingWorker;",
            "Lcom/truecaller/searchwarnings/supernova/GetSupernovaSettingsWorker;",
            "Lcom/truecaller/searchwarnings/supernova/SetSupernovaSettingsWorker;"
        )

        // AndroidX WorkManager success result return instruction snippet
        val returnSuccessInstructions = """
            invoke-static {}, Lp5q;->a()Lo5q;
            move-result-object v0
            return-object v0
        """.trimIndent()

        // TrackedCoroutineWorker precondition bypass (returning Boolean.FALSE skips payload)
        val returnFalseInstructions = """
            sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            return-object v0
        """.trimIndent()

        for (workerClass in targetWorkers) {
            try {
                val mutableClass = mutableClassDefBy(workerClass)
                for (method in mutableClass.methods) {
                    if (method.implementation == null) continue

                    // Match doWork or primary execution methods
                    if (method.name == "doWork" || (method.name in listOf("a", "e") && method.returnType == "Lp5q;")) {
                        method.addInstructions(0, returnSuccessInstructions)
                    } else if (method.name == "c" && method.returnType == "Ljava/lang/Object;") {
                        // In TrackedCoroutineWorker subclasses, c() returns Boolean determining if worker executes
                        method.addInstructions(0, returnFalseInstructions)
                    } else if (method.name in listOf("d", "e", "f") && method.returnType == "Ljava/lang/Object;") {
                        method.addInstructions(0, returnSuccessInstructions)
                    }
                }
            } catch (_: Exception) {
                // Fail-soft if worker is removed or absent
            }
        }
    }
}
