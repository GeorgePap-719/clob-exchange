plugins {
    kotlin("jvm") version "1.9.22"
    application
}

application {
    mainClass.set("org.example.bitvavo.jvm.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}