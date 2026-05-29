plugins {
    java
}

group = "com.wearemachina"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // The Paper/Purpur 1.21.11 API exposes CopperGolem first-class (verified via javap).
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

// Compile targeting Java 21 with whatever JDK runs Gradle (here: dev/jdk-linux, JDK 21).
// No toolchain block on purpose — avoids auto-provisioning in this offline-ish environment.
// For publishing, a `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }` block is fine.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("WorkOrders")
}
