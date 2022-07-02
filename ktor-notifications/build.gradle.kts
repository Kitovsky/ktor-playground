import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.archivesName

val ktorVersion = "2.0.3"
val kotlinVersion = "1.7.0"
val kotlinxSerializationVersion = "1.3.3"
val koinVersion = "3.2.0"
val kafkaVersion = "3.2.0"
val logbackVersion = "1.2.11"
val muVersion = "2.1.23"

plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
}

application {
    mainClass.set("com.lonerx.ApplicationKt")
}

tasks {
    shadowJar {
        archiveBaseName.set("service")
        archiveClassifier.set("all")
        archiveVersion.set("")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    implementation("io.github.microutils:kotlin-logging:$muVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
    config = files("$projectDir/config/detekt.yml") // point to your custom config defining rules to run
    baseline = file("$projectDir/config/baseline.xml") // a way of suppressing issues before introducing detekt
}