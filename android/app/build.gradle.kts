import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.reader(Charsets.UTF_8).use(::load)
    }
}

fun releaseSigningValue(environmentName: String, propertyName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseSigningValues = mapOf(
    "storeFile" to releaseSigningValue("RELEASE_STORE_FILE", "storeFile"),
    "storePassword" to releaseSigningValue("RELEASE_STORE_PASSWORD", "storePassword"),
    "keyAlias" to releaseSigningValue("RELEASE_KEY_ALIAS", "keyAlias"),
    "keyPassword" to releaseSigningValue("RELEASE_KEY_PASSWORD", "keyPassword")
)
val releaseKeystore = releaseSigningValues["storeFile"]?.let { rootProject.file(it) }

val checkReleaseSigningConfiguration by tasks.registering {
    group = "verification"
    description = "Check local or CI release signing without displaying credentials."
    doLast {
        val missing = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys
        check(missing.isEmpty()) {
            "Release signing is incomplete. Fill ${missing.joinToString()} in android/keystore.properties, or set the matching RELEASE_* environment variables."
        }
        check(releaseKeystore?.isFile == true) { "The configured release keystore was not found." }
    }
}

tasks.matching { it.name in setOf("packageRelease", "packageReleaseBundle", "signReleaseBundle") }.configureEach {
    dependsOn(checkReleaseSigningConfiguration)
}

val releaseVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}

fun resolveConfigValue(name: String, fallback: String): String {
    val fromLocal = localProperties.getProperty(name)
    val fromGradle = providers.gradleProperty(name).orNull
    return when {
        !fromLocal.isNullOrBlank() -> fromLocal
        !fromGradle.isNullOrBlank() -> fromGradle
        else -> fallback
    }
}

fun quotedConfig(name: String, fallback: String = ""): String =
    "\"" + resolveConfigValue(name, fallback).replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.bdbshs.crest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bdbshs.crest"
        minSdk = 24
        targetSdk = 35
        versionCode = releaseVersion.getProperty("versionCode").toInt()
        versionName = releaseVersion.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "STORAGE_PROVIDER", quotedConfig("STORAGE_PROVIDER", "supabase"))
        buildConfigField("String", "SUPABASE_URL", quotedConfig("SUPABASE_URL", "https://mkmirlnypehadajrtxvg.supabase.co"))
        buildConfigField("boolean", "FCM_ENABLED", (resolveConfigValue("FCM_ENABLED", "false") == "true").toString())

        buildConfigField(
            "String",
            "APPWRITE_ENDPOINT",
            "\"${resolveConfigValue("APPWRITE_ENDPOINT", "https://fra.cloud.appwrite.io/v1")}\""
        )
        buildConfigField(
            "String",
            "APPWRITE_PROJECT_ID",
            "\"${resolveConfigValue("APPWRITE_PROJECT_ID", "686a25c60006e47cfbea")}\""
        )
        buildConfigField(
            "String",
            "APPWRITE_BUCKET_ID",
            "\"${resolveConfigValue("APPWRITE_BUCKET_ID", "686a262b0024b8e10a35")}\""
        )
    }

    signingConfigs {
        create("release") {
            // CI environment values take priority over the ignored local file.
            storeFile = releaseKeystore
            storePassword = releaseSigningValues["storePassword"]
            keyAlias = releaseSigningValues["keyAlias"]
            keyPassword = releaseSigningValues["keyPassword"]
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-dev"
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            pickFirsts.add("META-INF/LICENSE.md")
            pickFirsts.add("META-INF/LICENSE-notice.md")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material)
    implementation(libs.sdk.for1.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    
    // Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    
    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.coil.compose)
}

kapt {
    correctErrorTypes = true
}
