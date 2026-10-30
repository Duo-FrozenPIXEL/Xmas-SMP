# Bliss SMP – Verhalten des Originals (Recherche)

Stand der Recherche: 2026-10-29. Erledigt `todo.md` Abschnitt 2. Diese Datei beschreibt nur, was das Original (bzw. die bekannten Nachbauten) tut, und listet Abweichungen des Xmas-SMP-Stands auf. Sie trifft keine Entscheidungen.

## 0. Wichtig vorab: Was ist „das Original“?

- Das Plugin der echten Bliss-SMP ist laut Fan-Wiki **Closed Source** und exklusiv für Bliss entwickelt. Es gibt keinen öffentlichen Quelltext und keine offizielle Dokumentation. Alles Öffentliche ist entweder **Fan-Wiki** (beobachtet aus den Videos und vom Server) oder ein **Community-Nachbau**.
- Die Mechanik **ändert sich pro Season**: Season 1 nutzte ein Herzen-Plugin (Lifesteal), ab Season 2 gibt es Gems und Energie, in Season 3 entfiel der Bann bei 0 Energie. Als Referenz gilt hier **Season 3** (aktuellste dokumentierte Fassung).
- Quellengruppen und ihre Verlässlichkeit:

| Kürzel | Quelle | Art | Bemerkung |
|---|---|---|---|
| **Wiki** | Bliss SMP Wiki (Miraheze), https://blisssmp.miraheze.org/ | Fan-Wiki zur echten SMP | Beste Quelle für das Original. Teils lückenhaft (Rezepte nur als Bild). |
| **Fandom** | https://blisssmp.fandom.com/ | Fan-Wiki | Beim Abruf HTTP 402; nur über Suchergebnis-Ausschnitte gesehen. Nicht selbst geprüft. |
| **GOAL** | https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md | Referenzdokument eines GitHub-Nachbaus (Referenzdokument zu Season 3) | Autor unbekannt, sekundär. Deckt sich mit dem Wiki, wo prüfbar. |
| **BlissGems** | Plugin „Bliss SMP plugin (BlissGems)“: https://modrinth.com/plugin/bliss-smp-plugin, Doku https://docs.xoperr.dev/plugins/blissgems/, Fabric-Port https://www.curseforge.com/minecraft/mc-mods/blissgems | Community-Nachbau (Paper/Purpur 1.21+, 26.1.x, 26.2) | **Kein** Original. Die Doku-Seiten widersprechen sich teils (siehe 1). |
| **UltimateBliss** | https://modrinth.com/plugin/ultimateblisssmp | Anderer Community-Nachbau | Eigene Regeln (3 Tiers, Reparatur-Shards). Nur zum Vergleich. |
| **Bliss SMP Mod** | https://modrinth.com/mod/bliss_smp | Alter Fabric-Mod (1.21.8 bis 1.21.11) mit anderen Gems (u. a. Wither) und Orb-Trader | Anderes System, hier nicht verwendet. |

Sicherheitsstufen in den Abschnitten unten:

- **bestätigt**: mindestens zwei voneinander unabhängige Quellen (z. B. Wiki und ein Nachbau), die sich nicht widersprechen
- **eine Quelle**: nur eine Seite sagt es
- **unklar**: Quellen widersprechen sich oder sagen nichts

Hinweis: GOAL stützt sich offenbar auf das Wiki; Übereinstimmung von GOAL und Wiki zählt deshalb nicht als zwei unabhängige Quellen.

---

## 1. Energiesystem

**Was das Original (Season 3) tut**

- Energie ist das „Lebenssystem“ der SMP und bestimmt die Stärke der Gems. Skala **0 bis 10**, also 11 Zustände.
- Der Startwert ist **5 = „Pristine“**.
- Stufen (Wiki „Energy Mechanics (S3)“ und „Energy“):

| Energie | Name | Wirkung laut Wiki |
|---|---|---|
| 10 | Pristine +5 | Höchste Stufe. Gem bekommt einen Verzauberungs-Glanz, Energie kann nicht mehr aufgenommen werden, wer so einen Spieler tötet, bekommt/erzeugt eine Energy Bottle (siehe 6) |
| 9, 8, 7, 6 | Pristine +4 … +1 | laut Wiki ohne Auswirkung auf Cooldowns oder Zugriff auf Kräfte |
| 5 | Pristine | Standard |
| 4 | Scratched | Cooldowns länger, Kräfte schwächer, manche Passiven fallen aus |
| 3 | Cracked | Cooldowns länger (eine Wiki-Seite nennt hier keine Wirkung) |
| 2 | Damaged (in anderen Quellen „Shattered“) | Kräfte (Fähigkeiten) nicht nutzbar |
| 1 | Ruined | Passiven nicht nutzbar |
| 0 | Broken | Gem nutzlos (siehe 2) |

