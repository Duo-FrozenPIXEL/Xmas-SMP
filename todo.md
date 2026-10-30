# Xmas-SMP – To-do-Liste

Abgeleitet aus `Instructions.md` und einem Abgleich mit dem Texturpack `BlissGemsV4.1-fixed.zip`.
Entscheidungen des Owners (Stand 2026-10-20) sind unter **0. Festgelegte Entscheidungen** festgehalten.
Die Spielregeln aus der Recherche-Runde (Energie, Items, Rezepte, Totem, Steuerung) und die Bau-Reihenfolge stehen in **`docs/implementierungsplan.md`**; alle Vorschläge dort hat der Owner am 2026-10-30 übernommen.
Der ausführliche Kräfte-Entwurf mit allen Zahlen steht in **`docs/kraefte-entwurf.md`** (wartet auf Freigabe).

**Stand 2026-10-30 – als Nächstes:**
1. Kräfte-Entwurf freigeben (`docs/kraefte-entwurf.md`, Fragen in Abschnitt 9), dann die 25 Fähigkeiten bauen (Abschnitt 5)
2. Testrunde mit echten Spielern auf einem Fabric-Server (Abschnitt 10, „Spieler-Test der neuen Systeme“)
3. Christmas-Twist-Datei vom Owner (Abschnitt 8)

Legende: `[ ]` offen · `[x]` erledigt · **(V)** = vor Umsetzung verifizieren (Versionsnummern, Original-Verhalten)

---

## 0. Festgelegte Entscheidungen

| Thema | Entscheidung |
|---|---|
| Plattform-Reihenfolge | **Fabric zuerst** (vollständig), danach **Paper** und **NeoForge** auf gemeinsamem Kern-Modul |
| Forge-Variante | **NeoForge** |
| Versionen | Minecraft Java **1.21.11 bis 26.3** |
| Kräfte | Werden selbst entworfen (Entwurf siehe Abschnitt 5), angelehnt ans Bliss-Original |
| Grundregeln | Repo nie löschen · Grundidee + Qualitätslevel halten · **jede Kraft hochwertig animiert mit interessanten Partikeln** |
| Energie | Start 5 (Pristine), 0–10. Bei 0 keine aktiven Kräfte, bei 1–2 nur Primärkraft, ab 3 Sekundär (+ Ultimate mit Tier 2). Passive immer, egal wo der Kristall liegt. Kill eines Opfers mit 0 Energie bringt nichts |
| Kräfte je Kristall | Primär, Sekundär, **Ultimate (Tier 2)** + Passive T1/T2 |
| Items | Fragments nur aus Energy Bottle (1 → 9). Trader per GUI mit freier Wahl. Repair Kit = Totem (15 Blöcke, 10 Min., auf 5 Energie). Revival Tome und All-for-One entfallen |
| Steuerung | Actionbar-HUD + optionaler Client-Mod mit Tasten in den Einstellungen. Ohne Mod: `F` = Primär, Schleichen + `F` = Sekundär, Schleichen + Rechtsklick mit dem Kristall = Ultimate |
| Rezepte | Upgrader wie Original; Händler 4 Diamanten + 4 Drachenatem + 1 Fragment; Repair Kit mit 2 Diamantblöcken statt Netherit; alle in `config.jsonc` änderbar |
| Exploit-Schutz | Flaschen-Register, Abfüllen nur mit ≥ 1 Rest-Energie und nicht im Kampf (15 s), Trinken bei 10 gesperrt, kein Ablauf |
| Totem-Details | Countdown wird beim Loslassen/Wegschauen/Tod zurückgesetzt; Teilnehmer = in den ersten 30 s im Kreis; zerstört = Kit verloren |
| Tier 2 | Gelber Name in der Tab-Liste, kein Glowing |

### Kristall → Textur-Zuordnung

| Kristall | Original-Gem (Textur) | Farbe | HUD-Icons (Vorschlag) |
|---|---|---|---|
| **Feuer** | Strength | rot | `fire1` (Feuerkugel), `fire2` (Lagerfeuer) |
| **Erde** | Fire | orange | `strength1` (Kette), `strength2` (Arm/Kraft) |
| **Wind** | Puff | weiß | `puff1` (Dash-Pfeil), `puff2` (Faust/Stoß) |
| **Schatten** | Astra | lila | `astra1` (Dolch), `astra2` (Geist) |
| **Natur** | Wealth | grün | `wealth1` (Kleeblatt), `wealth2` (Ranke) |

Drittes Icon (Ultimate) je Kristall: `ultimate_<kristall>.png` (Platzhalter-Grafiken, U+E020–E024).

