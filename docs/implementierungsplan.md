# Xmas-SMP – Implementierungsplan

Stand: 2026-10-30. Grundlage sind die Antworten des Owners aus dem Chat (Recherche-Runde nach `docs/bliss-original.md`) und der Code-Stand in diesem Ordner.
Dieser Plan legt fest, **was** in **welcher Reihenfolge** gebaut wird. Die offenen Punkte in `todo.md` verweisen hierher.

Legende: **E** = vom Owner entschieden · **V** = Vorschlag, wartet auf Bestätigung (Abschnitt 2) · **(V)** = vor Umsetzung im Spiel/in der Quelle prüfen

> **Stand der Quellen:** Der Branch `claude/relaxed-clarke-a5xdc1` hat nur Doku geändert (Recherche mit Owner-Entscheidungen). Seine Inhalte – Rezept-Anordnungen, Exploit-Design, Config-Werte – sind in diesen Plan und in den Code eingeflossen; der Branch muss nicht mehr gemergt werden.

---

## 1. Festgelegte Regeln (Entscheidungen des Owners)

### 1.1 Energie
| Regel | Status |
|---|---|
| Skala 0–10, Start **5** | E |
| 11 Zustände mit Original-Namen: Broken (0), Ruined (1), Damaged (2), Cracked (3), Scratched (4), Pristine (5), Pristine +1 … +5 (6–10) | E (Namen aus dem Original) |
| **0 Energie:** keine aktiven Kräfte | E |
| **1–2 Energie:** nur die Primärkraft. Sekundärkraft **und** Ultimate fallen weg | E |
| **≥ 3 Energie:** Primär + Sekundär, mit Tier 2 zusätzlich die Ultimate | E |
| **Passive wirken immer**, auch bei 0 Energie, und **egal wo** der Kristall im Inventar liegt | E |
| Tod: −1 Energie. Kill: +1 für den Killer | E |
| Opfer hat 0 Energie → Opfer verliert nichts, **Killer bekommt nichts** (kein Farmen mit Zweitaccounts) | E |
| Killer mit 0 Energie bekommt trotzdem +1, wenn das Opfer Energie verliert | E |
| Energy Bottle darf auch bei 0 Energie getrunken werden | E |
| Jeder Spieler bekommt beim ersten Login einen Kristall, auch wenn er später einsteigt | E (bereits umgesetzt: `crystalItem.giveOnJoin`) |

### 1.2 Tier-System
| Regel | Status |
|---|---|
| Je Kristall drei aktive Kräfte: **Primär**, **Sekundär**, **Ultimate**. Die bisherige „Aktiv 2“ im Kräfte-Entwurf wird zur Sekundärkraft | E |
| Tier 1: Primär + Sekundär. **Tier 2 schaltet die Ultimate frei** | E |
| Upgrader hebt Tier 1 → Tier 2 | E |

### 1.3 Items und Rezepte
| Item | Rezept | Status |
|---|---|---|
| **Gem Fragment** | 1 Energy Bottle → 9 Fragments, formlos, im 2×2-Inventar oder an der Werkbank. Das ist die **einzige** Quelle für Fragments | E |
| **Upgrader** | 4 Fragments, 4 Drachenatem, 1 Witherskelettschädel (wie im Original) | E (Anordnung: V) |
| **Gem Trader** | 4 Diamantblöcke, 4 Drachenatem, **1 Fragment in der Mitte** (statt Sculk-Katalysator) | E – Kosten noch offen, siehe 2 |
| **Repair Kit** | 4 Fragments, 2 Ambosse, **2 Diamantblöcke** (statt Netherit), 1 Netherit-Schmiedevorlage | E (Anordnung: V) |
| Revival Tome | **entfällt** (das Repair Kit holt auch Spieler mit 0 Energie zurück) | E |
| All-for-One-Gem | **entfällt** | E |

Grundsatz: **Trader sollen günstig sein**, damit der Spielspaß bleibt.

