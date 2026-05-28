plugins {
    java
}

group = "com.wearemachina"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API — exposes CopperGolem first-class (verified via javap). This 1.21.x API
    // artifact is binary-compatible with the 26.1.x server runtime (stable Bukkit API).
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

// Compiles for Java 21 (`release 21`) using whatever JDK 21+ runs Gradle.
// To pin the JDK for contributors instead, add:
//   java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("WorkOrders")
}