- [x] `Instructions.md` an die neue Zuordnung anpassen (dort steht noch „Feuer = Fire gem texturen“) – **nur nach Freigabe durch den Owner**
- [x] Entscheiden, ob die Icons wie oben „überkreuz“ verwendet werden (Feuer bekommt die Feuer-Icons, obwohl es die Strength-Gem-Textur nutzt) oder ob neue Icons gezeichnet werden

---

## 1. Befunde aus dem Asset-Abgleich (Texturpack reparieren)

Korrigierte Quelle liegt in `resourcepack/`, Details und CMD-Schema in `docs/resourcepack.md`, Prüfen/Bauen mit `python3 tools/resourcepack.py validate|build`.

Das Pack ist `pack_format 34` (= 1.21–1.21.1), nutzt aber bereits das Item-Definitions-Format (`assets/minecraft/items/*.json`) ab 1.21.4. Folgende Probleme wurden gefunden:

### 1.1 Fehler, die zu fehlenden (lila-schwarzen) Texturen führen
- [x] `strength_gem_tier1_pristine4.json` verweist auf `item/strengthgemtier1` → **Datei fehlt**
- [x] `strength_gem_tier2_pristine4.json` verweist auf `item/strengthgemtier2` → **Datei fehlt**
- [x] Ursache: Strength-Dateien sind anders benannt als alle anderen Gems:
  - `strengthgemtier1pristine.png` = stark gebrochene Variante (entspricht bei anderen Gems `…pristine2`)
  - `strengthgemtier1pristine2.png` = intakte Variante (entspricht bei anderen Gems `…tier1`)
  - gleiches Muster bei Tier 2
  - **Fix:** Strength-Texturen auf das Schema der anderen Gems umbenennen. Da Strength = **Feuer**, ist das Pflicht.
- [x] `echo_shard.json` CMD 9001/9002 verweisen auf `minecraft:item/allforone_gem_t1/_t2` → **Modelle existieren nicht** (Texturen `allforone_gem_t1/_t2.png` liegen aber vor). Entweder Modelle anlegen oder Einträge entfernen (für die 5 Kristalle nicht nötig).

### 1.2 Inkonsistenzen / Altlasten
- [x] Namens-Versatz dokumentieren: Modell `*_tier1` nutzt Textur `*tier1pristine2` (am stärksten gebrochen), Modell `*_pristine4` nutzt die intakte Textur `*tier1`. Modellname und Texturname sind also um eine Stufe verschoben → im Code eine eindeutige Tabelle „Energiestufe → Modell“ pflegen (oder Texturen/Modelle sauber umbenennen).
- [x] `nether_star.json` **und** `ghast_tear.json` mappen beide CMD 4001 auf `energy_bottle` → auf ein Basis-Item festlegen.
- [x] `enchanted_book.json`: `revival_tome` hat Schwelle `1` → jedes Buch mit CMD 1–3000 zeigt die Revival-Tome-Textur. Schwellen sauber setzen.
- [x] Ungültiges `"parent": "generated"` in `energy_bottle`, `gem_trader`, `trader`, `upgrader`
- [ ] `trader.json` (Modell) wird nirgends referenziert; nur `gem_trader.json` ist gemappt → eines wählen, das andere entfernen oder als Gem-Trader-Block/-Station nutzen.
- [x] `repair_kit.json` nutzt Textur `revivalkit.png` (Verbandskasten); `repairitem.png` wird nicht genutzt (sieht nach UI-Atlas-Fragment aus) → entfernt (liegt weiter im Original-ZIP).
- [x] `empty.png` (400×400, RGB, magenta) wird nirgends genutzt → entfernt (liegt weiter im Original-ZIP).
- [x] `assets/minecraft/items/white_dye-working.txt` ist eine Arbeitsdatei mit abweichendem Format → entfernen.
- [ ] `strengthgemtier2pristine3/4.png` haben graue Randpixel links → optisch nachbessern. Geprüft: Spalte x=0, Zeilen 8–15 (RGB 123,123,123); bei allen anderen Gems sind diese Pixel Teil der farbigen Kontur, Transparentmachen würde Löcher schneiden → Farbe muss gezeichnet/gewählt werden (auch `pristine2` betroffen)
- [ ] `gem-breaking/*.png` (8 graue Riss-Overlays) werden nicht genutzt → als Basis für Bruch-Animation/Partikel oder für eingefärbte neue Texturen verwenden.
- [ ] Nicht benötigte Gems (Flux, Life, Speed) aus dem eigenen Pack entfernen oder für spätere Erweiterungen (Christmas-Twist) markieren.

