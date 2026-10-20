# Xmas-SMP – To-do-Liste

Abgeleitet aus `Instructions.md` und einem Abgleich mit dem Texturpack `BlissGemsV4.1-fixed.zip`.
Entscheidungen des Owners (Stand 2026-10-20) sind unter **0. Festgelegte Entscheidungen** festgehalten.

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

### Kristall → Textur-Zuordnung

| Kristall | Original-Gem (Textur) | Farbe | HUD-Icons (Vorschlag) |
|---|---|---|---|
| **Feuer** | Strength | rot | `fire1` (Feuerkugel), `fire2` (Lagerfeuer) |
| **Erde** | Fire | orange | `strength1` (Kette), `strength2` (Arm/Kraft) |
| **Wind** | Puff | weiß | `puff1` (Dash-Pfeil), `puff2` (Faust/Stoß) |
| **Schatten** | Astra | lila | `astra1` (Dolch), `astra2` (Geist) |
| **Natur** | Wealth | grün | `wealth1` (Kleeblatt), `wealth2` (Ranke) |

- [x] `Instructions.md` an die neue Zuordnung anpassen (dort steht noch „Feuer = Fire gem texturen“) – **nur nach Freigabe durch den Owner**
- [ ] Entscheiden, ob die Icons wie oben „überkreuz“ verwendet werden (Feuer bekommt die Feuer-Icons, obwohl es die Strength-Gem-Textur nutzt) oder ob neue Icons gezeichnet werden

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

## 2. Recherche (Original-Bliss-Verhalten) **(V)**

Ziel laut Instructions: „schau dir an wie das Plugin normalerweise funktioniert (Foren)“.

- [ ] Energiesystem recherchieren: Wertebereich (meist 0–10), Gewinn bei Kill / Verlust bei Tod, Namen der Stufen (Broken … Pristine +X)
- [ ] Verhalten bei 0 Energie (Gem zerbricht? Fähigkeiten aus? Bann/Revive?)
- [ ] Tier-System: Wie wird Tier 2 erreicht (Upgrader), was schaltet Tier 2 frei?
- [ ] Gem Trader: Gem gegen zufälligen/gewählten anderen Gem tauschen? Kosten? Trader-Token? Shrine/Station?
- [ ] Gem Fragments: Wofür (Crafting des Traders/Upgraders/Repair)?
- [ ] Energy Bottle: Energie abzapfen / übertragen
- [ ] Repair-Kit / Revival-Tome: Gem reparieren, gebannte/tote Spieler wiederbeleben
- [ ] Aktivierung der Fähigkeiten im Original (Tasten, Offhand, Klick-Kombinationen), Cooldown-Anzeige (Actionbar mit Font-Icons ``–``)
- [ ] Startverhalten: Bekommt jeder Spieler beim ersten Join einen zufälligen Gem?
- [ ] Ergebnisse als `docs/bliss-original.md` dokumentieren und Abweichungen begründen

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
- [ ] Je Kristall: Tier 1 + Tier 2, je 4 Bruchstufen (Modelle/Texturen vorhanden, siehe 1.2)
- [ ] Item-Lore: Kristallname, Tier, Energie, Fähigkeiten, Farbcodes passend zur Kristallfarbe
- [ ] Kristall ist seelengebunden: kann nicht gedroppt/in Kisten gelegt/verloren werden (konfigurierbar)
- [ ] Nur ein Kristall pro Spieler aktiv
- [ ] Erster Join → zufälliger Kristall (konfigurierbar)

### 4.2 Breaking-Pristine-System (Energie)
- [ ] Energie-Wert pro Spieler (Standard 0–10 **(V)**)
- [ ] Kill eines Spielers → +Energie, eigener Tod → −Energie (konfigurierbar, Anti-Farming: gleicher Gegner Cooldown)
- [ ] Energie → Bruchstufe → Modell (4 sichtbare Stufen: zerbrochen, beschädigt, zerkratzt, makellos)
- [ ] Energie skaliert Fähigkeitsstärke (Schaden, Dauer, Radius) und schaltet bei 0 Fähigkeiten ab
- [ ] **Bruch-Animation** beim Stufenwechsel: Riss-Partikel (Overlay-Texturen `gem-breaking/*`), Glas-Sound, Item-Shards in Kristallfarbe
- [ ] **Reparatur-/Aufwertungs-Animation** beim Aufstieg
- [ ] Energy Bottle: Energie abfüllen (Rechtsklick/Befehl) und an andere übertragen
- [ ] Repair-Kit: Kristall-Energie wiederherstellen (Rezept + Effekt)
- [ ] Revival-Tome: Verhalten bei 0 Energie (Revive-Mechanik laut Recherche) **(V)**

