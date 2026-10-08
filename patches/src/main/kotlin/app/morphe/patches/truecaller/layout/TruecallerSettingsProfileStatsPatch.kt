package app.morphe.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.truecaller.shared.Constants.COMPATIBILITY_TRUECALLER
import app.morphe.patches.truecaller.shared.addCatchAll
import app.morphe.patches.truecaller.shared.ensureRegisters
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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
        // Cases:
        //  0  (0x0):  Profile card clicked -> EditProfileActivity
        //  88 (0x58): Share button clicked -> Launch share intent
        //  99 (0x63): Stats card clicked -> UsersHomeActivity
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
                                if-nez v1, :cond_check_share_click
                                iget-object v0, v0, Ls3a;->c:Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
                                invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                                move-result-object v0
                                new-instance v1, Landroid/content/Intent;
                                const-class v2, Lcom/truecaller/editprofile/impl/ui/EditProfileActivity;
                                invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                                invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                                return-void

                                :cond_check_share_click
                                # Case 88 (0x58): Share button clicked -> Intent.ACTION_SEND
                                const/16 v2, 0x58
                                if-ne v1, v2, :cond_check_stats_click
                                iget-object v0, v0, Ls3a;->c:Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
                                invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                                move-result-object v0
                                new-instance v1, Landroid/content/Intent;
                                const-string v2, "android.intent.action.SEND"
                                invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
                                const-string v2, "text/plain"
                                invoke-virtual {v1, v2}, Landroid/content/Intent;->setType(Ljava/lang/String;)Landroid/content/Intent;
                                const v2, 0x7f141933
                                invoke-virtual {v0, v2}, Landroid/content/Context;->getString(I)Ljava/lang/String;
                                move-result-object v2
                                const-string v3, "android.intent.extra.TEXT"
                                invoke-virtual {v1, v3, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
                                const/4 v2, 0x0
                                invoke-static {v1, v2}, Landroid/content/Intent;->createChooser(Landroid/content/Intent;Ljava/lang/CharSequence;)Landroid/content/Intent;
                                move-result-object v1
                                invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                                return-void

                                :cond_check_stats_click
                                # Case 99 (0x63): Stats card clicked -> UsersHomeActivity
                                const/16 v2, 0x63
                                if-ne v1, v2, :cond_normal_s3a_click
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

        // 3. Show % badge on Settings profile card ONLY (not in EditProfileActivity)
        // Class: Lv3a;
        // Method: emit(Ljava/lang/Object;Lorc;)Ljava/lang/Object;
        try {
            val v3aClass = mutableClassDefBy("Lv3a;")
            for (method in v3aClass.methods) {
                if (method.name == "emit" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Ljava/lang/Object;" &&
                    method.returnType == "Ljava/lang/Object;"
                ) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction && ins.reference.toString().endsWith("AvatarXView;->j(IZ)V")) {
                                if (ins is FiveRegisterInstruction) {
                                    val regProgress = ins.registerD
                                    val regBadge = ins.registerE
                                    method.addInstructions(
                                        i + 1,
                                        """
                                            const/4 v$regBadge, 0x0
                                        """.trimIndent(),
                                    )
                                    method.addInstructions(
                                        i,
                                        """
                                            const/4 v$regBadge, 0x1
                                        """.trimIndent(),
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 4. Proportional % badge dimensions on AvatarXView and Y position adjustment
        // Class: Lcom/truecaller/common/ui/avatar/AvatarXView;
        // Scale width N (44dp -> 28dp), height O (24dp -> 14dp), icon P (16dp -> 10dp), shadow Q (4dp -> 2dp)
        // Method: c(Canvas, RectF) -> shifts badge Y position upwards so % badge is fully visible without clipping
        try {
            val avatarClass = mutableClassDefBy("Lcom/truecaller/common/ui/avatar/AvatarXView;")
            for (method in avatarClass.methods) {
                if (method.name == "<init>") {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is OneRegisterInstruction && ins.opcode.name == "const/high16" &&
                                ins is NarrowLiteralInstruction
                            ) {
                                val reg = ins.registerA
                                when (ins.narrowLiteral) {
                                    0x42300000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x41e00000") // 44.0f -> 28.0f
                                    0x41c00000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x41600000") // 24.0f -> 14.0f
                                    0x41800000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x41200000") // 16.0f -> 10.0f
                                    0x40800000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x40000000") // 4.0f -> 2.0f
                                }
                            }
                        }
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

        // 5. Scale % badge text paint sizes (number: 16sp -> 10sp, % sign: 10sp -> 7sp)
        // Class: Lyw;
        // Method: invoke()Ljava/lang/Object;
        try {
            val ywClass = mutableClassDefBy("Lyw;")
            for (method in ywClass.methods) {
                if (method.name == "invoke" && method.parameterTypes.isEmpty() &&
                    method.returnType == "Ljava/lang/Object;"
                ) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is OneRegisterInstruction && ins.opcode.name == "const/high16" &&
                                ins is NarrowLiteralInstruction
                            ) {
                                val reg = ins.registerA
                                when (ins.narrowLiteral) {
                                    0x41800000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x41200000") // 16.0f sp -> 10.0f sp
                                    0x41200000 -> method.replaceInstruction(i, "const/high16 v$reg, 0x40e00000") // 10.0f sp -> 7.0f sp
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 6. Neutralize showBadge in EditProfile coroutine (Ldsf;)
        try {
            val dsfClass = mutableClassDefBy("Ldsf;")
            for (method in dsfClass.methods) {
                if (method.name == "invokeSuspend" && method.parameterTypes.size == 1 &&
                    method.parameterTypes[0] == "Ljava/lang/Object;"
                ) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction && ins.reference.toString().endsWith("AvatarXView;->j(IZ)V")) {
                                if (ins is FiveRegisterInstruction) {
                                    val regBadge = ins.registerE
                                    method.addInstructions(
                                        i,
                                        """
                                            const/4 v$regBadge, 0x0
                                        """.trimIndent(),
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 7. Safe vp70.<init> fallback for EXTRA_NAVIGATION_SOURCE if null
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

        // 8. 4-column Stats Card below About Truecaller & hide Help
        // Class: Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;
        // Method: onViewCreated(View, Bundle)
        try {
            val categoriesClass = mutableClassDefBy("Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;")
            for (method in categoriesClass.methods) {
                if (method.name == "onViewCreated" && method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == "Landroid/view/View;"
                ) {
                    if (method.implementation != null) {
                        val beforeCount = method.instructions.count()
                        method.addInstructions(
                            0,
                            """
                                move-object/from16 v0, p0
                                move-object/from16 v1, p1

                                # 1. Hide Help (0x7f0a0c00) and Help divider (0x7f0a0c01)
                                const v2, 0x7f0a0c00
                                invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v2
                                const/16 v3, 0x8
                                if-eqz v2, :cond_skip_hide_help
                                invoke-virtual {v2, v3}, Landroid/view/View;->setVisibility(I)V
                                :cond_skip_hide_help

                                const v2, 0x7f0a0c01
                                invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v2
                                if-eqz v2, :cond_skip_hide_div1
                                invoke-virtual {v2, v3}, Landroid/view/View;->setVisibility(I)V
                                :cond_skip_hide_div1

                                # 2. Locate parent outer layout (v2: item_about -> categories card -> outer layout)
                                const v2, 0x7f0a0bf4
                                invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v2
                                if-eqz v2, :cond_skip_all_stats
                                invoke-virtual {v2}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
                                move-result-object v2
                                check-cast v2, Landroid/view/ViewGroup;
                                if-eqz v2, :cond_skip_all_stats
                                invoke-virtual {v2}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
                                move-result-object v2
                                check-cast v2, Landroid/view/ViewGroup;
                                if-eqz v2, :cond_skip_all_stats

                                # Check if stats card already added (0x7f0a0c99)
                                const v3, 0x7f0a0c99
                                invoke-virtual {v2, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v3
                                if-nez v3, :cond_skip_all_stats

                                invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;
                                move-result-object v1
                                invoke-virtual {v1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                                move-result-object v3
                                invoke-virtual {v3}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
                                move-result-object v3
                                iget v3, v3, Landroid/util/DisplayMetrics;->density:F

                                const/high16 v4, 0x41800000
                                mul-float v4, v4, v3
                                float-to-int v4, v4
                                const/high16 v5, 0x41400000
                                mul-float v5, v5, v3
                                float-to-int v5, v5
                                const/high16 v6, 0x41c00000
                                mul-float v3, v6, v3
                                float-to-int v3, v3

                                # 3. Stats Card container (LinearLayout)
                                new-instance v6, Landroid/widget/LinearLayout;
                                invoke-direct {v6, v1}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                                const v7, 0x7f0a0c99
                                invoke-virtual {v6, v7}, Landroid/view/View;->setId(I)V
                                const/4 v7, 0x1
                                invoke-virtual {v6, v7}, Landroid/widget/LinearLayout;->setOrientation(I)V
                                const v7, 0x7f08026d
                                invoke-virtual {v6, v7}, Landroid/view/View;->setBackgroundResource(I)V

                                new-instance v7, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v8, -0x1
                                const/4 v9, -0x2
                                invoke-direct {v7, v8, v9}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                iput v4, v7, Landroid/widget/LinearLayout${'$'}LayoutParams;->topMargin:I
                                iput v3, v7, Landroid/widget/LinearLayout${'$'}LayoutParams;->bottomMargin:I
                                invoke-virtual {v6, v7}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                invoke-virtual {v6, v4, v4, v4, v4}, Landroid/view/View;->setPadding(IIII)V

                                new-instance v7, Ls3a;
                                const/16 v8, 0x63
                                invoke-direct {v7, v0, v8}, Ls3a;-><init>(Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;B)V
                                invoke-virtual {v6, v7}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V

                                # 4. Header Row
                                new-instance v7, Landroid/widget/LinearLayout;
                                invoke-direct {v7, v1}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                                const/4 v8, 0x0
                                invoke-virtual {v7, v8}, Landroid/widget/LinearLayout;->setOrientation(I)V
                                const/16 v8, 0x10
                                invoke-virtual {v7, v8}, Landroid/widget/LinearLayout;->setGravity(I)V
                                new-instance v8, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v9, -0x1
                                const/4 v10, -0x2
                                invoke-direct {v8, v9, v10}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                iput v4, v8, Landroid/widget/LinearLayout${'$'}LayoutParams;->bottomMargin:I
                                invoke-virtual {v7, v8}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                                # Bar chart icon
                                new-instance v8, Landroid/widget/ImageView;
                                invoke-direct {v8, v1}, Landroid/widget/ImageView;-><init>(Landroid/content/Context;)V
                                const v9, 0x7f080840
                                invoke-virtual {v8, v9}, Landroid/widget/ImageView;->setImageResource(I)V
                                new-instance v9, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                invoke-direct {v9, v3, v3}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                iput v5, v9, Landroid/widget/LinearLayout${'$'}LayoutParams;->rightMargin:I
                                invoke-virtual {v8, v9}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                invoke-virtual {v7, v8}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                # Title
                                new-instance v8, Landroid/widget/TextView;
                                invoke-direct {v8, v1}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
                                const v9, 0x7f141c58
                                invoke-virtual {v8, v9}, Landroid/widget/TextView;->setText(I)V
                                const/4 v9, 0x1
                                const/high16 v10, 0x41880000
                                invoke-virtual {v8, v9, v10}, Landroid/widget/TextView;->setTextSize(IF)V
                                sget-object v9, Landroid/graphics/Typeface;->DEFAULT_BOLD:Landroid/graphics/Typeface;
                                invoke-virtual {v8, v9}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
                                const v9, 0x7f1503b2
                                invoke-virtual {v8, v9}, Landroid/widget/TextView;->setTextAppearance(I)V
                                new-instance v9, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v10, 0x0
                                const/4 v11, -0x2
                                const/high16 v12, 0x3f800000
                                invoke-direct {v9, v10, v11, v12}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
                                invoke-virtual {v7, v8, v9}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V

                                # Share icon
                                new-instance v8, Landroid/widget/ImageView;
                                invoke-direct {v8, v1}, Landroid/widget/ImageView;-><init>(Landroid/content/Context;)V
                                const v9, 0x7f080b3e
                                invoke-virtual {v8, v9}, Landroid/widget/ImageView;->setImageResource(I)V
                                const v9, -0xa09b93
                                invoke-virtual {v8, v9}, Landroid/widget/ImageView;->setColorFilter(I)V
                                new-instance v9, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                invoke-direct {v9, v3, v3}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                invoke-virtual {v8, v9}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                new-instance v9, Ls3a;
                                const/16 v10, 0x58
                                invoke-direct {v9, v0, v10}, Ls3a;-><init>(Lcom/truecaller/settings/impl/ui/categories/CategoriesFragment;B)V
                                invoke-virtual {v8, v9}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
                                invoke-virtual {v7, v8}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                invoke-virtual {v6, v7}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                # 5. Inflater
                                invoke-static {v1}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;
                                move-result-object v0

                                # 6. Row 1
                                new-instance v5, Landroid/widget/LinearLayout;
                                invoke-direct {v5, v1}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                                const/4 v7, 0x0
                                invoke-virtual {v5, v7}, Landroid/widget/LinearLayout;->setOrientation(I)V
                                new-instance v7, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v8, -0x1
                                const/4 v9, -0x2
                                invoke-direct {v7, v8, v9}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                iput v4, v7, Landroid/widget/LinearLayout${'$'}LayoutParams;->bottomMargin:I
                                invoke-virtual {v5, v7}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                                # Item 1: Spam calls
                                const v7, 0x7f0d09bd
                                const/4 v8, 0x0
                                invoke-virtual {v0, v7, v5, v8}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
                                move-result-object v7
                                invoke-virtual {v7, v8}, Landroid/view/View;->setBackgroundColor(I)V
                                new-instance v9, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v10, 0x0
                                const/4 v11, -0x2
                                const/high16 v12, 0x3f800000
                                invoke-direct {v9, v10, v11, v12}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
                                invoke-virtual {v5, v7, v9}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                const v9, 0x7f0a0b0c
                                invoke-virtual {v7, v9}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v9
                                check-cast v9, Landroid/widget/ImageView;
                                if-nez v9, :cond_item1_img_done
                                goto :cond_item1_sub_setup
                                :cond_item1_img_done
                                const v10, 0x7f080a4d
                                invoke-virtual {v9, v10}, Landroid/widget/ImageView;->setImageResource(I)V
                                const v10, -0xf59301
                                invoke-virtual {v9, v10}, Landroid/widget/ImageView;->setColorFilter(I)V
                                :cond_item1_sub_setup
                                const v9, 0x7f0a136d
                                invoke-virtual {v7, v9}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v9
                                check-cast v9, Landroid/widget/TextView;
                                if-nez v9, :cond_item1_sub_done
                                goto :cond_item1_val_setup
                                :cond_item1_sub_done
                                const v10, 0x7f141934
                                invoke-virtual {v9, v10}, Landroid/widget/TextView;->setText(I)V
                                :cond_item1_val_setup
                                const v9, 0x7f0a14b7
                                invoke-virtual {v7, v9}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v9
                                check-cast v9, Landroid/widget/TextView;
                                const-string v7, "0"
                                if-nez v9, :cond_item1_val_done
                                goto :cond_item2_setup
                                :cond_item1_val_done
                                invoke-virtual {v9, v7}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                # Item 2: Time saved
                                :cond_item2_setup
                                const v7, 0x7f0d09bd
                                const/4 v8, 0x0
                                invoke-virtual {v0, v7, v5, v8}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
                                move-result-object v7
                                invoke-virtual {v7, v8}, Landroid/view/View;->setBackgroundColor(I)V
                                new-instance v8, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v10, 0x0
                                const/4 v11, -0x2
                                const/high16 v12, 0x3f800000
                                invoke-direct {v8, v10, v11, v12}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
                                invoke-virtual {v5, v7, v8}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                const v8, 0x7f0a0b0c
                                invoke-virtual {v7, v8}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v8
                                check-cast v8, Landroid/widget/ImageView;
                                if-nez v8, :cond_item2_img_done
                                goto :cond_item2_sub_setup
                                :cond_item2_img_done
                                const v10, 0x7f080a4b
                                invoke-virtual {v8, v10}, Landroid/widget/ImageView;->setImageResource(I)V
                                const v10, -0xf59301
                                invoke-virtual {v8, v10}, Landroid/widget/ImageView;->setColorFilter(I)V
                                :cond_item2_sub_setup
                                const v8, 0x7f0a136d
                                invoke-virtual {v7, v8}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v8
                                check-cast v8, Landroid/widget/TextView;
                                if-nez v8, :cond_item2_sub_done
                                goto :cond_item2_val_setup
                                :cond_item2_sub_done
                                const v10, 0x7f141936
                                invoke-virtual {v8, v10}, Landroid/widget/TextView;->setText(I)V
                                :cond_item2_val_setup
                                const v8, 0x7f0a14b7
                                invoke-virtual {v7, v8}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v7
                                check-cast v7, Landroid/widget/TextView;
                                const-string v8, "0s"
                                if-nez v7, :cond_item2_val_done
                                goto :cond_row1_done
                                :cond_item2_val_done
                                invoke-virtual {v7, v8}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_row1_done
                                invoke-virtual {v6, v5}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                # 7. Row 2
                                new-instance v5, Landroid/widget/LinearLayout;
                                invoke-direct {v5, v1}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
                                const/4 v8, 0x0
                                invoke-virtual {v5, v8}, Landroid/widget/LinearLayout;->setOrientation(I)V
                                new-instance v8, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v10, -0x1
                                const/4 v11, -0x2
                                invoke-direct {v8, v10, v11}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
                                invoke-virtual {v5, v8}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

                                # Item 3: Unknown numbers
                                const v8, 0x7f0d09bd
                                const/4 v10, 0x0
                                invoke-virtual {v0, v8, v5, v10}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
                                move-result-object v8
                                invoke-virtual {v8, v10}, Landroid/view/View;->setBackgroundColor(I)V
                                new-instance v10, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v11, 0x0
                                const/4 v12, -0x2
                                const/high16 v13, 0x3f800000
                                invoke-direct {v10, v11, v12, v13}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
                                invoke-virtual {v5, v8, v10}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                const v10, 0x7f0a0b0c
                                invoke-virtual {v8, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v10
                                check-cast v10, Landroid/widget/ImageView;
                                if-nez v10, :cond_item3_img_done
                                goto :cond_item3_sub_setup
                                :cond_item3_img_done
                                const v11, 0x7f080a4f
                                invoke-virtual {v10, v11}, Landroid/widget/ImageView;->setImageResource(I)V
                                const v11, -0xf59301
                                invoke-virtual {v10, v11}, Landroid/widget/ImageView;->setColorFilter(I)V
                                :cond_item3_sub_setup
                                const v10, 0x7f0a136d
                                invoke-virtual {v8, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v10
                                check-cast v10, Landroid/widget/TextView;
                                if-nez v10, :cond_item3_sub_done
                                goto :cond_item3_val_setup
                                :cond_item3_sub_done
                                const v11, 0x7f141938
                                invoke-virtual {v10, v11}, Landroid/widget/TextView;->setText(I)V
                                :cond_item3_val_setup
                                const v10, 0x7f0a14b7
                                invoke-virtual {v8, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v8
                                check-cast v8, Landroid/widget/TextView;
                                const-string v10, "0"
                                if-nez v8, :cond_item3_val_done
                                goto :cond_item4_setup
                                :cond_item3_val_done
                                invoke-virtual {v8, v10}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                # Item 4: Spam messages
                                :cond_item4_setup
                                const v10, 0x7f0d09bd
                                const/4 v11, 0x0
                                invoke-virtual {v0, v10, v5, v11}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
                                move-result-object v0
                                invoke-virtual {v0, v11}, Landroid/view/View;->setBackgroundColor(I)V
                                new-instance v10, Landroid/widget/LinearLayout${'$'}LayoutParams;
                                const/4 v11, 0x0
                                const/4 v12, -0x2
                                const/high16 v13, 0x3f800000
                                invoke-direct {v10, v11, v12, v13}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
                                invoke-virtual {v5, v0, v10}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup${'$'}LayoutParams;)V
                                const v10, 0x7f0a0b0c
                                invoke-virtual {v0, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v10
                                check-cast v10, Landroid/widget/ImageView;
                                if-nez v10, :cond_item4_img_done
                                goto :cond_item4_sub_setup
                                :cond_item4_img_done
                                const v11, 0x7f080a4e
                                invoke-virtual {v10, v11}, Landroid/widget/ImageView;->setImageResource(I)V
                                const v11, -0xf59301
                                invoke-virtual {v10, v11}, Landroid/widget/ImageView;->setColorFilter(I)V
                                :cond_item4_sub_setup
                                const v10, 0x7f0a136d
                                invoke-virtual {v0, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v10
                                check-cast v10, Landroid/widget/TextView;
                                if-nez v10, :cond_item4_sub_done
                                goto :cond_item4_val_setup
                                :cond_item4_sub_done
                                const v11, 0x7f141935
                                invoke-virtual {v10, v11}, Landroid/widget/TextView;->setText(I)V
                                :cond_item4_val_setup
                                const v10, 0x7f0a14b7
                                invoke-virtual {v0, v10}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                                move-result-object v0
                                check-cast v0, Landroid/widget/TextView;
                                const-string v10, "0"
                                if-nez v0, :cond_item4_val_done
                                goto :cond_row2_done
                                :cond_item4_val_done
                                invoke-virtual {v0, v10}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_row2_done
                                invoke-virtual {v6, v5}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                # 8. Add card to outer layout!
                                invoke-virtual {v2, v6}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

                                # 9. Dynamic queries
                                invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;
                                move-result-object v1
                                invoke-static {}, Luk60;->g()Landroid/net/Uri;
                                move-result-object v2
                                const-string v3, "COUNT()"
                                const-string v4, "contact_spam_type IS NOT NULL AND type IN (2,1,3)"
                                const/4 v5, 0x0
                                const/4 v6, 0x0
                                invoke-static/range {v1 .. v6}, Lsz0;->B(Landroid/content/ContentResolver;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Integer;
                                move-result-object v4
                                if-eqz v4, :cond_query_unknown
                                invoke-virtual {v4}, Ljava/lang/Integer;->intValue()I
                                move-result v4
                                invoke-static {v4}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                                move-result-object v5
                                if-eqz v9, :cond_calc_time
                                invoke-virtual {v9, v5}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_calc_time
                                int-to-float v5, v4
                                const v6, 0x41eb3333
                                mul-float v5, v5, v6
                                float-to-int v5, v5

                                const/16 v6, 0x3c
                                if-ge v5, v6, :cond_check_hour
                                new-instance v6, Ljava/lang/StringBuilder;
                                invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
                                move-result-object v6
                                const-string v5, "s"
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v6
                                invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                                move-result-object v5
                                goto :cond_set_time

                                :cond_check_hour
                                const/16 v10, 0xe10
                                if-ge v5, v10, :cond_is_hours
                                div-int/2addr v5, v6
                                new-instance v6, Ljava/lang/StringBuilder;
                                invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
                                move-result-object v6
                                const-string v5, "m"
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v6
                                invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                                move-result-object v5
                                goto :cond_set_time

                                :cond_is_hours
                                div-int/2addr v5, v10
                                new-instance v6, Ljava/lang/StringBuilder;
                                invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
                                move-result-object v6
                                const-string v5, "h"
                                invoke-virtual {v6, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v6
                                invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                                move-result-object v5

                                :cond_set_time
                                if-eqz v7, :cond_query_unknown
                                invoke-virtual {v7, v5}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_query_unknown
                                invoke-static {}, Luk60;->g()Landroid/net/Uri;
                                move-result-object v2
                                const-string v3, "COUNT()"
                                const-string v4, "contact_phonebook_id IS NULL AND contact_name IS NOT NULL AND contact_name != ''"
                                const/4 v5, 0x0
                                const/4 v6, 0x0
                                invoke-static/range {v1 .. v6}, Lsz0;->B(Landroid/content/ContentResolver;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Integer;
                                move-result-object v2
                                if-eqz v2, :cond_query_spam_msgs
                                invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I
                                move-result v2
                                invoke-static {v2}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                                move-result-object v2
                                if-eqz v8, :cond_query_spam_msgs
                                invoke-virtual {v8, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_query_spam_msgs
                                const-string v2, "content://com.truecaller/messages_moved_to_spam_query"
                                invoke-static {v2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                                move-result-object v2
                                const-string v3, "count"
                                const/4 v4, 0x0
                                const/4 v5, 0x0
                                const/4 v6, 0x0
                                invoke-static/range {v1 .. v6}, Lsz0;->B(Landroid/content/ContentResolver;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Integer;
                                move-result-object v1
                                if-eqz v1, :cond_skip_all_stats
                                invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I
                                move-result v1
                                invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
                                move-result-object v1
                                if-eqz v0, :cond_skip_all_stats
                                invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

                                :cond_skip_all_stats
                                goto :stats_safe_exit
                                :stats_safe_handler
                                move-exception v0
                                :stats_safe_exit
                            """.trimIndent(),
                        )
                        val afterCount = method.instructions.count()
                        val added = afterCount - beforeCount
                        if (added >= 3) {
                            method.addCatchAll(0, added - 2, added - 1)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 9. Neutralize showBadge in EditProfile Composable avatar lambda (Ls9f;)
        try {
            val s9fClass = mutableClassDefBy("Ls9f;")
            for (method in s9fClass.methods) {
                if (method.name == "invoke" && method.parameterTypes.size == 1) {
                    if (method.implementation != null) {
                        val instrs = method.instructions.toList()
                        for (i in instrs.indices) {
                            val ins = instrs[i]
                            if (ins is ReferenceInstruction && ins.reference.toString().endsWith("AvatarXView;->j(IZ)V")) {
                                if (ins is FiveRegisterInstruction) {
                                    val regBadge = ins.registerE
                                    method.addInstructions(
                                        i,
                                        """
                                            const/4 v$regBadge, 0x0
                                        """.trimIndent(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 10. Remove "Fill in with Google" button in Edit Profile (Le9h; - EditProfileUiState)
        try {
            val e9hClass = mutableClassDefBy("Le9h;")
            for (method in e9hClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 11) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p5, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }

        // 11. Remove "Fill in with Google" button in Create Profile (Luhd; - CreateProfileUiState)
        try {
            val uhdClass = mutableClassDefBy("Luhd;")
            for (method in uhdClass.methods) {
                if (method.name == "<init>" && method.parameterTypes.size == 7) {
                    if (method.implementation != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 p2, 0x0
                            """.trimIndent(),
                        )
                    }
                }
            }
        } catch (_: Exception) { /* fail-soft */ }
    }
}