### 1.3 Pack für 1.21.11 – 26.3 fit machen
- [x] `pack.mcmeta` auf das neue Format mit `min_format` / `max_format` umstellen (ab 1.21.9 Pflicht). Geprüft (`docs/versionen.md`): 1.21.11 = 75, 26.1 = 84, 26.2 = 88, 26.3 = 97.1 – `min_format` 75 / `max_format` 97 passt.
- [x] Beschreibung im `pack.mcmeta` auf „Xmas-SMP“ ändern (aktuell „BlissGems V4 (1.21.x)“); `pack.png` ggf. ersetzen.
- [ ] (teilweise) Eigene Server-Items über Polymer (`xmassmp:<typ>`) sind umgesetzt; die Anzeige läuft weiter über `custom_model_data`. Umstieg auf die `item_model`-Komponente erwägen (je Kristall/Stufe eine eigene Item-Definition unter `assets/xmassmp/items/`) – sauberer und versionsstabiler.
- [x] Namespace von `blissgems` auf eigenen Namespace `xmassmp` umziehen.
- [ ] Mit jeder Zielversion (1.21.11, 26.1, 26.2, 26.3) im Client laden und alle Modelle/Icons prüfen.
- [x] Pack-Validator-Script `tools/resourcepack.py` bauen (läuft in der CI, prüft auch die Sprachdateien): alle Modell→Textur- und Item→Modell-Referenzen prüfen (die Fehler aus 1.1 wären damit sofort aufgefallen).

### 1.4 Fehlende Assets (neu zu erstellen)
- [ ] Texturen/Modelle für die Admin-GUI (Hintergrund über Custom-Font, Buttons/Icons)
- [ ] Icons für Event-System (Kalender, Uhr, Event-Status)
- [ ] Ggf. eigene Partikel-Texturen (falls Vanilla-Partikel nicht reichen; nur mit Client-Pack möglich)
- [ ] Platzhalter für Christmas-Twist-Items (folgt in separater Datei)
- [x] HUD-Icon für die **Ultimate** je Kristall (`icons/ultimate_<kristall>.png`, U+E020–E024, Platzhalter-Grafiken)
- [x] Font-Zeichen für den Ladebalken beim Abfüllen der Energy Bottle (U+E030–E03A, als Untertitel unter dem Fadenkreuz – Position im Spiel prüfen)
- [x] Darstellung für das aufgestellte Totem: Client sieht einen aktiven, unheilvollen Tresor, darüber schwebt das Repair Kit als Item-Display
- [x] Lokalisierung: `assets/xmassmp/lang/de_de.json` und `en_us.json` für alle Itemnamen/Texte (Test prüft, dass jeder Schlüssel aus dem Code existiert)

---

## 2. Recherche (Original-Bliss-Verhalten) **(V)**

Ziel laut Instructions: „schau dir an wie das Plugin normalerweise funktioniert (Foren)“.
Ergebnisse mit Quellen und Sicherheit je Aussage: `docs/bliss-original.md`. Die dort als „Entscheidung des Owners nötig“ markierten Punkte sind größtenteils entschieden, siehe `docs/implementierungsplan.md` Abschnitt 1 (u. a. Startenergie 5 = Pristine wie im Original).

- [x] Energiesystem recherchieren: Wertebereich (meist 0–10), Gewinn bei Kill / Verlust bei Tod, Namen der Stufen (Broken … Pristine +X)
- [x] Verhalten bei 0 Energie (Gem zerbricht? Fähigkeiten aus? Bann/Revive?)
- [x] Tier-System: Wie wird Tier 2 erreicht (Upgrader), was schaltet Tier 2 frei?
- [x] Gem Trader: Gem gegen zufälligen/gewählten anderen Gem tauschen? Kosten? Trader-Token? Shrine/Station?
- [x] Gem Fragments: Wofür (Crafting des Traders/Upgraders/Repair)?
- [x] Energy Bottle: Energie abzapfen / übertragen
- [x] Repair-Kit / Revival-Tome: Gem reparieren, gebannte/tote Spieler wiederbeleben
- [x] Aktivierung der Fähigkeiten im Original (Tasten, Offhand, Klick-Kombinationen), Cooldown-Anzeige (Actionbar mit Font-Icons ``–``)
- [x] Startverhalten: Bekommt jeder Spieler beim ersten Join einen zufälligen Gem?
- [x] Ergebnisse als `docs/bliss-original.md` dokumentieren und Abweichungen auflisten (Begründungen bzw. Entscheidungen stehen beim Owner)

---

## 3. Projekt-Setup & Architektur