### 1.4 Gem Trader
- **Freie Wahl** des neuen Kristalls über eine GUI (kein Zufall). Der Trader wird gecraftet und beim Tausch verbraucht. **E**

### 1.5 Energy Bottle
- Leere Glasflasche in der Hand, **Schleichen + Rechtsklick gedrückt halten**. Nach etwa **5 Sekunden** wird daraus eine Energy Bottle, der Spieler verliert dabei 1 Energie. Während des Haltens erscheint ein **Ladebalken mittig am Fadenkreuz**. **E**
- **Rechtsklick auf die Energy Bottle** gibt 1 Energie zurück, entweder dem Ersteller selbst oder einem anderen Spieler, der die Flasche benutzt. **E**
- Der Energietransfer muss **exploit-sicher** sein (siehe Phase 3). **E**

### 1.6 Repair Kit (Totem)
| Regel | Status |
|---|---|
| Wird auf den Boden gestellt und ist dann ein **Totem** | E |
| Radius **15 Blöcke**, der Rand ist mit Partikeln sichtbar | E |
| Der Kreis ist **kein Schutzschild**, jeder kann hineinlaufen | E |
| Laufzeit **10 Minuten**. Danach zerbricht das Totem, **erst dann** bekommen die Spieler im Kreis ihre Energie | E |
| Wirkung: Energie **unter 5 wird auf 5 gesetzt** (auch von 0). Wer 5 oder mehr hat, bekommt nichts | E |
| Beliebig viele Spieler | E |
| Wer den Kreis verlässt, **stirbt oder ausloggt**, bekommt nichts (und kann bei diesem Totem nicht mehr gewinnen) | E |
| Die **Koordinaten werden geleakt**: im Chat und als Titel an alle | E |
| **Global läuft höchstens ein Totem** | E |
| Das Totem kann zerstört werden: **fest 10 Sekunden** ab dem ersten Schlag, egal mit welchem Werkzeug, Effizienz oder Eile. TNT, Kolben und Feuer wirken nicht | E |
| Beim Abbauen startet ein **Countdown mit Sound und Partikeln** (Details in Phase 6) | E |

### 1.7 Steuerung und HUD
- HUD mit Icons in der Actionbar (vorhanden). **E**
- **In den Minecraft-Einstellungen frei belegbare Tasten** über einen kleinen **optionalen Client-Mod**. Ohne Mod funktionieren weiterhin Tastenkombinationen. Der Server prüft Cooldown, Energie und Tier selbst; der Mod meldet nur, welche Taste gedrückt wurde. **E**

---

## 2. Fragen an den Owner

**Am 2026-10-30 hat der Owner alle Vorschläge dieser Tabelle übernommen** („ok, mach“). Sie sind umgesetzt und über die Config änderbar. Zusätzlich übernommen aus dem Recherche-Branch: Sieger mit voller Energie → Energie des Opfers fällt als Flasche zu Boden (`energy.dropBottleWhenKillerAtMax`).