- **Gewinn/Verlust**: Der Besitzer verliert bei seinem Tod **1** Energie, der Töter gewinnt **1** (Wiki „Energy Mechanics (S3)“). Die Wiki-Seite „Energy“ nennt die Beträge selbst nicht. Ob auch Tode ohne Gegner (Mob, Sturz, Lava) 1 Energie kosten, sagt keine Quelle ausdrücklich; UltimateBliss nennt nur den Tod durch einen anderen Spieler.
- **Energy Pool**: Die gesamte Energie, die nicht in Gems oder Flaschen steckt, liegt in einem serverweiten Pool (`/bliss pool` laut GOAL). Reparatur zieht aus dem Pool, abgelaufene Flaschen fließen dorthin zurück.
- Der Name der Stufe 2 ist uneinheitlich: Wiki „Damaged“, GOAL und UltimateBliss „Shattered“.
- Über 5 („Pristine +1 … +5“) steigt die Stärke laut Wiki **nicht**. UltimateBliss (150 % bei +5) und die xoperr-Doku (bis 150 %) behaupten das Gegenteil; sie sind Nachbauten.
- Die xoperr-Doku widerspricht sich selbst: Die Übersichtsseite nennt die Skala 0–10 mit „Ruined: Fähigkeiten gehen, Passiven nicht“, die „Energy“-Unterseite nutzt dagegen ein Prozent-System (BROKEN 0 %, DAMAGED 1–20 %, … PRISTINE+5 141–150 %) mit eigenen Verlustregeln (10 % pro Tod). Diese Unterseite ist für die Nachbildung des Originals **nicht verwertbar**.

**Quellen**: https://blisssmp.miraheze.org/wiki/Energy_Mechanics_(S3) · https://blisssmp.miraheze.org/wiki/Energy · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://docs.xoperr.dev/plugins/blissgems/ · https://modrinth.com/plugin/ultimateblisssmp

**Sicherheit**: Skala 0–10, Start 5, +1/−1 pro Kill/Tod: **bestätigt** (Wiki plus mehrere Nachbauten). Stufennamen 0, 1, 3, 4, 5, 6–10: **bestätigt**. Name der Stufe 2 und genaue Wirkung von Cracked/Scratched: **unklar**. Verlust bei Nicht-PvP-Toden: **unklar**.

**Abweichung Xmas-SMP** (Stand README/Config)

- Skala 0–10, Start 5, Gewinn 1 pro PvP-Kill, Verlust 1 pro Tod: gleich wie im Original (sofern die Wiki-Werte gelten).
- Xmas zeigt nur **4 Stufen**: zerbrochen (0), beschädigt (≥ 1), zerkratzt (≥ 4), makellos (≥ 7) (`energy.stages`). Das Original hat 11 Zustände mit Pristine ab **5**. **Folge:** Mit Startwert 5 beginnt ein Xmas-Spieler als „zerkratzt“, im Original als „Pristine“. – **Entscheidung des Owners nötig**
- Xmas verliert Energie bei **jedem** Tod (`deathLoss`, auch Mob/Sturz). Original: unklar (siehe oben). – **Entscheidung des Owners nötig**
- Xmas hat Anti-Farming (`sameVictimCooldownMinutes`, 30 Min.). In keiner Quelle belegt. – **Entscheidung des Owners nötig**
- Xmas skaliert die Fähigkeitsstärke linear von `minStrength` 0.6 bis 1.0 bei Energie 10. Original: Stufen wirken über Cooldown/Zugriff (Cooldown länger ab Scratched, Kräfte ab Damaged weg), kein stufenloser Skalierungsfaktor belegt. – **Entscheidung des Owners nötig**
- Der Energie-Pool existiert in Xmas nicht. – **Entscheidung des Owners nötig**

---

## 2. Verhalten bei 0 Energie („Broken“)

**Was das Original tut**

