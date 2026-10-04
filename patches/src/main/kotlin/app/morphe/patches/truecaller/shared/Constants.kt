package app.morphe.patches.truecaller.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val PACKAGE_NAME = "com.truecaller"

    val COMPATIBILITY_TRUECALLER = Compatibility(
        name = "Truecaller",
        packageName = PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x005AF0,
        targets = listOf(
            AppTarget(version = "26.10.6", minSdk = 24),
            AppTarget(version = "26.12.5", minSdk = 24),
            AppTarget(version = "26.39.6", minSdk = 24),
        )
    )
}