| # | Frage | Vorschlag | Betrifft |
|---|---|---|---|
| 1 | Kann der Totem-Countdown aufgehalten werden? | Er wird zurückgesetzt, wenn der Angreifer länger als 1 s nicht schlägt, stirbt oder sich mehr als 5 Blöcke entfernt. So können Verteidiger ihn stoppen | Phase 6 |
| 2 | Wie günstig soll der Trader sein? | 4 **Diamanten** statt 4 Diamantblöcke. Drachenatem bleibt (man muss ins End) oder wird durch etwas Leichteres ersetzt | Phase 2 |
| 3 | Ohne Mod: welche Taste für die Ultimate? Im Chat war „Schleichen + F“ für die Ultimate vorgesehen, das belegt aber schon die Sekundärkraft | F = Primär, Schleichen + F = Sekundär, **Kristall in der Haupthand + Schleichen + Rechtsklick = Ultimate** | Phase 7 |
| 4 | Ab wann zählt man beim Totem als Teilnehmer? Die volle Laufzeit schafft nur, wer von Anfang an im Kreis steht | Teilnehmer ist, wer in den ersten **30 Sekunden** nach dem Aufstellen im Kreis ist und dann bis zum Ende bleibt | Phase 6 |
| 5 | Was passiert mit dem Repair Kit, wenn das Totem zerstört wird? | Es ist verloren, niemand bekommt Energie | Phase 6 |
| 6 | Darf man Energie bis auf 0 abfüllen? Sonst kann man vor einem Kampf alle Energie in Flaschen in die Endertruhe legen. Der Killer bekommt dann nichts (Regel „Opfer hat 0“) | Abfüllen nur, solange danach **mindestens 1** Energie bleibt, und **nicht im Kampf** (15 s nach Schaden durch einen Spieler) | Phase 3 |
| 7 | Trinken bei 10 Energie (Maximum)? | Gesperrt, die Flasche bleibt erhalten | Phase 3 |
| 8 | Läuft eine Energy Bottle ab (Original: nach 1 Stunde)? | Nein, kein Ablauf | Phase 3 |
| 9 | Behalten Tier und Energie beim Trader-Tausch ihren Wert? | Ja, beides bleibt | Phase 5 |
| 10 | Tod ohne Spieler (Mob, Sturz, Lava) kostet weiterhin 1 Energie? | Ja (so ist es jetzt) | Phase 1 |
| 11 | Anti-Farming (30 Min. kein Gewinn gegen dasselbe Opfer) behalten? | Ja | Phase 1 |
| 12 | Fällt man bei 0 Energie auf Tier 1 zurück (Original)? | Nein, Tier bleibt | Phase 1 |
| 13 | Tier 2: Glowing und gelber Name in der Tab-Liste (Original)? | Nur der gelbe Tab-Name, kein Glowing (verrät sonst jede Position) | Phase 4 |
| 14 | Kräfte-Entwurf (todo Abschnitt 5) und die noch zu entwerfenden **Ultimates** | Entwurf der 5 Ultimates folgt in Phase 8, Freigabe vor dem Bau | Phase 8 |

---

## 3. Phasen

Reihenfolge nach Abhängigkeiten. Jede Phase endet mit grünen Kern-Tests und, sobald Phase 0 geschafft ist, einem kurzen Test auf dem Testserver.

### Phase 0 – Grundlagen klären (vor allem anderen)
1. Die zwei Stände zusammenführen (Hinweis oben) und den Ordner wieder als Git-Checkout nutzen.
2. **Fabric-Mod einmal auf einem Testserver (1.21.11) starten.** Bisher lief nichts im Spiel. Prüfen: `/xmas`-Befehle, Kristall beim ersten Join, Seelenbindung, HUD, Admin-GUI, Bruch- und Reparatur-Animation, Event-Simulation. Gefundene Fehler in `bug.md`.
3. **Item-Architektur entscheiden** (bug.md Nr. 23): Vorschlag **Polymer-Server-Items** (`PolymerItem`) statt Vanilla-Items mit `custom_model_data`. Eigene Items haben keine Vanilla-Funktion (kein Brauen mit der Ghast-Träne, kein Leuchtfeuer aus dem Repair Kit, kein Smaragd-Handel). Das spart viele Mixins und ist Voraussetzung für saubere Rezepte und den Flaschen-Schutz. Die Darstellung beim Client bleibt über das Pack gleich. Für Paper später: eigene PDC-Kennung und Listener für Crafting, Brauen, Platzieren und Handel.

