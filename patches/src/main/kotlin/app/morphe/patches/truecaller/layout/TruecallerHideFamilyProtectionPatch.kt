package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import app.morphe.patches.truecaller.shared.Fingerprints.FamilyProtectFeatureFingerprint

/**
 * Removes the Family Protection button/tab from all entry points (search bar, toolbar, navigation)
 * and neutralizes Family Protection background services and status APIs.
 */
@Suppress("unused")
val truecallerHideFamilyProtectionPatch = bytecodePatch(
    name = "Truecaller — Hide Family Protection Button & Neutralize Backend",
    description = "Hides the Family Protection button next to the search bar and neutralizes backend services and status APIs.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Legacy fingerprint (fail-soft if absent)
        try {
            FamilyProtectFeatureFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        } catch (_: PatchException) { /* fail-soft */ }

        // 2. Suppress modern search bar entry button in TruecallerInit (K0() -> Boolean.FALSE)
        try {
            val initClass = mutableClassDefBy("Lcom/truecaller/ui/TruecallerInit;")
            for (method in initClass.methods) {
                if (method.name == "K0" && method.returnType == "Ljava/lang/Boolean;") {
                    if (method.implementation != null) {
                        method.ensureRegisters(1)
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Suppress Family Protect onboarding Compose renderer (Lcl60;->invoke)
        try {
            val cl60Class = mutableClassDefBy("Lcl60;")
            for (method in cl60Class.methods) {
                if (method.name == "invoke" && method.returnType == "Ljava/lang/Object;") {
                    if (method.implementation != null) {
                        method.ensureRegisters(1)
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

        // 4. Neutralize FamilyGroupApi status providers to report permanently disabled (all false)
        val familyApiImplementations = listOf("Lhki;", "Lrki;", "Lski;")
        for (implName in familyApiImplementations) {
            try {
                val apiClass = mutableClassDefBy(implName)
                for (method in apiClass.methods) {
                    if (method.name == "getStatus" && method.returnType == "Luli;") {
                        if (method.implementation != null) {
                            method.ensureRegisters(6)
                            method.addInstructions(
                                0,
                                """
                                    new-instance v0, Luli;
                                    const/4 v1, 0x0
                                    const/4 v2, 0x0
                                    const/4 v3, 0x0
                                    const/4 v4, 0x0
                                    const/4 v5, 0x0
                                    invoke-direct/range {v0 .. v5}, Luli;-><init>(ZZZZZ)V
                                    return-object v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }

        // 5. Neutralize RemoteReject background services
        val serviceClasses = listOf(
            "Lcom/truecaller/familyprotect/domain/remotereject/admin/RemoteRejectAdminService;",
            "Lcom/truecaller/familyprotect/domain/remotereject/member/RemoteRejectMemberService;",
        )
        for (svcName in serviceClasses) {
            try {
                val svcClass = mutableClassDefBy(svcName)
                for (method in svcClass.methods) {
                    if (method.name == "onCreate") {
                        if (method.implementation != null) {
                            method.addInstructions(0, "return-void")
                        }
                    } else if (method.name == "onStartCommand") {
                        if (method.implementation != null) {
                            method.ensureRegisters(1)
                            method.addInstructions(
                                0,
                                """
                                    const/4 v0, 0x2
                                    return v0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* fail-soft */ }
        }
    }
}
