import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    java
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.spongepowered.org/maven")
}

dependencies {
    //compileOnly(kotlin("stdlib"))
    //compileOnly(kotlin("reflect"))

    //implementation("com.github.EmortalMC:Rayfast:07d8daf030")
    compileOnly("com.github.Minestom:Minestom:367c389bc6")
    compileOnly("com.github.EmortalMC:Immortal:eab3ac8420")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
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