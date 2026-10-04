group = "app.morphe"

patches {
    about {
        name = "Truecaller Minimal Privacy Patches"
        description = "Surgical zero-telemetry and privacy patches for Truecaller, preserving core caller ID."
        source = "https://github.com/Akash-Sriram/Morphe"
        author = "Akash-Sriram"
        contact = "na"
        website = "https://github.com/Akash-Sriram/Morphe"
        license = "GPL-3.0"
    }
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("${rootDir}/local-repo") }
    maven { url = uri("https://jitpack.io") }
}

val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

configurations.all {
    resolutionStrategy {
        force(libs.guava)
        force("com.google.errorprone:error_prone_annotations:2.50.0")
    }
}

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
    compileOnly(libs.guava)
    compileOnly(project(":patches:stub"))
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers", "-Xskip-metadata-version-check")
    }
}
