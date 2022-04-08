import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    application
}

group = "net.yusukezzz.javalin-restart-demo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:4.4.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}

// This kind of processing will often be a gradle plugin (This is just an example).
tasks.register("processFooResources") {
    // This will make gradle aware of the path and trigger a continuous build run.
    inputs.dir("src/main/foo")
    doFirst {
        println("Do process files here...")
    }
}

// In most cases, the gradle plugin will set this implicitly, so you don't need to worry about it.
// But if it doesn't work as expected, may have to define dependency yourself.
tasks.compileKotlin {
    dependsOn(tasks.named("processFooResources"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

tasks.wrapper {
    gradleVersion = "7.4"
    distributionType = Wrapper.DistributionType.ALL
}