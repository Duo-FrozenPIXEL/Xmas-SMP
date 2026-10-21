# Xmas-SMP – To-do-Liste

Abgeleitet aus `Instructions.md` und einem Abgleich mit dem Texturpack `BlissGemsV4.1-fixed.zip`.
Entscheidungen des Owners (Stand 2026-10-21) sind unter **0. Festgelegte Entscheidungen** festgehalten.

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
| Energie | 0–10, **Start 5**, Kill +1 / Tod −1 · **1–2 Energie: nur Primärkraft** · **0 Energie: alle Fähigkeiten aus** |
| Kräfte je Kristall | Passiv + Primär + Sekundär (Tier 1), **Ultimate ab Tier 2** |
| Gem Trader | Craftbar mit Originalrezept (4 Diamantblöcke, 4 Drachenatem, 1 Sculk-Katalysator), **GUI mit freier Kristallwahl** |
| Entfällt | Gem Fragments, All-for-One-Gem |
| Repair Kit | Platzierbares Totem, Radius 15, 10 min durchgehend im Kreis → Energie unter 5 wird auf 5 gesetzt; Koordinaten in Chat + Title; kein Schutzschild |
| Energy Bottle | Schleichen + Rechtsklick mit leerer Flasche ~5 s halten (Ladebalken am Fadenkreuz) → −1 Energie; Rechtsklick auf Flasche → +1 Energie (für jeden) |
| HUD / Steuerung | Actionbar mit Icons + Cooldowns; Tasten in den Einstellungen belegbar (optionaler Client-Mod + Vanilla-Fallback) |
| Erster Login | Jeder Spieler bekommt einen Kristall, auch Spätstarter |

Recherche und Abweichungen vom Original: `docs/bliss-original.md` (inkl. offener Fragen in Abschnitt 11).

### Kristall → Textur-Zuordnung

| Kristall | Original-Gem (Textur) | Farbe | HUD-Icons (Vorschlag) |
|---|---|---|---|
| **Feuer** | Strength | rot | `fire1` (Feuerkugel), `fire2` (Lagerfeuer) |
| **Erde** | Fire | orange | `strength1` (Kette), `strength2` (Arm/Kraft) |
| **Wind** | Puff | weiß | `puff1` (Dash-Pfeil), `puff2` (Faust/Stoß) |
| **Schatten** | Astra | lila | `astra1` (Dolch), `astra2` (Geist) |
| **Natur** | Wealth | grün | `wealth1` (Kleeblatt), `wealth2` (Ranke) |

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
- [ ] `repair_kit.json` nutzt Textur `revivalkit.png` (Verbandskasten); `repairitem.png` wird nicht genutzt (sieht nach UI-Atlas-Fragment aus) → klären/entfernen.
- [ ] `empty.png` (400×400, RGB, magenta) wird nirgends genutzt → entfernen oder als GUI-Hintergrund-Platzhalter dokumentieren.
- [x] `assets/minecraft/items/white_dye-working.txt` ist eine Arbeitsdatei mit abweichendem Format → entfernen.
- [ ] `strengthgemtier2pristine3/4.png` haben graue Randpixel links → optisch nachbessern
- [ ] `gem-breaking/*.png` (8 graue Riss-Overlays) werden nicht genutzt → als Basis für Bruch-Animation/Partikel oder für eingefärbte neue Texturen verwenden.
- [ ] Nicht benötigte Gems (Flux, Life, Speed) aus dem eigenen Pack entfernen oder für spätere Erweiterungen (Christmas-Twist) markieren.

### 1.3 Pack für 1.21.11 – 26.3 fit machen
- [x] `pack.mcmeta` auf das neue Format mit `min_format` / `max_format` umstellen (ab 1.21.9 Pflicht). 1.21.11 ≈ Format 75, 26.1 ≈ 84 **(V)** – exakte Werte für 26.3 im Minecraft-Wiki prüfen.
- [x] Beschreibung im `pack.mcmeta` auf „Xmas-SMP“ ändern (aktuell „BlissGems V4 (1.21.x)“); `pack.png` ggf. ersetzen.
- [ ] Umstieg von `custom_model_data` auf Basis-Items hin zur `item_model`-Komponente erwägen (je Kristall/Stufe eine eigene Item-Definition unter `assets/xmassmp/items/`) – sauberer und versionsstabiler.
- [ ] Namespace von `blissgems` auf eigenen Namespace (z. B. `xmassmp`) umziehen.
- [ ] Mit jeder Zielversion (1.21.11, 26.1, 26.2, 26.3) im Client laden und alle Modelle/Icons prüfen.
- [x] Pack-Validator-Script `tools/resourcepack.py` bauen (CI-Anbindung offen, siehe 3.1): alle Modell→Textur- und Item→Modell-Referenzen prüfen (die Fehler aus 1.1 wären damit sofort aufgefallen).

