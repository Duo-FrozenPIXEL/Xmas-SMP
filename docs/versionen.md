# Versions-Fakten (1.21.11 bis 26.3)

Stand der Recherche: 2026-10-29. Klärt die **(V)**-Punkte in `todo.md` (Abschnitte 1.3, 3.1, 10). Alle Zahlen stammen aus den unten genannten Quellen; wo etwas nicht eindeutig belegt ist, steht es so da. Die Markierung **primär** bedeutet: direkt vom Hersteller bzw. aus dem Paket-Repository, **sekundär**: Wiki, Blog oder Fremdprojekt.

## 1. Welche Minecraft-Java-Versionen sind veröffentlicht?

Die Versionsnummern folgen seit 26.1 dem Schema `Jahr.Drop[.Hotfix]` (26.1 ist der erste Drop 2026).

| Version | Release-Datum | Art | Name (sekundär) |
|---|---|---|---|
| 1.21.11 | 2025-12-09 | letztes Release im alten Schema | – |
| **26.1** | 2026-03-24 | Drop | Tiny Takeover |
| 26.1.1 | 2026-04-01 | Hotfix | – |
| 26.1.2 | 2026-04-09 | Hotfix | – |
| **26.2** | 2026-06-16 | Drop | Chaos Cubed (nur Drittseiten) |
| **26.3** | 2026-09-15 | Drop | Wilderness Bound |
| 26.4 | **nicht veröffentlicht** | nur Snapshots (`26.4-snapshot-1` bis `-6`, Stand heute) | – |

- Ergebnis: **26.1, 26.2 und 26.3 existieren tatsächlich.** 26.3 ist das aktuelle Release (Stand 2026-10-29). Ein Hotfix 26.3.x war im Mojang-Manifest nicht zu finden.
- 26.4 ist noch nicht erschienen. Die Spanne „1.21.11 bis 26.3“ aus `Instructions.md` deckt damit alle derzeit veröffentlichten Versionen ab 1.21.11 ab.
- Nicht jede Hotfix-Version hat eigene Mod-Builds: Fabric API und Polymer bauen für **26.1, 26.1.1 und 26.1.2** gemeinsam (ein Build, mehrere Spielversionen).

**Quellen**
- primär: Mojang-Versionsmanifest https://piston-meta.mojang.com/mc/game/version_manifest_v2.json (Release-Zeitpunkte, `latest.release` = 26.3, `latest.snapshot` = 26.4-snapshot-6)
- primär: Fabric-Meta-API https://meta.fabricmc.net/v2/versions/game (26.1, 26.1.1, 26.1.2, 26.2, 26.3 und 1.21.11 als stabil gelistet)
- sekundär: https://minecraft.wiki/w/Java_Edition_version_history, https://minecraft.wiki/w/Java_Edition_26.1, https://minecraft.wiki/w/Java_Edition_26.3 (Namen, Release-Daten)
- sekundär (nur für den Namen „Chaos Cubed“): https://xgamingserver.com/blog/minecraft-26-2-chaos-cubed-release-date-sulfur-caves-everything-new/

## 2. Benötigte Java-Version

| Version | Java (Mindestversion) | Laufzeit laut Mojang-Metadaten |
|---|---|---|
| 1.21.11 | **21** | `java-runtime-delta`, majorVersion 21 |
| 26.1, 26.1.2 | **25** | `java-runtime-epsilon`, majorVersion 25 |
| 26.2, 26.3 | **25** | `java-runtime-epsilon`, majorVersion 25 |

- 26.1 ist die erste Version, die Java 25 verlangt (Microsofts OpenJDK-25-Build wird mitgeliefert).
- Für Mods heißt das: Das **Gradle-JVM** muss für 26.x Java 25 sein (Fabric-Blog zu 26.1). Das Projekt kompiliert den Kern aktuell mit `release 21`; Java-21-Bytecode läuft auf Java 25, ein Fabric-Build für 26.x braucht aber trotzdem ein JDK 25 im Build. Die CI nutzt heute nur Java 21.
- Der Gradle-Wrapper des Repos steht auf **8.14.3**. Loom hat Gradle 9.x ab 1.14 (mind. 9.2) und mind. 9.4 ab 1.16 vorausgesetzt (siehe 5). Für 26.x ist also ein Gradle-Update nötig.

