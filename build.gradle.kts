plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "hilman.ai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.kwhat:jnativehook:2.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-cio:3.3.3")
    implementation("io.ktor:ktor-server-call-logging:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-jackson:3.3.3")
    implementation("io.ktor:ktor-server-cors:3.3.3")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("com.google.genai:google-genai:1.28.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

// ✅ Fix: shadowJar block must be inside tasks
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("my-fat-jar")
    archiveClassifier.set("") // No -all suffix
    archiveVersion.set("1.0.0")

    // Optional: specify main class if you want to make the jar executable
    manifest {
        attributes(mapOf("Main-Class" to "GlobalKeyListenerExample"))
    }
}