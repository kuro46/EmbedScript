import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    java
    maven
}

group = "com.github.kuro46"
version = "0.7.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compileOnly("org.spigotmc", "spigot-api", "1.12.2-R0.1-SNAPSHOT")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    archiveFileName.set("EmbedScript.jar")
}

tasks.withType<ProcessResources> {
    filter { it.replace("\$version", version.toString()) }
}

tasks.withType<ShadowJar> {
    minimize()
    relocatePackage("kotlin")
    relocatePackage("org.intellij")
    relocatePackage("org.jetbrains")
}

fun ShadowJar.relocatePackage(target: String) {
    relocate(target, "com.github.kuro46.embedscript.libs.$target")
}