- Season 3: Das Gem ist **nutzlos**, der Besitzer nimmt **doppelten Schaden durch negative Gem-Effekte** anderer (Wiki „Energy“; GOAL bestätigt es).
- Das Gem **fällt auf Tier 1 zurück** (Wiki „Energy“ und „Energy Mechanics (S3)“).
- Ein Broken-Spieler kann **keine Energie aufnehmen**: weder Energy Bottles noch durch Kills (Wiki).
- **Kein Bann in Season 3.** In Season 2 galt: Wer alle Energie verliert, wird vom Server gebannt (Wiki „Energy“, „Season 2“; Season 3 hat den Deathban komplett entfernt).
- Reparatur: **Repair Kits helfen nicht** bei Broken. Einziger Weg ist das **Restoration Ritual** (siehe 7), das das Gem auf Pristine setzt und den **Gem-Typ neu auswürfelt**.
- Im Nachbau BlissGems galt als Bug (behoben in 5.1.7), dass Energy Bottles ein Broken-Gem wiederbeleben konnten, also ist das nicht gewollt.

**Quellen**: https://blisssmp.miraheze.org/wiki/Energy · https://blisssmp.miraheze.org/wiki/Energy_Mechanics_(S3) · https://blisssmp.miraheze.org/wiki/Season_2 · https://blisssmp.miraheze.org/wiki/Restoration_Item · https://modrinth.com/plugin/bliss-smp-plugin/version/5.1.7

**Sicherheit**: Fähigkeiten aus, kein Aufnehmen, Restoration als einziger Weg: **bestätigt**. Doppelter Schaden und Rückfall auf Tier 1: **eine Quelle** (Wiki, von GOAL wiederholt). Bann in Season 2 / keiner in Season 3: **bestätigt** innerhalb des Wikis (zwei Seiten); unabhängig nicht prüfbar.

**Abweichung Xmas-SMP**

- Xmas: bei „zerbrochen“ ruhen die Fähigkeiten, es gibt keinen Bann: gleich wie Season 3.
- Doppelter Schaden bei Broken, Rückfall auf Tier 1, Aufnahmesperre (bei Broken und bei Energie 10) sind in Xmas nicht umgesetzt. – **Entscheidung des Owners nötig**
- Der Weg zurück aus „zerbrochen“ (Restoration/Revival Tome) fehlt noch. – **Entscheidung des Owners nötig** (siehe 7)

---

## 3. Tier-System

**Was das Original tut**

- Zwei Tiers. Ein **Upgrader** (Custom-Item) hebt ein Gem von **Tier 1 auf Tier 2**.
- Tier 1: Passiven und eine Kraft; **Tier 2 schaltet die weiteren Fähigkeiten frei** (Wiki „Gem Items“ / „Upgraders“; GOAL: Tier 1 „Passiven + Primärkraft“, Tier 2 „zusätzliche Fähigkeiten“). Das Wiki formuliert die Aufteilung zwischen Tier 1 und Tier 2 uneinheitlich.
- **Tier-2-Besitzer** haben dauerhaft den **Glowing-Effekt** und einen **gelben Namen in der Tab-Liste** (Wiki „Gem Items“, GOAL).
- Optik: Tier 1 Rauten-Form, Tier 2 Sechseck-Form (GOAL; passt zu den Pack-Texturen `tier1`/`tier2`).
- Upgrader-Rezept Season 3 (ab Progression 3 craftbar): 4 Gem Fragments, 1 Wither-Skelett-Schädel, 4 Dragon's Breath (nur GOAL; das Wiki zeigt das Rezept nur als Bild und markiert es als ungeprüft). Davor existierten nur wenige Upgrader als Boss-Drops (Zahl widersprüchlich: 3 oder 4).
- Nachbau BlissGems: Tier 1 = primäre Fähigkeit, Tier 2 = zweite (Shift + Rechtsklick) plus weitere per Befehl, zusätzlich Auto-Verzauberungen (z. B. Schärfe V) und stärkere Passiven. Ein universeller Upgrader gilt für alle Gem-Typen. UltimateBliss hat drei Tiers (mit „Gem Polisher“) und ist nicht Original.

**Quellen**: https://blisssmp.miraheze.org/wiki/Gem_Items · https://blisssmp.miraheze.org/wiki/Upgraders · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://docs.xoperr.dev/plugins/blissgems/ · https://docs.xoperr.dev/plugins/blissgems/gems/