**Quellen**
- primär: Versions-JSONs im Mojang-Manifest, z. B. https://piston-meta.mojang.com/v1/packages/96403ac4a56447675325d2b1b5e320d76a5b5625/1.21.11.json und https://piston-meta.mojang.com/v1/packages/5df1795e16eea19d75b923d66ecca9ce183353c5/26.3.json (Feld `javaVersion`); 26.1.2 und 26.2 analog
- sekundär: https://minecraft.wiki/w/Java_Edition_26.1 (Java SE 25 als Minimum)
- Hersteller des Loaders: https://fabricmc.net/2026/03/14/261.html (Java 25 für Gradle, IntelliJ 2025.3+ für Mixins)

## 3. Resource-Pack-Format je Version

| Version | Resource-Pack-Format | Data-Pack-Format |
|---|---|---|
| 1.21.9 – 1.21.10 | 69.0 | 88.0 |
| **1.21.11** | **75.0** | 94.1 |
| **26.1 – 26.1.2** | **84.0** | 101.1 |
| **26.2** | **88.0** | 107.1 |
| **26.3** | **97.1** | 121.0 |
| 26.4 (nur Snapshots) | Snapshots durchlaufen 98 bis 101 | – |

Format-Felder in `pack.mcmeta` (laut Minecraft-Wiki, gilt ab 1.21.9 / 25w31a):

- `min_format` und `max_format` sind Pflicht, `pack_format` und `supported_formats` entfallen (nur nötig, wenn das Pack auch ältere Versionen unterstützen soll).
- Die Werte sind entweder eine **Ganzzahl** (Hauptformat) oder eine Liste `[Haupt, Neben]`. Eine Ganzzahl bei `min_format` bedeutet Nebenversion 0, bei `max_format` **jede** Nebenversion. Daher deckt `max_format: 97` auch 97.1 ab.
- Die Nebenversion (z. B. 97.1) wird seit 25w31a bei nicht brechenden Änderungen hochgezählt statt der Hauptversion.

**Bewertung des bestehenden `resourcepack/pack.mcmeta`:** `min_format 75`, `max_format 97` passt zu 1.21.11 bis 26.3 und deckt 26.3 (97.1) ab. Die Annahme in `docs/resourcepack.md` („97 laut einer Quelle, noch zu verifizieren“) stimmt nach den Wiki-Angaben. **Grenze der Absicherung:** Beide Tabellen (Versionsseite und „Pack format“) stehen im Minecraft-Wiki, also sekundär. Das Mojang-Manifest enthält keine Pack-Formate. Die endgültige Prüfung ist das Laden des Packs im Client je Version (`todo.md` 1.3). Für 26.4 würde die Obergrenze 97 nicht mehr genügen; die Snapshots liegen bei 98 bis 101.

**Quellen**
- sekundär: https://minecraft.wiki/w/Pack_format, https://minecraft.wiki/w/Pack.mcmeta, https://minecraft.wiki/w/Resource_pack, die Versionsseiten https://minecraft.wiki/w/Java_Edition_1.21.11, /Java_Edition_26.1, /Java_Edition_26.2, /Java_Edition_26.3
- Die einzelnen Zwischenwerte (z. B. 76 bis 83 in 26.1) sind Snapshot-Stände und für Server-Releases unerheblich.

## 4. Gson in Minecraft (wichtig für `com.google.gson.Strictness`)

Aus den Mojang-Versions-JSONs (Bibliotheksliste des Spiels, primär):

| Version | Gson |
|---|---|
| 1.21.11 | **2.13.2** |
| 26.1, 26.1.2 | **2.13.2** |
| 26.2, 26.3 | **2.14.0** |

(Guava dazu: 33.5.0-jre in 1.21.11 und 26.1, 33.6.0-jre in 26.2 und 26.3.)

- `Strictness` wurde in **Gson 2.11.0** eingeführt (Release-Notes: neue Strictness-API, `JsonReader#setStrictness`). Alle oben genannten Versionen liefern mindestens 2.13.2, also ist `com.google.gson.Strictness` (genutzt in `common/.../util/JsonFiles.java`) unter Fabric auf 1.21.11 und 26.x verfügbar.
- Das Gradle-Projekt `common` kompiliert gegen `gson_version=2.11.0` (Untergrenze, passt). Auf Fabric zählt zur Laufzeit die Gson-Version von Minecraft.
- **Nicht geprüft:** die Gson-Version, die Paper und NeoForge zur Laufzeit mitbringen. Vor dem Paper-/NeoForge-Port bei Paper bzw. NeoForge nachsehen, ob dort mindestens 2.11.0 liegt.
- 26.1.1 wurde nicht einzeln abgerufen (26.1 und 26.1.2 haben dieselbe Version, sodass 2.13.2 sehr wahrscheinlich ist).

