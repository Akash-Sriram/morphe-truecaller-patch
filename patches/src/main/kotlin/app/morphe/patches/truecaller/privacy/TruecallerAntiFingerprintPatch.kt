package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Deep Privacy & Anti-Fingerprinting Patch:
 * 1. Zeroes out Google Advertising ID (GAID) to "00000000-0000-0000-0000-000000000000" in Lu70;.
 * 2. Suppresses background clipboard snooping in Lrc; and Lxta; (while keeping user paste intact).
 * 3. Masks outgoing network requests with generic Pixel 8 / AOSP User-Agent in Ldf70;
 *    and neutralizes network interface hardware enumeration in Lfg00;.
 */
@Suppress("unused")
val truecallerAntiFingerprintPatch = bytecodePatch(
    name = "Truecaller — Deep Privacy & Anti-Fingerprinting",
    description = "Zeroes Google Advertising ID (GAID), suppresses background clipboard snooping, and masks OkHttp requests with generic Pixel 8 User-Agent.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Google Advertising ID (GAID) Zeroing (Lu70;)
        try {
            val u70Class = mutableClassDefByOrNull("Lu70;")
                ?: mutableClassDefByOrNull { c ->
                    c.methods.any { m ->
                        m.name == "b" && m.implementation?.instructions?.any { inst ->
                            inst is ReferenceInstruction && (inst.reference as? MethodReference)?.name == "getAdvertisingIdInfo"
                        } == true
                    }
                }

            if (u70Class != null) {
                for (method in u70Class.methods) {
                    if (method.name == "a" && method.returnType == "Ljava/lang/String;" && method.parameterTypes.isEmpty()) {
                        method.implementation?.let {
                            method.ensureRegisters(1)
                            method.addInstructions(
                                0,
                                """
                                    const-string v0, "00000000-0000-0000-0000-000000000000"
                                    return-object v0
                                """.trimIndent(),
                            )
                        }
                    } else if (method.name == "b" && method.returnType == "V" && method.parameterTypes.isEmpty()) {
                        method.implementation?.let {
                            method.addInstructions(0, "return-void")
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Clipboard Snooping Suppression (Lrc; & Lxta;)
        try {
            val rcClass = mutableClassDefByOrNull("Lrc;")
            if (rcClass != null) {
                for (method in rcClass.methods) {
                    if (method.name == "z" && method.returnType == "Ljava/lang/String;" && method.parameterTypes.isEmpty()) {
                        method.implementation?.let {
                            method.ensureRegisters(1)
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
            }
        } catch (_: Exception) { /* fail-soft */ }

        try {
            val xtaClass = mutableClassDefByOrNull("Lxta;")
            if (xtaClass != null) {
                for (method in xtaClass.methods) {
                    if ((method.name == "a" || method.name == "b") && method.returnType == "Ljava/lang/Object;") {
                        method.implementation?.let {
                            method.addInstructions(
                                0,
                                """
                                    sget-object p0, Lkotlin/Unit;->a:Lkotlin/Unit;
                                    return-object p0
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }


        // 3. User-Agent Masking (Ldf70; OkHttp Interceptor)
        try {
            val df70Class = mutableClassDefByOrNull("Ldf70;")
                ?: mutableClassDefByOrNull { c ->
                    c.interfaces.contains("Lokhttp3/Interceptor;") &&
                        c.methods.any { m ->
                            m.implementation?.instructions?.any { inst ->
                                inst is ReferenceInstruction && (inst.reference as? StringReference)?.string == "User-Agent"
                            } == true
                        }
                }

            if (df70Class != null) {
                for (method in df70Class.methods) {
                    if (method.name == "intercept" && method.returnType == "Lokhttp3/Response;") {
                        val impl = method.implementation ?: continue
                        var userAgentIdx = -1
                        for ((idx, inst) in impl.instructions.withIndex()) {
                            if (inst is ReferenceInstruction && (inst.reference as? StringReference)?.string == "User-Agent") {
                                userAgentIdx = idx
                                break
                            }
                        }

                        if (userAgentIdx >= 0) {
                            // 1. Mask User-Agent with generic Pixel 8 / AOSP fingerprint
                            method.addInstructions(
                                userAgentIdx,
                                """
                                    const-string p0, "Truecaller/14.0.0 (Linux; Android 14; Pixel 8)"
                                """.trimIndent(),
                            )

                            // 2. Strip sensitive tracking and fingerprinting headers
                            val trackingHeaders = listOf(
                                "client-timestamp",
                                "advertising-id",
                                "x-clevertap-id",
                                "x-device-id",
                                "x-sim-serial",
                                "x-network-operator",
                                "x-carrier",
                            )
                            val stripHeadersInstructions = buildString {
                                for (header in trackingHeaders) {
                                    appendLine("const-string v2, \"$header\"")
                                    appendLine("invoke-virtual {v1, v2}, Lokhttp3/Headers\$Builder;->g(Ljava/lang/String;)V")
                                }
                            }.trimIndent()

                            // Find Request.Builder.b()Lokhttp3/Request; to insert before build()
                            var buildIdx = -1
                            for ((idx, inst) in impl.instructions.withIndex()) {
                                if (idx > userAgentIdx && inst is ReferenceInstruction && (inst.reference as? MethodReference)?.let {
                                    it.name == "b" && it.returnType == "Lokhttp3/Request;"
                                } == true) {
                                    buildIdx = idx
                                    break
                                }
                            }

                            if (buildIdx >= 0) {
                                method.addInstructions(buildIdx, stripHeadersInstructions)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Suppress Network Interface Enumeration / MAC Scans (Lfg00;)
        try {
            val fg00Class = mutableClassDefByOrNull("Lfg00;")
                ?: mutableClassDefByOrNull { c ->
                    c.methods.any { m ->
                        m.name == "b" && m.implementation?.instructions?.any { inst ->
                            inst is ReferenceInstruction && (inst.reference as? MethodReference)?.name == "getNetworkInterfaces"
                        } == true
                    }
                }

            if (fg00Class != null) {
                for (method in fg00Class.methods) {
                    if (method.name == "b" && method.returnType == "Ljava/lang/String;" && method.parameterTypes.isEmpty()) {
                        method.implementation?.let {
                            method.ensureRegisters(1)
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
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
