# Bliss-Original vs. Xmas-SMP

Recherche zum Verhalten des Bliss-SMP-Gem-Plugins (Stand: Season 3 / BlissGems 5.x) und die daraus abgeleiteten Regeln für Xmas-SMP.
Festlegungen des Owners (2026-10-21) sind mit **[Owner]** markiert, eigene Vorschläge mit **[Vorschlag]**. Noch offene Punkte stehen in Abschnitt 12.

## Quellenlage

Das echte Bliss-Plugin ist **closed source** und nur auf dem Bliss-Server im Einsatz. Alles hier stammt aus Fan-Wikis und aus öffentlichen Nachbauten:

| Quelle | Art | Bemerkung |
|---|---|---|
| [Bliss SMP Wiki (Miraheze)](https://blisssmp.miraheze.org/wiki/Gem_Items) – Seiten *Energy*, *Energy Mechanics (S3)*, *Traders*, *Upgraders*, *Repair Kit*, *Restoration Item*, *Gem Fragment* | Fan-Wiki | Aktuellste Quelle, teils als „unsicher“ markiert |
| [Bliss SMP Wiki (Fandom)](https://blisssmp.fandom.com/wiki/Season_3_Gems) – *Season 3 Gems*, *Mechanics*, *Trader*, *Upgrader*, *Repair Kit* | Fan-Wiki | Älter, an manchen Stellen abweichend |
| [BlissGems auf Modrinth](https://modrinth.com/plugin/bliss-smp-plugin) + [Doku](https://docs.xoperr.dev/plugins/blissgems/) | Nachbau (Paper) | Herkunft des Texturpacks `BlissGemsV4.1-fixed.zip` |
| [UltimateBlissSMP](https://modrinth.com/plugin/ultimateblisssmp), [BlissKeyBinds](https://modrinth.com/mod/blisskeybinds), [GitHub-Fork STRSC](https://github.com/afridy125/STRSC-Bliss-gem-5.0.2/releases/tag/BlissSmpPatch) | Nachbauten / Client-Mod | Nur zum Vergleich der Steuerung |

Bei Widersprüchen gilt: Miraheze > Fandom > Nachbauten. Die Seiten selbst konnten aus der Build-Umgebung nicht abgerufen werden; die Angaben stammen aus Suchauszügen und sind mit **(V)** markiert, wo eine Prüfung im Spiel sinnvoll ist.

---

## 1. Energiesystem

### Original

- Wertebereich **0–10**, Start **5** (Pristine).
- **Jeder Tod −1** (auch natürliche Tode), **jeder Spieler-Kill +1**.
- Bei 10 (Pristine +5) keine Aufnahme mehr; wer von einem Spieler mit 10 Energie getötet wird, droppt seine Energie als **Energy Bottle**.
- Seit Season 3 **kein Deathban** bei 0 Energie.
- Mit Broken-Gem keine Aufnahme per Kill oder Flasche; Rettung nur per Restoration-Ritual.
- Abstufung (Miraheze): Scratched/Cracked = längere Cooldowns, Damaged = keine aktiven Kräfte, Ruined = keine Passiven, Broken = alles aus.

| Energie | Stufe (Original) | Hinweis |
|---|---|---|
| 10 | Pristine +5 | Enchant-Glint, keine Aufnahme mehr |
| 6–9 | Pristine +1 … +4 | |
| 5 | Pristine | Startwert |
| 4 | Scratched | |
| 3 | Cracked | |
| 2 | Damaged (Fandom: *Shattered*) | |
| 1 | Ruined | |
| 0 | Broken | |

### Xmas-SMP

- 0–10, **Start 5**, Kill +1, Tod −1 (auch natürliche Tode, konfigurierbar) **[Owner]**.
- **Auch mit 0 Energie bekommt man per Kill wieder Energie** **[Owner]** (Abweichung vom Original) – aber nur, wenn das Opfer selbst Energie verliert. Ein Kill an einem Spieler mit 0 Energie bringt nichts **[Owner]**.
- Killer mit 10 Energie → die Energie des Opfers fällt als Energy Bottle auf den Boden (wie Original) **[Vorschlag]**.
- Fähigkeiten nach Energie **[Owner]**:

| Energie | Stufe | Passiv | Primär | Sekundär | Ultimate (nur Tier 2) |
|---|---|---|---|---|---|
| 3–10 | Gerissen (3) · Zerkratzt (4) · Makellos (5) · Makellos +1…+5 (6–10) | ✔ | ✔ | ✔ | ✔ |
| 1–2 | Ruiniert (1) · Beschädigt (2) | ✔ | ✔ | ✘ | ✘ |
| 0 | Zerbrochen | ✔ | ✘ | ✘ | ✘ |

- **Passive Kräfte sind immer aktiv** – unabhängig von der Energie und davon, wo der Gem im Inventar liegt **[Owner]**.
- Stufennamen englisch wie im Original im Item-Namen (`Pristine +2`, `Cracked` …), deutsche Übersetzung über `de_de.json` **[Vorschlag]**.
- **Abweichung:** Das Original sperrt aktive Kräfte schon bei 2 und Passive bei 1. Hier bleibt die Primärkraft bis 1 nutzbar, und Passive gehen nie verloren.

### Energie → Modell (vorhandene Texturen)

Das Pack hat je Tier nur 4 Optikstufen (siehe `docs/resourcepack.md`):

| Energie | Modell | Optik |
|---|---|---|
| 0 | `*_tierN` | zerbrochen |
| 1–2 | `*_tierN_pristine2` | beschädigt |
| 3–4 | `*_tierN_pristine3` | zerkratzt |
| 5–10 | `*_tierN_pristine4` | makellos (bei 10 zusätzlich Glint) |

---

## 2. Tier-System / Upgrader

### Original

- Upgrader macht aus Tier 1 einen Tier-2-Gem, Tier 2 = mehr/stärkere Fähigkeiten.
- Rezept S3 (laut Wiki „unsicher“): **4 Gem Fragments + 1 Witherskelettschädel + 4 Drachenatem** (V).

### Xmas-SMP

- Tier 1: Passiv + **Primär** + **Sekundär**. Tier 2: zusätzlich **Ultimate** **[Owner]**.
- Upgrader (`enchanted_book`, CMD 3001) behält Kristalltyp und Energie.
- **Rezept wie Original** **[Owner]** (Fragments kommen aus Energy Bottles, siehe Abschnitt 4):

```
F B F        F = Gem Fragment
B W B        B = Drachenatem
F B F        W = Witherskelettschädel
```

- **Folge für die Kräfte (todo.md, Abschnitt 5):** Jeder Kristall braucht 3 aktive Kräfte. Im Pack gibt es nur 2 HUD-Icons pro Gem; für die Ultimate kommt je ein neues dazu.

---

## 3. Gem Trader

### Original

- Würfelt den Gem **zufällig** in einen anderen Typ um und wird verbraucht. S3: lange Tausch-Animation gegen „Cycling“ (schnelles Hin-und-Her-Tauschen für überlappende Effekte).
- Rezept S3: 4 Diamantblöcke + 4 Drachenatem + 1 Sculk-Katalysator (Anordnung nicht dokumentiert, V).

### Xmas-SMP

- **Freie Wahl** des neuen Kristalls über ein **Chest-GUI** (5 Kristalle, aktueller ausgegraut) **[Owner]**.
- **Rezept:** Original, aber der Sculk-Katalysator wird durch ein **Gem Fragment** ersetzt **[Owner]**:

```
D B D        D = Diamantblock
B F B        B = Drachenatem
D B D        F = Gem Fragment
```

- 1 Fragment in der Mitte genügt – Trader sollen **günstig** bleiben, damit Ausprobieren Spaß macht **[Owner]**.
- Tier und Energie bleiben erhalten, Trader wird verbraucht **[Vorschlag]**.
- Tausch-Animation mit Pflicht-Dauer (z. B. 3 s, Abbruch bei Schaden) plus Cooldown nach dem Tausch gegen Cycling **[Vorschlag]**.
- Textur: `gem_trader` (CMD 4002). Das ungenutzte Modell `trader` kann als GUI-Icon dienen.

---

## 4. Gem Fragments

- Original: eigenes Rezept (4 Diamanten, 2 Amethyst-Cluster, 2 Smaragde, 1 Eisenblock), Zutat für Trader, Upgrader und Repair Kit.
- **Xmas-SMP [Owner]:** Fragments gibt es **nur** aus Energy Bottles: **1 Energy Bottle → 9 Gem Fragments**, formlos im 2×2-Inventar oder an der Werkbank. Dadurch bleiben die Originalrezepte nutzbar, und jedes Fragment kostet echte Energie (1 Energie = 9 Fragments).
- Es gibt **kein** Rückrezept (Fragments → Flasche) und kein Fragment-Rezept aus Erzen **[Vorschlag]**.
- Textur: `gem_fragment` (CMD 4004).
- Der **All-for-One-Gem entfällt** **[Owner]**.

---

## 5. Energy Bottle

### Original

- `/bliss withdraw` (−1 Energie) oder Drop, wenn der Killer schon bei 10 ist. Rechtsklick → aufnehmen (nicht bei 10 oder 0). Verfall nach 1 h.

### Xmas-SMP

- **Abzapfen [Owner]:** Leere Glasflasche in der Hand, **Schleichen + Rechtsklick gedrückt halten**. Am Fadenkreuz erscheint ein **Ladebalken**; nach **5 s** wird eine Glasflasche zur Energy Bottle und man verliert 1 Energie. Abbruch bei Loslassen, Aufstehen, Slot-Wechsel, Bewegung > 1 Block oder Schaden **[Vorschlag]**. Abzapfen nur ab 1 Energie.
- **Trinken [Owner]:** Rechtsklick → +1 Energie. Funktioniert **auch bei 0 Energie**, nicht bei 10. Jeder Spieler kann jede Flasche trinken.
- **Zerlegen [Owner]:** Flasche → 9 Gem Fragments (Abschnitt 4).
- Kein Verfall, kein Server-Pool **[Vorschlag]**.
- Technik (Fabric serverseitig, ohne Client-Mod):
  - Gehaltener Rechtsklick ist am Server erkennbar: der Client schickt alle 4 Ticks ein Use-Paket; bleibt es > 6 Ticks aus, gilt die Taste als losgelassen. Die 5 s werden in **Server-Ticks** gezählt.
  - Ladebalken: Font-Glyphen (11 Füllstufen) als **Title** mit negativem Abstand, mittig auf dem Fadenkreuz, Fade 0 Ticks.
  - Beim Schleichen wird das Vanilla-Verhalten der Flasche (Wasser schöpfen) unterdrückt.
  - Textur: `energy_bottle` (CMD 4001).
- Schutz vor Manipulation: siehe **Abschnitt 6**.

---

## 6. Schutz vor Exploits beim Energietransfer [Owner-Anforderung]

Ziel: Energie darf nur auf genau den vorgesehenen Wegen entstehen, wandern oder verschwinden. Grundprinzip: **Der Server ist die einzige Wahrheit**. Item-Daten auf der Flasche sind nur ein Verweis; was sie wert ist, steht in einem serverseitigen Register.

### 6.1 Flaschen-Register (Ledger)

- Jede Energy Bottle bekommt beim Erzeugen eine **zufällige UUID** in `custom_data`. Der Server legt dazu einen Eintrag an: `id, Herkunft (Spieler/Grund), Zeit, Status = AKTIV`.
- **Trinken oder Zerlegen** prüft und schließt den Eintrag in einem Schritt auf dem Server-Thread: Status `AKTIV` → `VERBRAUCHT`, erst danach gibt es Energie bzw. Fragments. Weil der Server-Thread einzeln läuft, können zwei Kopien nicht gleichzeitig durchrutschen.
- Unbekannte oder schon verbrauchte ID → **keine Energie**, das Item wird entfernt, ein Eintrag geht ins Admin-Log (Spieler, Ort, ID) und optional eine Meldung an Online-Admins.
- Damit sind wirkungslos: Dupe-Glitches (Disconnect-, Inventar-Desync-, Chunk-Dupes), Creative-Mittelklick-Kopien, per NBT-Editor gefälschte Flaschen.
- Das Register wird bei jeder Änderung sofort gespeichert (Append-only-Log, beim Start kompaktiert), damit ein Absturz keine „Geister-Flaschen“ erzeugt.

### 6.2 Item-Eigenschaften

- **Max. Stapelgröße 1** – jede Flasche hat eine eigene ID, Stapeln/Zusammenführen ist so ausgeschlossen.
- **Keine Vanilla-Verwendung:** Das Basis-Item (`ghast_tear`) ist im Vanilla-Spiel eine Zutat (Endkristall, Brauen). Auf Fabric sind Polymer-Items serverseitig eigene Items, Vanilla-Rezepte greifen also gar nicht. Auf Paper/NeoForge werden Werkbank, Brau- und Schmiedestand, Amboss, Schleifstein, Werfer und Trichter-Brauen explizit für Xmas-Items gesperrt. Das gilt genauso für Gem Fragments (`prismarine_shard`).
- Umbenennen am Amboss ändert nur den Namen, nicht die ID – erlaubt.

### 6.3 Abzapfen (Flasche → Energie runter)

- Start nur mit einer **echten Vanilla-Glasflasche** (keine Custom-Daten), im Überleben/Abenteuer, lebendig.
- Beim **Abschluss** wird alles noch einmal geprüft: Energie ≥ 1, gleicher Slot, immer noch Glasflasche, Spieler schleicht, keine laufende andere Aktion. Dann in **einem Tick**: Energie −1, eine Glasflasche aus dem Stapel entfernen, Flasche mit neuer ID erzeugen, Register-Eintrag anlegen.
- Inventar voll → Flasche fällt vor die Füße, nur der Ersteller kann sie in den ersten 30 s aufheben.
- Timer läuft in Server-Ticks: Speed-Hacks oder Makros können die 5 s nicht abkürzen.
- Der Ladebalken ist reine Anzeige; der Server entscheidet.

### 6.4 Trinken (Flasche → Energie hoch)

- Nur Haupthand, nur bei Energie < 10, kurzer Cooldown (1 s) zwischen zwei Flaschen.
- Reihenfolge: Register prüfen → Eintrag schließen → Item entfernen → Energie +1. Schlägt ein Schritt fehl, passiert nichts.

### 6.5 Zerlegen (Flasche → 9 Fragments)

- Das Rezept zeigt nur ein Ergebnis, wenn die Flasche im Register `AKTIV` ist.
- Beim Herausnehmen (auch Shift-Klick mit mehreren Flaschen) wird **jede** Flasche einzeln geprüft und geschlossen. So kann man eine kopierte Flasche nicht über Fragments „waschen“.
- Fragments sind stapelbar und haben deshalb keine eigene ID. Ihr Wert ist gering (1/9 Energie), und Creative ist Admins vorbehalten. Herkunft wird über das Register mitprotokolliert.

### 6.6 Kills und Tode

- Energie per Kill gibt es nur, wenn das **Opfer tatsächlich Energie verloren hat** (Opfer > 0). Sonst könnte man mit einem Zweitaccount auf 0 Energie beliebig Energie „erzeugen“ **[Owner]**.
- Derselbe Gegner bringt innerhalb von 30 min nur einmal Energie (Anti-Farming), konfigurierbar.
- Killer bei 10 → die Flasche des Opfers wird ganz normal mit Register-Eintrag erzeugt.

### 6.7 Protokoll und Kontrolle

- Jede Energieänderung landet im Audit-Log: `Zeit, Spieler, alt → neu, Grund (Kill/Tod/Abzapfen/Trinken/Repair Kit/Admin), Flaschen-ID`.
- Prüfsumme: Energie aller Spieler + aktive Flaschen. Ein unerklärter Sprung wird den Admins gemeldet.
- `/xmas energy audit <spieler>` zeigt die letzten Änderungen.

---

## 7. Repair Kit

### Original

- Ritual an einem festen Pedestal (Beacon auf Ancient Debris), nur zu festen Zeiten; heilt alle Gems unter Pristine im Umkreis aus einem Server-Pool. Rezept S3: 4 Gem Fragments + 2 Ambosse + 2 Netheritbarren + 1 Netherit-Schmiedevorlage (V).

### Xmas-SMP [Owner]

- Wird **auf den Boden platziert** und wird zum *Totem*.
- **Radius 15 Blöcke**, Grenze als Partikelring sichtbar.
- **Koordinaten werden geleakt:** Chat-Nachricht + Title an alle.
- Läuft **10 Minuten**. Erst wenn das Totem danach zerbricht, bekommen die berechtigten Spieler ihre Energie: **jede Energie unter 5 → 5** (auch 0). Spieler mit ≥ 5 bleiben unverändert.
- **Unbegrenzte Spielerzahl.** Berechtigt ist nur, wer **die vollen 10 Minuten** im Radius war.
- **Verlassen, Tod oder Ausloggen** = raus. Genau das ist das Ziel der Gegner – deshalb die geleakten Koordinaten.
- **Kein Schutzschild:** jeder kann rein, PvP läuft normal.
- **Zerstörbar** mit einer **festen Abbauzeit von 10 Sekunden** – unabhängig von Werkzeug, Effizienz, Eile oder Abbaulähmung **[Owner]**. Explosionen, Kolben und Feuer wirken nicht.
- **Countdown beim Abbauversuch [Owner]:** Sobald ein Spieler das Totem anschlägt (Abbauen beginnt), startet ein 10-s-Countdown, danach zerbricht es:
  - **Anzeige:** Riss-Animation am Block in 10 Stufen, Countdown über dem Totem (Text-Display „10 … 1“) und als Actionbar für alle im Radius; Chat-Meldung an alle Spieler „Totem wird angegriffen!“ mit Koordinaten.
  - **Sound:** jede Sekunde ein Warnton mit steigender Tonhöhe (z. B. `block.note_block.bell` + `block.vault.deactivate`), in den letzten 3 s schneller; beim Bruch Glas-/Beacon-Abschaltsound.
  - **Partikel:** rote/violette `dust`-Spirale, die sich um das Totem zusammenzieht, Block-Bruchpartikel bei jedem Schlag; beim Bruch Explosion aus `totem_of_undying`-, `end_rod`- und Splitterpartikeln, und der Partikelring am Radius fällt in sich zusammen.
  - **Abbruch [Vorschlag]:** Hört der Angreifer auf abzubauen (> 1 s kein Schlag), stirbt oder entfernt sich mehr als 5 Blöcke, wird der Countdown zurückgesetzt (Risse verschwinden, „Abgewehrt“-Sound). So können Verteidiger den Angreifer stoppen – siehe Frage 1.
  - Wird es zerstört, scheitert das Ritual für alle; Meldung an alle, Kit ist verloren.
- **Global nur ein Totem gleichzeitig.** Platzieren eines zweiten wird abgelehnt (mit Restzeit des laufenden).
- Technik **[Vorschlag]:** Server-Block, den der Client als unzerstörbar sieht (damit er nicht selbst „fertig abbaut“). Der Server wertet die Abbau-Pakete aus, zählt die 10 s in Ticks und schickt die Riss-Stufen selbst. Darüber schwebt das `repair_kit`-Modell als Item-Display. Der Block droppt nichts.
- **Rezept [Owner]:** Original, die 2 Netheritbarren werden durch **2 Diamantblöcke** ersetzt:

```
F A F        F = Gem Fragment
D T D        A = Amboss
F A F        D = Diamantblock
             T = Netherit-Aufwertungs-Schmiedevorlage
```

- Textur: `repair_kit` (CMD 4003).

### Revival Tome

- Wird nicht gebraucht (Repair Kit und Energy Bottles helfen auch bei 0, kein Deathban). Modell `revival_tome` bleibt für den Christmas-Twist reserviert **[Vorschlag]**.

---

## 8. Fähigkeiten aktivieren & HUD

### Original / Nachbauten

- Gem in der **Offhand** = Passive. Gem in der **Haupthand + Rechtsklick** = Primär, **Schleichen + Rechtsklick** = Sekundär.
- Client-Mods wie *BlissKeyBinds* oder der BlissGems-Fabric-Mod legen **frei belegbare Tasten** in *Optionen → Steuerung* an.
- Cooldowns in der **Actionbar** mit Font-Icons.

### Xmas-SMP

- **Passive wirken immer**, egal wo der Gem im Inventar liegt **[Owner]**.
- **Actionbar mit Icons + Cooldowns + Energie** **[Owner]**; bei Cooldown-Ende kurzer Sound + Partikel.
- **Hotkeys in den Einstellungen setzbar [Owner]**. Ein Server kann keine Tasten in *Optionen → Steuerung* anlegen, deshalb:
  1. **Optionaler Client-Companion-Mod** (Fabric, später NeoForge) mit drei Tasten (Primär / Sekundär / Ultimate, Standard `R`, `G`, `V`). Er schickt ein Custom-Payload-Paket – funktioniert gegen Fabric-, NeoForge- **und** Paper-Server. Der Server prüft dabei alles selbst (Cooldown, Energie, Tier); der Mod kann nur „Taste gedrückt“ melden.
  2. **Vanilla-Fallback** ohne Mod: Gem in der Haupthand + Rechtsklick = Primär, Schleichen + Rechtsklick = Sekundär, Schleichen + `F` = Ultimate.

---

## 9. Startverhalten

- **Original:** zufälliger Gem beim ersten Betreten.
- **Xmas-SMP [Owner]:** Jeder Spieler bekommt beim **ersten Login** einen Gem, auch wenn er später einsteigt. Start: Tier 1, 5 Energie.
- Zuteilung zufällig, optional ausgeglichen (seltenster Kristall bevorzugt) **[Vorschlag, konfigurierbar]**.
- Gem ist seelengebunden (kein Drop, nicht in Kisten, nicht wegwerfbar). Geht er verloren, kommt er beim nächsten Login bzw. per Admin-Befehl zurück **[Vorschlag]**.

---

## 10. Übersicht Original ↔ Xmas-SMP

| Feature | Übernommen | Anmerkung |
|---|---|---|
| Energie 0–10, Start 5, Kill +1 / Tod −1 | ✔ | Kill bringt auch bei 0 Energie +1 |
| Stufennamen Broken … Pristine +5 | ✔ | Optik in 4 Stufen |
| Kein Deathban | ✔ | |
| Abstufung der Kräfte | geändert | 1–2: nur Primär · 0: nur Passiv · Passive immer |
| Tier 2 per Upgrader | ✔ | Tier 2 = Ultimate, Originalrezept |
| Gem Trader | geändert | GUI mit freier Wahl; Fragment statt Sculk-Katalysator |
| Gem Fragments | geändert | nur aus Energy Bottles (1 → 9) |
| All-for-One-Gem | ✘ | |
| Energy Bottle | geändert | Abzapfen per Halten, auch bei 0 trinkbar, Ledger gegen Dupes |
| Repair Kit | geändert | Totem, 15 Blöcke, 10 min, Koordinaten-Leak, zerstörbar, global nur eins; Diamantblöcke statt Netherit |
| Restoration/Revival | ✘ | |
| Server-Pool, Pedestal, Zeitfenster | ✘ | |
| Zufälliger Gem beim ersten Join | ✔ | auch für Spätstarter |
| Steuerung per Klick-Kombination | ✔ | + optionaler Keybind-Mod |

---

## 11. Konfigurationswerte (Vorschlag)

```yaml
energy:
  start: 5
  min: 0
  max: 10
  gainPerKill: 1
  lossPerDeath: 1
  naturalDeathCostsEnergy: true
  killRequiresVictimEnergy: true   # Anti-Exploit, siehe 6.6
  sameVictimCooldownMinutes: 30
  dropBottleWhenKillerAtMax: true
  secondaryMinEnergy: 3            # gilt auch für die Ultimate
  primaryMinEnergy: 1
energyBottle:
  extractSeconds: 5
  drinkCooldownSeconds: 1
  fragmentsPerBottle: 9
  expires: false
repairKit:
  radius: 15
  durationMinutes: 10
  restoreTo: 5
  breakSeconds: 10                 # fest, ignoriert Werkzeug/Effizienz
  breakResetAfterIdleTicks: 20
  maxActiveGlobal: 1
  announceCoordinates: true
trader:
  keepTier: true
  keepEnergy: true
  cooldownMinutes: 10
firstJoin:
  giveGem: true
  distribution: random             # random | balanced
```

---

## 12. Offene Fragen an den Owner

1. **Countdown abbrechen:** Bricht der Countdown ab, wenn der Angreifer aufhört abzubauen, stirbt oder weggestoßen wird (Vorschlag), oder läuft er nach dem ersten Schlag auf jeden Fall durch?
2. **Trader noch günstiger?** Das Rezept enthält weiterhin 4 Diamantblöcke (= 36 Diamanten) und 4 Drachenatem (End nötig). Soll es für „günstig“ z. B. 4 Diamanten statt Diamantblöcken sein?

### Beantwortet (2026-10-21)

| Frage | Antwort |
|---|---|
| Ultimate bei 1–2 Energie | fällt weg |
| Passive bei niedriger Energie | immer verfügbar |
| Energy Bottle bei 0 Energie trinken | erlaubt |
| Passiv-Bedingung | wirkt überall im Inventar |
| Totem zerstörbar | ja, fest 10 s mit Countdown, Sound und Partikeln |
| Tod / Logout im Kreis | zählt als Verlassen |
| Mehrere Totems | global nur eins |
| Kill mit 0 Energie (Killer) | gibt Energie |
| Kill an Opfer mit 0 Energie | gibt keine Energie |
| Passive bei 0 Energie | aktiv |
| Trader-Rezept | 1 Fragment in der Mitte, Trader sollen günstig sein |
| Rezepte | Flasche → 9 Fragments, Originalrezepte bleiben; Repair Kit: Diamantblöcke statt Netherit; Trader: Fragment statt Sculk-Katalysator |