### 1.4 Fehlende Assets (neu zu erstellen)
- [ ] Texturen/Modelle für die Admin-GUI (Hintergrund über Custom-Font, Buttons/Icons)
- [ ] Icons für Event-System (Kalender, Uhr, Event-Status)
- [ ] Ggf. eigene Partikel-Texturen (falls Vanilla-Partikel nicht reichen; nur mit Client-Pack möglich)
- [ ] Platzhalter für Christmas-Twist-Items (folgt in separater Datei)
- [ ] Lokalisierung: `assets/xmassmp/lang/de_de.json` und `en_us.json` für alle Itemnamen/Texte

---

## 2. Recherche (Original-Bliss-Verhalten)

Ergebnisse: `docs/bliss-original.md`.

- [x] Energiesystem recherchieren: 0–10, Start 5, Kill +1 / Tod −1, Stufen Broken (0) … Pristine (5) … Pristine +5 (10)
- [x] Verhalten bei 0 Energie: Fähigkeiten aus, seit S3 kein Deathban, Rettung über Ritual
- [x] Tier-System: Upgrader (Rezept S3: Fragments + Wither-Schädel + Drachenatem), Tier 2 = mehr Kräfte
- [x] Gem Trader: Original würfelt zufällig um; Rezept S3 4 Diamantblöcke + 4 Drachenatem + 1 Sculk-Katalysator
- [x] Gem Fragments: Crafting-Material (Trader/Upgrader/Repair Kit) → entfällt
- [x] Energy Bottle: `/bliss withdraw` bzw. Drop bei Killer mit 10 Energie, Verfall nach 1 h
- [x] Repair-Kit / Restoration: Pedestal-Ritual mit Server-Pool bzw. 5 Flaschen für Broken-Gems
- [x] Aktivierung: Offhand = Passiv, Haupthand + Rechtsklick / Schleichen + Rechtsklick; Client-Mods für Keybinds; Actionbar mit Font-Icons
- [x] Startverhalten: zufälliger Gem beim ersten Join
- [x] Ergebnisse als `docs/bliss-original.md` dokumentiert, Abweichungen begründet
- [ ] Offene Fragen (Abschnitt 11 der Doku) mit dem Owner klären
- [ ] **(V)** Exakte Anordnung des Trader-Rezepts im Original (nur Zutaten bekannt) – ggf. im Bliss-Discord prüfen

---

## 3. Projekt-Setup & Architektur

### 3.1 Build-System
- [ ] Gradle-Multi-Projekt anlegen:
  - `common/` – plattformunabhängiger Kern (Kristalle, Energie, Level, Trader, Config, Event-Scheduler-Logik)
  - `fabric/` – **erste Plattform**
  - `paper/` – später
  - `neoforge/` – später
- [ ] Multiloader-Ansatz wählen: Architectury **oder** eigenes `common`-Modul mit Plattform-Interfaces
- [ ] Multi-Version-Builds 1.21.11 → 26.3 einrichten (z. B. **Stonecutter**) **(V)**
- [ ] Java-Versionen beachten: 1.21.11 = Java 21, 26.x = vermutlich Java 25 **(V)**
- [ ] Ab 26.1 ist Minecraft nicht mehr obfuskiert → Mappings/Loom-Setup je Version unterschiedlich **(V)**
- [ ] CI (GitHub Actions): Build + Tests für jede Zielversion/Plattform, Pack-Validator, Artefakte als Release
- [ ] `README.md` füllen (aktuell leer): Installation, Befehle, Permissions, Konfiguration, unterstützte Versionen

### 3.2 Fabric (Server-seitig, Vanilla-Clients)
- [ ] Server-seitige Mod, damit Spieler **keinen** Client-Mod brauchen
- [ ] **Polymer** (+ Polymer Resource Pack / AutoHost) für Custom Items und automatische Resource-Pack-Auslieferung
- [ ] **sgui** für Chest-GUIs (Admin-GUI, Gem Trader)
- [ ] **fabric-permissions-api** (LuckPerms-kompatibel) für Rechte
- [ ] Fabric API für Events (Join, Tod, Kill, Tick, Interaktion)

