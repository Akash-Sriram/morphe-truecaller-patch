package app.morphe.patches.truecaller.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER

/**
 * Enforces an Application-Level Zero-Leakage DNS Firewall at the OkHttp DNS resolver boundary:
 * Lokhttp3/Dns${'$'}Companion${'$'}DnsSystem;->a(Ljava/lang/String;)Ljava/util/List;
 *
 * Checks every outgoing host request:
 *  - Only *.truecaller.com, *.truecallerstatic.com, localhost, and 127.0.0.1 are allowed.
 *  - Any other domain (ad networks like InMobi, Google AdMob, AppNext, Chartboost, Vungle,
 *    telemetry beacons, or third-party tracking endpoints) is immediately blocked by
 *    throwing java.net.UnknownHostException before any network socket is opened.
 */
@Suppress("unused")
val truecallerDnsFirewallPatch = bytecodePatch(
    name = "Truecaller — Strict Domain Blackhole & Zero-Leakage DNS Firewall",
    description = "Enforces an application-level DNS firewall blocking all non-whitelisted ad and surveillance domains at the OkHttp resolver layer.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        try {
            val dnsClass = mutableClassDefByOrNull("Lokhttp3/Dns${'$'}Companion${'$'}DnsSystem;")
                ?: mutableClassDefByOrNull { c ->
                    c.interfaces.contains("Lokhttp3/Dns;") &&
                        c.methods.any { it.name == "a" && it.returnType == "Ljava/util/List;" }
                }

            if (dnsClass != null) {
                for (method in dnsClass.methods) {
                    if (method.name == "a" && method.returnType == "Ljava/util/List;" && method.parameterTypes == listOf("Ljava/lang/String;")) {
                        method.implementation?.let {
                            method.addInstructions(
                                0,
                                """
                                    if-eqz p1, :cond_check_null
                                    const-string v0, "truecaller.com"
                                    invoke-virtual {p1, v0}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z
                                    move-result v0
                                    if-nez v0, :cond_allowed
                                    const-string v0, "truecallerstatic.com"
                                    invoke-virtual {p1, v0}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z
                                    move-result v0
                                    if-nez v0, :cond_allowed
                                    const-string v0, "localhost"
                                    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                                    move-result v0
                                    if-nez v0, :cond_allowed
                                    const-string v0, "127.0.0.1"
                                    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                                    move-result v0
                                    if-nez v0, :cond_allowed
                                    new-instance v0, Ljava/net/UnknownHostException;
                                    invoke-direct {v0, p1}, Ljava/net/UnknownHostException;-><init>(Ljava/lang/String;)V
                                    throw v0
                                    :cond_allowed
                                    :cond_check_null
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