**Sicherheit**: Zwei Tiers, Upgrader, Tier 2 schaltet mehr Fähigkeiten frei: **bestätigt**. Glowing und gelber Tab-Name für Tier 2: **eine Quelle** (Wiki, GOAL wiederholt es). Upgrader-Rezept: **eine Quelle / unklar**.

**Abweichung Xmas-SMP**

- Xmas hat Tier 1–2 (Fähigkeit 2 ab Tier 2): grundsätzlich gleich.
- Tier lässt sich bisher nur per `/xmas set … tier` setzen; es gibt **keinen funktionierenden Upgrader**, kein Rezept. – **Entscheidung des Owners nötig** (Rezept, Beschaffung)
- Glowing und gelber Tab-Name für Tier 2 fehlen. – **Entscheidung des Owners nötig**
- Passiven je Tier, Auto-Verzauberungen: Kräfte werden selbst entworfen (todo Abschnitt 0 und 5), dort bereits entschieden.

---

## 4. Gem Trader

**Was das Original tut**

- Der Trader ist ein Custom-Item und **würfelt den Gem-Typ des Spielers zufällig neu** (Wiki „Traders“).
- Season 2: Rezept 4 Diamantblöcke + 1 Sculk-Katalysator. Spieler nutzten „Cycling“, also schnelles Durchtauschen, um Fähigkeiten verschiedener Gems zu überlappen.
- Season 3: Rezept **4 Diamantblöcke, 4 Dragon's Breath, 1 Sculk-Katalysator**. Eine **lange Animation zwischen den Tauschvorgängen** verhindert Cycling. Neu: **Tausch mit einem vertrauten Spieler** und **Energie opfern, um die Chance auf einen bestimmten Gem zu erhöhen** (beides laut Wiki wenig bekannt, nur kurz erwähnt).
- Der Wither-Boss in Progression 2 ließ 8 Trader fallen (GOAL).
- **Kosten/Token/Station**: Es gibt in den Quellen **keinen Trader-Token** und **keine Trader-Station**. Der Trader wird als Item benutzt. Der „Energy Token“ (aus Energy Bottles, siehe 6) ist ein anderes Ding (nur für Dorfbewohner-Handel). Die „Pedestal“-Station dient den Reparatur- und Restoration-Ritualen, nicht dem Trader. Die „Orb Trader“ im alten Fabric-Mod sind Dorfbewohner-Händler für Orbs, nicht der Gem Trader.
- Nachbauten: BlissGems „würfelt ein anderes Gem aus“; UltimateBliss „behält Tier und Energie“. Für das echte Original gibt es dazu keine Angabe.

**Quellen**: https://blisssmp.miraheze.org/wiki/Traders · https://blisssmp.miraheze.org/wiki/Gem_Items · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://www.curseforge.com/minecraft/mc-mods/blissgems · https://modrinth.com/plugin/ultimateblisssmp

**Sicherheit**: Zufälliger Typ-Tausch und Rezept Season 3: **bestätigt** (Wiki Traders und Gem Items). Animation gegen Cycling: **bestätigt** (Wiki). Tausch mit vertrauten Spielern / Energie opfern: **eine Quelle**, Details **unklar**. Ob Tier/Energie erhalten bleiben: **unklar** für das Original.

**Abweichung Xmas-SMP**

- Gem Trader ist nur als Item vorhanden (gebbar), ohne Funktion. – noch offen (todo 4.4)
- Instructions.md verlangt „den gem trader aus dem Original“. Ob Xmas zufällig tauscht, ein GUI zur Auswahl bietet (todo 4.4 nennt „GUI oder zufällig, konfigurierbar“), Tier/Energie erhält und wie das Rezept aussieht: **Entscheidung des Owners nötig**
- Anti-Cycling-Animation, Tausch mit Vertrauten, Energie-Opfer: **Entscheidung des Owners nötig**
- Optionale „Trader-Station“ (todo 4.4): kein Vorbild im Original gefunden.

---

## 5. Gem Fragments

**Was das Original tut**

- Craft-Zutat. Rezept Season 3: **4 Diamanten, 2 Amethyst-Cluster, 2 Smaragde, 1 Eisenblock** (Wiki „Gem Fragment“; GOAL gleich).
- Verwendung:
  - **Repair Kit** (S3: 4 Fragments, siehe 7): bestätigt (Wiki)
  - **Restoration Core** (4 Fragments): bestätigt (Wiki „Gem Items“, GOAL)
  - **Trader**: Wiki „Gem Fragment“ nennt ihn, das Trader-Rezept (Wiki „Traders“, GOAL) enthält aber kein Fragment → **widersprüchlich**
  - **Upgrader**: nur GOAL (4 Fragments im Rezept)
