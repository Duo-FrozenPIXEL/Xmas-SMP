# Xmas-SMP – Anleitung: Benutzen und Testen

Diese Datei beschreibt Schritt für Schritt, wie man die Mod **baut, installiert, benutzt und testet** – vom leeren Ordner bis zum abgehakten Testprotokoll. Sie richtet sich an Server-Betreiber, Admins und Tester. Alle Angaben stammen aus dem Code (Stand Version `0.9.0`, Fabric, Minecraft `1.21.11`).

> **Kurzfassung für Eilige:** JAR bauen oder aus der CI laden → mit der Fabric API in `mods/` eines Fabric-1.21.11-Servers → Server starten → OP geben → `/xmas` eintippen. Der Testplan steht in [Abschnitt 9](#9-manueller-testplan-im-spiel).
>
> **Direkt zu:** [alle Befehle](#81-befehlsreferenz) · [Crafting-Rezepte](#78-crafting-rezepte) · [Schnelltest in 45 Schritten](#91-schnelltest-ca-30-minuten-alle-kernfunktionen) · [ausführliche Testfälle](#92-ausführliche-testfälle) · [automatische Tests](#10-automatische-tests-entwickler)

---

## Inhalt

1. [Was die Mod im aktuellen Stand kann – und was nicht](#1-was-die-mod-im-aktuellen-stand-kann--und-was-nicht)
2. [Voraussetzungen](#2-voraussetzungen)
3. [JAR-Dateien besorgen](#3-jar-dateien-besorgen)
4. [Testserver aufsetzen (Schritt für Schritt)](#4-testserver-aufsetzen-schritt-für-schritt)
5. [Ressourcenpaket an die Spieler ausliefern](#5-ressourcenpaket-an-die-spieler-ausliefern)
6. [Client einrichten (Vanilla und optionaler Client-Mod)](#6-client-einrichten-vanilla-und-optionaler-client-mod)
7. [Benutzen – Spieler-Sicht](#7-benutzen--spieler-sicht)
8. [Benutzen – Admin-Sicht](#8-benutzen--admin-sicht)
9. [Manueller Testplan im Spiel](#9-manueller-testplan-im-spiel)
10. [Automatische Tests (Entwickler)](#10-automatische-tests-entwickler)
11. [Fehlersuche](#11-fehlersuche)
12. [Testprotokoll-Vorlage](#12-testprotokoll-vorlage)

---

## 1. Was die Mod im aktuellen Stand kann – und was nicht

Bevor du testest, musst du wissen, was überhaupt fertig ist. Sonst meldest du fehlende Funktionen als Fehler.

### Fertig und testbar

| System | Kurz |
|---|---|
| Kristalle | 5 Kristalle (Feuer, Erde, Wind, Schatten, Natur), Tier 1/2, 4 sichtbare Bruchstufen, seelengebunden, zufälliger Kristall beim ersten Join |
| Energie | 0–10, Start 5, PvP-Kill +1, Tod −1, Anti-Farming, Überlauf als Flasche, Energie-Protokoll |
| Energy Bottle | Abfüllen (5 s halten), Trinken, Zerlegen in 9 Splitter, Flaschen-Register gegen Dupes |
| Rezepte | Upgrader, Kristall-Händler, Repair Kit, Splitter – in der Config änderbar |
| Upgrader | Tier 1 → Tier 2 mit Animation, gelber Name in der Tab-Liste |
| Kristall-Händler | GUI mit freier Kristallwahl |
| Repair-Totem | Aufstellen, Teilnahme, Ausscheiden, Belohnung, Abbau in festen 10 s |
| HUD | Actionbar mit drei Fähigkeits-Slots, Energie, Totem-Anzeige |
| Admin | `/xmas`-Befehle, Admin-GUI, Audit-Log, Reload |
| Kreativ-Tab | Tab „Xmas-SMP“ mit allen Kristallen (Tier 1/2, alle Zustände) und Hilfs-Items; Energy Bottle nur für OPs |
| Events | Zeitplan, Ankündigungen, Countdown, Aktionen, Modifikatoren, Testmodus |
| Client-Mod | Optionale Tasten R/G/V |

### Noch NICHT fertig (kein Fehler, wenn es fehlt)

- **Es gibt noch keine einzige Kristall-Kraft** (weder aktiv noch passiv). Der Entwurf steht in `docs/kraefte-entwurf.md`. Folgen davon:
  - Das HUD zeigt bei allen drei Slots das Icon in Grau mit einem Strich: `[Icon] –`.
  - **F tauscht ganz normal die Hände**, weil der Kristall keine aktiven Kräfte hat (so gewollt, `AbilityControls#onSwapHands`).
  - **Schleichen + Rechtsklick mit dem eigenen Kristall** zeigt in der Actionbar: `Die Ultimate gibt es noch nicht.`
  - Die Client-Mod-Tasten zeigen `Die Primärkraft gibt es noch nicht.` / `Die Sekundärkraft …` / `Die Ultimate …`.
  - Der Event-Modifikator `cooldown_multiplier` ist eingebaut, aber im Spiel noch nicht sichtbar (keine Cooldowns ohne Kräfte).
- Paper und NeoForge: nur geplant (`paper/README.md`, `neoforge/README.md`).
- Minecraft 26.x: geplant, aktuell wird nur 1.21.11 gebaut.
- Christmas-Twist: wartet auf die Datei vom Owner.

---

## 2. Voraussetzungen

| Was | Version | Wofür |
|---|---|---|
| Java (JDK) | **21** (z. B. Temurin 21) | Server und Build. `java -version` muss `21` zeigen |
| Minecraft Java Edition | **1.21.11** | Client zum Testen |
| Fabric Loader | **0.18.1** (mindestens 0.17.0) | Server (und Client, falls Client-Mod) |
| Fabric API | **0.139.4+1.21.11** | Pflicht auf dem Server, Pflicht beim Client-Mod |
| Python | 3.10 oder neuer | Nur für den Ressourcenpaket-Validator `tools/resourcepack.py` |
| Git | beliebig | Nur zum Selbstbauen |
| Zweiter Minecraft-Account oder zweiter Client | – | Für PvP-, Totem- und Händler-Tests mit mehreren Spielern |

Die Mod bringt **Polymer**, **sgui** und **fabric-permissions-api** im eigenen JAR mit. Die musst du **nicht** extra installieren.

---

## 3. JAR-Dateien besorgen

Es gibt zwei JARs:

| Datei | Wohin | Pflicht? |
|---|---|---|
| `xmas-smp-fabric-0.9.0.jar` | `mods/` des **Servers** | ja |
| `xmas-smp-client-0.9.0.jar` | `mods/` des **Clients** | nein, nur für eigene Tasten |

### Weg A: aus der GitHub-CI herunterladen (empfohlen)

1. Im Repository auf **Actions** klicken.
2. Links den Workflow **Build** wählen.
3. Den obersten grünen Lauf öffnen (Branch `main` oder der Branch, den du testen willst).
4. Ganz unten unter **Artifacts** herunterladen:
   - `xmas-smp-fabric-1.21.11` → enthält das Server-JAR
   - `xmas-smp-client-1.21.11` → enthält das Client-JAR
   - `resourcepack` → enthält `Xmas-SMP-resourcepack.zip` (nur nötig, wenn du das Pack selbst hostest, siehe [5.3](#53-variante-c-pack-selbst-hosten))
5. ZIPs entpacken. Im Fabric-Artefakt liegen ggf. zwei Dateien: `xmas-smp-fabric-0.9.0.jar` und `xmas-smp-fabric-0.9.0-sources.jar`. **Nimm die ohne `-sources`.**

Einen Lauf von Hand starten: Actions → Build → **Run workflow** (der Workflow hat `workflow_dispatch`).

### Weg B: selbst bauen

```bash
git clone https://github.com/Duo-FrozenPIXEL/Xmas-SMP.git
```

```bash
cd Xmas-SMP
```

```bash
./gradlew :fabric:build :client:build
```

Unter Windows statt `./gradlew` → `gradlew.bat`.

Ergebnis:

- `fabric/build/libs/xmas-smp-fabric-0.9.0.jar`
- `client/build/libs/xmas-smp-client-0.9.0.jar`

Der Build braucht Internetzugriff auf `maven.fabricmc.net`, `maven.nucleoid.xyz` und Maven Central. Ohne Zugriff auf Fabric baut nur der Kern (siehe [Abschnitt 10](#10-automatische-tests-entwickler)).

---

## 4. Testserver aufsetzen (Schritt für Schritt)

### 4.1 Fabric-Server installieren

1. Leeren Ordner anlegen, z. B. `xmas-testserver/`.
2. Auf <https://fabricmc.net/use/server/> einstellen: Minecraft **1.21.11**, Loader **0.18.1**, Installer-Version: neueste. Den Server-Launcher (`fabric-server-mc.1.21.11-loader.0.18.1-launcher.<x>.jar`) herunterladen und in den Ordner legen.
3. Einmal starten (lädt den Vanilla-Server herunter und legt `eula.txt` an, beendet sich dann):

```bash
java -Xmx2G -jar fabric-server-mc.1.21.11-loader.0.18.1-launcher.1.1.0.jar nogui
```

   (Dateinamen an deinen Download anpassen.)
4. `eula.txt` öffnen, `eula=false` → `eula=true` ändern, speichern.

### 4.2 Mods einlegen

In den Ordner `mods/` (wird beim ersten Start angelegt, sonst selbst anlegen):

```
mods/
├── fabric-api-0.139.4+1.21.11.jar      ← von https://modrinth.com/mod/fabric-api (Version für 1.21.11)
└── xmas-smp-fabric-0.9.0.jar           ← aus Abschnitt 3
```

**Nicht** den Client-Mod auf den Server legen (er hat `"environment": "client"` und würde ignoriert, gehört aber nicht dorthin).

### 4.3 `server.properties` für Tests einstellen

Empfohlene Werte für einen Testserver (Datei entsteht beim ersten Start):

| Schlüssel | Wert | Warum |
|---|---|---|
| `pvp` | `true` | Energie durch Kills testen |
| `spawn-protection` | `0` | Im Spawn-Bereich bauen/Totem aufstellen auch ohne OP |
| `difficulty` | `easy` oder höher | Tode durch Mobs testen |
| `online-mode` | `true` (Standard) | Siehe Hinweis unten |
| `enable-rcon` | `true` (optional) | Befehle von außen, z. B. für Konsolen-Tests |
| `rcon.password` | eigenes Passwort | nur mit RCON |
| `gamemode` | `survival` | Fast alle Tests müssen im Überlebensmodus laufen (siehe unten) |

> **Zwei Spieler auf einem PC:** Mit `online-mode=false` kannst du mit zwei Offline-Konten (z. B. über den Prism Launcher) gleichzeitig auf den Server. **Nur lokal/LAN** verwenden und den Port nicht ins Internet freigeben – im Offline-Modus kann sich jeder als jeder ausgeben.

> **Kreativmodus verfälscht Tests:** Im Kreativmodus kann man keine Energie abfüllen (gewollt), das Totem bricht sofort statt nach 10 s, und man stirbt nicht. Admin-Befehle gehen in jedem Modus, Spielfunktionen bitte immer im **Überlebensmodus** testen.

### 4.4 Server starten

```bash
java -Xmx2G -jar fabric-server-mc.1.21.11-loader.0.18.1-launcher.1.1.0.jar nogui
```

**Erwartung im Log (`logs/latest.log`):**

- Fabric listet die geladenen Mods, darunter `xmassmp 0.9.0`, `fabric-api`, `polymer-*`, `sgui`, `fabric-permissions-api-v0`.
- Eine Zeile vom Logger `Xmas-SMP`: `Xmas-SMP gestartet (5 Kristalle)`.
- **Keine** Zeilen mit `ERROR` vom Logger `Xmas-SMP`.

**Erwartung im Dateisystem nach dem ersten Start:**

```
config/xmassmp/config.jsonc        ← Hauptkonfiguration (kommentiert)
config/xmassmp/events.jsonc        ← Events (Beispiel-Weihnachtswoche, alle deaktiviert)
world/xmassmp/                     ← Daten der Welt ("world" = level-name aus server.properties)
    playerdata/<uuid>.json         ← entsteht, sobald ein Spieler joint
    audit.log                      ← entsteht bei der ersten Admin-Änderung
    energy.log                     ← entsteht bei der ersten Energieänderung
    bottles.json                   ← entsteht bei der ersten Energy Bottle
    totem.json                     ← existiert nur, solange ein Totem läuft
    (Event-Zustand)                ← laufende Events für Neustarts
```

### 4.5 Rechte vergeben

Alle `/xmas`-Befehle brauchen das Recht **`xmassmp.admin`**.

- **Ohne Permission-Plugin:** OP-Level 2 genügt. In der Server-Konsole:

```
op <DeinName>
```

- **Mit LuckPerms (Fabric-Version):**

```
lp user <DeinName> permission set xmassmp.admin true
```

Spieler ohne das Recht sehen `/xmas` nicht einmal in der Tab-Completion.

### 4.6 Server stoppen

In der Konsole `stop`. Die Mod speichert dabei Spielerdaten, Event-Zustand und ein laufendes Totem (`Xmas-SMP gestoppt` im Log).

---

## 5. Ressourcenpaket an die Spieler ausliefern

Ohne Ressourcenpaket sehen Spieler die Basis-Items (Echoscherbe, Ghast-Träne, Smaragd, Leuchtfeuer, Prismarinscherbe, verzaubertes Buch) und im HUD Kästchen statt Icons. **Funktionieren** tut trotzdem alles – für einen echten Test brauchst du aber das Pack.

Die Mod registriert ihre Assets bei Polymer und markiert das Pack als **erforderlich** (`PolymerResourcePackUtils.markAsRequired()`).

### 5.1 Variante A: Polymer AutoHost (empfohlen für Server)

Polymer kann das Pack selbst bauen und an jeden Spieler beim Join schicken. Einrichtung laut Polymer-Doku (<https://polymer.pb4.eu>):

1. Server einmal starten und stoppen, damit Polymer seine Config-Dateien unter `config/polymer/` anlegt.
2. In `config/polymer/auto-host.json` den Wert `"enabled"` auf `true` setzen.
3. Server neu starten. Beim Join erscheint beim Spieler der Dialog „Server-Ressourcenpaket“ – annehmen.

> Die genauen Dateinamen und Optionen hängen von der Polymer-Version (`0.15.0+1.21.11`) ab. Wenn eine Datei anders heißt: Polymer-Doku ist maßgeblich.

### 5.2 Variante B: lokal zum Testen (am schnellsten)

1. Pack aus dem Repo bauen:

```bash
python3 tools/resourcepack.py build
```

   Ergebnis: `build/Xmas-SMP-resourcepack.zip`, der SHA-1 wird ausgegeben.
2. Die ZIP in den Ordner `.minecraft/resourcepacks/` des Test-Clients kopieren.
3. Im Spiel: Optionen → Ressourcenpakete → `Xmas-SMP` nach rechts schieben → Fertig.

Alternativ kann Polymer das Pack auch per Befehl erzeugen (`/polymer generate-pack`, Ausgabe laut Polymer-Doku unter `polymer/resource_pack.zip` im Serverordner).

### 5.3 Variante C: Pack selbst hosten

1. `python3 tools/resourcepack.py build` → ZIP und SHA-1.
2. ZIP auf einen Webserver legen (direkter Download-Link).
3. In `server.properties`:

```properties
resource-pack=https://example.org/Xmas-SMP-resourcepack.zip
resource-pack-sha1=<SHA-1 aus der Ausgabe>
require-resource-pack=true
```

### 5.4 Prüfen, ob das Pack aktiv ist

- `/xmas give <DeinName> item gem_fragment` → das Item darf **nicht** wie eine Prismarinscherbe aussehen.
- Die Actionbar zeigt drei kleine Bilder (keine leeren Kästchen) vor `✦ 5/10`.

---

## 6. Client einrichten (Vanilla und optionaler Client-Mod)

### 6.1 Vanilla-Client (Normalfall)

- Minecraft Java **1.21.11** starten, Server hinzufügen (`localhost` bei lokalem Server), verbinden.
- Ressourcenpaket annehmen (Variante A) oder lokal aktivieren (Variante B).
- Nichts weiter nötig.

### 6.2 Optionaler Client-Mod (eigene Tasten)

1. Fabric Loader für **1.21.11** auf dem Client installieren (Installer von <https://fabricmc.net/use/installer/>) und im Launcher das Fabric-Profil wählen.
2. In `.minecraft/mods/` legen:
   - `fabric-api-0.139.4+1.21.11.jar`
   - `xmas-smp-client-0.9.0.jar`
3. Spiel starten. Unter **Optionen → Steuerung → Tastenbelegung** gibt es die Kategorie **Xmas-SMP** mit:

| Fähigkeit | Standardtaste |
|---|---|
| Primär | **R** |
| Sekundär | **G** |
| Ultimate | **V** |

Der Mod schickt dem Server nur „Slot X gedrückt“ (Paket `xmassmp:ability`). Der Server prüft alles selbst. Auf Servern ohne Xmas-SMP sendet der Mod nichts. Der Server ignoriert Tasten, wenn in der Config `controls.clientKeys` auf `false` steht, und verwirft mehr als eine Taste pro 2 Ticks (Spam-Schutz).

### 6.3 Optional: Polymer im Client (Kreativ-Tab als echter Tab)

Nur für Admins/Tester interessant, die den Kreativ-Tab der Mod (siehe [8.6](#86-kreativ-tab)) als normalen Tab im Kreativinventar sehen wollen:

1. Fabric Loader und Fabric API wie in [6.2](#62-optionaler-client-mod-eigene-tasten) Schritt 1–2 einrichten.
2. Zusätzlich [Polymer](https://modrinth.com/mod/polymer) in der Version für **1.21.11** in `.minecraft/mods/` legen (passend zur Server-Version `0.15.0+1.21.11`).
3. Im Kreativinventar gibt es dann einen Tab **Xmas-SMP**.

Ohne Polymer im Client funktioniert der Tab trotzdem: im Kreativmodus `/polymer creative` eintippen.

---

## 7. Benutzen – Spieler-Sicht

### 7.1 Erster Join: der Kristall

- Wer noch keinen Kristall hat, bekommt beim Betreten **einen zufälligen aktivierten Kristall**. Chat: `Du hast den <Name>-Kristall erhalten. Er ist an dich gebunden.` Dazu eine aufsteigende Partikelspirale in Kristallfarbe.
- Das gilt auch für Spieler, die später neu dazukommen (`crystalItem.giveOnJoin`).
- Kristallfarben: **Feuer** rot · **Erde** orange · **Wind** weiß · **Schatten** lila · **Natur** grün.

### 7.2 Seelenbindung

Mit `crystalItem.soulbound = true` (Standard):

- Der Kristall lässt sich nicht droppen (Q), nicht in Kisten/Truhen/Werkbänke legen und geht beim Tod nicht verloren. Er taucht **innerhalb von 1 Sekunde** (Abgleich alle 20 Ticks) wieder im Inventar auf.
- Kopien (z. B. in Behältern oder Item-Rahmen) sind wirkungslos und verschwinden.
- Die **Spielerdaten** sind die Wahrheit, das Item ist nur die Anzeige. Ändert ein Admin Kristall/Tier/Energie, passt sich das Item sofort an.

### 7.3 Das Kristall-Item lesen

Der Tooltip zeigt: Name (`<Kristall>-Kristall`), `Tier 1|2`, `Zustand: …`, `Energie: X (Zustandsname)`, `Seelengebunden`.

Vier sichtbare Bruchstufen (Schwellen aus `energy.stages`):

| Energie | Aussehen (`Zustand`) |
|---|---|
| 0 | zerbrochen |
| 1–2 | beschädigt |
| 3–4 | zerkratzt |
| 5–10 | makellos |
| 10 | makellos + Verzauberungs-Schimmer |

Elf Zustandsnamen für die Energie: 0 Zerbrochen · 1 Ruiniert · 2 Beschädigt · 3 Gerissen · 4 Zerkratzt · 5 Makellos · 6–10 Makellos +1 … +5.

Beim Wechsel der Bruchstufe spielt eine **Bruch-Animation** (Abstieg) bzw. **Reparatur-Animation** (Aufstieg).

### 7.4 Das HUD (Actionbar über der Hotbar)

Aufbau: `[Icon Primär] Status   [Icon Sekundär] Status   [Icon Ultimate] Status   ✦ Energie/Max   [Totem-Info]`

| Anzeige | Bedeutung |
|---|---|
| `✔` (grün) | bereit |
| `12s` / `1:05` | Cooldown |
| `T2` | braucht Tier 2 |
| `✦3` (rot) | zu wenig Energie, braucht 3 |
| `✖` (rot) | Kristall zerbrochen (0 Energie) |
| `–` (grau) | Fähigkeit gibt es noch nicht (**aktuell bei allen drei Slots**) |
| `✦ 5/10` | Energie; rot, wenn zerbrochen |
| `⚠ Totem 7` | das Totem, in dessen Kreis du stehst, wird abgebaut; noch 7 s |

Kurze Rückmeldungen (z. B. Fehlermeldungen) überdecken das HUD für 2 Sekunden. Aktualisierung alle 10 Ticks (`hud.updateTicks`).

### 7.5 Steuerung

| Aktion | Ohne Client-Mod | Mit Client-Mod |
|---|---|---|
| Primärkraft | **F** (Zweithand-Taste) | **R** |
| Sekundärkraft | **Schleichen + F** | **G** |
| Ultimate (nur Tier 2) | **Schleichen + Rechtsklick** mit dem eigenen Kristall in der Haupthand | **V** |

Solange es keine Kräfte gibt: F tauscht normal die Hände, siehe [Abschnitt 1](#noch-nicht-fertig-kein-fehler-wenn-es-fehlt).

### 7.6 Energie-Regeln

| Ereignis | Wirkung |
|---|---|
| Du besiegst einen Spieler im PvP | +1 (`energy.killGain`, × `energy_multiplier` eines laufenden Events, gerundet) |
| Du stirbst (egal wodurch: Spieler, Mob, Lava, Sturz) | −1 (`energy.deathLoss`) |
| Opfer hatte 0 Energie | Sieger bekommt nichts. Chat: `Keine Energie: <Name> hatte keine Energie mehr.` |
| Gleiches Opfer innerhalb von 30 min erneut | nichts. Chat: `Keine Energie: Du hast <Name> erst vor kurzem besiegt.` (`energy.sameVictimCooldownMinutes`, wird bei Server-Neustart zurückgesetzt) |
| Sieger hat schon 10 | Energie des Opfers fällt als **Energy Bottle** zu Boden (`energy.dropBottleWhenKillerAtMax`) |
| `energy.pvpEnabled = false` | Kills und Tode ändern die Energie gar nicht mehr |

Als Kill zählt auch, wenn du jemanden schlägst und er danach z. B. durch Fallschaden stirbt (Minecraft-„Kill-Credit“).

Zugriff nach Energie (`energy.access`): **0** = keine aktiven Kräfte · **1–2** = nur Primär · **ab 3** = Primär + Sekundär, mit Tier 2 auch Ultimate. Passive wirken immer (sobald es welche gibt).

### 7.7 Energy Bottle

**Abfüllen (−1 Energie → 1 Flasche):**

1. Leere **Glasflasche** in die **Haupthand**.
2. **Schleichen** und **Rechtsklick gedrückt halten** – **5 Sekunden** (`energyBottle.chargeSeconds`).
3. Unter dem Fadenkreuz läuft ein Ladebalken (Untertitel), dazu ein Farbband von der Brust zur Hand und steigende Glockentöne.
4. Fertig: Titel `+1 Energy Bottle`, die Glasflasche ist weg, die Energy Bottle im Inventar, Energie −1.

Abbruch, wenn du: loslässt (länger als 6 Ticks kein Rechtsklick), aufhörst zu schleichen, den Hotbar-Slot wechselst, dich mehr als **1 Block** waagrecht bewegst, Schaden nimmst oder stirbst.

Abfüllen ist **gesperrt**:

| Grund | Meldung in der Actionbar |
|---|---|
| Kreativmodus | `Im Kreativmodus kannst du keine Energie abfüllen.` |
| Energie danach unter 1 (`keepAtLeast`) | `Du brauchst mindestens 2 Energie zum Abfüllen.` |
| In den letzten 15 s PvP-Schaden bekommen **oder ausgeteilt** | `Im Kampf kannst du nicht abfüllen (noch Xs)` |
| Kein Kristall | `Ohne Kristall gibt es keine Energie zum Abfüllen.` |
| Abgeschaltet | `Energy Bottles sind gerade abgeschaltet.` |

**Trinken (+1 Energie):** Energy Bottle in die Haupthand, **Rechtsklick**. Funktioniert auch bei 0 Energie und für jeden Spieler (auch für andere als den Abfüller). Danach bekommst du die leere Glasflasche zurück. Gesperrt bei voller Energie (`Deine Energie ist schon voll.`) und 1 s nach dem letzten Trinken.

**Zerlegen:** 1 Energy Bottle (formlos, im 2×2-Inventarfeld oder in der Werkbank) → **9 Kristall-Splitter**. Die leere Glasflasche bleibt im Feld liegen.

**Dupe-Schutz:** Jede Flasche hat eine eigene ID in `world/xmassmp/bottles.json`. Wird eine Kopie getrunken, verschwindet sie mit `Diese Energy Bottle ist ungültig und wurde entfernt.`, und im Energie-Protokoll steht ein rot markierter Eintrag `suspicious`.

### 7.8 Crafting-Rezepte

Alle Rezepte sind die **Standardwerte** aus `config/xmassmp/config.jsonc` (Abschnitt `recipes`). Sie lassen sich dort ohne Neubau ändern (siehe unten). Jedes Rezept funktioniert in der **Werkbank** und im **Crafter**-Block. Geformte Rezepte passen auch **gespiegelt** (wie in Vanilla). Im Rezeptbuch tauchen sie nicht zwingend auf – einfach von Hand einlegen.

Die Raster unten zeigen das 3×3-Feld der Werkbank von oben links nach unten rechts.

#### 7.8.1 Kristall-Splitter (aus der Energy Bottle)

Die **einzige** Quelle für Kristall-Splitter.

| | | |
|:---:|:---:|:---:|
| | | |
| | Energy Bottle | |
| | | |

- **Zutaten:** genau **1 Energy Bottle** – formlos, in **irgendein** Feld. Es darf **nichts anderes** im Gitter liegen.
- **Geht auch im 2×2-Feld** des Inventars.
- **Ergebnis:** **9 × Kristall-Splitter** (`xmassmp:gem_fragment`, stapelbar bis 64). Anzahl einstellbar: `recipes.gem_fragments.count`.
- **Rest:** Die leere **Glasflasche** bleibt im Feld liegen.
- **Bedingung:** Die Flasche muss im Flaschen-Register **aktiv** sein. Kopien, Fälschungen und unregistrierte Flaschen aus dem Kreativ-Tab zeigen **kein Ergebnis**. Beim Herausnehmen wird die Flasche im Register geschlossen (Grund `craft`).
- **Schalter:** `recipes.gem_fragments.enabled` und zusätzlich `energyBottle.enabled`.

#### 7.8.2 Upgrader

| | | |
|:---:|:---:|:---:|
| Kristall-Splitter | Drachenatem | Kristall-Splitter |
| Drachenatem | Witherskelettschädel | Drachenatem |
| Kristall-Splitter | Drachenatem | Kristall-Splitter |

| Zeichen in der Config | Item | ID | Anzahl |
|:---:|---|---|:---:|
| `F` | Kristall-Splitter | `xmassmp:gem_fragment` | 4 |
| `B` | Drachenatem | `minecraft:dragon_breath` | 4 |
| `W` | Witherskelettschädel | `minecraft:wither_skeleton_skull` | 1 |

**Ergebnis:** 1 × Upgrader (`xmassmp:upgrader`). Config: `"pattern": ["FBF", "BWB", "FBF"]`.

#### 7.8.3 Kristall-Händler

| | | |
|:---:|:---:|:---:|
| Diamant | Drachenatem | Diamant |
| Drachenatem | Kristall-Splitter | Drachenatem |
| Diamant | Drachenatem | Diamant |

| Zeichen in der Config | Item | ID | Anzahl |
|:---:|---|---|:---:|
| `D` | Diamant | `minecraft:diamond` | 4 |
| `B` | Drachenatem | `minecraft:dragon_breath` | 4 |
| `F` | Kristall-Splitter | `xmassmp:gem_fragment` | 1 |

**Ergebnis:** 1 × Kristall-Händler (`xmassmp:gem_trader`). Config: `"pattern": ["DBD", "BFB", "DBD"]`.

#### 7.8.4 Repair Kit

| | | |
|:---:|:---:|:---:|
| Kristall-Splitter | Amboss | Kristall-Splitter |
| Diamantblock | Netherit-Schmiedevorlage | Diamantblock |
| Kristall-Splitter | Amboss | Kristall-Splitter |

| Zeichen in der Config | Item | ID | Anzahl |
|:---:|---|---|:---:|
| `F` | Kristall-Splitter | `xmassmp:gem_fragment` | 4 |
| `A` | Amboss | `minecraft:anvil` | 2 |
| `D` | Diamantblock | `minecraft:diamond_block` | 2 |
| `T` | Netherit-Schmiedevorlage | `minecraft:netherite_upgrade_smithing_template` | 1 |

**Ergebnis:** 1 × Repair Kit / Reparatur-Set (`xmassmp:repair_kit`). Config: `"pattern": ["FAF", "DTD", "FAF"]`.

#### 7.8.5 Was kostet was? (Gesamtrechnung)

| Ziel | Energy Bottles (= Energie) | Weitere Zutaten |
|---|:---:|---|
| 9 Kristall-Splitter | 1 | – |
| Upgrader | 1 (4 von 9 Splittern) | 4 Drachenatem, 1 Witherskelettschädel |
| Kristall-Händler | 1 (1 von 9 Splittern) | 4 Diamanten, 4 Drachenatem |
| Repair Kit | 1 (4 von 9 Splittern) | 2 Ambosse, 2 Diamantblöcke, 1 Netherit-Schmiedevorlage |

Aus **einer** Energy Bottle reichen die 9 Splitter z. B. für Upgrader + Repair Kit + Händler (4 + 4 + 1).

Woher die Vanilla-Zutaten kommen:

| Zutat | Beschaffung im Überlebensmodus |
|---|---|
| Drachenatem | Leere Glasflasche in die lila Atemwolke des Enderdrachen halten (oder Drachenfeuerball-Wolke) |
| Witherskelettschädel | Seltener Drop von Witherskeletten (Netherfestung); Plünderung erhöht die Chance |
| Netherit-Schmiedevorlage | Truhen in Bastionsruinen; vervielfältigen: 7 Diamanten + 1 Netherrack + 1 Vorlage → 2 Vorlagen |
| Amboss | 3 Eisenblöcke + 4 Eisenbarren |
| Diamantblock | 9 Diamanten |

#### 7.8.6 Zutaten zum Testen schnell besorgen

Als OP im Chat (oder in der Konsole ohne `/` und mit Spielernamen statt `@s`):

```
/give @s minecraft:dragon_breath 8
/give @s minecraft:wither_skeleton_skull 1
/give @s minecraft:diamond 4
/give @s minecraft:anvil 2
/give @s minecraft:diamond_block 2
/give @s minecraft:netherite_upgrade_smithing_template 1
/give @s minecraft:glass_bottle 16
/give @s minecraft:crafter 1
/give @s minecraft:crafting_table 1
/xmas give @s item energy_bottle
/xmas give @s item gem_fragment
```

Hinweise:

- `/xmas give … item gem_fragment` gibt **einen** Splitter pro Aufruf. Für 9 Splitter einfach eine Energy Bottle geben und zerlegen, oder einen ganzen Stapel aus dem Kreativ-Tab nehmen.
- **Nicht** `/give @s xmassmp:<item>` benutzen: So entstehen Items **ohne** die internen Daten der Mod (falscher Name, kein eigenes Aussehen; Upgrader, Händler, Repair Kit und Energy Bottle reagieren nicht). Immer `/xmas give`, die Admin-GUI (Seite Items) oder den Kreativ-Tab nehmen.

#### 7.8.7 Rezepte ändern

In `config/xmassmp/config.jsonc`, Abschnitt `recipes`:

- `pattern`: 1 bis 3 Zeilen mit je 1 bis 3 Zeichen; **Leerzeichen = leeres Feld**.
- `key`: Zeichen → Item-ID (`minecraft:…` oder `xmassmp:gem_fragment`).
- `enabled`: Rezept an/aus (auch in der Admin-GUI → Gameplay).
- Beim Splitter-Rezept nur `enabled` und `count`.

Beispiel – Upgrader mit Netherstern statt Witherskelettschädel:

```jsonc
"upgrader": {
  "enabled": true,
  "pattern": ["FBF", "BWB", "FBF"],
  "key": { "F": "xmassmp:gem_fragment", "B": "minecraft:dragon_breath", "W": "minecraft:nether_star" }
},
```

Danach `/xmas reload`. Ist ein Rezept ungültig (z. B. Zeichen im `pattern` ohne Eintrag in `key`), meldet der Reload `recipes.<id>: … – Rezept abgeschaltet` und **nur dieses** Rezept ist aus.

#### 7.8.8 Kein Rezept (nur per Befehl/Kreativ-Tab)

- **Kristalle** – gibt es nur über den ersten Join, `/xmas set` (aktiver Kristall) oder als ungebundenes Deko-Item über `/xmas give … crystal` bzw. den Kreativ-Tab.
- **Energy Bottle** – nur durch Abfüllen ([7.7](#77-energy-bottle)), als Überlauf beim PvP-Kill, per `/xmas give … item energy_bottle`, Admin-GUI oder Kreativ-Tab.

Alle Xmas-Items sind eigene Server-Items ohne Vanilla-Funktion (kein Brauen, kein Platzieren als Leuchtfeuer, kein Handel mit Dorfbewohnern, keine Vanilla-Rezepte mit den Basis-Items).

### 7.9 Upgrader

Upgrader in die Haupthand, **Rechtsklick** → Kristall wird **Tier 2**, der Upgrader ist verbraucht. Titel `Tier 2 – Deine Ultimate ist freigeschaltet`, Chat-Nachricht, Animation (Säule, schwebender Kristall, Lichtblitz, Ring). Dein Name steht danach **gelb** in der Tab-Liste (`controls.tier2TabColor`).

Gesperrt: schon Tier 2 (`Dein Kristall ist schon Tier 2.`), kein Kristall, abgeschaltet.

### 7.10 Kristall-Händler

Händler in die Haupthand, **Rechtsklick** → Truhen-GUI `Kristall-Händler › Wähle deinen Kristall`:

- Reihe 2: die fünf Kristalle. Dein eigener ist grau (nicht wählbar), abgeschaltete zeigen eine Barriere.
- Klick auf einen anderen Kristall → Tausch. Erst **jetzt** wird der Händler verbraucht (er muss dann noch in der Haupthand sein).
- Tier und Energie bleiben (`trader.keepTier`, `trader.keepEnergy`), Cooldowns werden zurückgesetzt.
- Unten Mitte: **Abbrechen** – nichts wird verbraucht.
- Wartezeit zwischen zwei Täuschen: `trader.cooldownMinutes` (Standard 0).

### 7.11 Repair-Totem

1. **Repair Kit** in die Haupthand, **Rechtsklick auf einen Block** → über dem angeklickten Block entsteht das Totem (sieht aus wie ein aktiver, unheilvoller Tresor). Der Platz muss ersetzbar sein (Luft, Gras …) und innerhalb der Weltgrenze.
2. Alle Spieler bekommen einen Titel `✚ Repair-Totem` mit Koordinaten und eine Chat-Nachricht.
3. **Teilnehmer** ist, wer in den **ersten 30 Sekunden** im Kreis steht (Radius **15 Blöcke** waagrecht, ±15 Blöcke senkrecht, gleiche Dimension). Chat: `Du nimmst am Repair-Totem teil.`
4. Nach **10 Minuten** zerfällt das Totem. Alle verbliebenen Teilnehmer mit **weniger als 5** Energie bekommen **genau 5** (auch von 0). Wer schon ≥ 5 hat, bekommt nichts.
5. Ausscheiden: Kreis verlassen, sterben, ausloggen. Der Kreis ist **kein** Schutzschild.
6. Ankündigungen bei noch 5 min, 1 min, 30 s, 10 s.
7. **Global läuft höchstens ein Totem.**

**Zerstören:** Jeder kann das Totem abbauen – es dauert **fest 10 Sekunden**, egal mit welchem Werkzeug, Effizienz oder Eile. Loslassen, Wegschauen oder Sterben setzt den Countdown zurück. TNT, Kolben und Feuer wirken nicht. Beim ersten Schlag: Chat `⚠ Das Repair-Totem bei … wird angegriffen!` (höchstens alle 10 s). Wird es zerstört, bekommt **niemand** Energie und das Kit ist verloren.

---

## 8. Benutzen – Admin-Sicht

### 8.1 Befehlsreferenz

#### 8.1.1 Grundregeln für alle `/xmas`-Befehle

- **Recht:** `xmassmp.admin`. Ohne Permission-Plugin genügt **OP-Level 2** (`/op <name>`). Spieler ohne das Recht sehen `/xmas` nicht (auch nicht in der Tab-Completion).
- **Wo:** im Chat (mit `/`), in der **Server-Konsole** (ohne `/`), per **RCON**, aus **Befehlsblöcken** und Funktionen. Einzige Ausnahme: `/xmas admin` geht nur als Spieler.
- **Tab-Completion:** Jeder Teil lässt sich mit der Tab-Taste ergänzen (Kristalle, Items, Zustände, Event-IDs, Datumsvorschläge).
- **Schreibweise hier:** `<…>` = Pflicht, `[…]` = optional, `a|b` = eins davon.
- **`<spieler>`:** muss **online** sein. Erlaubt sind Namen und Minecraft-Selektoren wie `@s` (du selbst), `@p` (nächster Spieler). Selektoren für mehrere Spieler (`@a`) gehen nicht.
- **Antworten:** Erfolg mit dem Präfix der Mod in grau/weiß, Fehler in **rot**. Erfolgsmeldungen gehen wie bei Vanilla-Befehlen zusätzlich grau an andere OPs und ins Server-Log.
- **Kern nicht gestartet:** Jeder Befehl antwortet `Xmas-SMP ist nicht gestartet – Details im Server-Log` → `logs/latest.log` prüfen.
- **Protokoll:** Alle ändernden Befehle schreiben eine Zeile in `world/xmassmp/audit.log` (Wer, Was, Ziel, Zeit). `Wer` ist dein Spielername, `Konsole` für die Server-Konsole, sonst der Name der Quelle.

#### 8.1.2 Übersicht

| # | Befehl | Kurz |
|:---:|---|---|
| 1 | `/xmas` | Hilfe anzeigen |
| 2 | `/xmas admin` | Admin-GUI öffnen |
| 3 | `/xmas give <spieler> crystal <kristall> [tier] [zustand]` | Deko-Kristall geben |
| 4 | `/xmas give <spieler> item <item>` | Hilfs-Item geben |
| 5 | `/xmas set <spieler> crystal <kristall>` | Aktiven Kristall setzen |
| 6 | `/xmas set <spieler> tier <1-2>` | Tier setzen |
| 7 | `/xmas set <spieler> energy <wert>` | Energie setzen |
| 8 | `/xmas reload` | Config und Events neu laden |
| 9 | `/xmas event list` | Events anzeigen |
| 10 | `/xmas event start <id>` | Event sofort starten |
| 11 | `/xmas event stop <id>` | Event beenden |
| 12 | `/xmas event simulate <yyyy-MM-dd> <HH:mm> [dry]` | Testmodus mit simulierter Uhrzeit |
| 13 | `/xmas event simulate off` | Testmodus beenden |
| 14 | `/xmas energy audit [spieler]` | Energie-Protokoll anzeigen |
| 15 | `/xmas energy bottles` | Aktive Energy Bottles anzeigen |
| 16 | `/xmas totem status` | Laufendes Totem anzeigen |
| 17 | `/xmas totem cancel` | Laufendes Totem abbrechen |

#### 8.1.3 Jeder Befehl im Detail

---

##### 1 · `/xmas`

Zeigt die Kurzhilfe mit allen Befehlen (9 Zeilen). Ändert nichts.

---

##### 2 · `/xmas admin`

Öffnet die Admin-GUI (Truhen-Menü, siehe [8.2](#82-admin-gui-xmas-admin)). **Nur als Spieler** – aus Konsole/RCON kommt die Vanilla-Fehlermeldung, dass ein Spieler nötig ist. Schlägt das Öffnen fehl: `Admin-GUI konnte nicht geöffnet werden`.

---

##### 3 · `/xmas give <spieler> crystal <kristall> [tier] [zustand]`

Legt ein **Kristall-Item** ins Inventar des Spielers.

| Parameter | Werte | Standard |
|---|---|---|
| `<kristall>` | `fire` (Feuer), `earth` (Erde), `wind` (Wind), `shadow` (Schatten), `nature` (Natur) | – |
| `[tier]` | `1` oder `2` | `1` |
| `[zustand]` | `pristine` (makellos), `scratched` (zerkratzt), `damaged` (beschädigt), `broken` (zerbrochen) | `pristine` |

**Wichtig:** Dieses Item ist **ungebunden** – es gehört niemandem, hat **keine Kräfte** und ändert **nicht** den Kristall des Spielers. Es dient als Deko, zum Ansehen der Texturen oder für Tests. Den echten Kristall ändert Befehl 5. (Der Tooltip zeigt trotzdem „Seelengebunden“ – bekannte Unschönheit.) Einen Zustand kann man nur zusammen mit dem Tier angeben.

Beispiele:

```
/xmas give Steve crystal fire
/xmas give Steve crystal shadow 2
/xmas give @s crystal earth 2 broken
```

| Antwort | Bedeutung |
|---|---|
| `Steve hat einen Feuer-Kristall (Tier 1, pristine) erhalten` | OK |
| `Unbekannter Kristall: lava` | Kristall-ID falsch |
| `Unbekannter Zustand: kaputt (broken, damaged, scratched, pristine)` | Zustand falsch |

Audit: `give`, Details `crystal=… tier=… stage=…`.

---

##### 4 · `/xmas give <spieler> item <item>`

Legt ein Hilfs-Item ins Inventar. Pro Aufruf **ein** Stück.

| `<item>` | Item | Hinweis |
|---|---|---|
| `upgrader` | Upgrader | voll funktionsfähig |
| `energy_bottle` | Energy Bottle | wird **im Flaschen-Register angelegt** (Herkunft `admin`, Ersteller = du) und ist damit gültig |
| `gem_trader` | Kristall-Händler | voll funktionsfähig |
| `repair_kit` | Repair Kit (Reparatur-Set) | voll funktionsfähig |
| `gem_fragment` | Kristall-Splitter | 1 Stück |

Beispiele:

```
/xmas give Steve item repair_kit
/xmas give @s item energy_bottle
```

| Antwort | Bedeutung |
|---|---|
| `Steve hat Reparatur-Set erhalten` (bzw. `Upgrader`, `Energieflasche`, `Kristall-Händler`, `Kristall-Splitter`) | OK |
| `Unbekanntes Item: schwert` | Item-ID falsch |

Audit: `give`, Details `item=…`.

---

##### 5 · `/xmas set <spieler> crystal <kristall>`

Setzt den **aktiven** Kristall des Spielers (die Spielerdaten). Das Kristall-Item im Inventar passt sich sofort an, die HUD-Farbe wechselt. Tier, Energie und Cooldowns bleiben.

```
/xmas set Steve crystal nature
```

| Antwort | Bedeutung |
|---|---|
| `Kristall von Steve ist jetzt Natur` | OK |
| `… (Kristall ist in der Config deaktiviert)` (gelb angehängt) | Gesetzt, aber der Kristall ist in `crystals.<id>.enabled` aus |
| `Unbekannter Kristall: …` | ID falsch |

Audit: `set_crystal`, Details `alt -> neu`.

---

##### 6 · `/xmas set <spieler> tier <1-2>`

Setzt das Tier. Item passt sich an; Tier 2 = gelber Name in der Tab-Liste (`controls.tier2TabColor`). Werte außer 1 und 2 lehnt schon die Eingabe ab.

```
/xmas set Steve tier 2
```

Antwort: `Tier von Steve ist jetzt 2`. Audit: `set_tier`.

---

##### 7 · `/xmas set <spieler> energy <wert>`

Setzt die Energie auf genau diesen Wert. Erlaubt ist nur `energy.min` bis `energy.max` (Standard 0–10). Das Item wechselt ggf. die Bruchstufe (mit Animation), bei 10 schimmert es.

```
/xmas set Steve energy 0
/xmas set Steve energy 10
```

| Antwort | Bedeutung |
|---|---|
| `Energie von Steve ist jetzt 0` | OK |
| `Energie muss 0–10 sein` | Wert außerhalb der Grenzen |

Audit: `set_energy`. Zusätzlich ein Eintrag im **Energie-Protokoll** mit Grund `admin` und deinem Namen.

---

##### 8 · `/xmas reload`

Lädt `config/xmassmp/config.jsonc` **und** `config/xmassmp/events.jsonc` neu.

| Antwort | Bedeutung |
|---|---|
| `Neu geladen` + ggf. graue Zeilen `· …` | OK; die Zeilen sind Warnungen (z. B. ein ungültiger Wert, der auf einen Standard gesetzt wurde) |
| `Neu laden fehlgeschlagen – vorherige Einstellungen bleiben aktiv:` + Zeilen `· …` | Datei kaputt (z. B. JSON-Fehler); **nichts** wurde übernommen |

Audit: `reload` (bei Fehler mit `FEHLER: …`). Änderungen über die Admin-GUI brauchen **keinen** Reload.

---

##### 9 · `/xmas event list`

Listet alle Events aus `events.jsonc`.

```
Events (Zeitzone Europe/Berlin) [Testmodus: 25.12.2026 19:54]
xmas_week (Weihnachtswoche) – deaktiviert
advent_evening (Weihnachtsabend-Bonus) – nächster Start 25.12.2026 20:00
xmas_eve (Heiligabend) – läuft bis 24.12.2026 20:00
```

Mögliche Status: `läuft bis <Datum Zeit>`, `läuft bis zum Stopp`, `deaktiviert`, `nächster Start <Datum Zeit>`, `kein weiterer Termin`. Ohne Events: `Keine Events in events.jsonc`. Der Zusatz `[Testmodus: …]` erscheint nur bei aktivem Testmodus.

---

##### 10 · `/xmas event start <id>`

Startet ein Event **sofort** mit seinen `onStart`-Aktionen und Modifikatoren – auch wenn es `enabled: false` ist. Dauer laut `durationMinutes`.

```
/xmas event start xmas_eve
```

| Antwort | Bedeutung |
|---|---|
| `Event xmas_eve gestartet (120 min)` | OK |
| `Event new_year ausgelöst` | OK, punktuelles Event (Dauer 0) |
| `Event xmas_eve läuft bereits` | läuft schon |
| `Unbekanntes Event: …` | ID falsch |

Audit: `event_start`.

---

##### 11 · `/xmas event stop <id>`

Beendet ein laufendes Event **mit** seinen `onEnd`-Aktionen. Die Tab-Completion schlägt nur laufende Events vor.

Antworten: `Event xmas_eve beendet` bzw. `Event xmas_eve läuft nicht`. Audit: `event_stop`.

---

##### 12 · `/xmas event simulate <yyyy-MM-dd> <HH:mm> [dry]`

Startet den **Testmodus**: Ein eigener Event-Plan läuft ab der angegebenen Uhrzeit (Zeitzone `events.timezone`) und **zählt ab da in Echtzeit weiter**. Der echte Plan läuft unverändert im Hintergrund weiter.

| Variante | Was passiert |
|---|---|
| ohne `dry` | Aktionen werden **wirklich** ausgeführt (Chat, Titel, Sounds, Befehle, Modifikatoren). Antwort endet mit `– Aktionen werden ausgeführt!` |
| mit `dry` (Trockenlauf) | Aktionen landen **nur im Server-Log** als `[Trockenlauf] Broadcast: …`, `[Trockenlauf] Titel: …`, `[Trockenlauf] Sound: …`, `[Trockenlauf] Befehl: /…`. Modifikatoren wirken nicht. Antwort: `– Aktionen werden nur geloggt` |

**Nur Events mit `enabled: true` laufen im Testmodus.** Ein erneuter `simulate`-Befehl beendet die vorherige Simulation sauber und startet eine neue. Datum genau als `yyyy-MM-dd`, Uhrzeit als `HH:mm`.

```
/xmas event simulate 2026-12-24 17:55 dry
/xmas event simulate 2026-12-31 23:59
```

Tab-Vorschläge: heute, 24.12., 25.12., 31.12. dieses Jahres, 01.01. nächstes Jahr; Uhrzeiten `18:00`, `17:59 dry`, `23:59 dry`, `00:00`.

| Antwort | Bedeutung |
|---|---|
| `Testmodus aktiv: simulierte Zeit 24.12.2026 17:55 – Aktionen werden nur geloggt Beenden mit /xmas event simulate off` | OK |
| `Format: /xmas event simulate yyyy-MM-dd HH:mm [dry]` | Datum/Uhrzeit falsch geschrieben oder unbekanntes drittes Wort |

Audit: `event_simulate`.

---

##### 13 · `/xmas event simulate off`

Beendet den Testmodus. Simulierte Events, die gerade laufen, enden **mit** ihren End-Aktionen.

Antworten: `Testmodus beendet – simulierte Events wurden mit ihren End-Aktionen beendet` bzw. `Testmodus ist nicht aktiv`. Audit: `event_simulate_off`.

---

##### 14 · `/xmas energy audit [spieler]`

Zeigt die **letzten 15** Energieänderungen aus `world/xmassmp/energy.log` – für alle oder nur für einen (Online-)Spieler. Neueste stehen unten.

Beispiel (IDs gekürzt):

```
[Xmas-SMP] Energie-Protokoll von Steve (neueste unten):
2026-12-24 17:02:11 Steve 5 → 4 (death, Alex)
2026-12-24 17:05:40 Steve 4 → 3 (bottle_fill, 1f0c…)
2026-12-24 17:06:02 Steve 3 → 3 (suspicious, 1f0c… bereits verbraucht (Kopie))
```

- Zeitangaben in **UTC** (Berlin im Winter = UTC + 1 h).
- Gründe: `kill` (Bezug: Opfer), `death` (Bezug: Täter oder leer), `bottle_fill`, `bottle_drink` (Bezug: Flaschen-ID), `totem` (Bezug: wer es aufgestellt hat), `admin` (Bezug: Admin), `suspicious` (rot: ungültige Flasche, Energie unverändert).
- Ohne Einträge: `Keine Einträge im Energie-Protokoll.`

---

##### 15 · `/xmas energy bottles`

Zeigt, wie viele Energy Bottles im Register **aktiv** (noch nicht getrunken/zerlegt) sind, und die neuesten 15:

```
[Xmas-SMP] 3 aktive Energy Bottles:
1f0c2a9b fill · Steve · 2026-12-24T16:05:40.512Z
7d81e0c4 admin · Konsole · 2026-12-24T16:10:00.087Z
a3b9f210 creative · Alex · 2026-12-24T16:12:31.904Z
```

Spalten: erste 8 Zeichen der ID · Herkunft (`fill`, `admin`, `overflow`, `creative`) · Ersteller · Erstellzeit (UTC).

---

##### 16 · `/xmas totem status`

```
[Xmas-SMP] Totem bei 120 64 -35 (overworld) · noch 7:42 · 3 Teilnehmer · aufgestellt von Steve
```

Ohne Totem: `Kein Repair-Totem aktiv.`

---

##### 17 · `/xmas totem cancel`

Beendet das laufende Totem **ohne Wirkung** (niemand bekommt Energie, das Kit ist weg). An alle geht `Das Repair-Totem bei … wurde von <dein Name> beendet.`. Ohne Totem: rot `Kein Repair-Totem aktiv`. Audit: `totem_cancel`. Dasselbe geht in der Admin-GUI → Gameplay → „Totem beenden“.

#### 8.1.4 Weitere Befehle, die du zum Testen brauchst

Diese gehören nicht zur Mod, sind aber für Tests nützlich.

**Polymer** (in der Mod enthalten):

| Befehl | Wofür |
|---|---|
| `/polymer creative` | Kreativ-Tabs (auch **Xmas-SMP**) mit einem Vanilla-Client öffnen – nur im Kreativmodus |
| `/polymer generate-pack` | Ressourcenpaket erzeugen (Ausgabeort laut Polymer-Doku) |

**Vanilla** (OP nötig):

| Befehl | Wofür |
|---|---|
| `/op <name>` / `/deop <name>` | Admin-Recht (OP-Level) geben/nehmen |
| `/gamemode survival <name>` / `/gamemode creative <name>` | Modus wechseln – Spielfunktionen immer in **survival** testen |
| `/give <name> minecraft:glass_bottle 16` | Glasflaschen zum Abfüllen |
| `/give <name> minecraft:dragon_breath 8` usw. | Rezept-Zutaten, siehe [7.8.6](#786-zutaten-zum-testen-schnell-besorgen) |
| `/kill <name>` | Tod ohne Gegner (Energie −1) |
| `/tp <name> <x> <y> <z>` | Spieler in/aus dem Totem-Kreis bewegen |
| `/effect give <name> minecraft:haste 60 2` | Eile, um die feste Totem-Abbauzeit zu prüfen |
| `/enchant <name> minecraft:efficiency 5` | Effizienz auf das Werkzeug in der Hand |
| `/summon minecraft:tnt ~ ~1 ~` | TNT neben das Totem (Explosionsschutz testen) |
| `/summon minecraft:zombie` | Schaden durch Mobs (Abfüllen abbrechen, Tod durch Mob) |
| `/time set day` / `/weather clear` | Bessere Sicht für Animationen |
| `/gamerule keepInventory true` | **Nicht** setzen, wenn die Seelenbindung beim Tod getestet wird |

**Client-Mod-Tasten** (keine Befehle): **R** = Primär, **G** = Sekundär, **V** = Ultimate (frei belegbar, siehe [6.2](#62-optionaler-client-mod-eigene-tasten)). Ohne Client-Mod: **F**, **Schleichen + F**, **Schleichen + Rechtsklick** mit dem Kristall.

### 8.2 Admin-GUI (`/xmas admin`)

Truhen-Menü, funktioniert mit dem Vanilla-Client.

**Hauptmenü** (Reihe 2 von links): Allgemein (Komparator) · Kristalle (Amethystsplitter) · Energie (Glowstonestaub) · Spieler (Kopf) · Events (Uhr) · Items (Truhe) · Gameplay (Smaragd). Unten rechts: Schließen.

| Seite | Inhalt |
|---|---|
| **Allgemein** | Schalter: Plugin, Fähigkeiten, PvP-Energie, Kristall beim ersten Join, Seelengebunden, Aktivierung mit F, HUD. Unten Mitte: **Neu laden** |
| **Kristalle** | Je Kristall eine Spalte: Name, an/aus, Cooldown ×, Schaden × |
| **Energie** | Start, Minimum, Maximum, Gewinn pro Kill, Verlust pro Tod, Anti-Farming (min) |
| **Spieler** | Online-Spieler als Köpfe → Klick → Kristall (Links nächster, Rechts vorheriger), Tier (Klick wechselt), Energie (± wie unten), Cooldowns zurücksetzen |
| **Events** | Jedes Event: Linksklick = starten/stoppen, Rechtsklick = automatischen Start an/aus. Unten Mitte: Testmodus-Status, Klick beendet ihn |
| **Items** | Reihe 1: alle Hilfs-Items. Reihe 2: alle Kristalle (Klick Tier 1, Shift-Klick Tier 2, ungebunden) |
| **Gameplay** | Energy Bottles an/aus, Ladezeit, Kampfsperre; Upgrader, Händler, Händler-Wartezeit; Repair-Totem an/aus, Radius, Dauer, Ziel-Energie, Abbauzeit, Teilnahme-Fenster; Totem beenden; Rezepte an/aus |

**Zahlen ändern:** Linksklick **+1**, Rechtsklick **−1**, mit **Shift ×5**. Multiplikatoren in Schritten von **0.1**.

Jede Änderung wird **sofort** in `config.jsonc` bzw. `events.jsonc` geschrieben und wirkt **ohne Reload**. Nur der geänderte Wert wird ersetzt – Kommentare und Formatierung bleiben erhalten.

### 8.3 Konfiguration von Hand ändern

1. `config/xmassmp/config.jsonc` mit einem Texteditor öffnen. Kommentare mit `//` oder `#` sind erlaubt.
2. Wert ändern, speichern.
3. Im Spiel oder in der Konsole `/xmas reload`.
4. Antwort lesen: `Neu geladen` plus ggf. Warnungen (`· …`), oder `Neu laden fehlgeschlagen – vorherige Einstellungen bleiben aktiv:` plus Fehler.

Ungültige Werte werden mit einer Warnung auf sinnvolle Werte gesetzt, z. B.:

- `energy.start` außerhalb von min/max → wird begrenzt.
- `totem.durationMinutes < 1`, `totem.radius < 1`, `totem.breakSeconds < 1` → **alle** Totem-Werte auf Standard.
- `energyBottle.chargeSeconds < 1` → alle Flaschen-Werte auf Standard.
- Kaputtes Rezept → nur dieses Rezept wird abgeschaltet.
- Unbekannte Zeitzone → `Europe/Berlin`.

Alle Schlüssel mit Erklärung stehen kommentiert in der Datei selbst und in der Tabelle im `README.md`.

### 8.4 Events (`events.jsonc`)

Felder (ausführlich kommentiert in der Datei):

| Feld | Bedeutung |
|---|---|
| `id` | a–z, 0–9, `_`, `-`; für Befehle |
| `name` | Anzeigename, Platzhalter `{name}` |
| `enabled` | `false` = kein **automatischer** Start (manuell per `/xmas event start` geht trotzdem). **Achtung: Auch der Testmodus startet nur aktivierte Events.** |
| `dates` | `"2026-12-24"` (einmalig), `"12-24"` (jährlich), `{ "from": "12-24", "to": "12-31" }` (Bereich, auch über Silvester). Weglassen = jeden Tag |
| `weekdays` | optional, z. B. `["SATURDAY", "SUNDAY"]` |
| `times` | Startzeiten `"HH:mm"` |
| `durationMinutes` | Dauer, `0` = punktuell (nur `onStart`, keine Modifikatoren) |
| `announceMinutes` | Ankündigungen X Minuten vorher, z. B. `[10, 1]` |
| `countdownSeconds` | Sekunden-Countdown als Titel (0–60) |
| `onStart` / `onEnd` | Aktionen `broadcast`, `title`, `sound`, `command` (läuft als Konsole) |
| `modifiers` | `cooldown_multiplier`, `energy_multiplier` |

Texte mit `&`-Farbcodes, Platzhalter `{name}`, `{id}`, `{duration}`, bei Ankündigungen `{minutes}`, im Countdown `{seconds}`. Zeitzone: `events.timezone` in `config.jsonc` (Standard `Europe/Berlin`).

Die mitgelieferte Weihnachtswoche (`xmas_week`, `xmas_eve`, `advent_evening`, `new_year`) ist ein **Platzhalter, alles deaktiviert**.

### 8.5 Protokolle

| Datei | Inhalt | Format |
|---|---|---|
| `world/xmassmp/audit.log` | Jede Admin-Änderung (`give`, `set_*`, `reload`, `config`, `event_*`, `totem_cancel`, `reset_cooldowns`, `creative_bottle`) | Eine JSON-Zeile pro Eintrag: Zeit, Wer, Was, Ziel. Abschaltbar mit `audit.enabled` |
| `world/xmassmp/energy.log` | Jede Energieänderung | `{"time":"…","player":"<uuid>","name":"…","from":4,"to":5,"reason":"…","ref":"…"}` |
| `logs/latest.log` | Server-Log, Logger `Xmas-SMP` | Text |

`reason` im Energie-Protokoll: `kill`, `death`, `bottle_fill`, `bottle_drink`, `totem`, `admin`, `suspicious` (ungültige Flasche, Energie unverändert).

`Wer` im Audit-Log: Spielername, `Konsole` für die Server-Konsole, sonst der Name der Quelle (RCON, Befehlsblock).

Herkunft (`origin`) einer Energy Bottle in `/xmas energy bottles`: `fill` (abgefüllt), `admin` (`/xmas give`, Admin-GUI; Ersteller = Name des Admins), `overflow` (Sieger mit voller Energie), `creative` (aus dem Kreativ-Tab registriert).

### 8.6 Kreativ-Tab

Alle Items der Mod stehen im Kreativ-Tab **Xmas-SMP** (ID `xmassmp:items`).

**Öffnen:**

- Mit Polymer im Client ([6.3](#63-optional-polymer-im-client-kreativ-tab-als-echter-tab)): Kreativinventar öffnen (`E` im Kreativmodus), Tab **Xmas-SMP** wählen. Das Tab-Icon ist ein makelloser Tier-2-Feuer-Kristall.
- Mit Vanilla-Client: im **Kreativmodus** `/polymer creative` eintippen, dann den Tab Xmas-SMP wählen.

**Inhalt und Reihenfolge:**

1. Für jeden Kristall (Feuer, Erde, Wind, Schatten, Natur) erst Tier 1, dann Tier 2, jeweils in der Reihenfolge **makellos → zerkratzt → beschädigt → zerbrochen** (5 × 2 × 4 = **40 Kristalle**).
2. Danach die Hilfs-Items: Upgrader, Energy Bottle (nur für Operatoren), Kristall-Händler, Repair Kit, Kristall-Splitter.

**Regeln:**

- Die Kristalle aus dem Tab sind **ungebunden** (kein Besitzer), genau wie bei `/xmas give … crystal`: reine Deko-/Anzeige-Items **ohne Kräfte**. Der Abgleich der Seelenbindung fasst sie nicht an, sie verschwinden also nicht. Den **aktiven** Kristall eines Spielers ändert nur `/xmas set` bzw. die Admin-GUI.
- Upgrader, Kristall-Händler, Repair Kit und Splitter aus dem Tab sind **voll funktionsfähig** (wie gecraftet).
- Die **Energy Bottle** steht nur für Operatoren im Tab. Frisch aus dem Tab hat sie **keine ID im Flaschen-Register**. Was beim Rechtsklick (Haupthand) passiert:

| Wer klickt | Ergebnis |
|---|---|
| Admin (`xmassmp.admin` bzw. OP-Level 2) **im Kreativmodus** | Die Flasche wird gegen eine registrierte ausgetauscht (Herkunft `creative`, Audit-Eintrag `creative_bottle` mit der Flaschen-ID) und **im selben Klick getrunken**, sofern der Admin einen Kristall und weniger als volle Energie hat. Bei voller Energie (`Deine Energie ist schon voll.`) oder ohne Kristall bleibt die nun **registrierte** Flasche in der Hand und kann weitergegeben werden |
| Admin im **Überlebensmodus** | Keine Registrierung → `Diese Energy Bottle ist ungültig und wurde entfernt.`, Eintrag `suspicious` (`ohne-id … unbekannt (Fälschung)`) im Energie-Protokoll |
| Jeder andere Spieler (egal welcher Modus) | Wie oben: ungültig, wird entfernt, `suspicious` im Protokoll |

  Eine **unregistrierte** Tab-Flasche liefert in der Werkbank **keine Splitter**.

> **Tipp für Tester:** Wer eine gültige Flasche zum Weitergeben braucht, ohne sie zu trinken: vorher `/xmas set <DeinName> energy 10`, dann im Kreativmodus einmal mit der Tab-Flasche rechtsklicken. Einfacher geht es mit `/xmas give <spieler> item energy_bottle`.

---

## 9. Manueller Testplan im Spiel

### 9.0 Vorbereitung

1. Server nach [Abschnitt 4](#4-testserver-aufsetzen-schritt-für-schritt) frisch aufsetzen (**neue Welt**, damit „erster Join“ testbar ist).
2. Ressourcenpaket nach [Abschnitt 5](#5-ressourcenpaket-an-die-spieler-ausliefern) aktiv.
3. Zwei Spieler bereithalten: im Folgenden **A** (Admin, OP) und **B** (normaler Spieler, kein OP).
4. Beide im **Überlebensmodus**.
5. Für schnellere Tests in der Admin-GUI (Gameplay/Energie) bzw. per Config **vorübergehend** setzen:
   - `totem.durationMinutes` = `1`
   - `energy.sameVictimCooldownMinutes` = `1`
   - (Nach den Tests wieder auf Standard: 10 bzw. 30.)
6. Hilfreiche Vanilla-Befehle für A: `/gamemode survival|creative <name>`, `/give <name> minecraft:glass_bottle 16`, `/kill <name>`, `/tp <name> <x> <y> <z>`, `/time set day`, `/effect give <name> minecraft:haste 60 2`.

Jeder Testfall hat **Schritte** und **Erwartung**. Ergebnis im [Testprotokoll](#12-testprotokoll-vorlage) eintragen. Weicht etwas ab: Screenshot/Video + Ausschnitt aus `logs/latest.log` + Testfall-Nummer notieren.

---

### 9.1 Schnelltest (ca. 30 Minuten, alle Kernfunktionen)

Ein durchgehender Ablauf, der jede wichtige Funktion einmal anfasst. Du brauchst **zwei Spieler**: **A** (OP) und **B** (kein OP). Befehle tippt A im Chat. Hake jeden Schritt ab; weicht etwas ab, gehe zum passenden ausführlichen Testfall (T01–T22).

**Teil 1 – Start und Rechte**

1. [ ] Server starten → Log zeigt `Xmas-SMP gestartet (5 Kristalle)`.
2. [ ] A joint → Chat `Du hast den …-Kristall erhalten. Er ist an dich gebunden.`, Spirale, Kristall im Inventar.
3. [ ] Konsole: `op A` (A durch den echten Namen ersetzen).
4. [ ] A: `/xmas` → 9 Hilfezeilen.
5. [ ] B joint → bekommt ebenfalls einen Kristall. B tippt `/xmas` → unbekannter Befehl.
6. [ ] Beide: `/gamemode survival` (A setzt das für beide).

**Teil 2 – Kristall, HUD, Seelenbindung**

7. [ ] Actionbar bei B: drei graue Icons mit `–`, dahinter `✦ 5/10`.
8. [ ] B drückt Q auf den Kristall → spätestens nach 1 s wieder im Inventar.
9. [ ] B hält den Kristall, Schleichen + Rechtsklick → `Die Ultimate gibt es noch nicht.`
10. [ ] `/xmas set B crystal shadow` → B's Kristall wird lila.
11. [ ] `/xmas set B energy 2` → Item „beschädigt“, Bruch-Animation. `/xmas set B energy 5` → „makellos“, Reparatur-Animation.
12. [ ] `/xmas set B tier 2` → B steht gelb in der Tab-Liste. `/xmas set B tier 1` → wieder weiß.

**Teil 3 – Energie durch PvP**

13. [ ] A tötet B → A `+1 Energie`, B `-1 Energie`.
14. [ ] A tötet B sofort noch mal → A: `Keine Energie: Du hast B erst vor kurzem besiegt.`
15. [ ] `/kill B` → B verliert 1, niemand gewinnt.
16. [ ] `/xmas energy audit` → Einträge `kill`, `death`, `admin`.

**Teil 4 – Energy Bottle**

17. [ ] `/xmas set B energy 5`, `/give B minecraft:glass_bottle 4`.
18. [ ] B: Glasflasche in die Haupthand, **Schleichen + Rechtsklick 5 s halten** → Ladebalken, dann `+1 Energy Bottle`, Energie 4.
19. [ ] B startet erneut und läuft los → `Abfüllen abgebrochen: Du hast dich bewegt.`
20. [ ] B trinkt die Flasche (Rechtsklick) → `+1 Energie (5/10)`, leere Glasflasche zurück.
21. [ ] `/xmas give B item energy_bottle`, B legt sie ins 2×2-Feld → 9 Kristall-Splitter, Glasflasche bleibt liegen.
22. [ ] `/xmas energy bottles` → Liste stimmt (zerlegte/getrunkene Flaschen sind nicht mehr aktiv).

**Teil 5 – Rezepte, Upgrader, Händler**

23. [ ] A besorgt Zutaten ([7.8.6](#786-zutaten-zum-testen-schnell-besorgen)) und 9 Splitter (Energy Bottle zerlegen).
24. [ ] A craftet in der Werkbank: **Upgrader** (4 Splitter, 4 Drachenatem, 1 Witherskelettschädel) → Ergebnis erscheint.
25. [ ] A craftet **Kristall-Händler** (4 Diamanten, 4 Drachenatem, 1 Splitter).
26. [ ] A craftet **Repair Kit** (4 Splitter, 2 Ambosse, 2 Diamantblöcke, 1 Netherit-Schmiedevorlage).
27. [ ] A benutzt den Upgrader (Rechtsklick) → Titel `Tier 2`, Animation, Name gelb.
28. [ ] A benutzt den Händler → GUI, anderen Kristall wählen → Tausch-Animation, Tier 2 bleibt.

**Teil 6 – Repair-Totem**

29. [ ] Vorher in der Admin-GUI (`/xmas admin` → Gameplay → „Dauer (min)“) auf **1** stellen.
30. [ ] `/xmas set B energy 2`. A stellt das Repair Kit auf den Boden (Rechtsklick auf einen Block) → Titel mit Koordinaten bei allen.
31. [ ] B stellt sich innerhalb von 30 s in den Kreis → `Du nimmst am Repair-Totem teil.`
32. [ ] `/xmas totem status` → Ort, Restzeit, Teilnehmer.
33. [ ] Nach 1 min → B bekommt `✚ 5 Energie`, Totem zerfällt.
34. [ ] Zweites Kit (`/xmas give A item repair_kit`) aufstellen, B baut es in Survival ab und hält 10 s → `… hat das Repair-Totem … zerstört. Niemand bekommt Energie.`
35. [ ] Dauer in der GUI wieder auf **10** stellen.

**Teil 7 – Admin, Config, Events, Kreativ-Tab**

36. [ ] `/xmas admin` → alle 7 Seiten einmal öffnen; auf „Allgemein“ den Schalter „HUD“ aus- und wieder einschalten → HUD verschwindet/kommt zurück.
37. [ ] `config.jsonc` öffnen: Kommentare sind noch da, `hud.enabled` steht auf `true`.
38. [ ] `/xmas reload` → `Neu geladen`.
39. [ ] `/xmas event list` → 4 Events, alle `deaktiviert`.
40. [ ] `/xmas event start xmas_eve` → Titel `Frohe Weihnachten!`; `/xmas event stop xmas_eve` → beendet.
41. [ ] Admin-GUI → Events → **Rechtsklick** auf „Weihnachtsabend-Bonus“ (automatischen Start einschalten), dann `/xmas event simulate 2026-12-25 19:59` → nach ca. 1 min Chat `Weihnachtsabend-Bonus: Fähigkeiten laden doppelt so schnell – 60 Minuten lang!`
42. [ ] `/xmas event simulate off` → Testmodus beendet; Event in der GUI wieder per Rechtsklick deaktivieren.
43. [ ] A im Kreativmodus: `/polymer creative` → Tab **Xmas-SMP** mit 40 Kristallen und den Hilfs-Items.
44. [ ] `world/xmassmp/audit.log` öffnen → Einträge für alle Admin-Aktionen dieses Tests.
45. [ ] Server stoppen und starten → A und B haben Kristall, Tier und Energie wie vorher.

Wenn alle 45 Schritte passen, funktionieren die Kernfunktionen. Für eine vollständige Abnahme danach die ausführlichen Testfälle T01–T22 durchgehen.

### 9.2 Ausführliche Testfälle

#### T01 – Server-Start

| Schritt | Erwartung |
|---|---|
| Server mit beiden JARs starten | Log enthält `Xmas-SMP gestartet (5 Kristalle)`, keine `ERROR`-Zeilen vom Logger `Xmas-SMP` |
| Dateien prüfen | `config/xmassmp/config.jsonc` und `events.jsonc` existieren und sind kommentiert |
| Server stoppen (`stop`) | Log: `Xmas-SMP gestoppt`, kein Stacktrace |

#### T02 – Rechte

| Schritt | Erwartung |
|---|---|
| B tippt `/xmas` | Befehl unbekannt / keine Tab-Completion |
| A (OP) tippt `/xmas` | Hilfe mit 9 Zeilen (give, set, admin, reload, event, simulate, energy, totem) |
| A tippt `/xmas ` und drückt Tab | Vorschläge: `admin`, `energy`, `event`, `give`, `reload`, `set`, `totem` |
| Konsole: `xmas event list` | Funktioniert aus der Konsole |
| Konsole: `xmas admin` | Fehler (nur für Spieler) |

#### T03 – Erster Join und Kristall

| Schritt | Erwartung |
|---|---|
| B joint zum ersten Mal | Chat `Du hast den <X>-Kristall erhalten. Er ist an dich gebunden.`, Partikelspirale in Kristallfarbe, Kristall im Inventar |
| Tooltip des Kristalls ansehen | `Tier 1`, `Zustand: makellos`, `Energie: 5 (Makellos)`, `Seelengebunden` |
| B verlässt den Server und joint erneut | **Kein** neuer Kristall, keine Nachricht, derselbe Kristall |
| Datei `world/xmassmp/playerdata/<uuid-von-B>.json` | Existiert, enthält Kristall, Tier 1, Energie 5 |
| Mehrfach mit neuen Konten joinen (optional) | Kristalle sind zufällig verteilt |
| A schaltet in der GUI „Kristall beim ersten Join“ aus, neues Konto joint | Kein Kristall, kein HUD |

#### T04 – Seelenbindung

Mit `crystalItem.soulbound = true`:

| Schritt | Erwartung |
|---|---|
| B drückt Q auf dem Kristall | Kristall landet nicht dauerhaft am Boden; spätestens nach 1 s wieder im Inventar, gedropptes Item verschwindet |
| B legt den Kristall in eine Truhe | Aus der Truhe verschwunden, wieder im Inventar |
| B legt den Kristall in die Werkbank / ins 2×2-Feld | Verschwindet aus dem Feld, wieder im Inventar |
| B legt den Kristall in einen Item-Rahmen | Kopie im Rahmen ist wirkungslos / verschwindet; Kristall im Inventar |
| B stirbt (`/kill B`) | Kristall liegt nicht im Drop; nach dem Respawn sofort im Inventar |
| A setzt `crystalItem.soulbound` auf `aus` (GUI) und B droppt den Kristall | Kristall bleibt am Boden liegen, die Daten (HUD) bleiben unverändert |
| Wieder `an` schalten | Kristall erscheint wieder im Inventar |

#### T05 – HUD

| Schritt | Erwartung |
|---|---|
| B schaut auf die Actionbar | `[Icon] –   [Icon] –   [Icon] –   ✦ 5/10` in der Kristallfarbe (drei graue Icons mit Strich, weil es noch keine Kräfte gibt) |
| A: `/xmas set B energy 0` | `✦ 0/10` in **rot** |
| A: `/xmas set B energy 10` | `✦ 10/10`, Kristall schimmert verzaubert |
| A schaltet in der GUI „HUD“ aus | Actionbar bleibt leer (Rückmeldungen erscheinen trotzdem kurz) |
| A setzt `hud.updateTicks` auf `40`, `/xmas reload` | HUD aktualisiert spürbar seltener (alle 2 s) |
| `hud.updateTicks` auf `99`, `/xmas reload` | Warnung `hud.updateTicks muss 1–40 sein – 10 verwendet` |

#### T06 – Steuerung (Stand ohne Kräfte)

| Schritt | Erwartung |
|---|---|
| B hält ein Schwert, drückt **F** | Schwert wandert normal in die Zweithand (normaler Tausch) |
| B hält den eigenen Kristall, **Schleichen + Rechtsklick** | Actionbar: `Die Ultimate gibt es noch nicht.` |
| B hält einen per `/xmas give … crystal` gegebenen (ungebundenen) Kristall, Schleichen + Rechtsklick | Nichts (nur der eigene, gebundene Kristall löst aus) |
| Mit Client-Mod: **R**, **G**, **V** | `Die Primärkraft gibt es noch nicht.` / `Die Sekundärkraft gibt es noch nicht.` / `Die Ultimate gibt es noch nicht.` |
| Client-Mod: Tasten in Optionen → Steuerung umbelegen | Neue Tasten funktionieren, alte nicht mehr |
| A setzt `controls.clientKeys = false`, `/xmas reload`, B drückt R | Keine Reaktion |
| Client-Mod auf einem Vanilla-Server (ohne Xmas-SMP) | Kein Fehler, kein Kick |

#### T07 – Admin-Befehle `give` und `set`

| Schritt | Erwartung |
|---|---|
| `/xmas give B crystal fire` | `B hat einen Feuer-Kristall (Tier 1, pristine) erhalten` |
| `/xmas give B crystal earth 2 broken` | Item zeigt zerbrochenen Tier-2-Erde-Kristall |
| `/xmas give B crystal lava` | Rot: `Unbekannter Kristall: lava` |
| `/xmas give B crystal fire 1 kaputt` | Rot: `Unbekannter Zustand: kaputt (broken, damaged, scratched, pristine)` |
| `/xmas give B item <jedes Item>` (alle 5 durchgehen) | Item im Inventar, richtiges Aussehen und Name |
| `/xmas give B item schwert` | Rot: `Unbekanntes Item: schwert` |
| `/xmas set B crystal shadow` | Kristall-Item von B wird sofort zum Schatten-Kristall, HUD-Farbe lila |
| `/xmas set B tier 2` | Item zeigt Tier 2, B steht **gelb** in der Tab-Liste |
| `/xmas set B tier 1` | Tab-Name wieder normal |
| `/xmas set B energy 11` | Rot: `Energie muss 0–10 sein` |
| `/xmas set B energy 4`, dann `2`, dann `0`, dann `5` | Item wechselt: zerkratzt → beschädigt → zerbrochen → makellos; bei jedem Stufenwechsel Bruch- bzw. Reparatur-Animation |
| `world/xmassmp/audit.log` | Für jeden Befehl eine JSON-Zeile mit Wer=A |
| `/xmas energy audit B` | Die `set energy`-Änderungen mit Grund `admin` |

#### T08 – Energie durch PvP

Vorbereitung: A und B auf Energie 5, `energy.sameVictimCooldownMinutes = 1`.

| Schritt | Erwartung |
|---|---|
| A tötet B im Nahkampf | A: `+1 Energie …` (grün), B: `-1 Energie …` (rot); A hat 6, B 4 |
| B stirbt durch Lava/Fall ohne Gegner | B −1, niemand bekommt etwas |
| A schlägt B, B stirbt kurz danach durch Fallschaden | Zählt als Kill für A (sofern Anti-Farming nicht greift) |
| A tötet B innerhalb von 1 min erneut | A: `Keine Energie: Du hast B erst vor kurzem besiegt.`, B verliert trotzdem 1 |
| Nach 1 min erneut | A bekommt wieder +1 |
| `/xmas set B energy 0`, A tötet B | A: `Keine Energie: B hatte keine Energie mehr.`; B bleibt bei 0 |
| `/xmas set A energy 10`, `/xmas set B energy 5`, A tötet B (nach Ablauf Anti-Farming) | A bleibt bei 10, Chat bei A: `Deine Energie ist voll – die Energie von B liegt als Energy Bottle am Boden.`; eine Energy Bottle liegt bei B's Todesort |
| Diese Flasche aufheben und trinken (mit Energie < 10) | +1, gültig |
| `energy.pvpEnabled` aus (GUI), A tötet B | Keine Energieänderung, keine Nachricht |
| `/xmas energy audit` | Einträge `kill`, `death` mit Gegnernamen |

#### T09 – Energy Bottle: Abfüllen

Vorbereitung: B in Survival, Energie 5, 5 leere Glasflaschen.

| Schritt | Erwartung |
|---|---|
| Glasflasche in Haupthand, **Schleichen + Rechtsklick 5 s halten** | Ladebalken unter dem Fadenkreuz füllt sich (Position notieren!), Farbband und Töne, danach Titel `+1 Energy Bottle`, Energie 4, eine Glasflasche weniger |
| Nach 2 s loslassen | Abbruch ohne Meldung, nichts verbraucht |
| Nach 2 s aufhören zu schleichen | `Abfüllen abgebrochen.` |
| Während des Ladens Hotbar-Slot wechseln | `Abfüllen abgebrochen: Flasche gewechselt.` |
| Während des Ladens > 1 Block laufen | `Abfüllen abgebrochen: Du hast dich bewegt.` |
| Während des Ladens von einem Mob getroffen werden | `Abfüllen abgebrochen: Schaden genommen.` |
| Glasflasche in der **Zweithand** | Kein Abfüllen (nur Haupthand) |
| Ohne Schleichen Rechtsklick auf Wasser | Normales Wasser-Abfüllen (Vanilla) |
| `/xmas set B energy 1`, abfüllen | `Du brauchst mindestens 2 Energie zum Abfüllen.` |
| A schlägt B, B füllt sofort ab | `Im Kampf kannst du nicht abfüllen (noch Xs)`; nach 15 s geht es |
| B schlägt A (nicht umgekehrt), B füllt ab | Ebenfalls gesperrt (Sperre gilt für beide Seiten) |
| B im Kreativmodus | `Im Kreativmodus kannst du keine Energie abfüllen.` |
| GUI Gameplay: Energy Bottles aus | `Energy Bottles sind gerade abgeschaltet.` |
| GUI: Ladezeit auf 2 | Abfüllen dauert 2 s |
| `/xmas energy bottles` | Neue Flasche mit Herkunft `fill` und Ersteller B |

#### T10 – Energy Bottle: Trinken, Zerlegen, Exploits

| Schritt | Erwartung |
|---|---|
| B (Energie 4) trinkt eine Flasche (Rechtsklick) | `+1 Energie (5/10)`, Totem-Partikel, leere Glasflasche zurück |
| B mit Energie 10 trinkt | `Deine Energie ist schon voll.`, Flasche bleibt |
| B trinkt zwei Flaschen sehr schnell hintereinander | Zweite erst nach 1 s |
| B (Energie 0) trinkt | Energie 1, Kristall wechselt von zerbrochen auf beschädigt (Reparatur-Animation) |
| B gibt A eine von B abgefüllte Flasche, A trinkt | Funktioniert (Flaschen sind übertragbar) |
| Flasche ins 2×2-Feld | Ergebnis: **9 Kristall-Splitter**; nach Herausnehmen liegt eine leere Glasflasche im Feld |
| Dasselbe in der Werkbank, mit Shift-Klick | 9 Splitter pro Flasche |
| Dasselbe im **Crafter** (Redstone) | Funktioniert |
| **Dupe-Test:** A im Kreativmodus, Flasche im Inventar, Mittelklick (Stack kopieren), zurück in Survival | Erste Flasche trinken: +1. Kopie trinken: `Diese Energy Bottle ist ungültig und wurde entfernt.`, Energie unverändert |
| `/xmas energy audit A` | Roter Eintrag mit Grund `suspicious`, `… bereits verbraucht (Kopie)` |
| Kopie in die Werkbank legen, nachdem das Original zerlegt wurde | **Kein** Ergebnis |
| Original und Kopie gleichzeitig in zwei Werkbank-Felder | Nur eine liefert Splitter |
| `logs/latest.log` | Warnung `Ungültige Energy Bottle bei A: …` |
| Energy Bottle in einen Braustand legen | Geht nicht (keine Brau-Zutat) |

#### T11 – Rezepte

| Schritt | Erwartung |
|---|---|
| Upgrader-Rezept in die Werkbank legen (siehe [7.8](#78-crafting-rezepte)) | Ergebnis: Upgrader |
| Händler-Rezept | Ergebnis: Kristall-Händler |
| Repair-Kit-Rezept | Ergebnis: Reparatur-Set |
| Rezept mit **normalen** Prismarinscherben statt Splittern | **Kein** Ergebnis |
| Gleiches Rezept im Crafter | Funktioniert |
| `config.jsonc`: beim Upgrader `"W": "minecraft:dirt"`, `/xmas reload` | Rezept braucht jetzt Erde statt Schädel |
| `config.jsonc`: Upgrader-`pattern` kaputt machen (z. B. Zeichen ohne `key`), `/xmas reload` | Warnung `recipes.upgrader: … – Rezept abgeschaltet`; Upgrader nicht mehr craftbar, andere Rezepte gehen |
| GUI Gameplay: „Rezept gem_trader“ aus | Händler nicht mehr craftbar |
| `gem_fragments.count` auf `4`, reload | 1 Flasche → 4 Splitter |
| Splitter stapeln | Bis 64. Alle anderen Xmas-Items stapeln **nicht** |

Danach Config wieder zurücksetzen.

#### T12 – Upgrader

| Schritt | Erwartung |
|---|---|
| B (Tier 1) Rechtsklick mit Upgrader | Titel `Tier 2`, Untertitel `Deine Ultimate ist freigeschaltet`, Chat-Nachricht, Animation (Säule, schwebender Kristall, Blitz, Ring), Upgrader weg, Kristall-Item Tier 2, Name gelb in der Tab-Liste |
| HUD | Unverändert drei `–` (es gibt noch keine Ultimate) |
| Zweiter Upgrader bei Tier 2 | `Dein Kristall ist schon Tier 2.`, Upgrader bleibt |
| GUI: Upgrader aus | `Upgrader sind gerade abgeschaltet.` |
| `config.jsonc`: `controls.tier2TabColor = false`, `/xmas reload` (nicht in der GUI) | Name wieder weiß |
| Upgrader in der Zweithand, Rechtsklick | Nichts (nur Haupthand) |

#### T13 – Kristall-Händler

| Schritt | Erwartung |
|---|---|
| B (z. B. Feuer, Tier 2, Energie 7) Rechtsklick mit Händler | GUI `Kristall-Händler › Wähle deinen Kristall`, 5 Kristalle, Feuer grau |
| Klick auf den eigenen (grauen) | Nichts |
| **Abbrechen** | GUI zu, Händler noch da |
| GUI erneut öffnen, Klick auf Natur | Titel `Natur – Dein neuer Kristall`, Chat `Du hast den Feuer- gegen den Natur-Kristall getauscht.`, Wirbel-Animation, Händler weg, Kristall-Item jetzt Natur, **weiterhin Tier 2 und Energie 7** |
| GUI Kristalle: Wind aus, Händler öffnen | Wind zeigt Barriere `Gerade abgeschaltet.` |
| Händler öffnen, Händler in der GUI per Hotbar-Taste aus der Hand nehmen, dann Kristall wählen | `Halte den Kristall-Händler in der Haupthand.`, nichts getauscht |
| `trader.keepTier = false`, `keepEnergy = false`, reload, tauschen | Tier 1, Energie = Startwert 5 |
| GUI Gameplay: Händler-Wartezeit 1 min, zweimal tauschen | Zweiter Versuch: `Nächster Tausch in …` |
| GUI: Händler aus | `Der Kristall-Händler ist gerade abgeschaltet.` |
| Händler einem Dorfbewohner anbieten | Kein Handel (kein Smaragd) |

#### T14 – Repair-Totem

Vorbereitung: `totem.durationMinutes = 1`, A hat ein Repair Kit (`/xmas give A item repair_kit`), B auf Energie 2, beide in Survival.

| Schritt | Erwartung |
|---|---|
| A Rechtsklick mit dem Kit auf den Boden | Tresor-Block erscheint über dem Boden, Repair Kit schwebt darüber, Partikelring; **alle** bekommen Titel `✚ Repair-Totem` + Koordinaten; Chat-Ansage mit Regeln |
| B steht innerhalb der ersten 30 s im Kreis | Chat `Du nimmst am Repair-Totem teil.` |
| `/xmas totem status` | `Totem bei … · noch … · 1 Teilnehmer · aufgestellt von A` (A zählt, wenn er im Kreis stand) |
| Zweites Kit irgendwo aufstellen | `Es läuft schon ein Totem bei ….` |
| Kit oben auf einen Block an der Bauhöhengrenze (Y 319) oder außerhalb der Weltgrenze setzen | `Hier kann das Totem nicht stehen.` |
| B bleibt bis zum Ende im Kreis | Ankündigungen bei 30 s und 10 s; am Ende Titel `✚ 5 Energie`, B hat 5; Chat `… ist zerfallen. 1 Spieler bekamen ihre Energie zurück.`; Block weg |
| Wiederholen, B kommt erst **nach** 30 s in den Kreis | B wird kein Teilnehmer, bekommt nichts |
| Wiederholen, B verlässt den Kreis nach 40 s | `Du hast den Kreis verlassen – das Totem wirkt für dich nicht mehr.`; bekommt nichts, auch wenn er zurückkommt |
| Wiederholen, B stirbt im Kreis | `Du bist gestorben – das Totem wirkt für dich nicht mehr.` |
| Wiederholen, B loggt sich aus und wieder ein | B ist ausgeschieden |
| Wiederholen, B hat 7 Energie | `Du hattest schon genug Energie – das Totem gab dir nichts.` |
| Wiederholen, B hat 0 Energie | B bekommt 5 |
| **Abbau:** B baut das Totem in **Survival** ab und hält 10 s gedrückt | Chat an alle `⚠ … wird angegriffen!`, rot-violette Spirale zieht sich zusammen, Glockentöne werden schneller, im HUD der Spieler im Kreis `⚠ Totem <Sekunden>`; nach genau 10 s: `✖ B hat das Repair-Totem bei … zerstört. Niemand bekommt Energie.` |
| Abbau mit Hand vs. Netherit-Spitzhacke + Effizienz V + Eile II | Beide genau 10 s |
| Nach 5 s loslassen bzw. wegschauen | Countdown setzt zurück (Ton), Totem bleibt |
| Während des Abbaus stirbt der Abbauende | Countdown setzt zurück |
| TNT neben dem Totem zünden | Totem bleibt stehen |
| Kolben gegen das Totem | Totem bewegt sich nicht |
| Feuer/Lava daneben | Totem bleibt |
| `/xmas totem cancel` | Totem verschwindet ohne Wirkung; ohne Totem: rot `Kein Repair-Totem aktiv` |
| GUI Gameplay: „Totem beenden“ | Wie `cancel` |
| **Neustart:** Totem aufstellen, Server stoppen, starten, B joint innerhalb von 60 s wieder | Totem läuft weiter (`world/xmassmp/totem.json` existiert während der Laufzeit), B bleibt Teilnehmer |
| Neustart, B joint erst nach > 60 s | B ist ausgeschieden |
| GUI: Repair-Totem aus, Kit benutzen | `Repair Kits sind gerade abgeschaltet.` |
| Kit in der Hand, Rechtsklick in die Luft | Nichts (nur auf Blöcke) |
| `/xmas energy audit B` | Eintrag `totem` mit Name des Aufstellers |

Hinweis: Die Dauer wird beim **Aufstellen** festgelegt. Eine Änderung von `durationMinutes` wirkt erst beim nächsten Totem.

#### T15 – Admin-GUI

| Schritt | Erwartung |
|---|---|
| `/xmas admin` | Hauptmenü mit 7 Seiten + Schließen |
| Jede Seite öffnen, **Zurück** (Pfeil unten links) | Kommt zurück zum Hauptmenü |
| Allgemein: jeden Schalter einmal umschalten | Farbe/Text wechselt (an grün, aus rot); Wert steht sofort in `config.jsonc`; **Kommentare in der Datei bleiben erhalten** |
| Energie: „Start“ Linksklick, Rechtsklick, Shift-Links | +1, −1, +5 |
| Kristalle: „Cooldown ×“ Linksklick | +0.1 |
| Spieler: B wählen → Kristall Linksklick | B's Kristall wechselt sofort zum nächsten |
| Spieler: Tier klicken | Wechselt 1 ↔ 2 |
| Spieler: Energie ± | Energie und Item ändern sich |
| Spieler: Cooldowns zurücksetzen | Bestätigung; Audit-Eintrag `reset_cooldowns` |
| Items: jedes Item anklicken | Landet im Inventar von A; Energy Bottle ist gültig (trinkbar) |
| Items: Kristall Shift-Klick | Tier-2-Kristall (ungebunden) |
| Allgemein: **Neu laden** | Meldung `Neu geladen` |
| Ohne OP (B) versuchen, die GUI zu öffnen | Nicht möglich |
| Items aus der GUI per Shift-Klick / Ziehen in das eigene Inventar „stehlen“ (Deko-Buttons) | Nicht möglich |
| `audit.log` | Jede GUI-Änderung als Eintrag `config`, Wer=A |

#### T16 – Reload und kaputte Config

| Schritt | Erwartung |
|---|---|
| In `config.jsonc` eine schließende `}` löschen, `/xmas reload` | Rot `Neu laden fehlgeschlagen – vorherige Einstellungen bleiben aktiv:` + Fehlerbeschreibung; Spiel läuft mit alten Werten weiter |
| Datei reparieren, `/xmas reload` | `Neu geladen` |
| `"energy": { "start": 50 }` | Warnung `energy.start 50 liegt außerhalb min/max – auf 10 gesetzt` |
| `"events": { "timezone": "Mars/Olympus" }` | Warnung, `Europe/Berlin` verwendet |
| `events.jsonc` kaputt machen, reload | Fehler gemeldet, Events ruhen bzw. alte bleiben aktiv |
| Server mit kaputter `config.jsonc` starten | Server startet trotzdem, Fehler im Log |

#### T17 – Events

Vorbereitung: in der GUI-Seite **Events** (Rechtsklick) oder in `events.jsonc` das Event **`advent_evening`** auf `enabled: true` setzen (Bereich 25.–30.12., 20:00, 60 min, Ankündigung 5 min vorher, Modifikator `cooldown_multiplier 0.5`).

| Schritt | Erwartung |
|---|---|
| `/xmas event list` | Alle 4 Events mit Status; `advent_evening` mit `nächster Start 25.12.<Jahr> 20:00`, die anderen `deaktiviert` |
| `/xmas event start xmas_eve` (deaktiviert!) | `Event xmas_eve gestartet (120 min)`; Titel `Frohe Weihnachten!` + Sound |
| `/xmas event start xmas_eve` erneut | Rot `Event xmas_eve läuft bereits` |
| `/xmas event list` | `xmas_eve – läuft bis …` |
| `/xmas event stop xmas_eve` | `Event xmas_eve beendet` |
| `/xmas event start new_year` (Dauer 0) | `Event new_year ausgelöst`, Titel + zwei Feuerwerks-Sounds |
| **Trockenlauf:** `/xmas event simulate 2026-12-25 19:54 dry` | `Testmodus aktiv: simulierte Zeit 25.12.2026 19:54 – Aktionen werden nur geloggt …`; im Log `Event-Testmodus: simulierte Zeit … (Trockenlauf)` |
| ca. 1 min warten (simulierte Zeit läuft in Echtzeit weiter) | Im **Log**: `[Trockenlauf] Broadcast: …beginnt in 5 Minute(n)!` – im Chat **nichts** |
| `/xmas event list` | Kopfzeile `[Testmodus: 25.12.2026 19:5x]` |
| `/xmas event simulate off` | `Testmodus beendet – …` |
| **Echt simuliert:** `/xmas event simulate 2026-12-25 19:59` | Nach ~1 min Chat-Broadcast `Weihnachtsabend-Bonus: Fähigkeiten laden doppelt so schnell – 60 Minuten lang!` |
| Ankündigung testen: `/xmas event simulate 2026-12-25 19:54` | Nach ~1 min Chat `✦ Weihnachtsabend-Bonus beginnt in 5 Minute(n)!` + Glockenton |
| Countdown testen: `xmas_eve` aktivieren, `/xmas event simulate 2026-12-24 17:59` | Ankündigung `1 Minute(n)`, dann 5-Sekunden-Countdown als roter Titel, dann Start |
| `/xmas event simulate off` während das simulierte Event läuft | Simuliertes Event endet **mit** End-Aktionen |
| **Energie-Modifikator:** In `events.jsonc` bei einem Event `"modifiers": { "energy_multiplier": 2.0 }`, reload, Event starten, A tötet B | A bekommt **+2** |
| Ein Event in `onStart` mit `{ "type": "command", "command": "weather clear" }`, starten | Wetter wird klar (läuft als Konsole) |
| **Neustart:** Event mit langer Dauer starten, Server neu starten | `event list` zeigt es weiterhin als laufend |
| Simulation mit falschem Format: `/xmas event simulate 25.12.2026 20:00` | Rot `Format: /xmas event simulate yyyy-MM-dd HH:mm [dry]` |
| GUI Events: Linksklick auf ein Event | Startet/stoppt; Rechtsklick schaltet automatischen Start um (steht danach in `events.jsonc`, Kommentare bleiben) |
| `general.enabled` aus | Event-Plan ruht; nach dem Wiedereinschalten enden inzwischen abgelaufene Events mit End-Aktionen |

Nach den Tests alle Events wieder deaktivieren und Testmodus beenden.

#### T18 – Hauptschalter

| Schritt | Erwartung |
|---|---|
| GUI Allgemein: Plugin **aus** | HUD verschwindet, Kristall-Abgleich, Flaschen, Totem und Events ruhen |
| Energy Bottle abfüllen versuchen | Keine Reaktion bzw. abgeschaltet |
| Admin-Befehle (`/xmas set …`) | Funktionieren weiter; Änderungen werden gespeichert (Autosave läuft weiter) |
| Plugin wieder **an** | Alles läuft wieder |

#### T19 – Persistenz

| Schritt | Erwartung |
|---|---|
| B: Kristall Schatten, Tier 2, Energie 8; Server stoppen und starten | B hat alles unverändert |
| `playerdata/<uuid>.json` von Hand kaputt machen (Unsinn reinschreiben), Server starten, B joint | Datei wird als `<uuid>.broken-<zeit>.json` gesichert, B bekommt frische Daten (und ggf. einen neuen Kristall), Warnung im Log |
| Server hart beenden (Prozess killen) mit laufendem Totem | Nach dem Start läuft das Totem weiter (Stand der letzten vollen Sekunde) |

#### T20 – Ressourcenpaket im Spiel

| Schritt | Erwartung |
|---|---|
| Alle 5 Kristalle × 2 Tiers × 4 Zustände ansehen (40 Items) – am schnellsten im Kreativ-Tab (T22), sonst per `/xmas give` | Keine lila-schwarzen Texturen, Tier 2 sieht anders aus als Tier 1, Zustände unterscheidbar |
| Alle 5 Hilfs-Items ansehen | Eigene Texturen |
| HUD-Icons aller 5 Kristalle (per `/xmas set B crystal …` durchschalten) | Drei passende Icons je Kristall |
| Ladebalken beim Abfüllen | Sitzt sichtbar unter dem Fadenkreuz (Position dokumentieren) |
| Totem | Sieht wie ein aktiver, unheilvoller Tresor aus |
| Sprache Deutsch und Englisch umstellen | Item-Namen übersetzt |

#### T21 – Mehrspieler und Last (optional)

| Schritt | Erwartung |
|---|---|
| 5+ Spieler gleichzeitig im Totem-Kreis | Alle werden Teilnehmer, alle bekommen ihre Energie |
| Mehrere Spieler füllen gleichzeitig ab | Jeder Vorgang unabhängig |
| `effects.quality` auf `LOW` | Spürbar weniger Partikel |
| TPS beobachten (z. B. mit der Mod „spark“) während Totem + Animationen | Keine merklichen Einbrüche |

#### T22 – Kreativ-Tab

Vorbereitung: A ist OP und im **Kreativmodus**, B ist kein OP. Optional ein Client mit Polymer ([6.3](#63-optional-polymer-im-client-kreativ-tab-als-echter-tab)).

| Schritt | Erwartung |
|---|---|
| Client **mit** Polymer: A öffnet das Kreativinventar | Tab **Xmas-SMP** vorhanden, Icon = makelloser Tier-2-Feuer-Kristall, Tab-Name auf Deutsch und Englisch „Xmas-SMP“ |
| Vanilla-Client: A tippt `/polymer creative` | Polymer-Kreativ-Menü öffnet sich, Tab Xmas-SMP wählbar |
| Inhalt zählen | 40 Kristalle (je Kristall Tier 1 makellos → zerbrochen, dann Tier 2 makellos → zerbrochen), danach Upgrader, Energy Bottle, Kristall-Händler, Repair Kit, Kristall-Splitter |
| Alle Texturen ansehen | Keine lila-schwarzen Texturen, Namen korrekt |
| Spieler **ohne** OP öffnet den Tab (B im Kreativmodus) | Tab ohne Energy Bottle |
| A nimmt einen Kristall aus dem Tab, wartet 2 s | Kristall bleibt im Inventar (ungebunden, wird nicht entfernt), A's eigener Kristall bleibt unverändert |
| A: Schleichen + Rechtsklick mit dem Tab-Kristall | Nichts (keine Ultimate-Meldung) |
| A legt den Tab-Kristall in eine Truhe | Bleibt in der Truhe liegen |
| A (Kreativ, Energie 5, hat Kristall) nimmt die Energy Bottle aus dem Tab und rechtsklickt | Flasche wird getrunken: `+1 Energie (6/10)`; `audit.log` enthält `creative_bottle` mit `bottle=<id>`; `/xmas energy audit A` zeigt `bottle_drink` |
| `/xmas set A energy 10`, neue Tab-Flasche rechtsklicken | `Deine Energie ist schon voll.`; Flasche bleibt; `/xmas energy bottles` listet sie mit Herkunft `creative` |
| Diese registrierte Flasche B geben, B (Survival, Energie < 10) trinkt | `+1 Energie`, gültig |
| A nimmt eine Tab-Flasche, wechselt in **Survival**, rechtsklickt | `Diese Energy Bottle ist ungültig und wurde entfernt.`; `/xmas energy audit A` zeigt roten Eintrag `suspicious`, `ohne-id … unbekannt (Fälschung)` |
| A gibt B eine **unregistrierte** Tab-Flasche, B rechtsklickt | Ungültig, entfernt, `suspicious` |
| Unregistrierte Tab-Flasche in die Werkbank | Kein Ergebnis |
| Upgrader / Händler / Repair Kit aus dem Tab benutzen (in Survival) | Funktionieren wie gecraftete Items |

---

## 10. Automatische Tests (Entwickler)

### 10.1 Kern-Tests (JUnit 5)

Der Kern (`common/`) kennt keine Minecraft-Klassen und hat **269 Unit-Tests** (Event-Planer, Zeitzonen, Testmodus, Config, JSONC-Editor, Spielerdaten, Effekte, Energie, Energy Bottle inkl. Exploits, Rezepte, Upgrader, Händler, Totem, Admin, Admin-GUI, Abgleich mit dem Ressourcenpaket).

Alle Kern-Tests ohne Fabric (braucht nur Maven Central):

```bash
./gradlew :common:test -Pxmassmp.skipFabric
```

Kern bauen **und** testen (wie in der CI):

```bash
./gradlew :common:build -Pxmassmp.skipFabric
```

Eine einzelne Testklasse:

```bash
./gradlew :common:test -Pxmassmp.skipFabric --tests "de.xmassmp.totem.TotemServiceTest"
```

Eine einzelne Testmethode (Muster mit `*` erlaubt):

```bash
./gradlew :common:test -Pxmassmp.skipFabric --tests "de.xmassmp.bottle.BottleServiceTest.*copy*"
```

Erneut ausführen, obwohl nichts geändert wurde:

```bash
./gradlew :common:test -Pxmassmp.skipFabric --rerun-tasks
```

**Ergebnis lesen:**

- Konsole: `BUILD SUCCESSFUL` = alle grün. Bei Fehlern nennt Gradle die Klasse und Methode.
- HTML-Bericht: `common/build/reports/tests/test/index.html` im Browser öffnen.
- XML je Klasse: `common/build/test-results/test/`.

Die Tests finden das Ressourcenpaket über die System-Property `xmassmp.resourcepack` (setzt `common/build.gradle.kts` automatisch auf `resourcepack/`). Darum immer über Gradle starten, nicht direkt aus der IDE ohne diese Property.

Wichtige Testklassen:

| Bereich | Klasse(n) |
|---|---|
| Events | `EventSchedulerTest`, `EventScheduleTest`, `EventServiceTest`, `EventLoaderTest`, `EventRobustnessTest` |
| Config | `ConfigManagerTest`, `JsoncEditorTest` |
| Spielerdaten | `PlayerDataStoreTest`, `PlayerDataSafetyTest`, `PlayerEventsTest` |
| Energie | `EnergyServiceTest`, `StageEffectsTest`, `EnergyAccessTest` |
| Energy Bottle | `BottleServiceTest` |
| Gameplay | `UpgraderAndTraderTest`, `TotemServiceTest`, `RecipeServiceTest` |
| Kristalle / Pack | `CrystalServiceTest`, `CrystalPackTest` |
| Fähigkeiten-Gerüst | `AbilityRegistryTest`, `AbilityServiceTest`, `PassiveServiceTest`, `HudAndControlsTest` |
| Effekte | `EffectEngineTest`, `ShapesTest`, `DelayedSoundTest`, `DisplayEffectTest` |
| Admin | `AdminServiceTest`, `AdminGuiTest` |
| Texte | `MessagesTest` (jeder Sprachschlüssel aus dem Code existiert im Pack) |

Test-Hilfen: `TestPlatform` (Fake-Plattform), `RecordingHost` (zeichnet Event-Aktionen auf), `MutableClock` (steuerbare Uhr).

### 10.2 Ressourcenpaket prüfen

```bash
python3 tools/resourcepack.py validate
```

Prüft alle Modell→Textur- und Item→Modell-Referenzen, JSON-Syntax, PNG-Dateien und Sprachdateien. Erwartet: `0 Fehler`. Warnungen über unbenutzte `gem-breaking/*.png` sind bekannt (todo 1.2).

```bash
python3 tools/resourcepack.py build
```

Prüft und baut `build/Xmas-SMP-resourcepack.zip`, gibt den SHA-1 aus. Bei Fehlern wird nichts gebaut.

### 10.3 Fabric-JARs bauen

```bash
./gradlew :fabric:build :client:build
```

Andere Versionen der Abhängigkeiten probeweise (wie in der CI-Matrix):

```bash
./gradlew :fabric:build -Pminecraft_version=1.21.11 -Pfabric_api_version=0.139.4+1.21.11 -Ppolymer_version=0.15.0+1.21.11 -Psgui_version=1.12.0+1.21.11
```

### 10.4 Fabric-Server direkt aus dem Repo starten (Loom)

Fabric Loom bringt Startaufgaben mit, damit braucht man keinen separaten Server-Ordner:

```bash
./gradlew :fabric:runServer
```

Der Server läuft dann in `fabric/run/`. Beim ersten Mal `fabric/run/eula.txt` auf `eula=true` setzen und erneut starten. Für einen Client mit dem Client-Mod:

```bash
./gradlew :client:runClient
```

(Die Run-Aufgaben stellt Loom standardmäßig bereit; falls sie fehlen, mit `./gradlew :fabric:tasks` nachsehen.)

### 10.5 CI (GitHub Actions)

Workflow `.github/workflows/build.yml`, läuft bei Pushes auf `main`, bei Pull Requests und manuell:

| Job | Was | Artefakt |
|---|---|---|
| Ressourcenpaket prüfen | `python3 tools/resourcepack.py build` | `resourcepack` |
| Kern (Tests) | `./gradlew :common:build -Pxmassmp.skipFabric` | bei Fehlern `common-test-report` (HTML-Bericht) |
| Fabric 1.21.11 | `./gradlew :fabric:build :client:build …` | `xmas-smp-fabric-1.21.11`, `xmas-smp-client-1.21.11` |

Vor einem Merge müssen alle drei Jobs grün sein. Bei roten Tests: Artefakt `common-test-report` herunterladen und `index.html` öffnen.

---

## 11. Fehlersuche

| Problem | Ursache / Lösung |
|---|---|
| Server startet nicht, Fehler „requires fabric-api“ | Fabric API fehlt in `mods/` oder falsche Version |
| Fehler „requires minecraft >=1.21.11“ / Java-Fehler | Falsche Minecraft-Version oder Java < 21 |
| `/xmas` → `Xmas-SMP ist nicht gestartet – Details im Server-Log` | Kern ist beim Start abgestürzt. In `logs/latest.log` nach dem ersten Stacktrace mit `Xmas-SMP` suchen |
| `/xmas` unbekannt | Kein OP-Level 2 bzw. kein Recht `xmassmp.admin` |
| Items sehen aus wie Echoscherbe/Ghast-Träne/Smaragd | Ressourcenpaket nicht aktiv, siehe [Abschnitt 5](#5-ressourcenpaket-an-die-spieler-ausliefern) |
| Lila-schwarze Texturen | Pack veraltet oder defekt → `python3 tools/resourcepack.py validate` |
| HUD zeigt Kästchen statt Icons | Ressourcenpaket nicht aktiv |
| Kein Kristall beim Join | `crystalItem.giveOnJoin` aus, oder alle Kristalle in der Config deaktiviert, oder Spieler hatte schon einen |
| F tauscht die Hände statt eine Kraft auszulösen | Erwartet – es gibt noch keine Kräfte. Später: `controls.swapHandsActivates` prüfen |
| Abfüllen startet nicht | Kreativmodus? Nicht geschlichen? Flasche in der Zweithand? Energie < 2? Kampfsperre 15 s? Energy Bottles aus? |
| Abfüllen bricht ständig ab | Hohe Latenz (Pakete kommen > 6 Ticks auseinander) oder leichte Bewegung > 1 Block |
| Energy Bottle „ungültig“ | Kopie oder Fälschung → `/xmas energy audit` und `/xmas energy bottles` |
| Totem lässt sich in 1 Klick zerstören | Spieler war im Kreativmodus (Vanilla bricht sofort ab) |
| Totem lässt sich nicht aufstellen | Schon ein Totem aktiv (`/xmas totem status`), Platz belegt, außerhalb der Weltgrenze, oder Totem in der Config aus |
| Event startet im Testmodus nicht | Event ist `enabled: false` – der Testmodus startet nur aktivierte Events |
| Event startet zur falschen Uhrzeit | `events.timezone` prüfen (Standard `Europe/Berlin`) |
| Änderung in `config.jsonc` wirkt nicht | `/xmas reload` vergessen, oder Reload fehlgeschlagen (Antwort lesen) |
| Gradle: `Could not resolve …` / `429 Too Many Requests` | Kein oder gedrosselter Zugriff auf Maven-Repositories → später erneut versuchen; ohne `maven.fabricmc.net` nur `-Pxmassmp.skipFabric` |
| Kreativ-Tab fehlt im Kreativinventar | Ohne Polymer im Client gibt es keinen echten Tab → `/polymer creative` im Kreativmodus benutzen oder Polymer installieren ([6.3](#63-optional-polymer-im-client-kreativ-tab-als-echter-tab)) |
| Energy Bottle fehlt im Kreativ-Tab | Nur Operatoren sehen sie. Im Vanilla-Kreativinventar zählt dafür außerdem die Client-Option „Operator-Gegenstände-Tab“ (Optionen → Steuerung) – einschalten und Tab neu öffnen |
| Energy Bottle aus dem Kreativ-Tab „ungültig und entfernt“ | Erwartet, wenn sie nicht von einem Admin **im Kreativmodus** zuerst rechtsgeklickt wurde, siehe [8.6](#86-kreativ-tab) |
| Client-Mod-Tasten tun nichts | `controls.clientKeys` aus, Fabric API auf dem Client fehlt, oder Taste doppelt belegt (Optionen → Steuerung) |

Log-Stellen: `logs/latest.log` (Server), `.minecraft/logs/latest.log` (Client). Die Mod loggt unter dem Namen `Xmas-SMP`. Ein einzelner Fehler in einem Teil (z. B. HUD) bringt den Server nicht zum Absturz; er wird einmal geloggt, bis der Teil wieder läuft.

---

## 12. Testprotokoll-Vorlage

In eine neue Datei kopieren (z. B. `testlauf-2026-11-01.md`) und ausfüllen.

```markdown
# Testlauf Xmas-SMP

- Datum:
- Tester:
- Mod-Version / Commit:
- Server: Fabric 1.21.11, Loader …, Fabric API …
- Client: Vanilla / mit Client-Mod
- Ressourcenpaket: AutoHost / lokal / selbst gehostet
- Spieleranzahl:

| Test | Ergebnis (OK / FEHLER / übersprungen) | Notiz (was genau abwich, Screenshot, Log-Zeile) |
|---|---|---|
| T01 Server-Start | | |
| T02 Rechte | | |
| T03 Erster Join | | |
| T04 Seelenbindung | | |
| T05 HUD | | |
| T06 Steuerung | | |
| T07 give/set | | |
| T08 PvP-Energie | | |
| T09 Abfüllen | | |
| T10 Trinken/Zerlegen/Exploits | | |
| T11 Rezepte | | |
| T12 Upgrader | | |
| T13 Händler | | |
| T14 Repair-Totem | | |
| T15 Admin-GUI | | |
| T16 Reload/kaputte Config | | |
| T17 Events | | |
| T18 Hauptschalter | | |
| T19 Persistenz | | |
| T20 Ressourcenpaket | | |
| T21 Mehrspieler/Last | | |
| T22 Kreativ-Tab | | |

## Gefundene Fehler

1. Testfall: …
   Schritte: …
   Erwartet: …
   Tatsächlich: …
   Log: …
```

Gefundene Fehler gehören danach in `bug.md` (Format und Schweregrade siehe dort), erledigte Testpunkte in `todo.md` Abschnitt 10 abhaken.
