// Standalone JVM-only build for scoring module.
// Runs `./gradlew :scoring-jvm:test` with no Android SDK.
plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.enterprise.riskauditor"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}