### 3.3 Kern-Abstraktionen (in `common`)
- [ ] `Crystal`-Registry (5 Kristalle: Feuer, Erde, Wind, Schatten, Natur), erweiterbar für Christmas-Twist
- [ ] `Ability`-Interface (Passive / Aktiv, Cooldown, Tier-Voraussetzung, Energie-Skalierung)
- [ ] `EffectEngine` / Animations-API: Partikel-Formen (Ring, Spirale, Kugel, Linie, Welle), Keyframes über mehrere Ticks, Display-Entities, Sounds
- [ ] `PlayerData` (Kristall, Tier, Energie, Cooldowns, Level) mit Persistenz (pro Spieler JSON/NBT, später ggf. DB)
- [ ] Plattform-Adapter: Partikel senden, Entities spawnen, Items bauen, GUIs öffnen, Befehle registrieren, Scheduler

---

## 4. Gameplay-Systeme (aus dem Original)

### 4.1 Kristall-Items
- [ ] Je Kristall: Tier 1 + Tier 2, je 4 Bruchstufen (Modelle/Texturen vorhanden, Zuordnung Energie → Modell in `docs/bliss-original.md` §1)
- [ ] Item-Lore: Kristallname, Tier, Energie, Fähigkeiten, Farbcodes passend zur Kristallfarbe
- [ ] Kristall ist seelengebunden: kann nicht gedroppt/in Kisten gelegt/verloren werden (konfigurierbar)
- [ ] Nur ein Kristall pro Spieler aktiv
- [ ] Erster Login → Kristall für jeden Spieler, auch Spätstarter (zufällig oder ausgeglichen, konfigurierbar)

### 4.2 Breaking-Pristine-System (Energie)
- [ ] Energie-Wert pro Spieler (0–10, Start 5)
- [ ] Kill eines Spielers → +Energie, eigener Tod → −Energie (konfigurierbar, Anti-Farming: gleicher Gegner Cooldown)
- [ ] Energie → Bruchstufe → Modell (4 sichtbare Stufen: zerbrochen, beschädigt, zerkratzt, makellos)
- [ ] Freischaltung nach Energie: 3–10 alles, 1–2 nur Primärkraft (+ Passiv), 0 alles aus
- [ ] **Bruch-Animation** beim Stufenwechsel: Riss-Partikel (Overlay-Texturen `gem-breaking/*`), Glas-Sound, Item-Shards in Kristallfarbe
- [ ] **Reparatur-/Aufwertungs-Animation** beim Aufstieg
- [ ] Energy Bottle: Schleichen + Rechtsklick mit leerer Flasche ~5 s halten (Ladebalken als Title am Fadenkreuz) → −1 Energie; Rechtsklick auf die Flasche → +1 Energie
- [ ] Repair-Kit-Totem: platzieren, Radius 15 mit Partikelring, Koordinaten in Chat + Title, 10 min durchgehend im Kreis → Energie < 5 wird 5, Totem zerbricht am Ende; kein Schutzschild
- [ ] Revival-Tome: entfällt (Repair Kit deckt 0 Energie ab), Modell für Christmas-Twist reservieren

### 4.3 Level-/Tier-System
- [ ] Tier 1 → Tier 2 über **Upgrader**-Item (Rezept ohne Gem Fragments, Vorschlag in `docs/bliss-original.md` §2)
- [ ] Tier 2 schaltet die **Ultimative Attacke** (und ggf. stärkere Passive) frei
- [ ] Upgrade-Animation (Partikelsäule, Lichtblitz, Sound)
- [ ] Optional: Gem-Level/XP innerhalb eines Tiers (klären, ob gewünscht – „gem auf level system“)

### 4.4 Gem Trader
- [ ] Gem-Trader-Item (Modell `gem_trader`): Kristall gegen anderen tauschen
- [ ] Rezept wie Original S3: 4 Diamantblöcke + 4 Drachenatem + 1 Sculk-Katalysator
- [ ] GUI mit **freier Wahl** des neuen Kristalls, Tier/Energie bleiben erhalten (konfigurierbar), Tausch-Cooldown gegen „Cycling“
- [ ] Tausch-Animation (Kristall verschwindet in Partikelwirbel, neuer erscheint in neuer Farbe)
- [ ] Optional: platzierbare Trader-Station (Modell `trader`)