### 3.1 Build-System
- [x] Gradle-Multi-Projekt anlegen (`paper/`, `neoforge/` vorerst nur README):
  - `common/` – plattformunabhängiger Kern (Kristalle, Energie, Level, Trader, Config, Event-Scheduler-Logik)
  - `fabric/` – **erste Plattform**
  - `paper/` – später
  - `neoforge/` – später
- [x] Multiloader-Ansatz wählen: eigenes `common`-Modul mit Plattform-Interfaces (`Platform`, `EffectSink`, `EventHost`), Fabric kompiliert die Kern-Quellen mit
- [ ] Multi-Version-Builds 1.21.11 → 26.3 einrichten (z. B. **Stonecutter** 0.9.8) – braucht Gradle-9-Update, eigenes Loom-Setup je Version und sgui 2.x (API-Änderungen ungeprüft); nur mit lokalem Build testbar
- [x] Java-Versionen geprüft: 1.21.11 = Java 21, alle 26.x = Java 25 (`docs/versionen.md`)
- [x] Geprüft: ab 26.1 nicht mehr obfuskiert → für 26.x Plugin `net.fabricmc.fabric-loom` ohne Mappings, `implementation` statt `modImplementation`, Gradle 9.x nötig (Wrapper steht auf 8.14.3); Details und Versionen von Fabric API/Polymer/sgui je 26.x in `docs/versionen.md`
- [x] CI (GitHub Actions): Pack-Validator, Kern-Tests, Fabric-Build (1.21.11), JARs als Workflow-Artefakte
- [ ] CI: weitere Zielversionen (26.x) und Paper/NeoForge in die Matrix, Artefakte als GitHub-Release (Client-Mod wird bereits mitgebaut)
- [x] `README.md` füllen: Installation, Befehle, Permissions, Konfiguration, unterstützte Versionen

### 3.2 Fabric (Server-seitig, Vanilla-Clients)
- [x] Fabric-Modul baut in der CI (1.21.11, Versionen in `gradle.properties` bestätigt)
- [ ] Fabric-Mod auf einem Testserver starten und `/xmas`-Befehle, Items und Events im Spiel prüfen – Server-Start, Befehle (RCON), Totem gegen TNT und Rezepte im Crafter sind geprüft (2026-10-30); **mit echtem Spieler noch offen**
- [x] Server-seitige Mod, damit Spieler **keinen** Client-Mod brauchen
- [x] **Polymer** (+ Polymer Resource Pack / AutoHost) für Custom Items und automatische Resource-Pack-Auslieferung (Pack-Assets registriert; AutoHost konfiguriert der Server-Betreiber)
- [x] **sgui** für Chest-GUIs (Admin-GUI, Gem Trader)
- [x] Eigene Server-Items und Totem-Block über Polymer (`XmasRegistry`), Rezepte als eigene Rezepttypen (`XmasRecipe`, `data/xmassmp/recipe/`)
- [x] Paket `xmassmp:ability` für den Client-Mod (mit Spam-Schutz), gelber Tab-Name per Mixin
- [x] **fabric-permissions-api** (LuckPerms-kompatibel) für Rechte
- [x] Fabric API für Events (Join, Quit, Tod, Kill, Respawn, Tick, Item-Nutzung) + Mixin für die Zweithand-Taste `F` → `PlayerListener` im Kern

### 3.3 Kern-Abstraktionen (in `common`)
- [x] `Crystal`-Registry (5 Kristalle: Feuer, Erde, Wind, Schatten, Natur), erweiterbar für Christmas-Twist
- [x] `Ability`-Interface (Passive / Aktiv, Cooldown, Tier-Voraussetzung) + `AbilityService` (Schalter, Cooldown × Config × Event-Modifikator)
- [x] Energie-Skalierung der Fähigkeiten: `AbilityContext.strength` (0 = zerbrochen, sonst `energy.minStrength`–1.0); Zugriff je Slot über `energy.access`
- [x] `PassiveService`: Passive wirken immer (auch bei 0 Energie, Kristall egal wo im Inventar)
- [x] `EnergyLedger` (`energy.log`), `BottleRegistry` (`bottles.json`), `TotemService` (`totem.json`), `RecipeService`
- [x] `EffectEngine` / Animations-API: Partikel-Formen (Ring, Spirale, Kugel, Linie, Welle), Keyframes/Phasen über mehrere Ticks mit Easing, geschichtete Sounds, Qualitätsstufe + Partikel-Budget
- [x] Display-Entities (Block-/Item-Display) in der `EffectEngine`: Größe, Drehung, Leuchten, weiche Bewegung; Fabric über Polymer Virtual Entities (nur Pakete, nie in der Welt gespeichert), Budget `effects.maxDisplays`
- [x] `PlayerData` (Kristall, Tier, Energie, Cooldowns) mit Persistenz (pro Spieler JSON; defekte Dateien werden gesichert) – Level folgt, falls gewünscht (4.3)
- [x] Plattform-Adapter: Partikel senden, Items bauen, Befehle registrieren, Tick-Takt (Fabric)
- [x] Plattform-Adapter: `PlayerHandle` (Position, Blickrichtung, Schleichen, Actionbar, Chat), Online-Spieler
- [x] Plattform-Adapter: Display-Entities spawnen
- [x] Plattform-Adapter: GUIs öffnen (`GuiHost`/`GuiView` im Kern, Fabric über sgui `FabricGuiHost`)