### 4.3 Level-/Tier-System
- [ ] Tier 1 → Tier 2 über **Upgrader**-Item (Rezept, ggf. aus Gem Fragments)
- [ ] Tier 2 schaltet zweite aktive Fähigkeit und stärkere Passive frei
- [ ] Upgrade-Animation (Partikelsäule, Lichtblitz, Sound)
- [ ] Optional: Gem-Level/XP innerhalb eines Tiers (klären, ob gewünscht – „gem auf level system“)

### 4.4 Gem Trader
- [ ] Gem-Trader-Item (Modell `gem_trader`): Kristall gegen anderen tauschen
- [ ] GUI zur Auswahl (oder zufällig, konfigurierbar), Tier/Energie bleiben erhalten (konfigurierbar)
- [ ] Tausch-Animation (Kristall verschwindet in Partikelwirbel, neuer erscheint in neuer Farbe)
- [ ] Gem Fragments als Crafting-Material (Rezepte definieren)
- [ ] Optional: platzierbare Trader-Station (Modell `trader`)

### 4.5 Fähigkeits-Steuerung & HUD
- [ ] Aktivierung festlegen (z. B. Offhand-Swap-Taste `F` = Fähigkeit 1, Schleichen + `F` = Fähigkeit 2) – funktioniert ohne Client-Mod
- [ ] Actionbar-HUD mit Font-Icons (``–``) + Cooldown-Anzeige in Sekunden
- [ ] Cooldown-Ende: kleiner Sound + Partikel-Puls

---

## 5. Kräfte (Entwurf) – jede Kraft hochwertig animiert

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
- [ ] Aktiv 2 (T2) – **Meteor**: Glühender Block-Display fällt aus dem Himmel mit Rauch-/Funkenspur, Einschlag mit Schockwelle und Glut-Partikeln

### 5.2 Erde (orange, Fire-Textur)
- [ ] Passiv T1: Kein Rückstoß / Resistenz I
- [ ] Passiv T2: Eile beim Abbauen von Stein/Erze
- [ ] Aktiv 1 – **Erdbeben**: Wellenförmige Schockwelle, Block-Displays springen aus dem Boden (`block`-Partikel des Untergrunds, `falling_dust`), Gegner werden hochgeworfen + verlangsamt
- [ ] Aktiv 2 (T2) – **Steinwall/Felsbrocken**: Temporärer Wall aus Block-Displays oder geworfener Felsbrocken mit Trümmer-Partikeln

### 5.3 Wind (weiß, Puff-Textur)
- [ ] Passiv T1: Kein Fallschaden
- [ ] Passiv T2: Doppelsprung
- [ ] Aktiv 1 – **Böe**: Dash nach vorne, Gegner im Kegel werden weggestoßen (`gust`, `cloud`, spiralförmige weiße `dust`-Linien)
- [ ] Aktiv 2 (T2) – **Tornado**: Wandernder Wirbel, zieht Gegner hoch (mehrlagige Partikelspirale, `sweep_attack`, Wind-Sounds)

### 5.4 Schatten (lila, Astra-Textur)
- [ ] Passiv T1: Nachtsicht
- [ ] Passiv T2: In Dunkelheit beim Schleichen unsichtbar / Lebensraub nachts
- [ ] Aktiv 1 – **Schattenschritt**: Teleport hinter das anvisierte Ziel (`portal`, `squid_ink`, `reverse_portal`, lila Farbverlauf), kurze Spur zwischen Start und Ziel
- [ ] Aktiv 2 (T2) – **Seelenbann**: Dunkle Aura, Blindheit/Dunkelheit für Gegner (`sculk_soul`, `soul`, kreisende Geister-Partikel)

### 5.5 Natur (grün, Wealth-Textur)
- [ ] Passiv T1: Regeneration auf Gras/Laub
- [ ] Passiv T2: Bonus-Ernte / Glück
- [ ] Aktiv 1 – **Rankenfessel**: Wurzeln/Ranken (Block-Displays) wachsen um das Ziel und halten es fest (`happy_villager`, `composter`, `spore_blossom_air`)
- [ ] Aktiv 2 (T2) – **Blütenheilung**: Heilkreis für Verbündete (`cherry_leaves`, grüne `dust`-Spiralen, aufsteigende Blüten)

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
