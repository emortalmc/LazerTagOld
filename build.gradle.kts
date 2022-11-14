import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    java
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://jitpack.io")
}

dependencies {
    //compileOnly(kotlin("stdlib"))
    //compileOnly(kotlin("reflect"))

    //implementation("com.github.EmortalMC:Rayfast:07d8daf030")
    compileOnly("com.github.Minestom:Minestom:c3df2af306")
//    compileOnly("com.github.EmortalMC:Immortal:29cfc5b6be")
    compileOnly("dev.emortal.immortal:Immortal:3.0.1")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

tasks {
    processResources {
        filesMatching("extension.json") {
            expand(project.properties)
        }
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        mergeServiceFiles()
        minimize()
    }

    build { dependsOn(shadowJar) }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}