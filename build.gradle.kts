/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
plugins {
    id("java")
    id("io.freefair.lombok") version "5.2.1"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.volmit.iris"
version = "1.8.7"

val apiVersion = "1.16"
val mcVersion = "1.16.5"
val mcSubVersion = "R0.1"
val main = "com.volmit.iris.Iris"

allprojects {
    apply(plugin = "java")
    repositories {
        mavenLocal()
        maven("https://maven.fastmirror.net/repositories/minecraft")
        maven("https://mvn.lumine.io/repository/maven/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            content { includeGroup("org.spigotmc") }
        }
        maven("https://papermc.io/repo/repository/maven-public/") {
            content { includeGroup("io.papermc") }
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            content { includeGroup("me.clip") }
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") {
            content { includeGroup("net.kyori") }
        }
        maven("https://repo.jeff-media.de/maven2") {
            content { includeGroup("de.jeff_media") }
        }
        maven("https://repo.codemc.io/repository/maven-releases") {
            content { includeGroup("com.dfsek") }
        }
        maven("https://jitpack.io")
        mavenCentral()
    }
    dependencies {
        implementation("org.spigotmc:spigot-api:$mcVersion-$mcSubVersion-SNAPSHOT")
    }
}

dependencies {
    // Provided or Classpath
    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    implementation("org.bukkit:craftbukkit:$mcVersion-$mcSubVersion-SNAPSHOT")
    implementation("com.sk89q.worldedit:worldedit-bukkit:7.2.0-SNAPSHOT")
    implementation("me.clip:placeholderapi:2.10.10")

    // Shaded
    implementation("com.dfsek:Paralithic:0.4.0")
    implementation("io.papermc:paperlib:1.0.5")
    implementation("net.kyori:adventure-text-minimessage:4.2.0-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.0.0-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.9.1")

    // Dynamically Loaded
    implementation("io.timeandspace:smoothie-map:2.0.2")
    implementation("it.unimi.dsi:fastutil:8.5.4")
    implementation("com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2")
    implementation("org.zeroturnaround:zt-zip:1.14")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("org.ow2.asm:asm:9.2")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("bsf:bsf:2.4.0")
    implementation("rhino:js:1.7R2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.0.3")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

tasks {
    shadowJar {
        minimize()
        archiveClassifier.set(mcVersion)
        append("plugin.yml")
        relocate("com.dfsek.paralithic", "com.volmit.iris.util.paralithic")
        relocate("io.papermc.lib", "com.volmit.iris.util.paper")
        relocate("net.kyori", "com.volmit.iris.util.kyori")
        dependencies {
            include(dependency("io.papermc:paperlib"))
            include(dependency("com.dfsek:Paralithic"))
            include(dependency("net.kyori:"))
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(
                "name" to project.name,
                "version" to project.version,
                "main" to main,
                "apiversion" to apiVersion
            )
            include("plugin.yml")
        }
    }
}