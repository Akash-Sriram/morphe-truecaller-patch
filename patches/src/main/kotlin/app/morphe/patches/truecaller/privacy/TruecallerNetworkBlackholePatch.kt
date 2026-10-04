package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Blackholes all surveillance, telemetry, batch-logging, profile-sync, presence,
 * and ad-engine endpoints to local loopback (127.0.0.1) while preserving core Caller ID
 * lookups (SEARCH, PUSH_CALLER_ID_GRPC, ACCOUNT) and forcing the European GDPR privacy domain.
 */
@Suppress("unused")
val truecallerNetworkBlackholePatch = bytecodePatch(
    name = "Truecaller — Network Blackhole & GDPR Enforcement",
    description = "Blackholes telemetry, logging, and profile exfiltration endpoints to 127.0.0.1 while forcing European GDPR privacy domain.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Force KnownDomain.f (domain resolver) to always return DOMAIN_REGION_1 (EU / GDPR)
        try {
            val hsxClass = mutableClassDefBy("Lhsx;")
            for (method in hsxClass.methods) {
                if (method.name == "f" && method.parameterTypes.size == 1 && method.parameterTypes[0] == "Ljava/lang/String;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/common/network/KnownDomain;->DOMAIN_REGION_1:Lcom/truecaller/common/network/KnownDomain;
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Force KnownDomain.getValue() to always return "eu"
        try {
            val knownDomainClass = mutableClassDefBy("Lcom/truecaller/common/network/KnownDomain;")
            for (method in knownDomainClass.methods) {
                if (method.name == "getValue" && method.returnType == "Ljava/lang/String;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const-string v0, "eu"
                                return-object v0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

// 3. Blackhole KnownEndpoints.getHost() for all non-essential endpoints
        try {
            val knownEndpointsClass = mutableClassDefBy("Lcom/truecaller/common/network/util/KnownEndpoints;")
            for (method in knownEndpointsClass.methods) {
                if (method.name == "getHost" && method.parameterTypes.size == 1) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->SEARCH:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->CLIENT_SEARCH:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->PUSH_CALLER_ID_GRPC:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->ACCOUNT:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->ACCOUNT_ONBOARDING:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->FILTER:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->SEARCH_WARNINGS_GRPC:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->CONTACT:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->PROFILE:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->IMAGES:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed
                                const-string v0, "127.0.0.1"
                                return-object v0
                                :cond_allowed
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Blackhole KnownEndpoints.url() for non-essential endpoints
        try {
            val knownEndpointsClass = mutableClassDefBy("Lcom/truecaller/common/network/util/KnownEndpoints;")
            for (method in knownEndpointsClass.methods) {
                if (method.name == "url" && method.returnType == "Lokhttp3/HttpUrl;") {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->SEARCH:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->CLIENT_SEARCH:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->PUSH_CALLER_ID_GRPC:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->ACCOUNT:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->ACCOUNT_ONBOARDING:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->FILTER:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->SEARCH_WARNINGS_GRPC:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->CONTACT:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->PROFILE:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                sget-object v0, Lcom/truecaller/common/network/util/KnownEndpoints;->IMAGES:Lcom/truecaller/common/network/util/KnownEndpoints;
                                if-eq p0, v0, :cond_allowed_url
                                new-instance v0, Lokhttp3/HttpUrl${'$'}Builder;
                                invoke-direct {v0}, Lokhttp3/HttpUrl${'$'}Builder;-><init>()V
                                const-string v1, "http"
                                invoke-virtual {v0, v1}, Lokhttp3/HttpUrl${'$'}Builder;->f(Ljava/lang/String;)V
                                const-string v1, "127.0.0.1"
                                invoke-virtual {v0, v1}, Lokhttp3/HttpUrl${'$'}Builder;->c(Ljava/lang/String;)V
                                invoke-virtual {v0}, Lokhttp3/HttpUrl${'$'}Builder;->b()Lokhttp3/HttpUrl;
                                move-result-object v0
                                return-object v0
                                :cond_allowed_url
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