---

## 4. Gameplay-Systeme (aus dem Original)

### 4.1 Kristall-Items
- [x] Je Kristall: Tier 1 + Tier 2, je 4 Bruchstufen (Modelle/Texturen vorhanden, siehe 1.2) – Item folgt automatisch den Spielerdaten
- [x] Item-Lore: Kristallname, Tier, Zustand, Energie, Farbcodes passend zur Kristallfarbe
- [ ] Item-Lore: Fähigkeiten (sobald die Kräfte feststehen, Abschnitt 5)
- [x] Kristall ist seelengebunden: kann nicht gedroppt/in Kisten gelegt/verloren werden (`crystalItem.soulbound`) – Abgleich jede Sekunde, Kopien in Behältern/Item-Rahmen sind wirkungslos
- [x] Nur ein Kristall pro Spieler aktiv (Spielerdaten sind die Wahrheit, das Item nur Anzeige)
- [x] Erster Join → zufälliger aktivierter Kristall mit kleinem Effekt (`crystalItem.giveOnJoin`)

### 4.2 Breaking-Pristine-System (Energie)
- [x] Energie-Wert pro Spieler (0–10, Start 5 = „Makellos“)
- [x] Kill eines Spielers → +Energie, eigener Tod → −Energie (`energy.killGain`/`deathLoss`, Anti-Farming `sameVictimCooldownMinutes`, Event-Modifikator `energy_multiplier`)
- [x] Energie → Bruchstufe → Modell (4 sichtbare Stufen: zerbrochen, beschädigt, zerkratzt, makellos; Schwellen 1/3/5 in `energy.stages`; bei 10 Verzauberungs-Schimmer)
- [x] Energie skaliert Fähigkeitsstärke (Schaden, Dauer, Radius) und schaltet bei „zerbrochen“ Fähigkeiten ab (`AbilityService.Result.BROKEN`)
- [x] **Bruch-Animation** beim Stufenwechsel (`StageEffects`): Spannungsring, Splitter-Kugel, Funken, leuchtende Kristallscherben (Item-Displays) in Kristallfarbe, bei „zerbrochen“ Rauch und Nachhall – im Spiel noch nicht angesehen
  - [ ] Riss-Overlays `gem-breaking/*` einbinden (braucht eigene Item-Modelle im Pack, z. B. als Item-Display vor dem Spieler)
- [x] **Reparatur-/Aufwertungs-Animation** beim Aufstieg (`StageEffects`): Spirale sammelt Splitter, Scherbe setzt sich leuchtend zusammen, Lichtring am Boden – im Spiel noch nicht angesehen
- [x] Energieregeln umbauen (Plan Phase 1): 11 Zustände mit Original-Namen, Schwellen 1/3/5, Zugriff je Energie (0 = keine Aktiven, 1–2 = nur Primär, ab 3 alle), Passive immer, kein Gewinn für Kill eines 0-Energie-Opfers, Energie-Protokoll
- [x] Energy Bottle (Plan Phase 3): Schleichen + Rechtsklick mit Glasflasche ~5 s halten, Ladebalken am Fadenkreuz, −1 Energie; Rechtsklick auf die Flasche gibt +1 (auch bei 0, auch für andere)
- [x] Energy Bottle exploit-sicher: Flaschen-Register mit eindeutiger ID, nicht stapelbar, keine Vanilla-Verwendung, Ticks zählt der Server, Prüfung beim Zerlegen je Flasche, `/xmas energy audit`
- [x] Repair Kit als Totem (Plan Phase 6): 15 Blöcke Radius, 10 Min., danach Energie unter 5 → 5; Verlassen/Tod/Logout = raus; Koordinaten in Chat + Titel; global nur eins; Zerstören in festen 10 s mit Countdown, Sound und Partikeln
- [x] ~~Revival-Tome~~ entfällt (Repair Kit holt auch Spieler mit 0 Energie zurück)

