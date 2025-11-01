plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.grigri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"

dependencies {
    // Google Calendar API
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    // Command line parsing
    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    // Date/time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Koog AI SDK
    implementation("ai.koog:koog-agents:0.1.0")
    // Ktor client for Koog (required explicitly)
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("dev.grigri.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}