**Quellen**
- primär: Mojang-Versions-JSONs (siehe Abschnitt 2), Bibliothek `com.google.code.gson:gson`
- primär: Gson-Release-Notes https://github.com/google/gson/releases/tag/gson-parent-2.11.0

## 5. Unobfuskiert und Fabric Loom

**Ab wann:** Ab **Minecraft 26.1**. Belege:

- Im Mojang-Manifest hat 1.21.11 die Downloads `client_mappings` und `server_mappings`, 26.1, 26.1.2, 26.2 und 26.3 haben nur `client` und `server` (primär).
- Die Minecraft-Wiki nennt 26.1 die erste Java-Version, die vollständig unobfuskiert ohne obfuskierte Variante erscheint, 26.1 RC1 als erste Version nur mit unobfuskiertem Build (sekundär). Der erste 26.1-Snapshot erschien laut Wiki am 2025-12-16.
- Fabric (Blog zu 26.1): Für 26.1 gibt es keine Mappings, Mods für 1.21.11 und älter laufen nicht ohne mindestens Neukompilierung. **Yarn wird nicht mehr offiziell unterstützt**, Mojangs eigene Namen sind der Standard.

**Was das für Fabric Loom bedeutet** (Fabric-Doku und Blog):

| | 1.21.11 und älter (obfuskiert) | 26.1 und neuer (unobfuskiert) |
|---|---|---|
| Plugin-ID | `net.fabricmc.fabric-loom-remap` (die alte ID `fabric-loom` bleibt nur aus Kompatibilitätsgründen) | `net.fabricmc.fabric-loom` |
| Mappings | `mappings(loom.officialMojangMappings())` o. ä. | **keine** (Zeile entfernen) |
| Abhängigkeiten | `modImplementation`, `modCompileOnly` … | `implementation`, `compileOnly` (wie das Bündeln per `include(...)` in 26.x geschrieben wird, wurde nicht geprüft) |
| Jar-Task | `remapJar` | `jar` |
| Gradle / Java | – | Gradle-JVM Java 25 |

- Das heutige `fabric/build.gradle.kts` nutzt `id("fabric-loom")`, `loom.officialMojangMappings()` und `modImplementation(include(...))`. Das gilt für 1.21.11 und muss für 26.x pro Version unterschiedlich konfiguriert werden (genau das war der Anlass für den (V)-Punkt).
- Loom-Versionen laut GitHub-Releases (Datumsangaben dort ohne Jahr, daher nur die Reihenfolge belastbar): 1.13 letzte Version mit Gradle 8.x vorgesehen, **1.14** erste Version mit Unterstützung für unobfuskierte Versionen und den neuen Plugin-IDs (Gradle 9.2), **1.15** vom Fabric-Blog für 26.1 empfohlen, **1.16** mind. Gradle 9.4, 1.17 unterstützt Gradle 9.5, 1.18 nutzt Java 25 (Foreign Function and Memory API).
- Das Maven-Repository von Fabric führt als neuestes Release **1.19.3** (Metadaten zuletzt geändert 2026-10-22), die GitHub-Release-Liste führt 1.19 an der Spitze. Eine Quelle nennt Loom 1.18.4 in einem Fremdprojekt (siehe 6). Die genaue neueste Loom-Version unmittelbar vor dem Bauen auf https://fabricmc.net/develop prüfen.
- Das Fabric-API-Team hat mit 26.1 Namen und Module geändert (u. a. Mod-ID `fabric` entfällt zugunsten `fabric-api`, `HudRenderCallback` ersetzt durch `HudElementRegistry`, Umbenennungen nach Mojang-Namen, `ItemStack` kann erst nach Weltstart erzeugt werden). Das Projekt hängt im `fabric.mod.json` bereits an `fabric-api`; ob der Code sonst betroffen ist, muss beim Portieren geprüft werden (hier nicht geprüft).

