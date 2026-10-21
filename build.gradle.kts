subprojects {
    group = "de.xmassmp"
    version = rootProject.property("mod_version") as String

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }
    }

    repositories {
        mavenCentral()
    }
}