### 4.3 Level-/Tier-System
- [x] Tier 1 → Tier 2 über **Upgrader**-Item (Rezept: 4 Fragments, 4 Drachenatem, 1 Witherskelettschädel)
- [x] Tier 2 schaltet die **Ultimate** und die stärkere Passive frei (Primär + Sekundär gibt es schon auf Tier 1)
- [x] Upgrade-Animation (Partikelsäule, Lichtblitz, Sound) – im Spiel noch nicht angesehen
- [ ] Optional: Gem-Level/XP innerhalb eines Tiers (klären, ob gewünscht – „gem auf level system“)

### 4.4 Gem Trader
- [x] Gem-Trader-Item (Modell `gem_trader`): Kristall gegen anderen tauschen, Rezept 4 Diamanten, 4 Drachenatem, 1 Fragment in der Mitte
- [x] GUI mit **freier Wahl** des neuen Kristalls, Tier/Energie bleiben erhalten (Plan Frage 9)
- [x] Tausch-Animation (Kristall verschwindet in Partikelwirbel, neuer erscheint in neuer Farbe) – im Spiel noch nicht angesehen
- [x] Gem Fragments: einzige Quelle ist 1 Energy Bottle → 9 Fragments (2×2 oder Werkbank)
- [x] Repair-Kit-Rezept: 4 Fragments, 2 Ambosse, 2 Diamantblöcke, 1 Netherit-Schmiedevorlage
- [x] Rezepte in der Config statt im Code (Kosten ohne Neubau änderbar)
- [ ] Optional: platzierbare Trader-Station (Modell `trader`)

### 4.5 Fähigkeits-Steuerung & HUD
- [x] Aktivierung: Zweithand-Taste `F` = Primär, Schleichen + `F` = Sekundär – ohne Client-Mod, abschaltbar (`controls.swapHandsActivates`); Kristalle ohne Kräfte tauschen normal
- [x] Actionbar-HUD mit Font-Icons + Cooldown-Anzeige in Sekunden, Tier-Sperre, zu wenig Energie, zerbrochen, Energie, Totem-Restzeit/-Countdown (`hud.enabled`, `hud.updateTicks`); Rückmeldungen überdecken das HUD kurz
- [x] Cooldown-Ende: kleiner Sound + Partikel-Puls in Kristallfarbe
- [x] Dritter Slot für die Ultimate in Steuerung und HUD (ohne Mod: Schleichen + Rechtsklick mit dem Kristall)
- [x] Optionaler Client-Mod (`client/`): drei Tasten in den Minecraft-Einstellungen, schickt nur die Slot-Nummer; der Server prüft alles selbst

---

## 5. Kräfte (Entwurf) – jede Kraft hochwertig animiert

Kurzfassung. Der ausführliche Entwurf mit Zahlen, Animationsphasen, Partikeln, Sounds und Ultimates steht in `docs/kraefte-entwurf.md` und ersetzt bei Abweichungen diese Liste. Noch keine Kraft ist gebaut.

Allgemeine Qualitätsanforderungen für **jede** Kraft:
- [ ] Mehrphasige Animation (Aufladen → Auslösen → Nachklingen), nicht nur ein einzelner Partikel-Burst
- [ ] Mindestens 2–3 kombinierte Partikeltypen in der Kristallfarbe (`dust`/`dust_color_transition` mit Farbverlauf + thematische Partikel)
- [ ] Passende Sounds (geschichtet, Pitch-Variation)
- [ ] Wo sinnvoll Display-Entities (Block-/Item-Display) für 3D-Effekte (Felsbrocken, Ranken, Meteore)
- [ ] Performance-Budget (max. Partikel pro Tick, Sichtweite), konfigurierbare Qualitätsstufe
- [ ] Keine Block-Zerstörung durch Fähigkeiten (konfigurierbar), Respekt vor Spawn-/Claim-Schutz
- [ ] Balancing-Werte komplett in der Config

### 5.1 Feuer (rot, Strength-Textur)
- [ ] Passiv T1: Feuerresistenz
- [ ] Passiv T2: Nahkampftreffer setzen Gegner kurz in Brand
- [ ] Aktiv 1 – **Flammenstoß**: Sprung, bei Landung expandierender Feuerring (`flame`, `soul_fire_flame`-Akzent, `lava`, Rauch), Schaden + Brand
- [ ] Aktiv 2 – **Meteor**: Glühender Block-Display fällt aus dem Himmel mit Rauch-/Funkenspur, Einschlag mit Schockwelle und Glut-Partikeln

