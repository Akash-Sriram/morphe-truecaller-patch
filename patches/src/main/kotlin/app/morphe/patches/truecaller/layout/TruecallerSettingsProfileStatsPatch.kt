package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Streamlines Truecaller navigation and eliminates intermediate screens:
 *  1. Wire Calls tab profile click directly to SettingsActivity (bypasses UsersHomeActivity).
 *  2. Wire Settings profile card click directly to EditProfileActivity.
 *  3. Show percentage badge (% of profile) on Settings profile picture.
 *  4. Hide "Help" item and its divider in Settings.
 *  5. Relocate "Your Truecaller stats" to the bottom of Settings list using Compose.
 */
@Suppress("unused")
val truecallerSettingsProfileStatsPatch = bytecodePatch(
    name = "Truecaller — Settings Profile Navigation & Stats",
    description = "Wires profile avatar directly to Settings, profile card to Edit Profile, shows % badge, hides Help, and displays stats at the bottom of Settings.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Calls tab profile avatar click -> SettingsActivity directly
        // Class: Lnzv;
        // Method: k(Context, NavigationSource, UsersHomeDeepLinkView): Intent
        try {
            val nzvClass = mutableClassDefBy("Lnzv;")
            for (method in nzvClass.methods) {
                if (method.name == "k" && method.parameterTypes.size == 3 &&
                    method.parameterTypes[0] == "Landroid/content/Context;" &&
                    method.returnType == "Landroid/content/Intent;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(5)
                        method.addInstructions(
                            0,
                            """
                                move-object/from16 v0, p0
                                move-object/from16 v1, p1
                                new-instance v2, Landroid/content/Intent;
                                const-class v3, Lcom/truecaller/settings/impl/ui/SettingsActivity;
                                invoke-direct {v2, v0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                                const-string v3, "EXTRA_NAVIGATION_SOURCE"
                                invoke-virtual {v2, v3, v1}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/io/Serializable;)Landroid/content/Intent;
                                return-object v2
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 2. Settings profile card click -> EditProfileActivity directly
        // Class: Ls3a;
        // Method: onClick(View)
        // When s3a.b == 0 (click on item_profile): launch EditProfileActivity directly
        try {
            val s3aClass = mutableClassDefBy("Ls3a;")
            for (method in s3aClass.methods) {
                if (method.name == "onClick" && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == "Landroid/view/View;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(6)
                        method.addInstructions(
                            0,
                            """
                                move-object/from16 v0, p0
                                iget-byte v1, v0, Ls3a;->b:B

                                # Case 0: Profile card clicked -> EditProfileActivity
                                if-nez v1, :cond_check_stats_click
                                iget-object v0, v0, Ls3a;->c:Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
                                invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                                move-result-object v0
                                new-instance v1, Landroid/content/Intent;
                                const-class v2, Lcom/truecaller/editprofile/impl/ui/EditProfileActivity;
                                invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                                invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                                return-void

                                :cond_check_stats_click
                                # Case 8 (item_help) or Case 99: Your Truecaller stats clicked -> UsersHomeActivity
                                const/16 v2, 0x8
                                if-ne v1, v2, :cond_check_case_99
                                goto :cond_launch_stats

                                :cond_check_case_99
                                const/16 v2, 0x63
                                if-ne v1, v2, :cond_normal_s3a_click

                                :cond_launch_stats
                                iget-object v0, v0, Ls3a;->c:Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
                                invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                                move-result-object v0
                                new-instance v1, Landroid/content/Intent;
                                const-class v2, Lcom/truecaller/usershome/presentaion/ui/screen/UsersHomeActivity;
                                invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                                const-string v2, "EXTRA_NAVIGATION_SOURCE"
                                sget-object v3, Lcom/truecaller/analytics/AppEvents${'$'}UsersHome${'$'}NavigationSource;->CALLS_TAB:Lcom/truecaller/analytics/AppEvents${'$'}UsersHome${'$'}NavigationSource;
                                invoke-virtual {v1, v2, v3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/io/Serializable;)Landroid/content/Intent;
                                invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                                return-void

                                :cond_normal_s3a_click
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 3. Show % badge on Settings profile pic and adjust position to fit inside view bounds
        // Class: Lcom/truecaller/common/ui/avatar/AvatarXView;
        // Method: j(IZ)V -> forces badge display when progress > 0
        // Method: c(Canvas, RectF) -> shifts badge Y position upwards so % badge is fully visible without clipping
        try {
            val avatarClass = mutableClassDefBy("Lcom/truecaller/common/ui/avatar/AvatarXView;")
            for (method in avatarClass.methods) {
                if (method.name == "j" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "I" && method.parameterTypes[1] == "Z"
                ) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                if-lez p1, :cond_no_force_badge
                                const/4 p2, 0x1
                                :cond_no_force_badge
                            """.trimIndent(),
                        )
                    }
                }

                // Adjust badge Y position in method c(Canvas, RectF) so it doesn't get clipped at bottom
                if (method.name == "c" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Landroid/graphics/Canvas;" &&
                    method.parameterTypes[1] == "Landroid/graphics/RectF;" &&
                    method.returnType == "Landroid/graphics/RectF;"
                ) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction && ins.reference.toString().endsWith("->O:F")) {
                                for (j in i + 1 until minOf(i + 10, instrs.size)) {
                                    val subIns = instrs[j]
                                    if (subIns.opcode.name == "sub-float/2addr") {
                                        for (k in j + 1 until minOf(j + 5, instrs.size)) {
                                            val secondSub = instrs[k]
                                            if (secondSub.opcode.name == "sub-float/2addr") {
                                                method.addInstructions(
                                                    k + 1,
                                                    """
                                                        sub-float/2addr p2, v5
                                                    """.trimIndent(),
                                                )
                                                break
                                            }
                                        }
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Safe vp70.<init> fallback for EXTRA_NAVIGATION_SOURCE if null
        // Class: Lvp70;
        // When initialized from SettingsActivity, EXTRA_NAVIGATION_SOURCE may be null.
        // Fall back to CALLS_TAB enum constant instead of throwing "Required value was null."
        try {
            val vp70Class = mutableClassDefBy("Lvp70;")
            for (method in vp70Class.methods) {
                if (method.name == "<init>") {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction && ins.reference.toString() == "EXTRA_NAVIGATION_SOURCE") {
                                // The subsequent move-result-object v1 gets the result from SavedStateHandle.
                                // Insert fallback right after move-result-object.
                                for (j in i + 1 until minOf(i + 4, instrs.size)) {
                                    val nextIns = instrs[j]
                                    if (nextIns.opcode.name.startsWith("move-result")) {
                                        method.addInstructions(
                                            j + 1,
                                            """
                                                if-eqz v1, :cond_has_nav_source
                                                sget-object v1, Lcom/truecaller/analytics/AppEvents${'$'}UsersHome${'$'}NavigationSource;->CALLS_TAB:Lcom/truecaller/analytics/AppEvents${'$'}UsersHome${'$'}NavigationSource;
                                                :cond_has_nav_source
                                            """.trimIndent(),
                                        )
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 5. Repurpose item_help (0x7f0a0c00) at bottom of Settings into "Your Truecaller stats"
        // Class: Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
        // Method: onViewCreated(View, Bundle)
        try {
            val categoriesClass = mutableClassDefBy("Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;")
            for (method in categoriesClass.methods) {
                if (method.name == "onViewCreated" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Landroid/view/View;"
                ) {
                    if (method.implementation != null) {
                        method.ensureRegisters(5)
                        method.addInstructions(
                            0,
                            """
                                move-object/from16 v0, p0
                                move-object/from16 v1, p1

                                const v2, 0x7f0a0c00
                                invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v2
                                if-eqz v2, :cond_skip_stats_setup
                                check-cast v2, Landroid/widget/TextView;

                                const-string v3, "Your Truecaller stats"
                                invoke-virtual {v2, v3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                new-instance v3, Ls3a;
                                const/16 v4, 0x8
                                invoke-direct {v3, v0, v4}, Ls3a;-><init>(Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;B)V
                                invoke-virtual {v2, v3}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V

                                :cond_skip_stats_setup
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