### Phase 1 – Energieregeln umbauen (Kern, ohne neue Items)
- `PristineStage`: 11 Zustände mit Namen (Broken … Pristine +5) für Lore und HUD. Die **4 Modelle** bleiben: 0 = zerbrochen, 1–2 = beschädigt, 3–4 = zerkratzt, 5–10 = makellos. Bei 10 bekommt das Item einen Verzauberungsglanz.
- Config `energy.stages` auf die neuen Schwellen umstellen (`damaged 1`, `scratched 3`, `pristine 5`) und Start-Spieler damit als „Pristine“ führen. `bliss-original.md` Abweichung „Start 5 = zerkratzt“ ist damit erledigt.
- Neue Zugriffsregel im Kern, z. B. `AbilityAccess`: Slot PRIMARY ab 1 Energie, SECONDARY ab 3, ULTIMATE ab 3 **und** Tier 2. Schwellen in der Config (`energy.access.secondary`, `energy.access.ultimate`). `AbilityService` meldet ein neues Ergebnis `LOW_ENERGY` (HUD zeigt dann „zu wenig Energie“ statt „gesperrt“).
- `ActiveAbility.slot()` auf drei Slots (1 = Primär, 2 = Sekundär, 3 = Ultimate) erweitern; `HudService` zeigt drei Icons.
- Passive: neuer Tick-Dienst `PassiveService`, der für jeden Online-Spieler mit Kristall die Passiven anwendet, **unabhängig von Energie und Inventarplatz** (die Spielerdaten sind die Wahrheit, das Item ist nur Anzeige).
- `EnergyService.onDeath`: Killer bekommt nur dann Energie, wenn das Opfer tatsächlich etwas verloren hat (`lost != 0`). Killer mit 0 Energie bekommt weiterhin +1.
- **Energie-Protokoll** (`EnergyLedger`): jede Änderung mit Spieler, alt → neu, Grund (`kill`, `death`, `bottle_fill`, `bottle_drink`, `totem`, `admin`) und ggf. Flaschen-ID. Datei `<welt>/xmassmp/energy.log`. Wird in Phase 3 und 6 genutzt.
- Tests: Zugriff je Energie/Tier, Kill eines 0-Energie-Opfers, Killer bei 0, Passive bei 0, Stufen-Zuordnung, Protokoll.

### Phase 2 – Items und Rezepte
- `CustomItem`: Revival Tome entfernen (Pack-Einträge bleiben, Validator anpassen). Energy Bottle, Fragment, Upgrader, Trader und Repair Kit als Polymer-Items (Phase 0).
- **Nicht stapelbar**: Energy Bottle, Upgrader, Trader, Repair Kit. Fragments stapeln normal.
- Rezepte als Daten, nicht im Code: `recipes` in `config.jsonc` (Form und Zutaten je Item), damit Kosten ohne Neubau geändert werden können (Admin-GUI, Phase 9). Standardwerte siehe 1.3.
- Rezept **Energy Bottle → 9 Fragments** als eigener Server-Rezepttyp, der die Flasche über das Register prüft (Phase 3). Bis Phase 3 fertig ist, bleibt das Rezept aus.
- Lore für alle Items (Wirkung kurz erklärt), `de_de.json`/`en_us.json` ergänzen; der Sprachtest prüft die Schlüssel.
- Tests: Rezept-Parser, Standardrezepte, Sprachschlüssel. **(V)** im Spiel: Rezeptbuch und Shift-Klick-Crafting auf Vanilla-Clients.

### Phase 3 – Energy Bottle mit Exploit-Schutz
**Abfüllen**
- Auslöser: Schleichen + Rechtsklick mit **leerer Glasflasche**. Das normale Füllen mit Wasser ist dabei abgeschaltet.
- Gehaltener Rechtsklick: Der Client schickt die Nutzung wiederholt (etwa alle 4 Ticks). Der Server zählt **eigene Ticks**; bleiben die Pakete länger als ~6 Ticks aus, gilt die Taste als losgelassen und der Vorgang bricht ab. Makros und Speed-Hacks verkürzen die 5 Sekunden nicht.
- Ladebalken als **Titel/Untertitel mittig am Fadenkreuz** (Font-Zeichen in Kristallfarbe), dazu ein leiser, steigender Ton und Partikel, die von der Brust in die Flasche fließen. Abbruch, wenn der Spieler die Flasche wechselt, sich weiter als 0,5 Blöcke bewegt, Schaden nimmt oder das Schleichen beendet.
- Am Ende **im selben Tick**: Bedingungen erneut prüfen (Energie, Flasche noch in der Hand, Frage 6), dann Energie −1, eine Glasflasche weg, neue Energy Bottle mit **eindeutiger ID** in die Hand.

