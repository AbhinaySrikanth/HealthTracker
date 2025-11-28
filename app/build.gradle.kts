plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    }

android {
    namespace = "com.example.healthtracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.healthtracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "2.0"
    }
}

dependencies {

    // COMPOSE
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material:1.9.0")
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // HILT (KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ROOM (KSP)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // NETWORK
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)

    // WORK MANAGER
    implementation(libs.work.runtime.ktx)

    // SECURITY
    implementation(libs.security.crypto)

    // LIFECYCLE + COROUTINES
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // GOOGLE FIT
    implementation(libs.play.services.fitness)
    // GOOGLE SIGN-IN
    implementation("com.google.android.gms:play-services-auth:21.3.0")

}
