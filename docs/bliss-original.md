# Bliss-Original vs. Xmas-SMP

Recherche zum Verhalten des Bliss-SMP-Gem-Plugins (Stand: Season 3 / BlissGems 5.x) und die daraus abgeleiteten Regeln für Xmas-SMP.
Festlegungen des Owners vom 2026-10-21 sind mit **[Owner]** markiert, eigene Vorschläge mit **[Vorschlag]**, offene Punkte stehen in Abschnitt 11.

## Quellenlage

Das echte Bliss-Plugin ist **closed source** und nur auf dem Bliss-Server im Einsatz. Alles hier stammt aus Fan-Wikis und aus öffentlichen Nachbauten:

| Quelle | Art | Bemerkung |
|---|---|---|
| [Bliss SMP Wiki (Miraheze)](https://blisssmp.miraheze.org/wiki/Gem_Items) – Seiten *Energy*, *Energy Mechanics (S3)*, *Traders*, *Upgraders*, *Repair Kit*, *Restoration Item*, *Gem Fragment* | Fan-Wiki | Aktuellste Quelle, teils als „unsicher“ markiert |
| [Bliss SMP Wiki (Fandom)](https://blisssmp.fandom.com/wiki/Season_3_Gems) – *Season 3 Gems*, *Mechanics*, *Trader*, *Upgrader*, *Repair Kit* | Fan-Wiki | Älter, an manchen Stellen abweichend |
| [BlissGems auf Modrinth](https://modrinth.com/plugin/bliss-smp-plugin) + [Doku](https://docs.xoperr.dev/plugins/blissgems/) | Nachbau (Paper) | Herkunft des Texturpacks `BlissGemsV4.1-fixed.zip` |
| [UltimateBlissSMP](https://modrinth.com/plugin/ultimateblisssmp), [BlissKeyBinds](https://modrinth.com/mod/blisskeybinds), [GitHub-Fork STRSC](https://github.com/afridy125/STRSC-Bliss-gem-5.0.2/releases/tag/BlissSmpPatch) | Nachbauten / Client-Mod | Nur zum Vergleich der Steuerung |

Bei Widersprüchen gilt: Miraheze > Fandom > Nachbauten. Die Seiten selbst konnten aus der Build-Umgebung nicht abgerufen werden; die Angaben stammen aus Suchauszügen und sind daher mit **(V)** markiert, wo eine Prüfung im Spiel/Discord sinnvoll ist.

---

## 1. Energiesystem

### Original

- Wertebereich **0–10**, Start **5** (Pristine).
- **Jeder Tod −1** (auch natürliche Tode), **jeder Spieler-Kill +1**.
- Bei 10 (Pristine +5) kann keine Energie mehr aufgenommen werden; wer von einem Spieler mit 10 Energie getötet wird, droppt seine Energie als **Energy Bottle**.
- Ab Season 3 **kein Deathban** mehr bei 0 Energie – der Gem bleibt einfach zerbrochen.
- Mit Broken-Gem kann man weder per Kill noch per Flasche Energie aufnehmen; Rettung nur über Restoration-Ritual.
- Abstufung der Fähigkeiten (Miraheze): Scratched/Cracked = längere Cooldowns, Damaged = keine aktiven Kräfte mehr, Ruined = keine Passiven mehr, Broken = alles aus.

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

- 0–10, **Start 5** **[Owner]**, Kill +1, Tod −1, Tod bei 0 kostet nichts mehr – wie Original **[Owner: „wie vermutet“]**.
- Natürliche Tode kosten wie im Original 1 Energie **[Vorschlag, konfigurierbar]**.
- Anti-Farming: derselbe Gegner gibt innerhalb von z. B. 30 min nur einmal Energie **[Vorschlag, konfigurierbar]**.
- Killer mit 10 Energie → die Energie des Opfers fällt als Energy Bottle auf den Boden (wie Original) **[Vorschlag]**.
- Fähigkeiten nach Energie **[Owner]**:

| Energie | Stufe | Passiv | Primär | Sekundär | Ultimate (nur Tier 2) |
|---|---|---|---|---|---|
| 5–10 | Makellos (+X) | ✔ | ✔ | ✔ | ✔ |
| 3–4 | Zerkratzt / Gerissen | ✔ | ✔ | ✔ | ✔ |
| 1–2 | Ruiniert / Beschädigt | ✔ | ✔ | ✘ | ✘ (Annahme, s. Frage 1) |
| 0 | Zerbrochen | ✘ | ✘ | ✘ | ✘ |

- Stufennamen: englisch wie Original im Item-Namen (`Pristine +2`, `Cracked` …), deutsche Übersetzung über `de_de.json` **[Vorschlag]**.
- **Abweichung:** Das Original sperrt aktive Kräfte schon bei 2 und Passive bei 1. Der Owner möchte mehr Spielbarkeit bei niedriger Energie: Primärkraft bleibt bis 1, alles fällt erst bei 0 weg.

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

- Upgrader macht aus einem Tier-1- einen Tier-2-Gem; Tier 2 = mehr/stärkere Fähigkeiten.
- Rezept S3 (Progression 3, laut Wiki „unsicher“): **4 Gem Fragments + 1 Wither Skull + 4 Dragon's Breath** (V).
- In frühen Progressionen gab es Upgrader nur als Boss-Drop (Wither: 4 Upgrader).

### Xmas-SMP

- Tier 1: Passiv + **Primärkraft** + **Sekundärkraft**. Tier 2: zusätzlich **Ultimative Attacke** (und ggf. stärkere Passive) **[Owner]**.
- Upgrader-Item (`enchanted_book`, CMD 3001) bleibt, Upgrade behält Kristalltyp und Energie.
- Da Gem Fragments entfallen **[Owner]**, braucht das Rezept einen Ersatz – Vorschlag (Form an das Original angelehnt):

```
D B D        D = Diamantblock (ersetzt Gem Fragment)
B W B        B = Drachenatem
D B D        W = Witherskelettschädel
```

- **Folge für Kräfte-Entwurf (todo.md, Abschnitt 5):** Jeder Kristall braucht jetzt **3 aktive Kräfte** statt 2 – die bisherige „Aktiv 2 (T2)“ wird zur Sekundärkraft, eine Ultimate kommt neu dazu. Außerdem gibt es pro Kristall nur 2 HUD-Icons → ein drittes Icon (Ultimate) muss gezeichnet werden.

---

## 3. Gem Trader

### Original

- Trader würfelt den eigenen Gem **zufällig** in einen anderen Typ um und wird verbraucht.
- S3: lange Tausch-Animation (gegen „Cycling“, also schnelles Hin-und-Her-Tauschen für überlappende Effekte); Tausch mit vertrautem Teammitglied möglich.
- Rezept S2: 4 Diamantblöcke + 1 Sculk-Katalysator. **Rezept S3: 4 Diamantblöcke + 4 Drachenatem + 1 Sculk-Katalysator.** Die genaue Anordnung ist in keiner Textquelle beschrieben (V); der STRSC-Fork nutzt das Muster unten.

### Xmas-SMP

- **Freie Wahl** des neuen Kristalls über ein **GUI** (Chest-GUI mit den 5 Kristallen, aktueller Kristall ausgegraut) **[Owner]**.
- **Craftbar mit dem Originalrezept (S3)** **[Owner]**:

```
D B D        D = Diamantblock
B S B        B = Drachenatem
D B D        S = Sculk-Katalysator
```

- Tier und Energie bleiben beim Tausch erhalten, Trader wird verbraucht **[Vorschlag]**.
- Tausch-Animation mit Pflicht-Dauer (z. B. 3 s), Abbruch bei Schaden, plus Cooldown nach dem Tausch – verhindert Cycling wie im Original **[Vorschlag]**.
- Textur: `gem_trader` (Smaragd, CMD 4002). Das ungenutzte Modell `trader` kann als GUI-Icon dienen.
- **Abweichung:** freie Wahl statt Zufall (Owner-Wunsch). Dadurch fällt die Glückskomponente weg; die hohen Kosten (Drachenatem = End nötig) bremsen das.

---

## 4. Gem Fragments – entfällt

- Original: Crafting-Material für Trader (S3), Upgrader und Repair Kit; Rezept 4 Diamanten + 2 Amethyst-Cluster + 2 Smaragde + 1 Eisenblock.
- **Xmas-SMP: entfällt [Owner].** Wo das Original Fragments verlangt, werden sie durch Diamantblöcke ersetzt (siehe Upgrader und Repair Kit). Modell/Textur `gem_fragment` bleibt im Pack, wird aber nicht vergeben.
- Ebenso entfällt der **All-for-One-Gem** **[Owner]**.

---

## 5. Energy Bottle

### Original

- Entsteht per `/bliss withdraw` (freiwilliges Abzapfen, −1 Energie) oder als Drop, wenn der Killer schon bei 10 ist.
- Rechtsklick → Energie aufnehmen (nicht bei 10 oder 0).
- Flaschen **verfallen nach 1 Stunde**, die Energie geht in einen Server-Pool zurück.

### Xmas-SMP

- **Abzapfen [Owner]:** Leere Glasflasche in der Hand, **Schleichen + Rechtsklick gedrückt halten**. Am Fadenkreuz erscheint ein **Ladebalken**; nach **~5 s** wird die Flasche zur Energy Bottle und man verliert 1 Energie. Loslassen, Bewegung > 1 Block oder Schaden bricht ab **[Vorschlag]**.
- **Trinken [Owner]:** Rechtsklick auf die Energy Bottle → +1 Energie. Die Flasche ist handelbar, also kann sie jeder Spieler trinken.
- Abzapfen nur ab 1 Energie, Trinken nur unter 10 **[Vorschlag]**; Verhalten bei 0 Energie siehe Frage 3.
- Kein Verfall nach 1 h und kein Server-Pool **[Vorschlag]** – das Original braucht den Pool nur für seine Repair-Kit-Mechanik, die hier anders läuft (Abschnitt 6).
- Technik (Fabric serverseitig, ohne Client-Mod):
  - Gehaltener Rechtsklick ist am Server erkennbar: der Client schickt alle 4 Ticks ein neues Use-Paket; bleibt es > 6 Ticks aus, gilt die Taste als losgelassen.
  - Ladebalken: Font-Glyphen (11 Füllstufen) als **Title** mit negativem Abstand, damit er mittig auf dem Fadenkreuz liegt. Fade-In/Out = 0 Ticks.
  - Vanilla-Verhalten der Flasche (Wasser schöpfen) wird beim Schleichen unterdrückt.
  - Textur: `energy_bottle` auf `ghast_tear` (CMD 4001).

---

## 6. Repair Kit / Revival Tome

### Original

- **Repair Kit:** Ritual am zentralen *Pedestal* (Beacon auf Ancient Debris), nur zu festen Zeiten (Mi/Sa/So je 1 h). Heilt alle Gems im Umkreis, die **unter Pristine** sind, bis maximal 10 Energie aus dem Server-Pool verbraucht sind. Broken-Gems sind ausgeschlossen. Rezept S3: 4 Gem Fragments + 2 Ambosse + 2 Netheritbarren + 1 Netherit-Schmiedevorlage (V).
- **Restoration Item / Revival:** Repariert Broken-Gems am Pedestal; der Spieler muss 5 Energy Bottles einzahlen und bekommt einen **zufälligen** neuen Gem auf Pristine.

### Xmas-SMP – Repair Kit [Owner]

- Wird **auf den Boden platziert** und wird dort zum *Totem* (Item-/Block-Display, kein echter Beacon-Block).
- **Radius 15 Blöcke**, Grenze als Partikelring sichtbar (plus aufsteigende Partikel am Totem).
- **Koordinaten werden geleakt:** Chat-Nachricht an alle + Title für alle Spieler.
- Läuft **10 Minuten**. Erst wenn das Totem nach den 10 min zerbricht, bekommen die berechtigten Spieler ihre Energie: **jede Energie unter 5 → 5**, auch 0. Spieler mit ≥ 5 bleiben unverändert.
- **Unbegrenzte Spielerzahl.**
- Berechtigt ist nur, wer **die vollen 10 Minuten** im Radius war. Wer den Kreis verlässt, fällt raus. Da das Totem nach 10 min zerbricht, kann nur profitieren, wer schon beim Platzieren im Kreis stand.
- **Kein Schutzschild:** jeder kann hinein, PvP im Kreis ist normal möglich.
- Rezept – Original ohne Fragments **[Vorschlag]**:

```
N A N        N = Netheritbarren
D T D        A = Amboss
N A N        D = Diamantblock (ersetzt Gem Fragment)
             T = Netherit-Aufwertungs-Schmiedevorlage
```

  (Original: 4 Fragments, 2 Ambosse, 2 Netherit, 1 Vorlage. Mit 4 Netherit + 2 Diamantblöcken bleibt es teuer.)
- Textur: `repair_kit` auf `beacon` (CMD 4003).
- **Abweichung:** Kein festes Pedestal, keine Zeitfenster, kein Energie-Pool. Stattdessen ein riskantes Ritual an einem öffentlich bekannten Ort – das erzeugt PvP-Brennpunkte statt Terminplanung.

### Xmas-SMP – Revival Tome

- Wird nicht gebraucht: Das Repair Kit holt auch Spieler mit 0 Energie zurück, und es gibt keinen Deathban. Modell `revival_tome` bleibt für den Christmas-Twist reserviert **[Vorschlag]**.

---

## 7. Fähigkeiten aktivieren & HUD

### Original / Nachbauten

- Gem in der **Offhand** = Passive aktiv. Aktivieren (BlissGems-Doku): Gem in der **Haupthand + Rechtsklick** = Primär, **Schleichen + Rechtsklick** = Sekundär (Tier 2).
- Neuere Versionen nutzen zusätzlich `F` / `Schleichen + F` und Linksklick-Slots; Client-Mods wie *BlissKeyBinds* (V/R/G/X) oder der BlissGems-Fabric-Mod (R/G/V/H) legen **frei belegbare Tasten** in *Optionen → Steuerung* an.
- Cooldowns stehen in der **Actionbar** mit Font-Icons pro Fähigkeit (die Glyphen ``… im Pack).

### Xmas-SMP

- **Actionbar mit Icons + Cooldown** **[Owner]**, z. B. `[Icon1] 12s  [Icon2] ✔  [Icon3] 1:40` plus Energie-Anzeige; bei Cooldown-Ende kurzer Sound + Partikel.
- **Hotkeys in den Einstellungen setzbar [Owner]**. Das geht technisch nur mit einem **Client-Mod**, denn ein Server kann keine Tasten in *Optionen → Steuerung* anlegen. Plan:
  1. **Optionaler Client-Companion-Mod** (Fabric, später NeoForge) mit drei Tasten (Primär / Sekundär / Ultimate, Standard z. B. `R`, `G`, `V`). Er schickt ein Custom-Payload-Paket an den Server – das funktioniert gegen Fabric-, NeoForge- **und** Paper-Server (Plugin-Messaging-Channel).
  2. **Vanilla-Fallback** für Spieler ohne Mod: Gem in der Haupthand + Rechtsklick = Primär, Schleichen + Rechtsklick = Sekundär, Schleichen + `F` (Hand tauschen) = Ultimate. Passive wirken, solange der Gem im Inventar ist **[Vorschlag]** – siehe Frage 4.
- Die Pack-Icons reichen für 2 Kräfte je Kristall; für die Ultimate kommt je ein neues Icon dazu (todo.md 1.4).

---

## 8. Startverhalten

- **Original:** Jeder Spieler bekommt beim ersten Betreten einen **zufälligen** Gem.
- **Xmas-SMP [Owner]:** Jeder Spieler bekommt beim **ersten Login** einen Gem, auch wenn er erst später einsteigt. Start mit Tier 1 und 5 Energie.
- Zuteilung zufällig, optional „ausgeglichen“ (der am seltensten vergebene Kristall wird bevorzugt) **[Vorschlag, konfigurierbar]**.
- Der Gem ist seelengebunden (kein Drop beim Tod, nicht in Kisten, nicht wegwerfbar). Geht er trotzdem verloren, erhält man ihn beim nächsten Login bzw. per `/xmas` zurück **[Vorschlag]**.

---

## 9. Was aus dem Original übernommen wird / was nicht

| Feature | Übernommen | Anmerkung |
|---|---|---|
| Energie 0–10, Start 5, Kill +1 / Tod −1 | ✔ | |
| Stufennamen Broken … Pristine +5 | ✔ | Optik in 4 Stufen |
| Kein Deathban | ✔ | |
| Abstufung der Kräfte nach Energie | geändert | 1–2: nur Primär · 0: alles aus |
| Tier 2 per Upgrader | ✔ | Tier 2 = Ultimate; Rezept ohne Fragments |
| Gem Trader | geändert | GUI mit freier Wahl, Originalrezept S3 |
| Gem Fragments | ✘ | |
| All-for-One-Gem | ✘ | |
| Energy Bottle | geändert | Abzapfen per Halten statt Befehl, kein Verfall |
| Repair Kit | geändert | platzierbares Totem, 15 Blöcke, 10 min, Koordinaten-Leak |
| Restoration/Revival | ✘ | vom Repair Kit abgedeckt |
| Server-Energie-Pool, Pedestal, Zeitfenster | ✘ | |
| Zufälliger Gem beim ersten Join | ✔ | auch für Spätstarter |
| Steuerung per Klick-Kombination | ✔ | + optionaler Keybind-Mod |

---

## 10. Konfigurationswerte (Vorschlag)

```yaml
energy:
  start: 5
  min: 0
  max: 10
  gainPerKill: 1
  lossPerDeath: 1
  naturalDeathCostsEnergy: true
  sameVictimCooldownMinutes: 30
  dropBottleWhenKillerAtMax: true
  secondaryMinEnergy: 3      # 1–2: nur Primär
  abilitiesMinEnergy: 1      # 0: alles aus
energyBottle:
  extractSeconds: 5
  expires: false
repairKit:
  radius: 15
  durationMinutes: 10
  restoreTo: 5
  announceCoordinates: true
trader:
  keepTier: true
  keepEnergy: true
  cooldownMinutes: 10
firstJoin:
  giveGem: true
  distribution: random       # random | balanced
```

---

## 11. Offene Fragen an den Owner

1. **Ultimate bei 1–2 Energie:** Fällt die Ultimate zusammen mit der Sekundärkraft weg (Annahme), oder bleibt sie bis 0?
2. **Passive bei 1–2 Energie:** Bleiben die Passiven bis 0 aktiv (Annahme)?
3. **Energy Bottle bei 0 Energie:** Darf ein Spieler mit 0 Energie eine Flasche trinken, oder hilft bei 0 nur das Repair Kit (Original: nur Ritual)?
4. **Passiv-Bedingung:** Gem nur in der Offhand (Original) oder irgendwo im Inventar?
5. **Repair-Kit-Totem:** Darf man es zerstören, bevor die 10 min um sind (z. B. 10 Treffer)? Zählt Tod oder Logout im Kreis als „Kreis verlassen“ (Annahme: ja)? Dürfen mehrere Totems gleichzeitig laufen?
6. **Kill mit 0 Energie:** Bekommt ein Spieler mit 0 Energie per Kill wieder Energie (Original: nein)?
7. **Upgrader- und Repair-Kit-Rezept:** Sind die Ersatzrezepte oben (Diamantblock statt Gem Fragment) in Ordnung?