**Trinken / Übertragen**
- Rechtsklick mit der Energy Bottle: +1 Energie für den Benutzer (auch bei 0). Die Flasche verschwindet, eine leere Glasflasche bleibt.
- Animation: Kristallfarbener Wirbel steigt auf, Amethyst-Klang, kurzer Lichtring.

**Schutz gegen Exploits**
- **Flaschen-Register** `<welt>/xmassmp/bottles.json`: ID → Zustand (gültig/verbraucht), Ersteller, Zeitpunkt. Trinken und Zerlegen prüfen den Eintrag und schließen ihn **in einem Schritt** auf dem Server-Thread.
- Kopien (Dupe-Glitches, Creative-Mittelklick) tragen dieselbe ID: nur die erste Nutzung wirkt. Gefälschte Flaschen (NBT-Editor) haben keinen Eintrag und wirken nie. Ungültige Flaschen werden entfernt und im Protokoll für Admins vermerkt.
- **Zerlegen in Fragments**: Auch bei Shift-Klick wird jede Flasche einzeln geprüft und verbraucht. Fragments lassen sich nicht zurück in Energie verwandeln, deshalb sind kopierte Fragments kein Energie-Exploit.
- Keine Vanilla-Verwendung (Phase 0): kein Brauen, kein Amboss-Umbenennen in ein anderes Item, kein Werfer-Trick.
- `/xmas energy audit [spieler]` zeigt das Energie-Protokoll; `/xmas energy bottles` listet offene Flaschen.
- Tests: Register (doppelte ID, unbekannte ID, Zerlegen), Abfüll-Zähler mit Lücken, Abbruchgründe, Grenzen (Frage 6 und 7).

### Phase 4 – Upgrader und Tier 2
- Rechtsklick mit dem Upgrader: Tier 1 → Tier 2, das Item wird verbraucht. Bei Tier 2 passiert nichts und das Item bleibt.
- **Upgrade-Animation**: Partikelsäule in Kristallfarbe, der Kristall (Item-Display) hebt sich und dreht sich schneller, Lichtblitz, Wechsel von Rauten- auf Sechseck-Modell, geschichteter Sound (Beacon-Aktivierung, Amethyst, Totem). Spieler kurz unbeweglich.
- Tier-2-Markierung laut Frage 13.
- Tests: Upgrade, schon Tier 2, kein Kristall.

### Phase 5 – Gem Trader
- Rechtsklick öffnet eine sgui-GUI mit den aktivierten Kristallen (Icon, Farbe, Kurzbeschreibung der Kräfte). Der eigene Kristall ist ausgegraut.
- Bestätigen: Trader wird verbraucht, Kristall gewechselt, Tier und Energie bleiben (Frage 9), Cooldowns werden zurückgesetzt.
- **Tausch-Animation**: Der alte Kristall löst sich in einem Partikelwirbel seiner Farbe auf, der Wirbel wechselt die Farbe, der neue Kristall setzt sich zusammen.
- Tests: Auswahl, deaktivierter Kristall, Trader schließt GUI ohne Wahl (nichts verbraucht).

### Phase 6 – Repair Kit / Totem
**Aufstellen**
- Repair Kit auf den Boden setzen: Es entsteht ein Totem (Polymer-Block plus schwebendes Item-Display). Nur wenn **kein anderes Totem läuft**, sonst Meldung mit den Koordinaten des laufenden.
- Sofort an alle: Chat-Nachricht und **Titel mit Koordinaten** (Welt, x, y, z). Eine Bossbar zeigt die Restzeit für alle Spieler.
- Rand des Kreises (15 Blöcke) als Partikelring, dazu eine langsam rotierende Innenspirale. Kein Schutzschild.