- Season 2 hatte „Wire Fragments“ (7 Stück, Finale, Gold Gem). Das ist ein Event-Item und etwas anderes.

**Quellen**: https://blisssmp.miraheze.org/wiki/Gem_Fragment · https://blisssmp.miraheze.org/wiki/Gem_Items · https://blisssmp.miraheze.org/wiki/Season_2_Items · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md

**Sicherheit**: Rezept und Verwendung für Repair Kit/Restoration Core: **bestätigt**. Verwendung für Trader/Upgrader: **unklar**.

**Abweichung Xmas-SMP**

- Fragment ist nur ein gebbares Item ohne Rezept. – **Entscheidung des Owners nötig** (Rezept, Verwendung; todo 4.4)

---

## 6. Energy Bottle (und Energy Token)

**Was das Original tut**

- Eine Flasche enthält **1 Energie**.
- Entstehung: (a) Wer von einem Spieler auf **Pristine +5** getötet wird, lässt eine Flasche fallen; die Wiki-Seite „Energy“ nennt ungenauer Gems auf Broken oder Pristine +5 als Auslöser. (b) Befehl `/bliss withdraw` entnimmt dem eigenen Gem 1 Energie als Flasche („abzapfen“).
- Benutzung: **Rechtsklick nimmt die Energie auf**. Das geht **nicht** bei Pristine +5 und nicht bei Broken.
- Die Flasche **läuft nach 1 Stunde ab** (Warnton); die Energie geht zurück in den Pool.
- Linksklick auf die Flasche macht daraus einen **Energy Token** (hellgrün, läuft nie ab, nur für Händler-Dorfbewohner, nicht zurückwandelbar; z. B. 4 Tokens je Netherit-Rüstungsteil/-Waffe, 8 je Mace laut GOAL).
- Übertragen an andere: Es gibt laut Wiki keinen direkten Transfer; die Energie wandert über Flaschen, Pool und Rituale (Restoration: 5 Flaschen).
- Nachbau BlissGems: gleiche Quellen/Regeln für Flasche und `/bliss withdraw`, ergänzt durch ein Menü mit „Withdraw“-Knopf.

**Quellen**: https://blisssmp.miraheze.org/wiki/Gem_Items · https://blisssmp.miraheze.org/wiki/Energy · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://www.curseforge.com/minecraft/mc-mods/blissgems

**Sicherheit**: Flasche = 1 Energie, Rechtsklick, 1 h Ablauf, `/bliss withdraw`: **bestätigt**. Drop-Bedingung: **unklar** (Pristine +5 vs. „Broken oder Pristine +5“). Energy Token: **eine Quelle** (GOAL; Wiki erwähnt Tokens an der geprüften Stelle nicht).

**Abweichung Xmas-SMP**

- Flasche ist nur ein gebbares Item (todo 4.2: abfüllen/übertragen offen). – **Entscheidung des Owners nötig** (Abfüllen per Befehl wie `/bliss withdraw`, Ablaufzeit, Energy Token, Drop bei Maximum)
- Bei Energie 10 (Maximum) gibt es in Xmas keine Sperre und keinen Flaschen-Drop. – **Entscheidung des Owners nötig**

---

## 7. Repair Kit, Restoration/Revival

**Repair Kit – was das Original tut (Season 3)**

- Wird in einem **Repair Ritual am Pedestal** verbraucht. Das Ritual hebt die Gems **aller Teilnehmer in der Nähe** bis höchstens **Pristine** an und verbraucht dafür **bis zu 10 Energie aus dem Pool**. Wer die wenigste Energie hat, kommt zuerst dran.
- Es endet, wenn alle Pristine sind, 10 Energie verbraucht sind oder das Pedestal zerstört wird. Nicht verbrauchte Energie kommt als Repair Kit mit entsprechender Kapazität zurück.
- **Broken-Gems lassen sich so nicht reparieren.**
- Rezept Season 3: 4 Gem Fragments, 2 Ambosse, 2 Netherit-Barren, 1 Netherit-Aufwertungsvorlage (Season 2: 4 End-Kristalle, 2 Netherit-Barren, 2 Fragments, 1 Upgrader). Das Wiki vermerkt, dass sich das Rezept mehrfach geändert hat.
- Das **Pedestal** ist die Ritual-Station (Welt-Spawn der SMP): Es war in Season 3 nur **1 Stunde mittwochs, samstags und sonntags** offen, ein Ritual wird mit Beacon und Ancient Debris gestartet (Wiki „Pedestal“, „Energy“). Das ist serverspezifisch.
- Nachbau BlissGems: Ein Repair Kit auf einem Beacon baut ein Pedestal, das die Energie aller in der Nähe auffüllt. UltimateBliss nutzt „Reparation Shard“ (+1) und „Pedestal“ (+5) und ist nicht Original.

