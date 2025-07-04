plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core + Engine
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-call-logging:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-server-cors:2.3.0")

    // Kotlinx JSON serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Exposed & PostgreSQL
    implementation("org.jetbrains.exposed:exposed-core:0.43.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.43.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.43.0")
    implementation("org.postgresql:postgresql:42.6.0")

    // Logging Implementation (ADD THIS LINE)
    implementation("ch.qos.logback:logback-classic:1.4.7") // Or the latest stable version

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.ApplicationKt")
}

kotlin {
    jvmToolchain(17) // Set the JVM toolchain to Java 17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
