// Top-level build file — declara los plugins usados por los módulos sin
// aplicarlos aquí (apply false). Versiones verificadas 2026-09-20 contra
// maven-metadata.xml oficial (dl.google.com / repo1.maven.org), no
// búsqueda aproximada ni memoria: AGP 9.4.1, Kotlin 2.4.20 (ya era la
// última estable), Google Services plugin 4.5.0.
// org.jetbrains.kotlin.android NO se declara — AGP 9.0+ trae soporte de
// Kotlin integrado, aplicarlo por separado rompe la compilación (ver
// comentario en app/build.gradle.kts).
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
