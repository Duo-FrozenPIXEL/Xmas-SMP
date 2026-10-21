pluginManagement {
    val loom_version: String by settings
    plugins {
        id("fabric-loom") version loom_version
    }
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "xmas-smp"

include("common")

// Fabric braucht maven.fabricmc.net. Ohne Zugriff darauf lässt sich der Kern allein bauen:
//   gradle build -Pxmassmp.skipFabric
if (!providers.gradleProperty("xmassmp.skipFabric").isPresent) {
    include("fabric")
}

// paper/ und neoforge/ folgen nach Fabric (siehe todo.md, Abschnitt 9).
