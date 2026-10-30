plugins {
    id("fabric-loom")
}

// Optionaler Client-Mod: legt die drei Kristall-Fähigkeiten auf frei belegbare Tasten.
// Der Server braucht ihn nicht und prüft alles selbst (Cooldown, Energie, Tier).
base {
    archivesName.set("xmas-smp-client")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    // Mojang-Namen wie im Fabric-Modul.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
