plugins {
    id("fabric-loom")
}

base {
    archivesName.set("xmas-smp-fabric")
}

repositories {
    // Polymer, sgui, fabric-permissions-api
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

evaluationDependsOn(":common")
val commonMain: SourceSet = project(":common").the<SourceSetContainer>()["main"]

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    // Mojang-Namen: ab 26.1 ist Minecraft nicht mehr obfuskiert und nutzt dieselben Namen.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // Server-seitig für Vanilla-Clients: Resource-Pack-Auslieferung, Chest-GUIs, Rechte.
    modImplementation(include("eu.pb4:polymer-core:${property("polymer_version")}")!!)
    modImplementation(include("eu.pb4:polymer-resource-pack:${property("polymer_version")}")!!)
    modImplementation(include("eu.pb4:sgui:${property("sgui_version")}")!!)
    modImplementation(include("me.lucko:fabric-permissions-api:${property("permissions_api_version")}")!!)

    // Kern wird direkt mitkompiliert (siehe unten); Gson liefert Minecraft.
    compileOnly(project(":common"))
}

tasks.compileJava {
    source(commonMain.allJava)
}

tasks.processResources {
    from(commonMain.resources)
    // Ressourcenpaket (Modelle, Texturen, Sprachdateien) – Polymer liefert es an die Clients aus.
    from(rootProject.file("resourcepack")) {
        include("assets/**")
    }
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