**Restoration Ritual (= Wiederherstellung eines Broken-Gems)**

- Benötigt: 1 **Restoration Item**, 1 Beacon, 1 Block Ancient Debris und **5 Energy Bottles** (Wiki „Restoration Item“, GOAL; das Wiki „Energy“ beschreibt das Einzahlen von 5 Energie durch Spieler). Es entnimmt **nichts** aus dem Pool.
- Wirkung: Das Gem wird **Pristine** und der **Gem-Typ wird zufällig neu bestimmt**. Wetter wechselt zu Regen, dann Gewitter; der Starter schwebt, Umstehende werden zurückgestoßen.
- Restoration Item (S3): ein **Restoration Core** (4 Gem Fragments, 2 Drachenköpfe, 2 Repair Kits, 1 Wiederherstellungskompass) plus 8 passend gefärbte Kerzen. In Season 2 war es nicht craftbar, es existierte genau eines.
- Einen **Wiederbelebungsmechanismus für gebannte Spieler** gibt es in Season 3 nicht (kein Deathban). GOAL erwähnt Revive-Bücher nur als Boss-Drop (2 Stück, Progression 2), ohne Wirkung zu nennen.
- Nachbau BlissGems nutzt zwei getrennte Dinge: ein **Restoration Book** (Broken-Gem wird neues zufälliges Gem) und einen **Revive Beacon** (holt einen toten Spieler zurück). Das ist wohl der Ursprung des Namens „Revival Tome“ im Pack, das Original-Wiki kennt dieses Item aber nicht unter diesem Namen.

**Quellen**: https://blisssmp.miraheze.org/wiki/Repair_Kit · https://blisssmp.miraheze.org/wiki/Restoration_Item · https://blisssmp.miraheze.org/wiki/Pedestal · https://blisssmp.miraheze.org/wiki/Energy · https://blisssmp.miraheze.org/wiki/Gem_Items · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://www.curseforge.com/minecraft/mc-mods/blissgems · https://modrinth.com/plugin/bliss-smp-plugin/version/V3.5

**Sicherheit**: Repair Ritual (bis 10 aus dem Pool, bis Pristine, nicht für Broken): **bestätigt**. Restoration (5 Flaschen, zufälliger neuer Typ, Pristine): **bestätigt**. Genaue Rezepte: **eine Quelle** je Season, ändern sich. Name/Funktion „Revival Tome“ im Original: **unklar**.

**Abweichung Xmas-SMP**

- Repair Kit, Revival Tome: nur gebbare Items, keine Funktion (todo 4.2). – **Entscheidung des Owners nötig** (Ritual/Pedestal oder einfachere Nutzung; was „Revival Tome“ genau tut; ob Pool, Gem-Typ neu würfeln, Rezepte)
- Instructions.md erwähnt keine Revive-Mechanik. Ob die Bann-/Revive-Variante (Season 2) oder die Restoration-Variante (Season 3) gelten soll: **Entscheidung des Owners nötig**

---

## 8. Aktivierung der Fähigkeiten und Cooldown-Anzeige

**Was bekannt ist**