### 4.5 Fähigkeits-Steuerung & HUD
- [ ] Optionaler Client-Companion-Mod mit in den Einstellungen belegbaren Tasten (Primär / Sekundär / Ultimate), Custom-Payload an den Server (Fabric, NeoForge, Paper)
- [ ] Vanilla-Fallback ohne Mod: Rechtsklick = Primär, Schleichen + Rechtsklick = Sekundär, Schleichen + `F` = Ultimate
- [ ] Actionbar-HUD mit Font-Icons (``–``) + Cooldown-Anzeige in Sekunden + Energie
- [ ] Je Kristall ein drittes HUD-Icon für die Ultimate zeichnen (Pack hat nur 2 pro Gem)
- [ ] Cooldown-Ende: kleiner Sound + Partikel-Puls

---

## 5. Kräfte (Entwurf) – jede Kraft hochwertig animiert

Struktur je Kristall: Passiv · Primär · Sekundär (bei 1–2 Energie gesperrt) · Ultimate (nur Tier 2).

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
- [ ] Primär – **Flammenstoß**: Sprung, bei Landung expandierender Feuerring (`flame`, `soul_fire_flame`-Akzent, `lava`, Rauch), Schaden + Brand
- [ ] Sekundär – **Meteor**: Glühender Block-Display fällt aus dem Himmel mit Rauch-/Funkenspur, Einschlag mit Schockwelle und Glut-Partikeln
- [ ] Ultimate (T2) – *noch zu entwerfen*

### 5.2 Erde (orange, Fire-Textur)
- [ ] Passiv T1: Kein Rückstoß / Resistenz I
- [ ] Passiv T2: Eile beim Abbauen von Stein/Erze
- [ ] Primär – **Erdbeben**: Wellenförmige Schockwelle, Block-Displays springen aus dem Boden (`block`-Partikel des Untergrunds, `falling_dust`), Gegner werden hochgeworfen + verlangsamt
- [ ] Sekundär – **Steinwall/Felsbrocken**: Temporärer Wall aus Block-Displays oder geworfener Felsbrocken mit Trümmer-Partikeln
- [ ] Ultimate (T2) – *noch zu entwerfen*

### 5.3 Wind (weiß, Puff-Textur)
- [ ] Passiv T1: Kein Fallschaden
- [ ] Passiv T2: Doppelsprung
- [ ] Primär – **Böe**: Dash nach vorne, Gegner im Kegel werden weggestoßen (`gust`, `cloud`, spiralförmige weiße `dust`-Linien)
- [ ] Sekundär – **Tornado**: Wandernder Wirbel, zieht Gegner hoch (mehrlagige Partikelspirale, `sweep_attack`, Wind-Sounds)
- [ ] Ultimate (T2) – *noch zu entwerfen*

### 5.4 Schatten (lila, Astra-Textur)
- [ ] Passiv T1: Nachtsicht
- [ ] Passiv T2: In Dunkelheit beim Schleichen unsichtbar / Lebensraub nachts
- [ ] Primär – **Schattenschritt**: Teleport hinter das anvisierte Ziel (`portal`, `squid_ink`, `reverse_portal`, lila Farbverlauf), kurze Spur zwischen Start und Ziel
- [ ] Sekundär – **Seelenbann**: Dunkle Aura, Blindheit/Dunkelheit für Gegner (`sculk_soul`, `soul`, kreisende Geister-Partikel)
- [ ] Ultimate (T2) – *noch zu entwerfen*

### 5.5 Natur (grün, Wealth-Textur)
- [ ] Passiv T1: Regeneration auf Gras/Laub
- [ ] Passiv T2: Bonus-Ernte / Glück
- [ ] Primär – **Rankenfessel**: Wurzeln/Ranken (Block-Displays) wachsen um das Ziel und halten es fest (`happy_villager`, `composter`, `spore_blossom_air`)
- [ ] Sekundär – **Blütenheilung**: Heilkreis für Verbündete (`cherry_leaves`, grüne `dust`-Spiralen, aufsteigende Blüten)
- [ ] Ultimate (T2) – *noch zu entwerfen*

- [ ] Entwurf mit dem Owner abstimmen, bevor implementiert wird

---

## 6. Admin-/Moderationssystem

