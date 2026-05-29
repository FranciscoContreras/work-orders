plugins {
    java
}

group = "com.wearemachina"
version = "1.0.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // The Paper/Purpur 1.21.11 API exposes CopperGolem first-class (verified via javap).
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Unit tests run on plain JUnit 5 (no MockBukkit, no running server): we cover the pure logic that
    // fails silently if wrong — the tick scheduling math (Slice), the persistence codec round-trip and
    // fail-safety (GolemStateCodec), and the item filter. paper-api is on the test classpath only so the
    // Material enum those touch resolves; no Bukkit server is started.
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

// Compile targeting Java 21 with whatever JDK runs Gradle (here: dev/jdk-linux, JDK 21).
// No toolchain block on purpose — avoids auto-provisioning in this offline-ish environment.
// For publishing, a `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }` block is fine.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("WorkOrders")
}