**Laufzeit (10 Minuten)**
- Teilnehmer nach Frage 4. Wer den Kreis verlässt, stirbt oder sich ausloggt, fliegt raus; er bekommt eine Meldung, die Ringfarbe zeigt es ihm.
- Der Zustand wird gespeichert; nach einem Server-Neustart läuft das Totem weiter. Wer durch den Neustart „ausgeloggt“ wurde, zählt **nicht** als ausgeschieden.

**Ende**
- Nach 10 Minuten zerbricht das Totem mit großer Animation. Erst dann bekommen alle verbliebenen Teilnehmer mit Energie unter 5 genau 5 Energie (Protokoll-Grund `totem`). Die bestehende Reparatur-Animation (`StageEffects`) läuft bei jedem.

**Zerstören**
- Totem ist für Vanilla-Abbau unzerbrechlich. Der erste Schlag startet den eigenen **10-Sekunden-Countdown** (Werkzeug, Effizienz und Eile egal). TNT, Kolben und Feuer wirken nicht.
- Anzeige: Risse am Block (Block-Zerstörungs-Paket), eine Zahl „10 … 1“ über dem Totem, Actionbar für alle im Kreis, im Chat an alle „Totem wird angegriffen!“ mit Koordinaten.
- Sound: jede Sekunde ein Warnton, der höher wird, in den letzten 3 Sekunden doppelt so schnell. Beim Bruch Glasklirren und das Abschalten eines Beacons.
- Partikel: rot-violette Spirale zieht sich um das Totem zusammen, bei jedem Schlag fliegen Splitter. Beim Bruch eine Explosion aus Totem- und End-Rod-Partikeln, der Ring fällt in sich zusammen.
- Abbruch des Countdowns nach Frage 1. Folgen des Zerstörens nach Frage 5.

- Tests: nur ein Totem, Teilnehmerregeln (verlassen, Tod, Logout, Neustart), Wirkung nur unter 5, Countdown inkl. Reset, Explosionsschutz.

### Phase 7 – Steuerung, Client-Mod, HUD
- Ohne Mod: Tastenbelegung nach Frage 3, Schalter in `controls`.
- **Optionaler Client-Mod** `client/` (Fabric zuerst, später NeoForge): drei Tasten in den Minecraft-Einstellungen (Standard z. B. V, R, G für Primär, Sekundär, Ultimate). Er schickt nur ein Paket `xmassmp:ability` mit der Slot-Nummer. Der Server nimmt es nur von Spielern an, die den Kanal angemeldet haben, und prüft wie bei der Taste F. Paper später über einen Plugin-Kanal.
- HUD: drittes Icon pro Kristall für die Ultimate. **Neue Icons nötig** (das Pack hat nur 2 je Gem); bis dahin ein Platzhalter.
- Tests: Paket mit ungültigem Slot, Paket ohne Kristall, Spam (Rate-Limit), HUD mit drei Slots.

### Phase 8 – Kräfte
1. Ultimates für alle 5 Kristalle entwerfen und **zusammen mit dem bisherigen Entwurf freigeben lassen** (Frage 14). Vorher wird keine Kraft gebaut.
2. Danach pro Kristall: Passive T1/T2, Primär, Sekundär, Ultimate. Reihenfolge: Feuer, Wind, Erde, Schatten, Natur. Jede Kraft erfüllt die Qualitätsregeln aus `todo.md` Abschnitt 5 (mehrphasig, mehrere Partikeltypen, geschichtete Sounds, Display-Entities, Budget, kein Block-Schaden, Werte in der Config).
3. Je Kristall: Lore der Fähigkeiten, Test im Spiel mit Video oder GIF.

### Phase 9 – Admin-GUI und Befehle ergänzen
- Neue Seite **Items & Rezepte** (ersetzt den Platzhalter „Trader/Upgrader“): Rezepte an/aus, Kosten.
- Neue Seite **Totem**: läuft eins, wo, Restzeit, Teilnehmer; abbrechen, hinteleportieren.
- Seite **Energie**: Zugriffsschwellen, Protokoll-Ansicht.
- Befehle: `/xmas energy audit|bottles`, `/xmas totem status|cancel`.

