import java.io.FileInputStream
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services" )

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    //alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.seo4d696b75.android.glance_widget_demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seo4d696b75.android.glance_widget_demo"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties") // project.rootProject.file() もOK
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { input ->
                properties.load(input)
            }
        }

        val apiKey = properties["apiKey"] as String? ?: "DEFAULT_API_KEY_IF_NOT_FOUND"
        buildConfigField ("String", "apiKey", "\"$apiKey\"")
    }



    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/androidx.compose.material3_material3.version"
        }
    }
}
kotlin {
    jvmToolchain(17) // Specify the desired Java version for the toolchain
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.google.firebase.appcheck.playintegrity)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)

    implementation(project(":ui"))
    implementation(project(":widget"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":theme"))

    // coreモジュールへの依存関係を追加
    implementation(project(":core"))

    implementation(libs.hilt.android)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.material3)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    testImplementation(libs.junit)
    //androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    implementation("androidx.work:work-rxjava2:2.10.2")

    implementation("com.google.code.gson:gson:2.10.1")

}