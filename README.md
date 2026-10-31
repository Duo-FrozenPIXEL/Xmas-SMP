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
   - `<welt>/xmassmp/` – Spielerdaten, Event-Zustand, `audit.log`, `energy.log`, `bottles.json`, `totem.json`
3. Das Ressourcenpaket stellt Polymer bereit. Für die automatische Auslieferung an Spieler Polymers AutoHost einrichten (siehe Polymer-Doku) oder das Paket aus `python3 tools/resourcepack.py build` selbst hosten.

## Spielen

- Wer noch keinen Kristall hat, bekommt beim ersten Betreten einen zufälligen – auch wer später einsteigt. Er ist seelengebunden: Er lässt sich nicht droppen, einlagern oder beim Tod verlieren.
- Jeder Kristall hat eine **Primär**- und eine **Sekundärkraft** (Tier 1) und eine **Ultimate** (Tier 2), dazu Passive. Ohne Client-Mod: **F** = Primär, **Schleichen + F** = Sekundär, **Schleichen + Rechtsklick** mit dem Kristall in der Haupthand = Ultimate.
- Die Actionbar zeigt alle drei Fähigkeiten (bereit ✔, Cooldown in Sekunden, gesperrt `T2`, zu wenig Energie `✦3`, zerbrochen ✖) und die Energie ✦.

### Energie

- Skala 0–10, Start 5 („Makellos“). Zustände wie im Original: Zerbrochen (0), Ruiniert (1), Beschädigt (2), Gerissen (3), Zerkratzt (4), Makellos (5), Makellos +1 … +5.
- PvP-Sieg +1, jeder Tod −1. Wer ein Opfer **ohne** Energie besiegt, bekommt nichts. Hat der Sieger schon 10, fällt die Energie des Opfers als Energy Bottle zu Boden.
- **0 Energie:** keine aktiven Kräfte. **1–2:** nur die Primärkraft. **Ab 3:** Primär und Sekundär, mit Tier 2 auch die Ultimate. **Passive wirken immer**, auch bei 0 und egal, wo der Kristall im Inventar liegt.
- Jede Energieänderung steht in `<welt>/xmassmp/energy.log` (`/xmas energy audit`).

### Items

| Item | So bekommt man es | Wirkung |
|---|---|---|
| **Energy Bottle** | Leere Glasflasche in die Hand, **Schleichen + Rechtsklick 5 s halten** (Ladebalken am Fadenkreuz) – kostet 1 Energie | Rechtsklick: +1 Energie, auch bei 0 und für jeden Spieler |
| **Kristall-Splitter** | 1 Energy Bottle → 9 Splitter (Werkbank oder 2×2-Feld) | Zutat |
| **Upgrader** | 4 Splitter, 4 Drachenatem, 1 Witherskelettschädel | Rechtsklick: Tier 2, schaltet die Ultimate frei |
| **Kristall-Händler** | 4 Diamanten, 4 Drachenatem, 1 Splitter | Rechtsklick: GUI, neuen Kristall frei wählen (Tier und Energie bleiben) |
| **Repair Kit** | 4 Splitter, 2 Ambosse, 2 Diamantblöcke, 1 Netherit-Schmiedevorlage | Auf den Boden stellen → **Totem** |

Rezepte stehen in `config.jsonc` unter `recipes` und lassen sich ohne Neubau ändern. Alle Items sind eigene Server-Items: Sie haben keine Vanilla-Funktion (kein Brauen, Platzieren, Handeln).

**Schutz vor Exploits:** Jede Energy Bottle hat eine eigene ID im Flaschen-Register (`<welt>/xmassmp/bottles.json`). Trinken und Zerlegen schließen den Eintrag in einem Schritt. Kopien (Dupe-Glitches, Creative-Mittelklick) wirken deshalb nur einmal, gefälschte Flaschen nie; beides wird entfernt und im Energie-Protokoll vermerkt. Die 5 s Ladezeit zählt der Server selbst. Abfüllen geht nur, solange danach mindestens 1 Energie bleibt, und nicht in den 15 s nach PvP-Schaden.

### Repair-Totem

- Das Repair Kit wird auf den Boden gestellt. **Global läuft höchstens ein Totem.** Die Koordinaten gehen per Chat und Titel an alle.
- Wer in den ersten 30 s im Kreis (15 Blöcke, Partikelring) steht, nimmt teil. Nach **10 Minuten** zerbricht das Totem, dann bekommen alle verbliebenen Teilnehmer mit weniger als 5 Energie genau 5 – auch von 0.
- Wer den Kreis verlässt, stirbt oder sich ausloggt, scheidet aus. Der Kreis ist kein Schutzschild.
- Gegner können das Totem in **festen 10 Sekunden** abbauen (Werkzeug und Effizienz egal; TNT, Kolben und Feuer wirken nicht). Loslassen oder Wegschauen setzt den Countdown zurück. Wird es zerstört, bekommt niemand Energie.

