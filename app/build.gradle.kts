import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signing = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(name: String): String? = System.getenv(name) ?: signing.getProperty(name)
val hasSigning = listOf("KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").all { !secret(it).isNullOrBlank() }

android {
    namespace = "com.deploydulupulangnanti.asmrcuttingfruit"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.deploydulupulangnanti.asmrcuttingfruit"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (hasSigning) create("upload") {
            storeFile = file(secret("KEYSTORE_PATH")!!)
            storePassword = secret("KEYSTORE_PASSWORD")
            keyAlias = secret("KEY_ALIAS")
            keyPassword = secret("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasSigning) signingConfig = signingConfigs.getByName("upload")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

tasks.register("requireReleaseSigning") {
    doLast { check(hasSigning) { "Release signing is missing. Configure the four signing values; see README.md." } }
}
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach { dependsOn("requireReleaseSigning") }

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