### 5.2 Erde (orange, Fire-Textur)
- [ ] Passiv T1: Kein Rückstoß / Resistenz I
- [ ] Passiv T2: Eile beim Abbauen von Stein/Erze
- [ ] Aktiv 1 – **Erdbeben**: Wellenförmige Schockwelle, Block-Displays springen aus dem Boden (`block`-Partikel des Untergrunds, `falling_dust`), Gegner werden hochgeworfen + verlangsamt
- [ ] Aktiv 2 – **Steinwall/Felsbrocken**: Temporärer Wall aus Block-Displays oder geworfener Felsbrocken mit Trümmer-Partikeln

### 5.3 Wind (weiß, Puff-Textur)
- [ ] Passiv T1: Kein Fallschaden
- [ ] Passiv T2: Doppelsprung
- [ ] Aktiv 1 – **Böe**: Dash nach vorne, Gegner im Kegel werden weggestoßen (`gust`, `cloud`, spiralförmige weiße `dust`-Linien)
- [ ] Aktiv 2 – **Tornado**: Wandernder Wirbel, zieht Gegner hoch (mehrlagige Partikelspirale, `sweep_attack`, Wind-Sounds)

### 5.4 Schatten (lila, Astra-Textur)
- [ ] Passiv T1: Nachtsicht
- [ ] Passiv T2: In Dunkelheit beim Schleichen unsichtbar / Lebensraub nachts
- [ ] Aktiv 1 – **Schattenschritt**: Teleport hinter das anvisierte Ziel (`portal`, `squid_ink`, `reverse_portal`, lila Farbverlauf), kurze Spur zwischen Start und Ziel
- [ ] Aktiv 2 – **Seelenbann**: Dunkle Aura, Blindheit/Dunkelheit für Gegner (`sculk_soul`, `soul`, kreisende Geister-Partikel)

### 5.5 Natur (grün, Wealth-Textur)
- [ ] Passiv T1: Regeneration auf Gras/Laub
- [ ] Passiv T2: Bonus-Ernte / Glück
- [ ] Aktiv 1 – **Rankenfessel**: Wurzeln/Ranken (Block-Displays) wachsen um das Ziel und halten es fest (`happy_villager`, `composter`, `spore_blossom_air`)
- [ ] Aktiv 2 – **Blütenheilung**: Heilkreis für Verbündete (`cherry_leaves`, grüne `dust`-Spiralen, aufsteigende Blüten)

- [x] „Aktiv 1“ = **Primär**, „Aktiv 2“ = **Sekundär** (beide ab Tier 1) – im Entwurf so übernommen
- [x] **Ultimate (Tier 2)** für jeden der 5 Kristalle entwerfen → `docs/kraefte-entwurf.md`
- [ ] Entwurf inkl. Ultimates (`docs/kraefte-entwurf.md`, offene Fragen in Abschnitt 9) mit dem Owner abstimmen, bevor implementiert wird

---

## 6. Admin-/Moderationssystem

- [x] Befehl `/xmas admin` (Permission `xmassmp.admin`, Fallback OP-Level 2) öffnet Konfigurations-GUI
- [x] GUI-Seiten (`AdminGui`, im Spiel noch nicht angesehen):
  - [x] **Allgemein**: Plugin an/aus, Fähigkeiten global an/aus, PvP-Energie an/aus, Reload
  - [x] **Kristalle**: je Kristall aktivieren/deaktivieren, Cooldown-/Schadens-Multiplikator
  - [x] **Energie**: Start-, Min-, Max-Energie, Gewinn/Verlust pro Kill/Tod
  - [x] **Gameplay**: Energy Bottle, Upgrader, Händler, Rezepte an/aus (Muster in `config.jsonc`)
  - [x] **Totem** (auf der Gameplay-Seite): Werte, laufendes Totem mit Restzeit/Teilnehmern, beenden – Hinteleportieren fehlt noch
  - [x] **Spieler**: Online-Spieler wählen → Kristall setzen, Tier setzen, Energie setzen, Cooldowns zurücksetzen
  - [x] **Events**: geplante Events ansehen, aktivieren/deaktivieren, manuell starten/stoppen, Testmodus
  - [x] **Items**: alle Custom-Items zum Herausnehmen (Kristalle, Trader, Upgrader, Energy Bottle, Repair-Kit, Fragments)
- [x] Änderungen aus der GUI sofort speichern und live anwenden: `JsoncEditor` ersetzt nur den geänderten Wert, Kommentare und Formatierung in `config.jsonc`/`events.jsonc` bleiben erhalten
- [x] Zusätzliche Text-Befehle (für Konsole/Automatisierung): `/xmas give`, `/xmas set <spieler> crystal|tier|energy`, `/xmas reload`, `/xmas event start|stop|list|simulate`, `/xmas energy audit|bottles`, `/xmas totem status|cancel`
- [x] Tab-Completion für alle Befehle
- [x] Audit-Log: Admin-Änderungen protokollieren (wer, was, wann) → `<welt>/xmassmp/audit.log`
- [x] Konfigurationsdatei dokumentiert (Kommentare/README)