- Für die **echte SMP** nennen Wiki und GOAL keine Tastenbelegung. Aus GOAL: Passiven wirken nur, wenn das Gem in **Off- oder Mainhand** liegt; Conduction (Flux) nutzt Schleichen + Linksklick; Fire/Flux-Fähigkeiten werden aufgeladen.
- **Nachbau BlissGems** (Quellen sind konsistent):
  - Gem in der **Zweithand** = Passiven. Fähigkeiten werden mit dem Gem in der Hand ausgelöst: **Rechtsklick = Primär**, **Shift + Rechtsklick = Sekundär** (Tier 2), Tertiär/Quartär per Befehl (`/bliss ability:tertiary`). Klick-Aktivierung ist abschaltbar.
  - Für Fire und Flux: Rechtsklick startet das Aufladen, ein zweiter Rechtsklick feuert; Anzeige des Ladestands in der Actionbar.
  - Optional ein clientseitiger Mod (BlissKeyBinds) mit Standardtasten **V, R, G, X** (Primär bis Quartär) und weiteren für Zusatzfunktionen; das Mod schickt nur den passenden `/bliss`-Befehl. Die Fabric-Mod-Variante bietet zusätzlich Klick-Kombinationen (Rechtsklick, Shift + Rechtsklick, Schlag, Shift + Schlag) und ein Menü auf **B**.
  - Neuere Versionen belegen laut einer Marktplatz-Beschreibung außerdem Linksklick/Shift + Linksklick für weitere Slots und **F / Shift + F** für Gold-Gem-Funktionen (nur ein Suchergebnis-Ausschnitt, nicht selbst geprüft).
- **Cooldown-Anzeige**: Im Nachbau BlissGems in der **Actionbar** (xoperr-Doku; Changelog 5.1.7 bestätigt für eine Fähigkeit). Dass die echte SMP die Actionbar mit **Font-Icons** nutzt, ist durch keine gefundene Web-Quelle belegt. Das Pack enthält eine `font/default.json`, die das nahelegt, das ist aber nur Hinweis aus dem Repo.
- Trusted-Spieler-System (kein Schaden unter Vertrauten): Nachbau `/bliss trust`, im echten Wiki beim Trader/Tausch erwähnt („trusted player“).

**Quellen**: https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://docs.xoperr.dev/plugins/blissgems/gems/ · https://docs.xoperr.dev/plugins/blissgems/ · https://modrinth.com/project/68OcxWhi · https://www.curseforge.com/minecraft/mc-mods/blissgems · https://modrinth.com/plugin/bliss-smp-plugin · https://modrinth.com/plugin/bliss-smp-plugin/version/5.1.7

**Sicherheit**: Rechtsklick/Shift + Rechtsklick im Nachbau: **bestätigt** (mehrere Seiten desselben Projekts). Verhalten der echten SMP: **unklar** (nicht dokumentiert). Cooldown in der Actionbar: **eine Quelle**.

**Abweichung Xmas-SMP**

- Xmas löst Fähigkeiten über **F** (Zweithand-Taste, Fähigkeit 1) und **Schleichen + F** (Fähigkeit 2) aus, ohne Client-Mod und ohne Rechtsklick. Der Nachbau nutzt Rechtsklick/Shift + Rechtsklick (und optional Client-Tasten). – **Entscheidung des Owners nötig** (todo 4.5 hat es als erledigt markiert, in Abschnitt 0 steht dazu keine Entscheidung)
- Xmas-Actionbar-HUD mit Icons, Cooldown-Sekunden, Tier-Sperre, Energie: deckt sich in der Absicht mit der Actionbar-Anzeige des Nachbaus.
- Tertiäre/Quartäre Fähigkeiten gibt es im Entwurf nicht (zwei Fähigkeiten je Kristall).

---

## 9. Startverhalten

**Was bekannt ist**

- Nachbau BlissGems (Changelog V3.5, Listener `PlayerJoinListener`): Neue Spieler bekommen beim Betreten **ein zufälliges Gem mit Ritual-Animation**.
- GOAL (Season 3): „Jeder Spieler erhält beim Betreten ein zufälliges Gem“, Start-Energie 5.
- Die Fabric-Variante von BlissGems verteilt die Gems nicht beim Beitritt, sondern per Admin-Befehl `/bliss smp start`; die xoperr-Doku nennt `/bliss give` als Befehl.
- Eine Wiki-Beschreibung, wie die echte SMP die Gems zu Beginn verteilte, gibt es nicht (Seiten „Season 3“, „Season 2“, „Gem Information“ schweigen dazu).
- Einschränkungen im Nachbau (xoperr-Doku): Gem lässt sich nicht droppen, nicht in Behälter oder Item-Rahmen legen; pro Spieler nur ein Gem; Gems bleiben beim Tod erhalten (bei Respawn zurück).

**Quellen**: https://modrinth.com/plugin/bliss-smp-plugin/version/V3.5 · https://github.com/ItzJustNett/blissgems/blob/main/GOAL.md · https://www.curseforge.com/minecraft/mc-mods/blissgems · https://docs.xoperr.dev/plugins/blissgems/

