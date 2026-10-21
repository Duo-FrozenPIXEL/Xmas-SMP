plugins {
    `java-library`
}

dependencies {
    // Minecraft liefert Gson mit; auf Paper/NeoForge ebenfalls vorhanden.
    api("com.google.code.gson:gson:${property("gson_version")}")

    testImplementation(platform("org.junit:junit-bom:${property("junit_version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Tests gleichen Code und Ressourcenpaket ab.
    systemProperty("xmassmp.resourcepack", rootProject.file("resourcepack").absolutePath)
}