### Optionaler Client-Mod (Tasten)

Wer möchte, legt die drei Fähigkeiten auf eigene Tasten: `xmas-smp-client-<version>.jar` mit der [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods/`-Ordner des **Clients** legen. Die Tasten stehen unter *Optionen → Steuerung → Xmas-SMP* und lassen sich dort frei belegen.

| Fähigkeit | Standardtaste |
|---|---|
| Primär | **R** |
| Sekundär | **G** |
| Ultimate | **V** |

Der Mod meldet dem Server nur „Taste gedrückt“. Ob die Fähigkeit auslöst (Cooldown, Energie, Tier), entscheidet weiterhin der Server. Auf Servern ohne Xmas-SMP sendet er nichts.

Nötig ist er nicht. Ohne Mod gilt: **F** = Primär, **Schleichen + F** = Sekundär, **Schleichen + Rechtsklick** mit dem Kristall in der Haupthand = Ultimate.

## Befehle

Alle Befehle brauchen das Recht `xmassmp.admin`. Ohne Permission-Plugin (z. B. LuckPerms) gilt OP-Level 2. Alle Befehle haben Tab-Completion.

| Befehl | Wirkung |
|---|---|
| `/xmas` | Hilfe |
| `/xmas admin` | Konfigurations-GUI (nur für Spieler, siehe unten) |
| `/xmas give <spieler> crystal <kristall> [tier] [zustand]` | Kristall-Item geben. Kristalle: `fire`, `earth`, `wind`, `shadow`, `nature`. Tier `1`–`2`, Zustand `broken`, `damaged`, `scratched`, `pristine` (Standard: Tier 1, `pristine`) |
| `/xmas give <spieler> item <item>` | Hilfs-Item geben: `upgrader`, `energy_bottle` (mit Register-Eintrag), `gem_trader`, `repair_kit`, `gem_fragment` |
| `/xmas set <spieler> crystal <kristall>` | Aktiven Kristall setzen |
| `/xmas set <spieler> tier <1-2>` | Tier setzen |
| `/xmas set <spieler> energy <wert>` | Energie setzen (Grenzen aus der Config) |
| `/xmas reload` | `config.jsonc` und `events.jsonc` neu laden; Fehler werden angezeigt, die alte Konfiguration bleibt dann aktiv |
| `/xmas event list` | Alle Events mit Status und nächstem Start |
| `/xmas event start <id>` / `stop <id>` | Event sofort starten/beenden (auch deaktivierte) |
| `/xmas event simulate <yyyy-MM-dd> <HH:mm> [dry]` | Testmodus: Uhrzeit simulieren. Mit `dry` werden Aktionen nur geloggt |
| `/xmas event simulate off` | Testmodus beenden; simulierte Events enden mit ihren End-Aktionen |
| `/xmas energy audit [spieler]` | Die letzten Energieänderungen mit Grund und Flaschen-ID |
| `/xmas energy bottles` | Aktive Energy Bottles im Register |
| `/xmas totem status` / `cancel` | Laufendes Repair-Totem anzeigen bzw. ohne Wirkung beenden |

Jede Änderung über `give`, `set`, `reload`, `event` und die Admin-GUI landet in `<welt>/xmassmp/audit.log` (eine JSON-Zeile pro Eintrag: Zeit, Wer, Was, Ziel). Abschaltbar mit `audit.enabled`.

### Kreativ-Tab

Alle Items der Mod stehen im Kreativ-Tab **Xmas-SMP** (`xmassmp:items`): jeder Kristall in Tier 1 und 2 und in jedem Zustand, dazu Upgrader, Energy Bottle, Kristall-Händler, Repair Kit und Kristall-Splitter.

- Mit installiertem [Polymer](https://modrinth.com/mod/polymer) im Client erscheint er als normaler Tab im Kreativinventar. Vanilla-Clients öffnen ihn im Kreativmodus mit `/polymer creative`.
- Die Kristalle aus dem Tab sind **ungebunden** (wie bei `/xmas give … crystal`): Deko- und Anzeige-Items ohne Kräfte. Den aktiven Kristall eines Spielers setzt `/xmas set`.
- Die **Energy Bottle** steht nur für Operatoren im Tab. Beim ersten Rechtsklick eines Admins (`xmassmp.admin` bzw. OP 2) im Kreativmodus wird sie ins Flaschen-Register eingetragen (Herkunft `creative`, Eintrag im `audit.log`) und wirkt dann normal. Bei allen anderen bleibt sie ungültig und wird beim Benutzen entfernt.

### Admin-GUI

`/xmas admin` öffnet ein Kisten-Menü (Vanilla-Client genügt):

- **Allgemein**: Plugin, Fähigkeiten, PvP-Energie, Kristall beim ersten Join, Seelenbindung, Aktivierung mit F und HUD an/aus; Neu laden.
- **Kristalle**: je Kristall an/aus, Cooldown- und Schadens-Multiplikator.
- **Energie**: Start, Minimum, Maximum, Gewinn pro Kill, Verlust pro Tod, Anti-Farming.
- **Spieler**: Online-Spieler wählen, dann Kristall, Tier und Energie setzen oder Cooldowns zurücksetzen.
- **Events**: Status aller Events; Linksklick startet bzw. stoppt, Rechtsklick schaltet den automatischen Start um; laufenden Testmodus beenden.
- **Items**: alle Custom-Items und Kristalle herausnehmen.
- **Gameplay**: Energy Bottles, Upgrader, Händler und Repair-Totem an/aus und ihre Werte; laufendes Totem beenden; Rezepte an/aus.

Zahlen: Linksklick +1, Rechtsklick −1, mit Shift ×5 (Multiplikatoren in Schritten von 0.1). Änderungen werden sofort in `config.jsonc` bzw. `events.jsonc` geschrieben und wirken ohne Reload. Dabei wird nur der geänderte Wert ersetzt, Kommentare und Formatierung bleiben erhalten.

## Konfiguration

`config/xmassmp/config.jsonc` ist kommentiert. Kurzüberblick:

| Schlüssel | Bedeutung |
|---|---|
| `general.enabled` / `general.abilitiesEnabled` | Hauptschalter / alle Fähigkeiten |
| `crystals.<id>.enabled`, `cooldownMultiplier`, `damageMultiplier` | pro Kristall |
| `crystalItem.giveOnJoin` | Spieler ohne Kristall bekommen beim Betreten einen zufälligen |
| `crystalItem.soulbound` | Kristall kann nicht gedroppt, eingelagert oder beim Tod verloren werden |
| `energy.pvpEnabled` | Energie-Gewinn und -Verlust durch Kills und Tode an/aus |
| `energy.start`, `min`, `max` | Energie (Standard 5, 0, 10) |
| `energy.killGain`, `deathLoss` | Gewinn beim PvP-Kill, Verlust bei jedem Tod |
| `energy.sameVictimCooldownMinutes` | Anti-Farming: kein Gewinn für denselben Gegner innerhalb dieser Zeit |
| `energy.dropBottleWhenKillerAtMax` | Sieger mit voller Energie: Energie des Opfers fällt als Flasche zu Boden |
| `energy.stages` | ab welcher Energie der Kristall beschädigt/zerkratzt/makellos aussieht (1/3/5); darunter zerbrochen |
| `energy.access` | Mindest-Energie für Primär/Sekundär/Ultimate (1/3/3) |
| `energy.minStrength` | Fähigkeitsstärke bei niedrigster Energie (steigt bis 1.0 bei `max`) |
| `energyBottle.*` | Ladezeit, Mindest-Rest-Energie, Kampfsperre, Trink-Pause |
| `upgrader.enabled`, `trader.*` | Upgrader an/aus; Händler: Tier/Energie behalten, Wartezeit |
| `totem.*` | Radius, Dauer, Ziel-Energie, Abbauzeit, Teilnahmefenster, Koordinaten ansagen |
| `recipes.<id>` | Rezepte (`pattern` + `key`), `gem_fragments.count` |
| `controls.swapHandsActivates` | F / Schleichen + F lösen Fähigkeiten aus statt die Hände zu tauschen |
| `controls.sneakUseUltimate`, `clientKeys`, `tier2TabColor` | Ultimate per Schleichen + Rechtsklick, Tasten aus dem Client-Mod, gelber Tab-Name für Tier 2 |
| `hud.enabled`, `hud.updateTicks` | Actionbar-HUD an/aus und wie oft es aktualisiert wird |
| `effects.quality` | `LOW`, `MEDIUM`, `HIGH` – dünnt Partikel aus |
| `effects.maxParticlesPerTick` | Partikel-Budget aller Effekte pro Tick |
| `effects.maxDisplays` | Obergrenze gleichzeitiger Display-Entities in Effekten |
| `events.timezone` | Zeitzone der Event-Zeiten (Standard `Europe/Berlin`) |
| `events.catchUpMissed` | Events nachholen, deren Start in eine Server-Pause fiel |
| `audit.enabled` | Audit-Log an/aus |

## Events

Events laufen an festen Tagen zu festen Uhrzeiten. Beschrieben werden sie in `config/xmassmp/events.jsonc`, die Felder sind dort dokumentiert. Das System kann:

- Termine als einmaliges Datum (`2026-12-24`), jährlich (`12-24`) oder als Bereich, auch über Silvester. Dazu optional Wochentage und mehrere Uhrzeiten pro Tag.
- Ankündigungen X Minuten vorher und einen Sekunden-Countdown als Titel.
- Aktionen beim Start und Ende: `broadcast`, `title`, `sound`, `command`. Eigene Typen lassen sich im Code registrieren.
- Modifikatoren, solange ein Event läuft, z. B. `"cooldown_multiplier": 0.5` oder `"energy_multiplier": 2.0`.
- Laufende Events überstehen einen Neustart. Starts, die in eine Server-Pause fielen, werden je nach Config nachgeholt oder übersprungen.
- Ist das Plugin abgeschaltet oder `events.jsonc` defekt, ruht der Plan. Danach enden inzwischen abgelaufene Events mit ihren End-Aktionen (nach einem Serverausfall dagegen still).
- Wird die Systemuhr zurückgestellt (bis 1 Stunde), wartet der Plan, damit nichts doppelt läuft. Bei größeren Sprüngen setzt er ohne Nachholen neu auf.
- Testmodus: Ein eigener Plan läuft ab dem simulierten Datum. Der echte Plan läuft unverändert weiter. Im Trockenlauf (`dry`) wirken simulierte Modifikatoren nicht auf das Spiel.
- Sommer-/Winterzeit: Jede Startzeit feuert genau einmal pro Tag.

## Entwicklung

```
common/          plattformunabhängiger Kern (Kristalle, Spielerdaten, Effekte, Config, Events, Admin) + Tests
fabric/          Fabric-Adapter (erste Plattform)
client/          optionaler Fabric-Client-Mod (Tasten für die Fähigkeiten)
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

Die GitHub Actions bauen und prüfen bei Pushes auf `main`, bei Pull Requests und manuell (`workflow_dispatch`) das Ressourcenpaket, den Kern (inkl. Tests) und das Fabric-JAR. Die Ergebnisse liegen als Artefakte bereit.

### Architektur

Der Kern (`XmasCore`) kennt keine Minecraft-Klassen. Jede Plattform implementiert:

- `Platform`: Ordner, Logger, Online-Spieler
- `PlayerHandle`: Sicht auf einen Spieler (Position, Blickrichtung, Schleichen, Actionbar, Chat)
- `EffectSink`: Partikel und Sounds senden
- `EventHost`: Broadcast, Titel, Sound, Konsolenbefehl

Außerdem ruft sie `XmasCore#tick()` jeden Server-Tick auf und meldet Spieler-Ereignisse (`onPlayerJoin`, `onPlayerQuit`, `onPlayerDeath`, `onPlayerRespawn`, `onSwapHands`, `onUseItem`). Eigene Systeme reagieren darauf über einen `PlayerListener`. Für die Zweithand-Taste nutzt Fabric einen Mixin, weil die Fabric API dafür kein Event hat. Den Befehlsbaum baut sie selbst, die Logik dahinter liegt in `AdminService`.

Kräfte entstehen als `ActiveAbility` / `PassiveAbility`. Animationen baut man mit `EffectTimeline` (Phasen *Aufladen → Auslösen → Nachklingen*) aus `Shapes` (Ring, Spirale, Kugel, Linie, Welle), `ParticleSpec` (inkl. Farbverläufen) und `SoundStack` (geschichtete Sounds mit Tonhöhen-Variation). Für 3D-Effekte spawnt `EffectCanvas#display` Block- oder Item-Displays (`DisplaySpec`), die sich weich bewegen, drehen und skalieren lassen und mit dem Effekt verschwinden.

Die Spielerdaten (`PlayerData`) sind die Wahrheit über Kristall, Tier und Energie. `CrystalService` hält das Kristall-Item im Inventar passend dazu und gleicht es jede Sekunde ab. `EnergyService` verteilt Energie bei Kills und Toden und liefert Bruchstufe und Fähigkeitsstärke. `AbilityControls` setzt die F-Taste in Fähigkeiten um, `HudService` zeichnet die Actionbar.