- [ ] Befehl `/xmas admin` (Permission `xmassmp.admin`, Fallback OP-Level 2) öffnet Konfigurations-GUI
- [ ] GUI-Seiten:
  - [ ] **Allgemein**: Plugin an/aus, Fähigkeiten global an/aus, PvP-Energie an/aus, Reload
  - [ ] **Kristalle**: je Kristall aktivieren/deaktivieren, Cooldown-/Schadens-Multiplikator
  - [ ] **Energie**: Start-, Min-, Max-Energie, Gewinn/Verlust pro Kill/Tod
  - [ ] **Trader/Upgrader**: aktiv, Rezepte, Kosten
  - [ ] **Spieler**: Online-Spieler wählen → Kristall setzen, Tier setzen, Energie setzen, Cooldowns zurücksetzen
  - [ ] **Events**: geplante Events ansehen, aktivieren/deaktivieren, manuell starten/stoppen, Testmodus
  - [ ] **Items**: alle Custom-Items zum Herausnehmen (Kristalle, Trader, Upgrader, Energy Bottle, Repair-Kit, Fragments)
- [ ] Änderungen aus der GUI sofort speichern (Config-Datei) und live anwenden
- [ ] Zusätzliche Text-Befehle (für Konsole/Automatisierung): `/xmas give`, `/xmas set <spieler> crystal|tier|energy`, `/xmas reload`, `/xmas event start|stop|list`
- [ ] Tab-Completion für alle Befehle
- [ ] Audit-Log: Admin-Änderungen protokollieren (wer, was, wann)
- [ ] Konfigurationsdatei dokumentiert (Kommentare/README)

---

## 7. Event-System (Grundlage für die Weihnachtswoche)

- [ ] Event-Scheduler: Events an **bestimmten Tagen zu bestimmten Uhrzeiten** ausführen
- [ ] Zeitzone konfigurierbar (Standard `Europe/Berlin`)
- [ ] Event-Definition in Config: ID, Name, Datum/Zeitraum (z. B. 24.12.–31.12.), Uhrzeit(en), Dauer, Wiederholung, Aktionen
- [ ] Aktionstypen (erweiterbar):
  - [ ] Broadcast/Titel/Sound an alle
  - [ ] Befehl ausführen
  - [ ] Globale Modifikatoren (z. B. doppelte Energie, verkürzte Cooldowns)
  - [ ] Hook-Interface für spätere Christmas-Twist-Events
- [ ] Ankündigungen vor Eventstart (z. B. 10 min, 1 min, Countdown)
- [ ] Persistenz: Laufende Events überstehen Server-Neustart; verpasste Events beim Start nachholen oder überspringen (konfigurierbar)
- [ ] Testmodus: Datum/Uhrzeit simulieren (`/xmas event simulate <datum> <uhrzeit>`)
- [ ] Integration in Admin-GUI (Abschnitt 6)
- [ ] Weihnachtswoche als Beispiel-Config mit leeren Platzhalter-Events anlegen

---

## 8. Christmas-Twist (Platzhalter)

- [ ] Wartet auf separate Datei vom Owner
- [ ] Erweiterungspunkte vorbereiten: Crystal-Registry, Ability-Interface, Event-Aktionen, Pack-Namespace für neue Texturen

---

## 9. Weitere Plattformen (nach Fabric)

- [ ] **Paper**: Plattform-Adapter, Ressourcenpack-Auslieferung über `server.properties`/API, Inventory-GUIs, Bukkit-Permissions, `paper-plugin.yml`
- [ ] **NeoForge**: Server-seitige Mod, Plattform-Adapter, gleiche Resource-Pack-Auslieferung
- [ ] Gleiches Verhalten auf allen Plattformen sicherstellen (gemeinsame Tests im `common`-Modul)

---

## 10. Qualitätssicherung

- [ ] Unit-Tests für Kernlogik (Energie, Tier, Trader, Event-Scheduler inkl. Zeitzonen/Datumsgrenzen)
- [ ] Testserver je Plattform und Version (1.21.11, 26.1, 26.2, 26.3) – **(V)** welche 26.x-Versionen tatsächlich existieren
- [ ] Jede Kraft im Spiel testen und Video/GIF aufnehmen (Qualitäts-Check Animation/Partikel)
- [ ] Performance-Test mit vielen Spielern und gleichzeitigen Fähigkeiten
- [ ] Mehrspieler-Test: PvP-Energie, Anti-Farming, Trader, Admin-GUI
- [ ] Release-Checkliste: Changelog, Versionsnummer, Ressourcenpack-Hash, README aktuell
