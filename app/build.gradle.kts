plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dependency.license.report)
}

val releaseStoreFilePath = providers.gradleProperty("ALFAJR_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("ALFAJR_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("ALFAJR_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("ALFAJR_RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("ALFAJR_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("ALFAJR_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("ALFAJR_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("ALFAJR_RELEASE_KEY_PASSWORD"))
val releaseSigningConfigured = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }

android {
    namespace = "io.github.sourcem7.alfajralarm"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.sourcem7.alfajralarm"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = file(releaseStoreFilePath.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.adhan)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
}

tasks.register("printVersionName") {
    group = "release"
    description = "Prints the Android version name for release-tag validation."
    inputs.property("versionName", android.defaultConfig.versionName.orEmpty())
    doLast {
        println(inputs.properties.getValue("versionName"))
    }
}

tasks.register("verifyReleaseManifestPrivacy") {
    group = "verification"
    description = "Fails when the merged release manifest requests a prohibited permission."
    dependsOn("processReleaseMainManifest")

    inputs.file(layout.buildDirectory.file(
        "intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml",
    ))
    inputs.property(
        "prohibitedPermissions",
        setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
        ),
    )

    doLast {
        val manifest = inputs.files.singleFile
        check(manifest.isFile) { "Merged release manifest was not generated: ${manifest.path}" }
        val prohibitedPermissions = inputs.properties.getValue("prohibitedPermissions") as Set<*>
        val declared = prohibitedPermissions.filter { permission ->
            manifest.readText().contains(permission.toString())
        }
        check(declared.isEmpty()) {
            "Release manifest requests prohibited permissions: ${declared.joinToString()}"
        }
    }
}
