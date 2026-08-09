plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val tapLockVersionCode =
    (findProperty("tapLockVersionCode") as String?)?.toInt()
        ?: error("Set tapLockVersionCode in gradle.properties")
val tapLockVersionName =
    findProperty("tapLockVersionName") as String?
        ?: error("Set tapLockVersionName in gradle.properties")

android {
    namespace = "com.taplock.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taplock.app"
        minSdk = 28
        targetSdk = 35
        versionCode = tapLockVersionCode
        versionName = tapLockVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            val version = defaultConfig.versionName
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                .outputFileName = "TapLock-$version.apk"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.7.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.2")
}


val debugApkFile =
    layout.buildDirectory.file("outputs/apk/debug/TapLock-$tapLockVersionName.apk")
val releaseApkFile =
    layout.buildDirectory.file("outputs/apk/release/TapLock-$tapLockVersionName.apk")

tasks.register("printVersionName") {
    group = "version"
    description = "Print tapLockVersionName from gradle.properties"
    doLast { println(tapLockVersionName) }
}

tasks.register("printDebugApkPath") {
    group = "version"
    description = "Print the debug APK path for the current version"
    doLast { println(debugApkFile.get().asFile.absolutePath) }
}

tasks.register("printReleaseApkPath") {
    group = "version"
    description = "Print the release APK path for the current version"
    doLast { println(releaseApkFile.get().asFile.absolutePath) }
}

tasks.register<Exec>("installTapLockRelease") {
    group = "install"
    description = "Build and install the release APK via adb"
    dependsOn("assembleRelease")
    commandLine("adb", "install", "-r", releaseApkFile.get().asFile.absolutePath)
}

tasks.register<Exec>("installTapLockDebug") {
    group = "install"
    description = "Build and install the debug APK via adb"
    dependsOn("assembleDebug")
    commandLine("adb", "install", "-r", debugApkFile.get().asFile.absolutePath)
}

tasks.register("grantTapLockPermissions") {
    group = "install"
    description = "Grant ghost-mode permissions via adb (run after install)"
    mustRunAfter("installTapLockDebug")
    doLast {
        exec {
            commandLine(
                "adb", "shell", "pm", "grant", "com.taplock.app",
                "android.permission.WRITE_SECURE_SETTINGS"
            )
        }
        exec {
            commandLine(
                "adb", "shell", "cmd", "appops", "set",
                "com.taplock.app", "ACCESS_RESTRICTED_SETTINGS", "allow"
            )
        }
    }
}

tasks.register("deployTapLockDebug") {
    group = "install"
    description = "Build, install debug APK, and grant ghost-mode permissions"
    dependsOn("installTapLockDebug", "grantTapLockPermissions")
}
