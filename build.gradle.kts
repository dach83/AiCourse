plugins {
    kotlin("jvm") version "2.1.0"
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

sourceSets {
    main {
        // Include src/ so that src/Main.kt is part of the Gradle source set.
        // lesson1 files live under src/main/kotlin/lesson1/ which is a subdirectory of src/.
        kotlin.setSrcDirs(listOf("src"))
    }
}

kotlin {
    jvmToolchain(17)
}