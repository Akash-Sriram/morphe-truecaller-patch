package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Strips non-core bloat categories from Truecaller Settings:
 *  - AI Call Assistant, screening rules, and digital voice cloning (SETTINGS_CALL_ASSISTANT)
 *  - Premium subscription store & upsell hub (SETTINGS_PREMIUM)
 *  - Android Watch / Wear OS companion guide (SETTINGS_WATCH)
 *  - Carrier voicemail integration (SETTINGS_VOICEMAIL)
 *  - Video Caller ID in Calls Settings (VideoCallerIdOptions)
 *  - Review nag & Community link in Help Settings (RateOnGooglePlay, TruetalksCommunity)
 *  - Strips Crown badge & premium subtitle on "Hide for non-phonebook contacts"
 *  - Suppresses telemetry/surveillance toggles in Privacy Center (ControlAds, AnonymizedData,
 *    FraudMessageLogging, Supernova, VerifiedBusinessCallLogs, BizPrivacyVCEventsToggle, PublicationCertificate)
 *  - Suppresses "Let Assistant answer" in Spam Protection settings (Lr6y.k)
 *  - Google Drive Backup in General Settings (GeneralSettings${'$'}Backup, BackupSmsBanner, BackupStorageFullBanner)
 */
@Suppress("unused")
val truecallerDebloatSettingsCategoriesPatch = bytecodePatch(
    name = "Truecaller — Debloat Settings Categories",
    description = "Removes AI Call Assistant, Premium Store, Wear OS Watch, Voicemail, Video Caller ID, Privacy Center telemetry toggles, Assistant spam handling, and Google Drive Backup.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. CategoriesSettingsState (Ll4a;): Force all bloat category booleans to false
        // Signature: Ll4a;.<init>:(ZZZZZZLs1y;)V
        // p1: isPremiumItemVisible
        // p2: isWatchItemVisible
        // p3: isCallAssistantItemVisible
        // p4: isVoicemailItemVisible
        try {
            val l4aClass = mutableClassDefBy("Ll4a;")
            for (method in l4aClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size >= 4) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p1, 0x0
                                const/4 p2, 0x0
                                const/4 p3, 0x0
                                const/4 p4, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Suppress Category sections in Lkux;->z
        // Signature: Lkux;->z(Lx6a;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lgf50;Ljava/lang/Integer;Lkotlin/jvm/functions/Function1;I)V
        // Targets: GeneralSettings$Shortcuts, CallsSettings$VideoCallerIdOptions, HelpSettings$Rate, GeneralSettings$Backup
        try {
            val kuxClass = mutableClassDefBy("Lkux;")
            for (method in kuxClass.methods) {
                if (method.name == "z" && method.parameterTypes.size == 6 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_pass_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Shortcuts${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}VideoCallerIdOptions${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/help/HelpSettings${'$'}Rate${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}Troubleshoot${'$'}Companion;
                                if-nez v0, :cond_suppress_z
                                goto :cond_pass_z
                                :cond_suppress_z
                                return-void
                                :cond_pass_z
                            """.trimIndent(),
                        )
                    }
                }

                // 3. Suppress Row / Card items in Lkux;->r
                // Signature: Lkux;->r(Lfc40;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lkotlin/jvm/functions/Function1;)V
                // Targets: CallsSettings$VideoCallerIdOptions$Appearance, GeneralSettings$Backup
                if (method.name == "r" && method.parameterTypes.size == 3 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_pass_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}VideoCallerIdOptions${'$'}Appearance;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup${'$'}Companion;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}SmsPermission;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}BackupStorageFull;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}Troubleshoot${'$'}Calls;
                                if-nez v0, :cond_suppress_r
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}Troubleshoot${'$'}Companion;
                                if-nez v0, :cond_suppress_r
                                goto :cond_pass_r
                                :cond_suppress_r
                                return-void
                                :cond_pass_r
                            """.trimIndent(),
                        )
                    }
                }

                // 3b. Suppress list items in Lkux;->w
                // Signature: Lkux;->w(Ldc40;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lgf50;Ljava/lang/Integer;)V
                if (method.name == "w" && method.parameterTypes.size == 4 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_pass_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup;
                                if-nez v0, :cond_suppress_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup${'$'}Companion;
                                if-nez v0, :cond_suppress_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}Troubleshoot${'$'}Calls;
                                if-nez v0, :cond_suppress_w
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}Troubleshoot${'$'}Companion;
                                if-nez v0, :cond_suppress_w
                                goto :cond_pass_w
                                :cond_suppress_w
                                return-void
                                :cond_pass_w
                            """.trimIndent(),
                        )
                    }
                }

                // 4. Suppress Navigation items in Lkux;->E
                // Signature: Lkux;->E(Lfc40;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lgf50;Ljava/lang/Integer;Lif50;Ljf20;Lgf50;I)V
                // Targets: HelpSettings$Rate$RateOnGooglePlay, HelpSettings$Support$TruetalksCommunity, PrivacySettings$Activity$ControlAds, GeneralSettings$Backup
                if (method.name == "E" && method.parameterTypes.size == 8 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_pass_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/help/HelpSettings${'$'}Rate${'$'}RateOnGooglePlay;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/help/HelpSettings${'$'}Support${'$'}TruetalksCommunity;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}ControlAds;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup${'$'}Companion;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_e
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_e
                                goto :cond_pass_e
                                :cond_suppress_e
                                return-void
                                :cond_pass_e
                            """.trimIndent(),
                        )
                    }
                }

                // 5. Strip Crown Badge on HideAcsForNonPbContacts & Suppress Privacy Telemetry & Backup in Lkux;->A
                // Signature: Lkux;->A(Lfc40;Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;Lgf50;Lgf50;Lgf50;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljf20;Lgf50;Lgf50;I)V
                if (method.name == "A" && method.parameterTypes.size == 13 &&
                    method.parameterTypes[1] == "Lcom/truecaller/settings/impl/framework/ui/model/CategoryType;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(2)
                        method.addInstructions(
                            0,
                            """
                                if-eqz p1, :cond_pass_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/calls/CallsSettings${'$'}AcsOptions${'$'}HideAcsForNonPbContacts;
                                if-nez v0, :cond_strip_crown
                                goto :cond_check_privacy_a
                                :cond_strip_crown
                                const/4 p4, 0x0
                                const/4 p5, 0x0
                                const/4 p8, 0x0

                                :cond_check_privacy_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}ControlAds;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}AnonymizedData;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}FraudMessageLogging;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}Supernova;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}VerifiedBusinessCallLogs;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}Activity${'$'}BizPrivacyVCEventsToggle;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/privacy/PrivacySettings${'$'}ManageData${'$'}PublicationCertificate;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}Backup${'$'}Companion;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupSmsBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_a
                                instance-of v0, p1, Lcom/truecaller/settings/impl/ui/general/GeneralSettings${'$'}BackupStorageFullBanner${'$'}Companion;
                                if-nez v0, :cond_suppress_a
                                goto :cond_pass_a

                                :cond_suppress_a
                                return-void

                                :cond_pass_a
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 6. Suppress "Let Assistant answer" in Spam Protection settings (Lr6y;->k)
        // Signature: Lr6y;->k(Lads;Ll9m;Lkotlin/jvm/functions/Function1;ZLbzk;I)V
        try {
            val r6yClass = mutableClassDefBy("Lr6y;")
            for (method in r6yClass.methods) {
                if (method.name == "k" && method.parameterTypes.size == 6 && method.returnType == "V") {
                    val instrs = method.instructions.toList()
                    var blockInvokeIdx = -1
                    var assistantInvokeIdx = -1
                    for (i in instrs.indices) {
                        val ins = instrs[i]
                        if (ins is ReferenceInstruction) {
                            val ref = ins.reference.toString()
                            if (ref == "how_to_handle_spam_calls_item_block_call") {
                                for (j in i until instrs.size) {
                                    val nextIns = instrs[j]
                                    if (nextIns is ReferenceInstruction && nextIns.reference.toString().contains("Lr6y;->e")) {
                                        blockInvokeIdx = j
                                        break
                                    }
                                }
                            } else if (ref == "how_to_handle_spam_calls_item_assistant_answer") {
                                for (j in i until instrs.size) {
                                    val nextIns = instrs[j]
                                    if (nextIns is ReferenceInstruction && nextIns.reference.toString().contains("Lr6y;->e")) {
                                        assistantInvokeIdx = j
                                        break
                                    }
                                }
                            }
                        }
                    }
                    if (blockInvokeIdx != -1 && assistantInvokeIdx > blockInvokeIdx) {
                        val countToRemove = assistantInvokeIdx - blockInvokeIdx
                        method.removeInstructions(blockInvokeIdx + 1, countToRemove)
                        method.addInstructions(
                            blockInvokeIdx + 1,
                            """
                                move-object/from16 v3, p2
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}