---

## 7. Event-System (Grundlage für die Weihnachtswoche)

- [x] Event-Scheduler: Events an **bestimmten Tagen zu bestimmten Uhrzeiten** ausführen
- [x] Zeitzone konfigurierbar (Standard `Europe/Berlin`), Sommerzeit-sicher
- [x] Event-Definition in Config: ID, Name, Datum/Zeitraum (z. B. 24.12.–31.12.), Uhrzeit(en), Dauer, Wiederholung (jährlich `MM-dd`, Wochentage), Aktionen
- [x] Aktionstypen (erweiterbar):
  - [x] Broadcast/Titel/Sound an alle
  - [x] Befehl ausführen
  - [x] Globale Modifikatoren (z. B. doppelte Energie, verkürzte Cooldowns) – `cooldown_multiplier` und `energy_multiplier` wirken
  - [x] Hook-Interface für spätere Christmas-Twist-Events (`EventActionRegistry`, `EventScheduler.Listener`)
- [x] Ankündigungen vor Eventstart (z. B. 10 min, 1 min, Countdown)
- [x] Persistenz: Laufende Events überstehen Server-Neustart; verpasste Events beim Start nachholen oder überspringen (konfigurierbar)
- [x] Testmodus: Datum/Uhrzeit simulieren (`/xmas event simulate <datum> <uhrzeit> [dry]`)
- [x] Integration in Admin-GUI (Abschnitt 6)
- [x] Weihnachtswoche als Beispiel-Config mit leeren Platzhalter-Events anlegen

---

## 8. Christmas-Twist (Platzhalter)

- [ ] Wartet auf separate Datei vom Owner
- [x] Erweiterungspunkte vorbereiten: `CrystalRegistry.register`, `Ability`/`AbilityRegistry`, `EventActionRegistry`, `EventService.addListener` (gilt auch für nicht-trockene Simulationen), Pack-Namespace `xmassmp`

---

## 9. Weitere Plattformen (nach Fabric)

- [ ] **Paper**: Plattform-Adapter, Ressourcenpack-Auslieferung über `server.properties`/API, Inventory-GUIs, Bukkit-Permissions, `paper-plugin.yml`
- [ ] **NeoForge**: Server-seitige Mod, Plattform-Adapter, gleiche Resource-Pack-Auslieferung
- [ ] Gleiches Verhalten auf allen Plattformen sicherstellen (gemeinsame Tests im `common`-Modul)

---

## 10. Qualitätssicherung

- [x] Unit-Tests für vorhandene Kernlogik (Event-Scheduler inkl. Zeitzonen/Datumsgrenzen, Testmodus, Pausen, Zeitsprünge, Config inkl. JSONC-Editor, Spielerdaten, Effekte, Stufen-Animationen, Cooldowns, Admin inkl. Admin-GUI, Pack-Abgleich)
- [x] Unit-Tests für Energie-Regeln und -Zugriff, Energy Bottle inkl. Exploits, Rezepte, Upgrader, Trader, Totem, Passive, Admin-Seite Gameplay – zusammen 269 Tests, alle grün
- [x] Lokaler Server-Test Fabric 1.21.11 ohne Spieler: Start ohne Fehler, Befehle per RCON, Totem übersteht TNT, Upgrader-Rezept im Crafter, gefälschte Flasche ergibt keine Splitter
- [ ] Spieler-Test der neuen Systeme: Abfüllen (Ladebalken-Position am Fadenkreuz), Trinken, Zerlegen, Händler-GUI, Upgrader, Totem-Ritual inkl. Abbau-Countdown, Client-Mod-Tasten, gelber Tab-Name, alle Animationen ansehen
- [ ] Testserver je Plattform und Version (1.21.11, 26.1, 26.2, 26.3 – alle veröffentlicht, siehe `docs/versionen.md`)
- [ ] Jede Kraft im Spiel testen und Video/GIF aufnehmen (Qualitäts-Check Animation/Partikel)
- [ ] Performance-Test mit vielen Spielern und gleichzeitigen Fähigkeiten
- [ ] Mehrspieler-Test: PvP-Energie, Anti-Farming, Trader, Admin-GUI
- [ ] Release-Checkliste: Changelog, Versionsnummer, Ressourcenpack-Hash, README aktuell
