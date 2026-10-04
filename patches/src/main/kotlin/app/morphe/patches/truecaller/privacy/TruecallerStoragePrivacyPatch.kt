package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Neutralizes local disk-bound telemetry and surveillance logging:
 * 1. AnalyticsRepository (Lv4i;): Hooks h(), b(), e() to return Unit immediately,
 *    preventing any insertion into persisted_event in analytics.db.
 * 2. ProfileViewRecorder (Li0v;): Hooks b0() to return void immediately,
 *    preventing any insertion into profile_view_events in tc.db.
 * 3. CrashlyticsRegistrar: Hooks getComponents() to return Collections.emptyList(),
 *    stopping Firebase Crashlytics runtime initialization.
 * 4. TransportRegistrar: Hooks getComponents() to return Collections.emptyList(),
 *    stopping Google DataTransport runtime logging.
 */
@Suppress("unused")
val truecallerStoragePrivacyPatch = bytecodePatch(
    name = "Truecaller — Storage & Local Telemetry Minimization",
    description = "Neutralizes local SQLite telemetry logging (persisted_event in analytics.db), outgoing contact view logging (profile_view_events in tc.db), and Google Crashlytics/DataTransport registrars.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Drop analytics.db persisted_event insertions (Lv4i;)
        try {
            val v4iClass = mutableClassDefBy("Lv4i;")
            for (method in v4iClass.methods) {
                if (method.name == "h" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                } else if ((method.name == "b" || method.name == "e") && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 1b. Drop analytics event dispatching at manager level (Lbdo;)
        try {
            val bdoClass = mutableClassDefBy("Lbdo;")
            for (method in bdoClass.methods) {
                if (method.name == "c" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                } else if (method.name == "d" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                } else if (method.name == "a" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Drop tc.db profile_view_events recording (Li0v;)
        try {
            val i0vClass = mutableClassDefBy("Li0v;")
            for (method in i0vClass.methods) {
                if (method.name == "b0" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Neutralize Google Firebase Crashlytics
        try {
            val crashlyticsClass = mutableClassDefBy("Lcom/google/firebase/crashlytics/CrashlyticsRegistrar;")
            for (method in crashlyticsClass.methods) {
                if (method.name == "getComponents" && method.returnType == "Ljava/util/List;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Neutralize CleverTap event and user profile syncing (Lhqa;)
        try {
            val hqaClass = mutableClassDefBy("Lhqa;")
            for (method in hqaClass.methods) {
                if (method.returnType == "V" && method.name != "<init>") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 5. Neutralize Google Analytics, App Measurement, Sessions & Crashlytics Registrars
        for (registrarClassDef in listOf(
            "Lcom/google/firebase/analytics/connector/internal/AnalyticsConnectorRegistrar;",
            "Lcom/google/firebase/analytics/ktx/FirebaseAnalyticsLegacyRegistrar;",
            "Lcom/google/firebase/sessions/FirebaseSessionsRegistrar;",
            "Lcom/google/firebase/crashlytics/FirebaseCrashlyticsKtxRegistrar;",
        )) {
            try {
                val regClass = mutableClassDefBy(registrarClassDef)
                for (method in regClass.methods) {
                    if (method.name == "getComponents" && method.returnType == "Ljava/util/List;") {
                        if (method.implementation != null) {
                            method.addInstructions(
                                0,
                                """
                                    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                                    move-result-object v0
                                    return-object v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }

        // 6. Neutralize FirebaseAnalytics direct calls & AppMeasurement receivers
        try {
            val fbAnalytics = mutableClassDefBy("Lcom/google/firebase/analytics/FirebaseAnalytics;")
            for (method in fbAnalytics.methods) {
                if (method.name in listOf("logEvent", "setCurrentScreen", "setUserProperty") && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        for (receiverClassDef in listOf(
            "Lcom/google/android/gms/measurement/AppMeasurementReceiver;",
            "Lcom/google/android/gms/measurement/AppMeasurementInstallReferrerReceiver;",
        )) {
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

        // 7. Neutralize Firebase HeartBeat Info Storage (Lrcm;)
        try {
            val rcmClass = mutableClassDefBy("Lrcm;")
            for (method in rcmClass.methods) {
                if (method.name != "<init>" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                } else if (method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 8. Neutralize Google Play Services AppMeasurement Dynamite Service & AppMeasurement
        try {
            val dynamiteClass = mutableClassDefBy("Lcom/google/android/gms/measurement/internal/AppMeasurementDynamiteService;")
            for (method in dynamiteClass.methods) {
                if (method.name != "<init>" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        try {
            val appMeasurementClass = mutableClassDefBy("Lcom/google/android/gms/measurement/AppMeasurement;")
            for (method in appMeasurementClass.methods) {
                if (method.name == "getInstance" && method.returnType == "Lcom/google/android/gms/measurement/AppMeasurement;") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                    }
                } else if (method.name != "<init>" && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 9. Neutralize AppMeasurement and Session background services
        for (serviceClassDef in listOf(
            "Lcom/google/android/gms/measurement/AppMeasurementService;",
            "Lcom/google/android/gms/measurement/AppMeasurementJobService;",
            "Lcom/google/firebase/sessions/SessionLifecycleService;",
        )) {
            try {
                val sClass = mutableClassDefBy(serviceClassDef)
                for (method in sClass.methods) {
                    if (method.name in listOf("onCreate", "onDestroy") && method.returnType == "V") {
                        if (method.implementation != null) {
                            method.addInstructions(0, "return-void")
                        }
                    } else if (method.name == "onStartCommand" && method.returnType == "I") {
                        if (method.implementation != null) {
                            method.addInstructions(0, "const/4 v0, 0x2\nreturn v0")
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }

        // 10. Neutralize Session Storage Lifecycle Recorder (Lb3i;) and Preference Holder (Ly2i;)
        try {
            val b3iClass = mutableClassDefBy("Lb3i;")
            for (method in b3iClass.methods) {
                if (method.name in listOf("onCreate", "onDestroy", "onStart", "onStop", "onResume", "onPause") && method.returnType == "V") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "return-void")
                    }
                } else if (method.name == "b" && method.returnType == "Z") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        try {
            val y2iClass = mutableClassDefBy("Ly2i;")
            for (method in y2iClass.methods) {
                if (method.name == "x" && method.returnType == "I") {
                    if (method.implementation != null) {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}