### Phase 10 – Abnahme Fabric, dann weitere Plattformen
- Mehrspieler-Test: Energie-Regeln, Flaschen-Exploits gezielt versuchen (Dupe per Creative, Shift-Klick-Crafting, Abfüllen mit Makro), Totem mit Angreifern, Trader, Upgrader.
- Leistungstest mit vielen Spielern und Totem plus Fähigkeiten gleichzeitig.
- Multi-Version 1.21.11 → 26.3, dann Paper und NeoForge (`todo.md` Abschnitt 9).

---

## 4. Umsetzungsstand (2026-10-30, Branch `claude/gameplay-systeme`)

| Phase | Stand |
|---|---|
| 0 | Ordner ist Git-Checkout. Fabric-Server 1.21.11 lokal gestartet: Mod lädt ohne Fehler, Befehle per RCON geprüft, Totem übersteht TNT, Upgrader-Rezept im Crafter erzeugt das richtige Item, gefälschte Flasche ergibt keine Splitter. **Mit echtem Spieler noch nicht getestet.** Item-Architektur: Polymer-Server-Items (`xmassmp:<typ>`), bug.md Nr. 23 damit erledigt |
| 1 | Fertig: 11 Zustände (`EnergyLevel`), Schwellen 1/3/5, `energy.access`, `LOW_ENERGY`, 3 Slots, `PassiveService` (Passive immer), Kill-Regeln, `EnergyLedger` (`energy.log`) |
| 2 | Fertig: eigene Items, Rezepte aus `config.jsonc` (`RecipeService`, `XmasRecipe`), Lore, Sprachdateien. Revival Tome entfernt |
| 3 | Fertig: Abfüllen (gehaltene Eingabe, Server-Ticks, Ladebalken als Untertitel), Trinken, Zerlegen, `BottleRegistry` (`bottles.json`), Kampfsperre, `/xmas energy audit|bottles`. Abweichung: Bewegungsgrenze 1 Block statt 0,5 |
| 4 | Fertig: Upgrader mit Animation, gelber Tab-Name (`ServerPlayerMixin`) |
| 5 | Fertig: Händler-GUI mit freier Wahl, Tausch-Animation, optionale Wartezeit |
| 6 | Fertig: Totem-Block (Client sieht unheilvollen Tresor), Teilnahme, Ausscheiden, Neustart-sicher (`totem.json`), fester 10-s-Abbau über Polymer, Countdown-Sound/-Partikel, `/xmas totem status|cancel`. Abweichung: Restzeit und Countdown stehen in der Actionbar (HUD) statt Bossbar und schwebender Zahl |
| 7 | Fertig: Ultimate per Schleichen + Rechtsklick, Client-Mod `client/` (Tasten R/G/V), Paket `xmassmp:ability` mit Spam-Schutz, 3. HUD-Icon (Platzhalter-Grafiken) |
| 8 | Entwurf mit Ultimates in `docs/kraefte-entwurf.md` – **wartet auf Freigabe**, noch keine Kraft gebaut |
| 9 | Fertig: Befehle und Admin-Seite **Gameplay** (Flaschen, Upgrader, Händler, Totem-Werte, laufendes Totem beenden, Rezepte an/aus). Rezept-Muster selbst nur in `config.jsonc` |
| 10 | Offen |

## 5. Was schon vor diesem Plan erledigt war

- Erster Join bekommt einen Kristall (auch Spät-Einsteiger): `crystalItem.giveOnJoin`.
- Actionbar-HUD mit Icons und Cooldowns: `HudService`.
- Bruch- und Reparatur-Animation beim Stufenwechsel: `StageEffects` (wird von Totem und Flasche wiederverwendet).
- Seelenbindung, Spielerdaten, Audit-Log für Admins, Effekt-Engine mit Display-Entities.
