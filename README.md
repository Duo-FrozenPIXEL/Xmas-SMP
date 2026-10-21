# Xmas-SMP

Server-Mod/Plugin für den Xmas-SMP: fünf magische Kristalle (Feuer, Erde, Wind, Schatten, Natur) nach dem Vorbild des Bliss-SMP, mit Breaking-Pristine-System, Gem Trader, Admin-Werkzeugen und einem Event-Planer für die Weihnachtswoche.

Spieler brauchen **keinen Client-Mod**: Der Server liefert das Ressourcenpaket aus, alle Items sind Vanilla-Items mit Custom-Model-Data.

> **Stand:** Grundgerüst. Kern, Event-System, Admin-Befehle und Ressourcenpaket sind fertig. Kräfte, Energie-Regeln, Trader und Admin-GUI folgen (siehe [`todo.md`](todo.md)).

## Unterstützte Versionen

| Plattform | Status | Minecraft |
|---|---|---|
| Fabric | in Arbeit | 1.21.11 (weitere bis 26.3 geplant) |
| Paper | geplant | 1.21.11 – 26.3 |
| NeoForge | geplant | 1.21.11 – 26.3 |

Java 21.

## Installation (Fabric)

1. `xmas-smp-fabric-<version>.jar` und die [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods/`-Ordner des Servers legen. Polymer, sgui und fabric-permissions-api sind im JAR enthalten.
2. Server starten. Beim ersten Start entstehen
   - `config/xmassmp/config.jsonc` – Hauptkonfiguration
   - `config/xmassmp/events.jsonc` – Events (Beispiel-Weihnachtswoche, alles deaktiviert)
   - `<welt>/xmassmp/` – Spielerdaten, Event-Zustand, `audit.log`
3. Das Ressourcenpaket stellt Polymer bereit. Für die automatische Auslieferung an Spieler Polymers AutoHost einrichten (siehe Polymer-Doku) oder das Paket aus `python3 tools/resourcepack.py build` selbst hosten.

## Befehle

Alle Befehle brauchen das Recht `xmassmp.admin`. Ohne Permission-Plugin (z. B. LuckPerms) gilt OP-Level 2. Alle Befehle haben Tab-Completion.

| Befehl | Wirkung |
|---|---|
| `/xmas` | Hilfe |
| `/xmas give <spieler> crystal <kristall> [tier] [zustand]` | Kristall-Item geben. Kristalle: `fire`, `earth`, `wind`, `shadow`, `nature`. Tier `1`–`2`, Zustand `broken`, `damaged`, `scratched`, `pristine` (Standard: Tier 1, `pristine`) |
| `/xmas give <spieler> item <item>` | Hilfs-Item geben: `revival_tome`, `upgrader`, `energy_bottle`, `gem_trader`, `repair_kit`, `gem_fragment` |
| `/xmas set <spieler> crystal <kristall>` | Aktiven Kristall setzen |
| `/xmas set <spieler> tier <1-2>` | Tier setzen |
| `/xmas set <spieler> energy <wert>` | Energie setzen (Grenzen aus der Config) |
| `/xmas reload` | `config.jsonc` und `events.jsonc` neu laden; Fehler werden angezeigt, die alte Konfiguration bleibt dann aktiv |
| `/xmas event list` | Alle Events mit Status und nächstem Start |
| `/xmas event start <id>` / `stop <id>` | Event sofort starten/beenden (auch deaktivierte) |
| `/xmas event simulate <yyyy-MM-dd> <HH:mm> [dry]` | Testmodus: Uhrzeit simulieren. Mit `dry` werden Aktionen nur geloggt |
| `/xmas event simulate off` | Testmodus beenden, echten Event-Zustand wiederherstellen |

Jede Änderung über `give`, `set`, `reload` und `event` landet in `<welt>/xmassmp/audit.log` (eine JSON-Zeile pro Eintrag: Zeit, Wer, Was, Ziel). Abschaltbar mit `audit.enabled`.

## Konfiguration

`config/xmassmp/config.jsonc` ist kommentiert. Kurzüberblick:

| Schlüssel | Bedeutung |
|---|---|
| `general.enabled` / `general.abilitiesEnabled` | Hauptschalter / alle Fähigkeiten |
| `crystals.<id>.enabled`, `cooldownMultiplier`, `damageMultiplier` | pro Kristall |
| `energy.start`, `min`, `max` | Energie (vorläufige Werte bis zur Recherche des Originals) |
| `effects.quality` | `LOW`, `MEDIUM`, `HIGH` – dünnt Partikel aus |
| `effects.maxParticlesPerTick` | Partikel-Budget aller Effekte pro Tick |
| `events.timezone` | Zeitzone der Event-Zeiten (Standard `Europe/Berlin`) |
| `events.catchUpMissed` | Events nachholen, deren Start in eine Server-Pause fiel |
| `audit.enabled` | Audit-Log an/aus |

## Events

Events laufen an festen Tagen zu festen Uhrzeiten. Beschrieben werden sie in `config/xmassmp/events.jsonc`, die Felder sind dort dokumentiert. Das System kann:

- Termine als einmaliges Datum (`2026-12-24`), jährlich (`12-24`) oder als Bereich, auch über Silvester. Dazu optional Wochentage und mehrere Uhrzeiten pro Tag.
- Ankündigungen X Minuten vorher und einen Sekunden-Countdown als Titel.
- Aktionen beim Start und Ende: `broadcast`, `title`, `sound`, `command`. Eigene Typen lassen sich im Code registrieren.
- Modifikatoren, solange ein Event läuft, z. B. `"cooldown_multiplier": 0.5`.
- Laufende Events überstehen einen Neustart. Starts, die in eine Server-Pause fielen, werden je nach Config nachgeholt oder übersprungen.
- Sommer-/Winterzeit: Jede Startzeit feuert genau einmal pro Tag.

## Entwicklung

```
common/          plattformunabhängiger Kern (Kristalle, Spielerdaten, Effekte, Config, Events, Admin) + Tests
fabric/          Fabric-Adapter (erste Plattform)
paper/, neoforge/  geplant
resourcepack/    Ressourcenpaket-Quelle (Doku: docs/resourcepack.md)
tools/           Pack-Validator und -Builder
```

```bash
./gradlew build                               # alles (braucht Zugriff auf maven.fabricmc.net)
./gradlew :common:test -Pxmassmp.skipFabric   # nur Kern + Tests
python3 tools/resourcepack.py validate        # Pack prüfen
python3 tools/resourcepack.py build           # build/Xmas-SMP-resourcepack.zip inkl. SHA-1
```

Die Fabric-Abhängigkeiten stehen in `gradle.properties`. Vor dem ersten Build die Versionen auf [fabricmc.net/develop](https://fabricmc.net/develop) prüfen.

Die GitHub Actions bauen und prüfen bei jedem Push das Ressourcenpaket, den Kern (inkl. Tests) und das Fabric-JAR. Die Ergebnisse liegen als Artefakte bereit.

### Architektur

Der Kern (`XmasCore`) kennt keine Minecraft-Klassen. Jede Plattform implementiert:

- `Platform`: Ordner und Logger
- `EffectSink`: Partikel und Sounds senden
- `EventHost`: Broadcast, Titel, Sound, Konsolenbefehl

Außerdem ruft sie `XmasCore#tick()` jeden Server-Tick auf. Den Befehlsbaum baut sie selbst, die Logik dahinter liegt in `AdminService`.

Kräfte entstehen als `ActiveAbility` / `PassiveAbility`. Animationen baut man mit `EffectTimeline` (Phasen *Aufladen → Auslösen → Nachklingen*) aus `Shapes` (Ring, Spirale, Kugel, Linie, Welle), `ParticleSpec` (inkl. Farbverläufen) und `SoundStack` (geschichtete Sounds mit Tonhöhen-Variation).