**Quellen**
- primär (Fabric): https://fabricmc.net/2026/03/14/261.html, https://docs.fabricmc.net/develop/loom/, https://docs.fabricmc.net/develop/porting/mappings/, https://github.com/FabricMC/fabric-loom/releases, https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- primär (Mojang-Metadaten): siehe Abschnitt 2
- sekundär: https://minecraft.wiki/w/Java_Edition_26.1, https://minecraft.wiki/w/Java_Edition_26.1_Release_Candidate_1

## 6. Aktuelle Versionen für 26.x

Stand 2026-10-29. Zur Gegenüberstellung steht die Version aus `gradle.properties` (1.21.11) in der letzten Spalte.

| Baustein | 26.1.x | 26.2 | 26.3 | im Repo (1.21.11) |
|---|---|---|---|---|
| **Fabric Loader** | 0.20.1 stabil (Meta-API, versionsunabhängig) – der Fabric-Blog vom 2026-03-14 empfahl damals 0.18.4 | gleich | gleich | 0.18.1 |
| **Fabric API** | 0.155.3+26.1.2 (gilt für 26.1, 26.1.1, 26.1.2) | 0.161.0+26.2 | 0.165.0+26.3 (2026-10-23) | 0.139.4+1.21.11 (neuester für 1.21.11: 0.141.6) |
| **Polymer** (`eu.pb4:polymer-*`) | 0.16.5+26.1.2 | 0.17.5+26.2 | 0.18.5+26.3 (2026-10-24) | 0.15.0+1.21.11 (neuester: 0.15.2) |
| **sgui** (`eu.pb4:sgui`) | 2.0.0+26.1 | 2.1.0+26.2 | 2.2.1+26.3 | 1.12.0+1.21.11 (neuester für 1.21.11: 1.12.0) |
| **fabric-permissions-api** (`me.lucko`) | 0.7.0 (Modrinth: 26.1 bis 26.3) | 0.7.0 | 0.7.0 | 0.6.1 (1.21.11) |
| **Stonecutter** (`dev.kikugie.stonecutter`) | 0.9.8, versionsunabhängiges Plugin | | | – |

Anmerkungen:

- Der Loader hat keine Minecraft-Versionsbindung im Namen; die Meta-API listet 0.20.1 als neueste stabile Version. Die Mindest-Loader-Version, die 26.x tatsächlich braucht, ist nicht eindeutig belegt (Fabric-Blog nannte 0.18.4 als Empfehlung, Loom 1.16 verlangt für ClassTweaker-Enum-Erweiterungen Loader 0.19.0 oder neuer). Das `fabric.mod.json` verlangt heute `fabricloader >= 0.17.0`.
- **sgui springt für 26.1 auf Version 2.0** (Major-Version): API-Änderungen sind möglich, aber hier nicht geprüft. Die Maven-Metadaten (https://maven.nucleoid.xyz/eu/pb4/sgui/maven-metadata.xml) listen 1.12.0+1.21.11, 2.0.0-beta.1/beta.2/2.0.0+26.1, 2.1.0+26.2, 2.2.0/2.2.1+26.3. Die Modrinth-Abfrage für sgui war fehlgeschlagen (404, evtl. anderer Slug), die Angabe stammt aus dem Maven.
- **Polymer**: `polymer-core`-Maven-Metadaten (https://maven.nucleoid.xyz/eu/pb4/polymer-core/maven-metadata.xml) listen 0.16.0+26.1.1 bis 0.16.5+26.1.2, 0.17.1 bis 0.17.5+26.2, 0.18.1 bis 0.18.5+26.3; die Modrinth-Fassung heißt „polymer-bundled“. Ob die im Projekt genutzten Module (`polymer-core`, `polymer-resource-pack`, `polymer-virtual-entity`) in allen Versionen dieselben Namen tragen, wurde nicht einzeln geprüft.
- **fabric-permissions-api**: Das Nucleoid-Maven führt nur ältere Versionen (0.1-SNAPSHOT bis 0.4.2-patbox.1), neuere liegen auf Maven Central (`me.lucko:fabric-permissions-api`, neueste 0.7.0, Reihe 0.3.2, 0.3.3, 0.4.0, 0.4.1, 0.5.0, 0.6.0, 0.6.1, 0.7.0). Das Projekt nutzt `mavenCentral()`, passt also.
- **Stonecutter**: Offiziell ist die Unterstützung von 26.x **nicht** auf der Projektseite geprüft worden (die Dokumentationsseite stonecutter.kikugie.dev lieferte beim Abruf keinen brauchbaren Text, die GitHub-Release-Seite war leer). Belegt ist nur, dass Stonecutter 0.9.8 (veröffentlicht 2026-08-31) in Fremdprojekten 1.21.x **bis 26.3** abdeckt, siehe unten. Stonecutter selbst bearbeitet nur den Quelltext je Versions-Knoten; ob 26.x klappt, hängt vom Loom-Setup ab.

Belege für Stonecutter in Fremdprojekten (sekundär, nicht vom Stonecutter-Autor):

- https://github.com/OddHero/minecraft-mod-template: Stonecutter 0.9.8, ein Quellbaum für Minecraft 1.21 bis 26.3 mit Fabric und NeoForge. 1.21.x-Knoten (Java 21) mit Remapping, 26.x-Knoten (Java 25) ohne Remapping (`loom-no-remap` bei Architectury), `implementation` statt `modImplementation`.
- https://github.com/Wulfic/enchanting-2/pull/1: Stonecutter 0.9.8, Fabric Loom 1.18.4, Hilfsplugin `loom-back-compat` 0.4.3 (nur im KikuGie-Repository), Versions-Bänder 1.21.11 / 26.1–26.1.2 / 26.2 / 26.3 mit je eigenem Jar. Dort heißt es, 26.x-Jars brauchen Java 25 und die API ändert sich zwischen 26.1.2, 26.2 und 26.3 so, dass pro Drop eigener Code nötig wurde (laut Beschreibung 8 bzw. 23 gebrochene Referenzen). Das ist ein Hinweis, dass ein einziger 26.x-Build für alle Drops nicht reicht.
- Die Vorlage https://github.com/stonecutter-versioning/stonecutter-template-fabric tauchte nur in Suchergebnissen auf und wurde nicht geöffnet.

**Quellen für die Tabelle**
- Fabric Loader: https://meta.fabricmc.net/v2/versions/loader
- Fabric API: Modrinth-API `https://api.modrinth.com/v2/project/fabric-api/version?loaders=["fabric"]&game_versions=["<version>"]`
- Polymer: Modrinth-API für `polymer`, Maven https://maven.nucleoid.xyz/eu/pb4/polymer-core/maven-metadata.xml
- sgui: https://maven.nucleoid.xyz/eu/pb4/sgui/maven-metadata.xml
- fabric-permissions-api: https://api.modrinth.com/v2/project/fabric-permissions-api/version, https://repo1.maven.org/maven2/me/lucko/fabric-permissions-api/maven-metadata.xml
- Stonecutter: https://maven.kikugie.dev/releases/dev/kikugie/stonecutter/maven-metadata.xml (neueste 0.9.8, lastUpdated 2026-08-31), https://plugins.gradle.org/plugin/dev.kikugie.stonecutter

## 7. Folgerungen für das Projekt (nur Beobachtungen, nichts umgesetzt)

1. **Gson/Strictness**: kein Problem für Fabric auf 1.21.11 und 26.x. Offen bleibt die Gson-Version von Paper und NeoForge.
2. **Pack-Format**: `min_format 75` / `max_format 97` ist laut Wiki für 1.21.11 bis 26.3 korrekt. Beim Hinzufügen von 26.4 muss `max_format` angehoben werden.
3. **Build für 26.x** braucht: JDK 25, Gradle 9.4 oder neuer (heute 8.14.3), Loom-Plugin `net.fabricmc.fabric-loom` ohne Mappings, `implementation` statt `modImplementation`, `jar` statt `remapJar`. Für 1.21.11 bleibt das bisherige Setup (`fabric-loom-remap`/`fabric-loom`, Java 21).
4. **Abhängigkeiten je Drop** sind einzeln versioniert (Fabric API, Polymer, sgui, Permissions); 26.1.x, 26.2 und 26.3 verlangen jeweils eigene Builds dieser Bibliotheken (sgui sogar ein Major-Update).
5. **Mehrversions-Build**: Stonecutter wird in Fremdprojekten für genau diesen Bereich verwendet, ist aber hier nicht offiziell bestätigt und braucht je Knoten ein eigenes Loom-Setup. Mehrere Drops (26.1, 26.2, 26.3) können unterschiedlichen Code erfordern.
6. **Nicht geprüft**: Ob der aktuelle Code (Fabric-Adapter, Mixin für die Zweithand-Taste, Polymer-Virtual-Entities, sgui-Nutzung) auf 26.x kompiliert; nur die Rahmenbedingungen wurden recherchiert.
