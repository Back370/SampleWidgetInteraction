plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.seo4d696b75.android.glance_widget_demo.widget"
    compileSdk = 35

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}
kotlin {
    jvmToolchain(17) // Specify the desired Java version for the toolchain
}

dependencies {

    implementation(libs.androidx.core)
    implementation(libs.coroutines)
    implementation(libs.glide)
    
    // Hilt for dependency injection
    implementation(libs.hilt.android)
    implementation(libs.play.services.basement)
    ksp(libs.hilt.android.compiler)

    // Google Sign-In for calendar integration
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation(project(":domain"))
    implementation(project(":data"))

    // coreモジュールへの依存関係を追加
    implementation(project(":core"))
}