**Sicherheit**: Zufälliges Gem beim Betreten im Nachbau: **bestätigt** (zwei Quellen, von denen eine nur sekundär). Im echten Original: **unklar**. Unlösbarkeit des Gems: **eine Quelle**.

**Abweichung Xmas-SMP**

- Xmas gibt beim ersten Betreten ein zufälliges Gem (`crystalItem.giveOnJoin`), seelengebunden: gleich wie im Nachbau. Ob ohne Ritual-Animation: Die Animation fehlt in Xmas. – **Entscheidung des Owners nötig**

---

## 10. Gesamtübersicht der Abweichungen

„Entschieden“ bedeutet: Es steht in `todo.md` Abschnitt 0.

| Thema | Original / Nachbau | Xmas-SMP | Status |
|---|---|---|---|
| 5 Kristalle statt 8 Gems, Namen Feuer/Erde/Wind/Schatten/Natur, Textur-Zuordnung | 8 Gems (Astra, Fire, Flux, Life, Puff, Speed, Strength, Wealth) | 5 Kristalle, Texturen gemappt | Entschieden (Abschnitt 0) |
| Eigene Kräfte statt der Original-Kräfte | Kräfte des Originals | Selbst entworfen | Entschieden (Abschnitt 0) |
| Startenergie und Stufenschwellen | Start 5 = Pristine, 11 Zustände | Start 5 = „zerkratzt“, 4 Stufen (Pristine ab 7) | Entscheidung des Owners nötig |
| Tod ohne Gegner kostet Energie | unklar | ja | Entscheidung des Owners nötig |
| Anti-Farming gegen denselben Gegner | nicht belegt | 30 Min. | Entscheidung des Owners nötig |
| Stufenlose Stärke 0.6 bis 1.0 | Wirkung über Cooldown/Zugriff, ab +1 keine Wirkung | linear nach Energie | Entscheidung des Owners nötig |
| Energy Pool | vorhanden (S3) | fehlt | Entscheidung des Owners nötig |
| Broken: doppelter Schaden, Rückfall auf Tier 1, Aufnahmesperre | ja (Wiki) | nicht umgesetzt | Entscheidung des Owners nötig |
| Max-Energie: Aufnahmesperre, Flaschen-Drop, Glanz | ja | nicht umgesetzt | Entscheidung des Owners nötig |
| Upgrader (Rezept, Beschaffung), Glowing/Tab-Name Tier 2 | ja | nur Item/Befehl | Entscheidung des Owners nötig |
| Gem Trader (zufällig, Animation, Tausch mit Vertrauten, Energie opfern) | ja | nur Item | Entscheidung des Owners nötig |
| Gem Fragments und Rezepte | ja | nur Item | Entscheidung des Owners nötig |
| Energy Bottle, `/bliss withdraw`, Ablauf 1 h, Energy Token | ja | nur Item | Entscheidung des Owners nötig |
| Repair Kit mit Ritual am Pedestal | ja | nur Item | Entscheidung des Owners nötig |
| Restoration/Revival (Broken-Gem wiederherstellen, ggf. Revive) | Restoration Ritual (S3) | nur Item „Revival Tome“ | Entscheidung des Owners nötig |
| Aktivierung | Nachbau: Rechtsklick/Shift + Rechtsklick (+ optionale Client-Tasten); Original unklar | F / Schleichen + F | Entscheidung des Owners nötig |
| Zufälliges Gem beim Betreten, Seelenbindung | Nachbau ja | ja | gleich (Animation fehlt: Entscheidung des Owners nötig) |

## 11. Offene Lücken der Recherche

- Das Fandom-Wiki war nicht abrufbar (HTTP 402); nur Suchausschnitte.
- Kein Video, Reddit oder Discord wurde ausgewertet (Suchmaschine lieferte keine Reddit-Treffer). Das tatsächliche Aktivierungsverhalten und die Cooldown-Anzeige der echten SMP bleiben daher unbelegt. Falls das wichtig ist, bräuchte es eine Sichtung von VOD-Material.
- Rezepte sind im Wiki oft nur als Bild hinterlegt (nicht auslesbar).
- Die Mechanik wurde in Season 3 laufend angepasst („Progression“-Phasen); einzelne Werte können je Phase abweichen.
