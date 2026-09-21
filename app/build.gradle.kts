// Módulo de la app. Versiones verificadas 2026-09-20 contra maven-metadata.xml
// oficial (ver build.gradle.kts raíz) — Compose BOM 2026.09.00, Firebase BOM
// 34.19.0. minSdk 26 (Android 8.0) cubre 96.1% de dispositivos activos
// (Statcounter/apilevels.com, abril 2026).
// AGP 9.0+ trae soporte de Kotlin integrado — NO se aplica
// org.jetbrains.kotlin.android por separado (error real de compilación
// encontrado 2026-09-20, confirmado contra developer.android.com/build/
// releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin).
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hacz.jojmakabbalah"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hacz.jojmakabbalah"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
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

    // `kotlinOptions{}` no existe con Kotlin integrado en AGP 9 (error real
    // de compilación: "Unresolved reference 'kotlinOptions'") — jvmTarget
    // se deriva de compileOptions arriba.

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Sin sufijo -ktx: eliminados del BOM desde v34.0.0 (jul. 2025) — el
    // KTX quedó integrado en los módulos base. Error real de compilación
    // encontrado 2026-09-20 (dependencia no resoluble), confirmado contra
    // firebase.google.com/docs/android/learn-more.
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    // .await() sobre com.google.android.gms.tasks.Task — lo que devuelven
    // las llamadas de Firebase Storage/Firestore.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
