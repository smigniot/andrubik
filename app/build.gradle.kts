plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.smigniot.andrubik"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.smigniot.andrubik"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        viewBinding = true
    }

    // Reproducible-build hardening: don't embed the dependency block (signed,
    // non-deterministic) into the APK. See F-Droid reproducible builds docs.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
}

// ---------------------------------------------------------------------------
// Web assets pipeline: bundles web/ (cubing.js scrambler) into
// app/src/main/assets/web/ via esbuild. Wired to run before the Android build.
// F-Droid runs the equivalent step via its build recipe (init/prebuild hooks).
// ---------------------------------------------------------------------------
val webDir = rootProject.file("web")
val webAssetsOut = layout.projectDirectory.dir("src/main/assets/web")

val installWeb = tasks.register<Exec>("installWeb") {
    workingDir = webDir
    commandLine("sh", "-c", "npm install --no-audit --no-fund --prefer-offline")
    inputs.file(webDir.resolve("package.json"))
    outputs.dir(webDir.resolve("node_modules"))
}

val buildWeb = tasks.register<Exec>("buildWeb") {
    dependsOn(installWeb)
    workingDir = webDir
    commandLine("sh", "-c", "npm run build")
    inputs.dir(webDir.resolve("src"))
    inputs.file(webDir.resolve("esbuild.config.mjs"))
    inputs.file(webDir.resolve("package.json"))
    outputs.dir(webAssetsOut)
}

// Make sure assets are bundled before Android merges them.
tasks.named("preBuild").configure {
    dependsOn(buildWeb)
}
