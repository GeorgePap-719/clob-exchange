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
    // For info on test execution.
    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        // Only execute on the outermost suite.
        if (desc.parent == null) {
            println("Tests: ${result.testCount}")
            println("Passed: ${result.successfulTestCount}")
            println("Failed: ${result.failedTestCount}")
            println("Skipped: ${result.skippedTestCount}")
        }
    }))
}

kotlin {
    jvmToolchain(17)
}