# Xmas-SMP – Kräfte-Entwurf (alle 25 Fähigkeiten)

**Status: Entwurf, wartet auf Freigabe des Owners.** Stand 2026-10-30.
Grundlage: `todo.md` Abschnitt 0 und 5, `docs/implementierungsplan.md` Abschnitt 1 (Energie- und Tier-Regeln), Effekt-Engine in `common/src/main/java/de/xmassmp/effect/`.
„Aktiv 1“ aus `todo.md` heißt hier **Primär**, „Aktiv 2“ heißt **Sekundär**. Die fünf **Ultimates** sind neu.

Legende: **T** = Server-Tick (20 T = 1 s) · **♥** = Herz (= 2 Lebenspunkte, Rohschaden vor Rüstung) · **(V)** = ID bzw. Verhalten vor Umsetzung im Spiel prüfen · **[E]** = braucht eine Erweiterung der Effekt-Engine (siehe 1.6)

---

## 1. Gemeinsame Regeln

### 1.1 Freischaltung und Energie (vom Owner festgelegt)
| Energie | Primär | Sekundär | Ultimate | Passive |
|---|---|---|---|---|
| 0 (Broken) | – | – | – | **ja** |
| 1–2 | ja | – | – | ja |
| ≥ 3 | ja | ja | nur mit **Tier 2** | ja |

- Passive T1 gilt ab Tier 1, Passive T2 zusätzlich ab Tier 2. Passive wirken **immer**, auch bei 0 Energie, und egal wo der Kristall im Inventar liegt. Sie skalieren **nicht** mit der Energie (feste Werte), weil `strength` bei 0 Energie 0 ist.
- Stärke: `AbilityContext.strength` liegt mit der aktuellen Config zwischen **0,6** (Energie 1) und **1,0** (Energie 10). Alle Zahlen unten gelten für **strength = 1,0**.
  - Schaden: `scaledDamage(base)` = Basis × `damageMultiplier` × strength.
  - Effektdauern (Brand, Langsamkeit, Fessel …): × strength.
  - Radien und Reichweiten: × (0,8 + 0,2 × strength), damit die Kraft auch bei wenig Energie noch trifft (Faktor 0,92 bei Energie 1).
  - Cooldowns skalieren **nicht** mit der Energie (nur `cooldownMultiplier` und Events).
- Hinweis zum Code-Stand: `AbilityService` kennt bisher nur Slot 1 und 2 und sperrt Sekundär nicht unter 3 Energie. Slot 3 (Ultimate) und die Energie-Sperre gehören zu Phase 7/8.

### 1.2 Cooldown-Größenordnung je Slot
| Slot | Cooldown | Rolle |
|---|---|---|
| Primär | **15–20 s** | Bewegungs- oder Kontrollwerkzeug, häufig nutzbar, wenig Schaden |
| Sekundär | **35–45 s** | Hauptschaden oder Team-Nutzen, klar telegraphiert |
| Ultimate | **180 s** | Kampfentscheidend, lange Vorwarnung, deutlicher Konter |

Primär und Sekundär haben getrennte Cooldowns. Die Ultimate hat zusätzlich eine **globale Sperre von 10 s** nach dem Respawn und nach dem Einloggen (kein Ultimate-Spam beim Wiedereinstieg).

### 1.3 Telegraphing (Vorwarnung für Gegner)
- Jede Kraft, die Schaden macht oder Kontrolle ausübt, hat eine **sichtbare und hörbare Aufladephase**: Primär mindestens **4–10 T**, Sekundär mindestens **12–30 T**, Ultimate mindestens **30 T (1,5 s)**.
- Flächenwirkungen zeigen ihre **Fläche vor dem Auslösen am Boden** (Ring aus `dust` in Kristallfarbe). Was außerhalb des Rings steht, wird nicht getroffen.
- Ultimates: Beim Start bekommen alle Spieler im Umkreis von **48 Blöcken** einen Warnsound (Ebene 1 im Sound-Stack, Lautstärke 2,0 → große Hörweite) und, wer im Wirkradius + 4 steht, ein Actionbar-Symbol „⚠“ in der Kristallfarbe.
- **Unterbrechung:** Nimmt der Anwender während der Ultimate-Aufladung **≥ 3 ♥** Schaden durch Spieler, bricht die Ultimate ab und bekommt nur **50 %** des Cooldowns. Primär und Sekundär sind nicht unterbrechbar (zu kurz).

### 1.4 PvP-Fairness
- Jedes Ziel wird von einer Wirkung (Welle, Strahl, Ring) **höchstens einmal** getroffen, außer es steht ausdrücklich anders da (z. B. Tornado-Ticks).
- **Obergrenze Burst:** Eine Ultimate macht einem einzelnen Ziel höchstens **8 ♥** Rohschaden. Sekundär höchstens **5 ♥**, Primär höchstens **3 ♥**.
- Schaden läuft über einen eigenen Damage-Type `xmassmp:crystal` (per Datapack, für Vanilla-Clients unproblematisch **(V)**), der von **Rüstung und Schutz** reduziert wird und Kills korrekt dem Anwender zuschreibt (Energie +1).
- Kontrolle (Festhalten, Hochwerfen, Verlangsamen) bekommt **Diminishing Returns**: Wer in den letzten 6 s schon kontrolliert wurde, bekommt nur die halbe Dauer; ein dritter Effekt in diesem Fenster wirkt nicht.
- Schild: Ein erhobener Schild blockt Direkttreffer von Projektil-Kräften (Felsbrocken, Feuerstrahl) frontal, wie bei Vanilla-Projektilen.
- Keine Wirkung auf den Anwender selbst und (Vorschlag, siehe Frage 2) nicht auf Verbündete.

### 1.5 Welt- und Serverschutz
- **Keine Block-Zerstörung, kein Platzieren echter Blöcke.** Alle „Felsen, Säulen, Ranken, Bäume“ sind Block-/Item-Displays ohne Hitbox und verschwinden mit dem Effekt (siehe `DisplaySpec`).
- Kein echtes Feuer: Brand wird nur auf Entities gesetzt (`setOnFire`), Feuer-„Felder“ sind reine Partikel.
- Spawn-Schutz und Claims: Ein Ziel in geschützter Zone wird nicht getroffen. Steht der **Anwender** in geschützter Zone, startet die Kraft nicht (kein Cooldown-Verbrauch, Meldung „Hier nicht möglich“). Der Mittelpunkt einer gezielten Kraft (Meteor, Inferno-Sonne …) darf ebenfalls nicht in geschützter Zone liegen.
- Teleporte (Schattenschritt, Böe-Dash) prüfen Kollision und Sichtlinie, landen nie in Blöcken und nie in geschützten Zonen.
- Mobs (außer Spielern) werden normal getroffen; Haustiere anderer Spieler und Dorfbewohner nicht (Config `abilities.common.hitPassiveMobs`).

### 1.6 Partikel- und Display-Budget
- Global gilt `effects.maxParticlesPerTick` (Standard 3000) und `effects.maxDisplays` (256). Die Qualität (`LOW/MEDIUM/HIGH`) dünnt Formen aus.
- **Richtwerte pro Anwendung bei HIGH** (Spitze in einem Tick / Durchschnitt):
  | Slot | Partikel Spitze | Partikel Ø/Tick | Displays |
  |---|---|---|---|
  | Primär | ≤ 180 | ≤ 60 | ≤ 8 |
  | Sekundär | ≤ 260 | ≤ 90 | ≤ 12 |
  | Ultimate | ≤ 400 | ≤ 150 | ≤ 32 |
- Sichtweite: normale Partikel gehen an Spieler im Umkreis von 32 Blöcken. **Telegraph-Ringe der Ultimates** werden mit „force/long distance“ geschickt, damit sie bis 64 Blöcke sichtbar sind **[E]**.
- **Engine-Erweiterungen, die dieser Entwurf voraussetzt [E]:**
  1. `ParticleSpec` mit Block-Zustand für `minecraft:block`, `minecraft:falling_dust`, `minecraft:block_crumble` **(V, ab 1.21.2)** und `minecraft:dust_pillar` (z. B. Untergrund-Block unter dem Ziel).
  2. Optional `minecraft:trail` **(V, ab 1.21.2; braucht Ziel, Farbe, Dauer)** für Strahlen.
  3. `DisplaySpec` mit Block-Zustand (z. B. `pointed_dripstone[thickness=tip]`, `pink_petals[flower_amount=4]`).
  4. `EffectSink` mit „force“-Flag für weite Sichtbarkeit.
  5. Interaction-Entity (unsichtbare Hitbox) für den Weltenbaum.
- Farben (aus `Crystals.java`): Feuer `#FF3B1F → #FFB400`, Erde `#E07A1F → #6B4423`, Wind `#F2F7FF → #9FD8FF`, Schatten `#8E3BFF → #2A0B4F`, Natur `#3FD45A → #FF9FD6`.

### 1.7 Animations-Grundmuster
Jede aktive Kraft ist eine `EffectTimeline` mit drei Phasen wie in `StageEffects`:
1. **Aufladen** (Easing `EASE_IN`): Energie sammelt sich, Form zieht sich zusammen oder baut sich auf, leiser werdender Grundton.
2. **Auslösen** (1–3 T): Burst aus mindestens zwei Partikeltypen, Sound-Stack aus 2–3 Ebenen mit Pitch-Variation ±0,05–0,15.
3. **Nachklingen** (Easing `EASE_OUT`): Reste (Glut, Staub, Blüten, Rauch) sinken, Displays schrumpfen und verschwinden.

Sound-Stacks werden unten als `ID (Lautstärke, Pitch ±Variation, +Verzögerung T)` angegeben.

---

## 2. Feuer (rot, Strength-Textur)

Leitmotiv: Glut, Funken, aufsteigende Hitze. Farbverlauf rot → goldgelb, Akzent `soul_fire_flame` sparsam.

### 2.1 Passiv T1 – Glutherz
- Dauerhafte **Feuerresistenz** (Effekt ohne Partikel, Icon sichtbar).
- Optik: keine (Passive sollen im Kampf nicht verraten).

### 2.2 Passiv T2 – Glutklinge
- Nahkampftreffer setzen das Ziel **3 s** in Brand (verlängert vorhandenen Brand nicht über 3 s hinaus). Pro Ziel höchstens alle **4 s**.
- Optik beim Auslösen: 6× `minecraft:flame` + 4× `minecraft:dust` (#FF3B1F, 0,8) am Trefferpunkt; Sound `minecraft:item.firecharge.use (0,4; 1,4 ±0,1)`.

### 2.3 Primär – Flammenstoß
| | |
|---|---|
| Wirkung | Sprung (0,9 nach oben, 0,4 nach vorn). Bei der Landung (spätestens nach 30 T) **expandierender Feuerring** bis Radius **5**. Getroffen wird, wer am Boden ist, wenn die Ringfront ihn erreicht: **2,5 ♥** + **4 s** Brand + Rückstoß 0,6. Kein Fallschaden für den Anwender bei dieser Landung. |
| Cooldown | **18 s** |
| Aufladen 0–5 T | Flammenspirale um die Füße (`Shapes.spiral` r 0,8, 1,2 hoch, 18 Punkte): `minecraft:flame` + `minecraft:dust_color_transition` (#FF3B1F→#FFB400, 1,0). Ab T 3: 4× `minecraft:small_flame` steigen auf. |
| Flug | Alle 2 T Spur am Körper: `minecraft:flame` (2), `minecraft:smoke` (1), `minecraft:dust` (#FFB400, 0,7). |
| Auslösen (Landung, 1 T) | `minecraft:explosion` (1) am Boden, `minecraft:lava` (8, Streuung 1,0), Sphere-Burst aus `minecraft:dust_color_transition` (r 0,6, 20 Punkte). |
| Ring 0–10 T (`EASE_OUT`) | Ring r 0,5 → 5, 40 Punkte: abwechselnd `minecraft:flame` und `minecraft:dust_color_transition` (1,4); jeder 5. Punkt `minecraft:soul_fire_flame` als Akzent; an der Front 1× `minecraft:landing_lava`. |
| Nachklingen 20 T | Glutfeld: 3 zufällige Punkte/T im Radius `minecraft:small_flame`, alle 4 T `minecraft:campfire_cosy_smoke` (1) steigt auf. |
| Displays | 8× `minecraft:magma_block` (Block, Maßstab 0,3, fullBright) springen auf dem Ring 0,6 hoch und schrumpfen in 12 T auf 0. |
| Sounds | Start: `minecraft:entity.blaze.shoot (0,9; 0,8 ±0,1)`, `minecraft:item.firecharge.use (0,6; 1,2 ±0,1; +2)`. Landung: `minecraft:entity.generic.explode (0,6; 1,4 ±0,1)`, `minecraft:entity.ghast.shoot (0,7; 0,7 ±0,05)`, `minecraft:block.lava.pop (0,8; 1,0 ±0,2; +4)`. |
| Konter | Der Sprung ist 1–1,5 s vorher sichtbar; der Ring braucht 10 T bis zum Rand → herauslaufen oder **im richtigen Moment springen** (in der Luft wird man nicht getroffen). Wasser/Feuerresistenz-Trank neutralisiert den Brand. |

### 2.4 Sekundär – Meteor
| | |
|---|---|
| Wirkung | Zielpunkt = angeschauter Block bis **30** Blöcke (sonst 30 Blöcke vor dem Spieler auf Bodenhöhe). Nach **30 T** Vorwarnung schlägt ein Meteor ein: **4,5 ♥** im Zentrum (r ≤ 1,5), abfallend auf **2 ♥** am Rand (r **4**). Brand **5 s**, Rückstoß 0,8 vom Zentrum weg + 0,4 hoch. Danach 3 s Glutfeld (nur Optik). |
| Cooldown | **40 s** |
| Aufladen 0–29 T | Zielkreis am Boden (r 4, 32 Punkte, `minecraft:dust` #FF3B1F 1,2), dreht sich; innen ein schrumpfender zweiter Ring (r 4 → 0,5) aus `minecraft:dust_color_transition`. Alle 3 T `minecraft:lava` (1) im Kreis. Am Himmel (20 Blöcke über Ziel) ab T 0 sichtbar: Meteor erscheint. |
| Fall 10–29 T (`EASE_IN`) | Meteor-Display fällt von +20 auf 0 (diagonal, 15° aus Blickrichtung des Anwenders). Spur je T: `minecraft:large_smoke` (3), `minecraft:flame` (4, Streuung 0,4), `minecraft:dust_color_transition` (#FFB400→#FF3B1F, 1,6, 3), alle 2 T `minecraft:lava` (1). |
| Auslösen T 30 | `minecraft:explosion_emitter` (1), Ring r 0 → 4 in 6 T aus `minecraft:flame` + `minecraft:dust` (1,6), `minecraft:lava` (16, Streuung 1,5), `minecraft:crit` (24, Geschwindigkeit 0,8). |
| Nachklingen 60 T | Glutfeld: `minecraft:small_flame` (2/T zufällig in r 3), alle 5 T `minecraft:campfire_signal_smoke` (1) aus dem Zentrum, `minecraft:ash` (2/T). Meteor-Rest (Display) sinkt 0,5 ein, wird dunkler (`minecraft:magma_block` → nach 20 T gegen `minecraft:blackstone` getauscht) und schrumpft in den letzten 20 T. |
| Displays | Meteor: `minecraft:magma_block` (Block, Maßstab 1,2, glow #FF3B1F, fullBright, dreht sich 20°/T) + 3 kleine `minecraft:magma_block` (0,4) als Trümmer, die beim Einschlag nach außen fliegen. |
| Sounds | Zielmarke: `minecraft:block.respawn_anchor.charge (1,0; 0,8 ±0,05)`, `minecraft:entity.blaze.ambient (0,6; 0,6; +8)`. Fall: `minecraft:entity.ghast.warn (0,8; 0,6 ±0,05; +12)`, `minecraft:block.fire.ambient (1,0; 0,7; +16)`. Einschlag: `minecraft:entity.generic.explode (1,2; 0,8 ±0,1)`, `minecraft:entity.dragon_fireball.explode (0,9; 1,0 ±0,1)`, `minecraft:block.lava.pop (1,0; 0,8 ±0,2; +3)`. |
| Konter | 1,5 s sichtbarer Zielkreis und Meteor am Himmel → aus dem Kreis laufen. Rand-Schaden ist niedrig. |

### 2.5 Ultimate (Tier 2) – Inferno-Sonne *(neu)*
| | |
|---|---|
| Idee | Über dem Anwender entsteht eine kleine Sonne, die mehrere Sekunden lang gezielte Feuerstrahlen auf Gegner schießt und am Ende kollabiert. Stark gegen Gruppen im Freien, schwach gegen Gegner in Deckung. |
| Wirkung | Die Sonne entsteht **8 Blöcke über** der Position des Anwenders und bleibt dort (fester Punkt). Wirkradius **12**. Nach 40 T Aufladung schießt sie **6 Strahlen** (alle 20 T) auf den jeweils nächsten Gegner mit **Sichtlinie** zur Sonne: je **1,5 ♥** + **3 s** Brand, höchstens **3 Strahlen pro Ziel**. Am Ende **Kollaps**: alle Gegner im Radius **7** mit Sichtlinie zur Sonne **2 ♥** + Rückstoß 1,0. Maximum pro Ziel: 4,5 + 2 = **6,5 ♥**. Der Anwender ist während der Sonne normal beweglich und bekommt **Feuer-Immunität** (ohnehin Passiv). |
| Cooldown | **180 s** |
| Aufladen 0–39 T (Telegraph) | Bodenkreis r 12 (64 Punkte, `minecraft:dust` #FF3B1F 1,5, long distance **[E]**) und innerer Kreis r 7 (`minecraft:dust_color_transition` #FFB400→#FF3B1F). Funken steigen spiralförmig vom Anwender zur Sonne (`Shapes.spiral` r 1,2, 8 Blöcke hoch, 2,5 Windungen: `minecraft:flame` + `minecraft:electric_spark`). Die Sonne wächst: Kugel (`Shapes.sphere`) r 0,2 → 1,6 aus `minecraft:dust_color_transition` (#FFB400→#FF3B1F, 1,8), innen `minecraft:flame` (6/T). |
| Glühen 40–159 T | Sonne pulsiert (r 1,6 ± 0,2, Sinus 20 T). Korona: zwei gegenläufige Ringe r 2,2 (`minecraft:dust` #FFB400 1,0 / `minecraft:small_flame`). Je T 2× `minecraft:lava` fällt nach unten. |
| Feuerstrahl (je 20 T) | **10 T Zielwarnung:** dünne Linie Sonne → Ziel aus `minecraft:dust` (#FF3B1F, 0,5, alle 0,6 Blöcke), flackert, das Ziel hört `minecraft:block.fire.ambient (1,0; 1,6)`. Dann **Strahl 3 T:** dichte Linie (`Shapes.line`, 0,25 Abstand) aus `minecraft:flame` + `minecraft:dust_color_transition` (2,0), optional `minecraft:trail` **[E] (V)**; am Ziel `minecraft:lava` (6) und `minecraft:explosion` (1). Bricht die Sichtlinie während der Warnung ab, verpufft der Strahl (`minecraft:smoke` an der Kante). |
| Kollaps 160–179 T | Sonne schrumpft (`EASE_IN`) auf r 0,3, Partikel werden nach innen gesaugt (Kugel r 4 → 0,3 aus `minecraft:dust_color_transition`). |
| Auslösen T 180 | `minecraft:explosion_emitter` (1) an der Sonne, `minecraft:flash` **(V: ab 1.21.9 evtl. mit Farbe)**, Bodenring r 0 → 7 in 8 T aus `minecraft:flame` + `minecraft:dust` (1,8), `minecraft:lava` (24). |
| Nachklingen 40 T | `minecraft:ash` + `minecraft:white_ash` rieseln im Radius 7 (4/T), `minecraft:campfire_signal_smoke` (1 alle 4 T) aus dem Sonnenpunkt. |
| Displays | Sonnenkern: `minecraft:shroomlight` (Block, Maßstab 0,2 → 1,4, fullBright, glow #FFB400, dreht sich 6°/T) und umhüllend `minecraft:magma_block` (Maßstab 1,8, gegenläufig rotierend, leicht transparent wirkend durch Partikel). 4× `minecraft:blaze_rod` (Item, 0,8) kreisen als Strahlenkranz. Gesamt 6 Displays. |
| Sounds | Start (Warnung, Lautstärke 2,0): `minecraft:block.beacon.activate (2,0; 0,6)`, `minecraft:block.respawn_anchor.charge (1,2; 0,7 ±0,05; +6)`, `minecraft:entity.blaze.ambient (1,0; 0,5; +20)`. Glühen: alle 40 T `minecraft:block.fire.ambient (1,2; 0,6 ±0,1)`. Strahl: `minecraft:entity.blaze.shoot (1,0; 0,7 ±0,1)`, `minecraft:item.firecharge.use (0,8; 0,9 ±0,1; +1)`. Kollaps: `minecraft:block.beacon.deactivate (1,5; 0,5)`, Explosion: `minecraft:entity.generic.explode (2,0; 0,7)`, `minecraft:entity.dragon_fireball.explode (1,5; 0,8 ±0,05; +2)`. |
| Konter | 2 s Aufladung mit riesigem Bodenkreis; **Deckung brechen die Sichtlinie** (unter Dächer, hinter Wände, in Höhlen); jeder Strahl hat 0,5 s Zielwarnung; Kollaps nur im Radius 7. Anwender unterbrechen (≥ 3 ♥ in der Aufladung). Feuerresistenz-Trank nimmt den Brand. |

---

## 3. Erde (orange, Fire-Textur)

Leitmotiv: Gewicht, Beben, Steinbrocken. Farbverlauf orange → erdbraun, dazu Block-Partikel des tatsächlichen Untergrunds.

### 3.1 Passiv T1 – Standfest
- **Rückstoß-Resistenz +50 %** (Attribut `minecraft:knockback_resistance` +0,5). Präzisiert aus „Kein Rückstoß / Resistenz I“: dauerhafte Resistenz I wäre im PvP zu stark und bei 0 Energie zu wertvoll.

### 3.2 Passiv T2 – Bergmannsblut
- **Eile II** für 4 s nach jedem Abbau von Stein, Tiefenschiefer, Erzen (Block-Tag `xmassmp:earth_mineable`, Standard: `#minecraft:base_stone_overworld`, `#minecraft:base_stone_nether`, alle `*_ores`). Ohne Partikel.

### 3.3 Primär – Erdbeben
| | |
|---|---|
| Wirkung | Stampfen; eine Schockwelle läuft in **12 T** von r 0,5 bis r **6**. Getroffen wird, wer **am Boden** steht, wenn die Welle ihn erreicht: **2 ♥**, Hochwurf 0,6, **Langsamkeit II 3 s**. |
| Cooldown | **18 s** |
| Aufladen 0–7 T | Anwender sinkt leicht ein (Langsamkeit für sich selbst 8 T). Risse: 6 Linien radial r 0 → 2 aus `minecraft:block_crumble` **[E] (V)** bzw. Ersatz `minecraft:block` (Untergrund) + `minecraft:dust_color_transition` (#E07A1F→#6B4423, 1,0). Unter den Füßen `minecraft:falling_dust` (Untergrund) **[E]**. |
| Auslösen T 8 | `minecraft:explosion` (1) unter dem Anwender, `minecraft:dust_plume` (8), `minecraft:crit` (12, nach außen), Bodenwelle startet. |
| Welle 8–19 T (`EASE_OUT`) | Ring r 0,5 → 6 (36 Punkte): `minecraft:block` (Untergrund, 3 pro Punkt, Geschwindigkeit 0,2) **[E]**, `minecraft:dust_color_transition` (1,4), alle 2 Punkte `minecraft:poof`. An der Front springen Displays hoch. |
| Nachklingen 20 T | `minecraft:falling_dust` (Untergrund) rieselt aus 1,5 Höhe im Radius (3/T), `minecraft:white_ash` (2/T). |
| Displays | 8 Block-Displays des Untergrunds (Fallback `minecraft:coarse_dirt`; Maßstab 0,5, eckig gekippt) springen beim Durchlauf der Welle 0,8 hoch (6 T) und fallen zurück (6 T), dann weg. |
| Sounds | Aufladen: `minecraft:block.rooted_dirt.break (0,8; 0,7 ±0,1)`, `minecraft:entity.warden.dig (0,5; 1,2; +2)`. Auslösen: `minecraft:item.mace.smash_ground_heavy (1,0; 0,8 ±0,05)`, `minecraft:entity.generic.explode (0,5; 0,6 ±0,05)`, `minecraft:block.stone.break (1,0; 0,6 ±0,1; +3)`, `minecraft:block.gravel.break (0,8; 0,8 ±0,1; +6)`. |
| Konter | 0,4 s Aufladung mit Rissen; **im richtigen Moment springen** weicht der Welle aus; Rückstoß-Resistenz verringert den Hochwurf. |

### 3.4 Sekundär – Felsbrocken
Präzisiert aus „Steinwall/Felsbrocken“: Ein Wall ohne echte Blöcke würde nichts aufhalten. Der Wall-Gedanke steckt in der Ultimate (Säulen).

| | |
|---|---|
| Wirkung | Anwender reißt einen Felsbrocken aus dem Boden und wirft ihn in Blickrichtung (Geschwindigkeit 1,3 Blöcke/T, Schwerkraft 0,03/T², Reichweite ca. **28** Blöcke). Einschlag am ersten Spieler/Block: Direkttreffer **4 ♥**, Splash **2 ♥** im Radius **2,5**, Rückstoß 1,0, **Langsamkeit I 2 s**. Schild blockt den Direkttreffer frontal (dann nur Splash). |
| Cooldown | **35 s** |
| Aufladen 0–14 T | Boden vor dem Anwender bricht auf: `minecraft:block` (Untergrund, 4/T) **[E]**, `minecraft:dust_pillar` (Untergrund, 2/T) **[E]**; Felsbrocken-Displays steigen 1,5 Blöcke über die Schulter, drehen sich; orange Funken `minecraft:dust` (#E07A1F, 0,8). |
| Flug | Je T am Brocken: `minecraft:falling_dust` (Untergrund, 2) **[E]**, `minecraft:dust_color_transition` (#E07A1F→#6B4423, 1,2, 2), alle 3 T `minecraft:poof` (1). |
| Auslösen (Einschlag) | `minecraft:explosion` (1), `minecraft:block` (Untergrund, 30, Geschwindigkeit 0,4) **[E]**, `minecraft:crit` (14), Ring r 0 → 2,5 in 5 T aus `minecraft:dust` (#E07A1F, 1,4). |
| Nachklingen 25 T | Brocken zerfällt in 5 kleine Displays (0,3), die wegrollen und schrumpfen; `minecraft:white_ash` + `minecraft:dust_plume` steigen auf. |
| Displays | Brocken = 3 überlagerte Block-Displays: `minecraft:cobblestone` (0,9), `minecraft:andesite` (0,7, versetzt), `minecraft:coarse_dirt` (0,6, versetzt); Rotation 15°/T. Danach 5 Trümmer `minecraft:cobblestone` (0,3). |
| Sounds | Aufladen: `minecraft:block.stone.break (1,0; 0,6 ±0,1)`, `minecraft:block.rooted_dirt.break (0,8; 0,8; +4)`, `minecraft:entity.iron_golem.attack (0,9; 0,7 ±0,05; +10)`. Wurf: `minecraft:entity.player.attack.sweep (0,8; 0,6)`. Einschlag: `minecraft:block.anvil.land (0,6; 0,6 ±0,05)`, `minecraft:entity.generic.explode (0,7; 1,1 ±0,1)`, `minecraft:block.deepslate.break (1,0; 0,7 ±0,1; +2)`. |
| Konter | 0,75 s sichtbares Aufladen, langsames gut sichtbares Projektil → seitlich ausweichen; Schild. |

### 3.5 Ultimate (Tier 2) – Titanenschlag *(neu)*
| | |
|---|---|
| Idee | Der Anwender springt hoch, schwebt kurz über dem Schlachtfeld und schmettert in den Boden: drei Schockwellen, danach schießen Steinsäulen unter Gegnern hervor. |
| Wirkung | Sprung auf **+6** Blöcke, 20 T Schweben (Anwender kann sich nicht bewegen, ist aber angreifbar), dann Sturz in 4 T. Beim Aufprall **drei Wellen** (r 3, 6, 9; je 8 T Abstand). Jedes Ziel am Boden wird von **der ersten Welle getroffen, die es erreicht**: 4 ♥ / 3 ♥ / 2 ♥, Hochwurf 0,5, Langsamkeit II 3 s. Danach **Säulen**: unter jedem Gegner im Radius 9 (max. 6) nach **12 T Vorwarnung** eine Säule: **2 ♥**, Hochwurf 1,1, Langsamkeit III 2 s. Maximum pro Ziel **6 ♥**. Anwender: kein Fallschaden, **Resistenz II 4 s** nach dem Aufprall. |
| Cooldown | **180 s** |
| Aufladen 0–29 T (Sprung + Schweben, Telegraph) | Bodenkreis r 9 (`minecraft:dust` #E07A1F 1,5, long distance **[E]**), zwei innere Ringe r 3 und 6 (`minecraft:dust_color_transition` #E07A1F→#6B4423). Felsbrocken (Displays) kreisen um den schwebenden Anwender (r 1,8, 18°/T). Vom Boden steigen Krümel auf: `minecraft:falling_dust` umgekehrt (Ersatz: `minecraft:block` mit Geschwindigkeit nach oben) **[E]**, `minecraft:white_ash`. |
| Sturz 30–33 T | Spur `minecraft:cloud` (3/T), `minecraft:crit` (4/T); Felsbrocken ziehen sich an den Anwender heran. |
| Auslösen T 34 | `minecraft:explosion_emitter` (1), `minecraft:dust_plume` (20), `minecraft:block` (Untergrund, 50) **[E]**, Kugel-Burst `minecraft:dust_color_transition` (r 1,2, 30 Punkte). |
| Wellen 34–57 T | Je Welle Ring r 0 → Ziel in 8 T, 48 Punkte: `minecraft:block` (Untergrund) **[E]**, `minecraft:dust_color_transition` (1,6), `minecraft:poof`; an der Front 8 Block-Displays (`minecraft:cobbled_deepslate`, 0,5) springen hoch. |
| Säulen 58–90 T | **Vorwarnung 12 T** unter jedem Ziel: Ring r 1 aus `minecraft:dust` (#E07A1F 1,0) + `minecraft:dust_pillar` (Untergrund, 3/T) **[E]** + `minecraft:crit` nach oben. Dann Säule: 3 gestapelte Block-Displays (`minecraft:dripstone_block` unten, `minecraft:pointed_dripstone` als Spitze mit Zustand `thickness=tip` **[E]**) schießen in 4 T von −2 auf +2,5, bleiben 20 T, sinken in 10 T ein. Beim Hervorbrechen `minecraft:block` (Untergrund, 20) **[E]**. |
| Nachklingen 40 T | `minecraft:falling_dust` (Untergrund) und `minecraft:white_ash` im Radius 9 (4/T), Restbeben: alle 10 T kleiner Ring r 9 aus `minecraft:dust` (#6B4423 0,8). |
| Displays | 4 kreisende Felsbrocken (`minecraft:cobblestone`/`minecraft:tuff`, 0,6), 24 Wellen-Displays (3 × 8, kurzlebig), bis zu 6 Säulen × 3 = 18 → zeitlich gestaffelt höchstens **32** gleichzeitig. |
| Sounds | Start (Warnung): `minecraft:entity.ravager.roar (2,0; 0,7)`, `minecraft:entity.warden.emerge (1,0; 1,2; +4)` **(V: langer Sound, ggf. kürzen durch Lautstärke)**. Schweben: `minecraft:block.basalt.break (1,0; 0,6 ±0,1)` alle 6 T. Aufprall: `minecraft:item.mace.smash_ground_heavy (2,0; 0,6)`, `minecraft:entity.generic.explode (1,5; 0,5 ±0,05)`, `minecraft:entity.warden.sonic_boom (0,6; 0,6; +2)`. Wellen: `minecraft:block.deepslate.break (1,2; 0,6 ±0,1)` je Welle. Säulen: Vorwarnung `minecraft:entity.warden.dig (0,8; 1,4)`, Ausbruch `minecraft:block.pointed_dripstone.land (1,2; 0,6 ±0,1)`, `minecraft:block.stone.break (1,0; 0,7 ±0,1; +1)`. |
| Konter | 1,5 s Schweben in der Luft: Anwender ist mit Bogen/Armbrust/Windladung treffbar und **unterbrechbar** (≥ 3 ♥ → Abbruch, Anwender fällt normal). Bodenkreis r 9 → herauslaufen. Wellen: springen. Säulen: 0,6 s Vorwarnung unter den Füßen → einen Schritt zur Seite. |

---

## 4. Wind (weiß, Puff-Textur)

Leitmotiv: Bewegung, Spiralen, Druckwellen. Farbverlauf weiß → hellblau, viele `gust`-Varianten.

### 4.1 Passiv T1 – Federleicht
- **Kein Fallschaden** (auch nicht durch Mace-Smash-Rückprall). Enderperlen-Schaden bleibt.

### 4.2 Passiv T2 – Doppelsprung
- In der Luft erneut springen: Sprung auf ca. **+3** Blöcke, 1× pro Bodenkontakt, interner Cooldown **2 s**. Nicht mit Elytra und nicht im Wasser.
- Technik ohne Client-Mod: `mayfly` wird gesetzt, der Flug-Umschalter (doppelt Leertaste) wird abgefangen und in den Sprung umgewandelt **(V: Anti-Cheat-/Flug-Kick prüfen)**. Mit Client-Mod: eigene Taste.
- Optik: Ring r 0,6 unter den Füßen aus `minecraft:small_gust` (1) + `minecraft:cloud` (8, Ring), Sound `minecraft:entity.breeze.jump (0,6; 1,3 ±0,1)`.

### 4.3 Primär – Böe
| | |
|---|---|
| Wirkung | Dash in Blickrichtung (horizontal 1,8, leicht nach oben 0,2) und gleichzeitig ein Windstoß im **Kegel 60°** bis **6** Blöcke vor dem Anwender: **1 ♥**, starker Rückstoß **1,6** + 0,4 hoch. |
| Cooldown | **15 s** |
| Aufladen 0–3 T | Luft wird eingesogen: 6 Linien aus `minecraft:dust_color_transition` (#F2F7FF→#9FD8FF, 0,8) laufen von r 2,5 zum Anwender; `minecraft:small_gust` (2). |
| Auslösen T 4 | `minecraft:gust_emitter_small` (1) vor dem Anwender, Kegel aus 3 aufgefächerten Spirallinien (`Shapes.spiral` gedreht mit `Shapes.orient`) aus `minecraft:dust` (#F2F7FF, 1,2) + `minecraft:gust` (je 1,5 Blöcke 1), `minecraft:sweep_attack` (3) in Kegelmitte. |
| Nachklingen 15 T | Windspur entlang der Dash-Bahn: `minecraft:cloud` (2/T, langsam), `minecraft:end_rod` (1 alle 2 T), Spirale aus `minecraft:dust_color_transition` (0,6) zerfasert. |
| Displays | keine |
| Sounds | Aufladen: `minecraft:entity.breeze.inhale (0,8; 1,2 ±0,1)`. Auslösen: `minecraft:entity.breeze.wind_burst (1,0; 1,0 ±0,1)`, `minecraft:entity.wind_charge.wind_burst (0,8; 1,2 ±0,1; +1)`, `minecraft:entity.player.attack.sweep (0,6; 0,8 ±0,1; +2)`. |
| Konter | Kurzes, gut hörbares Einsaugen; Kegel seitlich verlassen; Rückstoß-Resistenz (Netherit, Erde-Passiv) mindert den Stoß. Dash stoppt an Wänden (keine Teleport-Kollisionen). |

### 4.4 Sekundär – Tornado
| | |
|---|---|
| Wirkung | Wirbel entsteht 3 Blöcke vor dem Anwender und wandert **0,2 Blöcke/T** (= 4 Blöcke/s, langsamer als Sprinten) in Blickrichtung, folgt dem Gelände, Dauer **5 s**. Sog im Radius **4** (0,12/T zur Mitte, **Schleichen halbiert den Sog**). Im Kern (r **1,5**): Hub (Geschwindigkeit nach oben 0,35, höchstens alle 10 T) und **0,5 ♥ pro Sekunde** (max. **2,5 ♥**). Am Ende schleudert der Wirbel alle im Kern 1,2 nach außen. Fallschaden danach zählt normal. |
| Cooldown | **40 s** |
| Aufladen 0–11 T | Am Startpunkt bildet sich ein flacher Ring (r 2 → 0,5) aus `minecraft:cloud` + `minecraft:small_gust`, `minecraft:dust` (#9FD8FF 1,0) steigt auf. |
| Auslösen T 12 | `minecraft:gust_emitter_large` (1), Burst `minecraft:poof` (12). |
| Wirbel 12–111 T | **Drei Lagen** Spiralen (`Shapes.spiral`, Höhe 6, Phase steigt pro T): innen r 0,6 schnell (`minecraft:dust` #F2F7FF 1,0), Mitte r 1,2 (`minecraft:dust_color_transition` #F2F7FF→#9FD8FF 1,3), außen r 1,9 langsam (`minecraft:cloud`, 1 je Punkt, jeder 2.). Basis: `minecraft:white_smoke` (3/T), Kern alle 4 T `minecraft:sweep_attack`, alle 6 T `minecraft:gust` (1) zufällig am Rand. Auf HIGH ca. 70 Partikel/T. |
| Nachklingen 20 T | Wirbel öffnet sich (Radien × 2, Höhe sinkt, `EASE_OUT`), `minecraft:cloud` verteilt sich, `minecraft:snowflake` (8) als Abschluss. |
| Displays | 4 Item-Displays (`minecraft:oak_leaves`, `minecraft:short_grass`, `minecraft:feather`, `minecraft:oak_leaves`, Maßstab 0,5) kreisen in der mittleren Spirale und steigen; am Ende fliegen sie nach außen. |
| Sounds | Start: `minecraft:item.trident.riptide_2 (0,8; 1,2 ±0,1)`, `minecraft:entity.breeze.shoot (0,8; 0,8; +3)`. Schleife alle 20 T: `minecraft:entity.breeze.whirl (1,0; 0,8 ±0,1)`, `minecraft:entity.breeze.idle_air (0,6; 0,7 ±0,1; +10)`. Ende: `minecraft:entity.wind_charge.wind_burst (1,0; 0,7 ±0,05)`. |
| Konter | Langsam und groß sichtbar → wegsprinten; Schleichen halbiert den Sog; Tornado stoppt an Wänden höher als 2 Blöcke. |

### 4.5 Ultimate (Tier 2) – Sturmauge *(neu)*
| | |
|---|---|
| Idee | Der Anwender steigt in die Luft und wird zum Auge eines Sturms. Wer im Sturmkreis unter freiem Himmel steht, wird hochgerissen, hängt kurz wehrlos in der Luft und wird dann zu Boden geschmettert. |
| Wirkung | Anwender steigt auf **+4** und schwebt (Slow Falling). Sturmkreis Radius **10** um den Startpunkt (fest). Nach **35 T** Aufladung werden alle Gegner im Kreis **mit freiem Himmel** (kein Block bis 8 Blöcke über ihnen) **6 Blöcke hochgerissen** und **40 T** in der Luft gehalten (kein Springen, eingeschränkte Bewegung, Pfeile/Nahkampf auf sie normal möglich). Dann **Niederschlag**: Aufprall **3 ♥** (eigener Schaden, kein Fallschaden zusätzlich) + Langsamkeit II 2 s. Gegner **mit Dach** werden nur 1,2 nach außen gestoßen (0 ♥). Maximum pro Ziel **3 ♥** direkt; der Rest ist Gelegenheit für Team-Treffer. |
| Cooldown | **180 s** |
| Aufladen 0–34 T (Telegraph) | Wolkenwand am Rand: Zylinder r 10, 3 Ebenen (0 / 2 / 4 Blöcke), rotiert 4°/T: `minecraft:cloud` + `minecraft:dust` (#F2F7FF 1,5, long distance **[E]**). Bodenring `minecraft:dust_color_transition` (#F2F7FF→#9FD8FF). Spirale vom Boden zum Anwender aus `minecraft:small_gust`. Wer im Kreis steht, sieht Windlinien nach oben an den eigenen Füßen (`minecraft:white_smoke`). |
| Auslösen T 35 | `minecraft:gust_emitter_large` (1) unter jedem Ziel, vertikale Säule aus `minecraft:gust` (3) + `minecraft:cloud` (10). Am Anwender Kugel-Burst `minecraft:dust_color_transition` (r 1,5, 30 Punkte). |
| Schwebe 35–74 T | Um jedes gehaltene Ziel ein Mini-Wirbel (`Shapes.spiral` r 0,8, Höhe 2, `minecraft:dust` #9FD8FF 0,8 + `minecraft:small_gust` alle 5 T). Wolkenwand zieht sich auf r 6 zusammen. |
| Niederschlag T 75 | Vom Anwender je Ziel eine Linie (`Shapes.line`) aus `minecraft:end_rod` + `minecraft:dust` (#F2F7FF 1,2); Ziel wird mit −2,5 nach unten gerissen. Beim Aufprall: `minecraft:explosion` (1), `minecraft:cloud` (16, Ring r 1,5), `minecraft:sweep_attack` (4). |
| Nachklingen 40 T | Windstille: `minecraft:snowflake` und `minecraft:white_ash` rieseln im Radius 10 (4/T), Wolkenwand löst sich nach außen auf (`EASE_OUT`). Anwender sinkt langsam (Slow Falling 3 s). |
| Displays | 8 Item-Displays `minecraft:feather` (0,6) kreisen in der Wolkenwand; 1 Item-Display `minecraft:wind_charge` (1,2, fullBright) dreht sich über dem Kopf des Anwenders. |
| Sounds | Start (Warnung, 2,0): `minecraft:entity.breeze.charge (2,0; 0,6)`, `minecraft:entity.ender_dragon.flap (1,2; 0,7 ±0,05; +5)`, Schleife alle 10 T `minecraft:entity.breeze.whirl (1,0; 0,6 ±0,1)`. Hochreißen: `minecraft:entity.breeze.wind_burst (1,5; 0,6)`, `minecraft:item.trident.riptide_3 (1,0; 1,0 ±0,1; +2)`. Schwebe: `minecraft:entity.breeze.idle_air (0,8; 1,0 ±0,1)` alle 15 T. Niederschlag: `minecraft:item.mace.smash_air (1,2; 0,8)`, `minecraft:item.mace.smash_ground (1,2; 0,7 ±0,05; +4)`, `minecraft:entity.wind_charge.wind_burst (1,0; 0,6; +4)`. |
| Konter | 1,75 s Wolkenwand → Kreis verlassen oder **unter ein Dach / in eine Höhle** gehen (auch einfach 1 Block über dem Kopf platzieren). Wasser: Ziele im Wasser werden nicht gehoben. Anwender schwebt sichtbar und ist unterbrechbar. Kontrolle unterliegt Diminishing Returns. |

---

## 5. Schatten (lila, Astra-Textur)

Leitmotiv: Verschwinden, Seelen, Dunkelheit. Farbverlauf lila → fast schwarz, dazu `portal`/`reverse_portal`, `sculk_soul`, `squid_ink`.

### 5.1 Passiv T1 – Nachtauge
- Dauerhafte **Nachtsicht** (ohne Partikel). Schützt **nicht** gegen den Effekt Dunkelheit.

### 5.2 Passiv T2 – Schattenmantel
- Wer **2 s** in Dunkelheit (Lichtlevel ≤ **5**) schleicht, wird **unsichtbar** (Rüstung bleibt sichtbar, Vanilla-Verhalten), solange er schleicht und im Dunkeln bleibt. Endet sofort bei Angriff, Schaden oder Fähigkeit; danach 8 s gesperrt.
- Präzisiert aus „Unsichtbar / Lebensraub nachts“: nur die Unsichtbarkeit, weil sie klar gekontert werden kann (Licht, Rüstung sichtbar). Alternative siehe Frage 4.
- Optik beim Eintreten: `minecraft:squid_ink` (4) + `minecraft:dust` (#2A0B4F 0,8, 6) am Boden, Sound `minecraft:entity.illusioner.mirror_move (0,3; 1,4)` nur für den Spieler selbst hörbar.

### 5.3 Primär – Schattenschritt
| | |
|---|---|
| Wirkung | Teleport **1,5 Blöcke hinter** das angeschaute Ziel (Spieler/Mob, Sichtlinie, bis **20** Blöcke), Blick zum Ziel. Ohne Ziel: zum angeschauten Block, bis **12** Blöcke. Danach **Hinterhalt**: der nächste Nahkampftreffer innerhalb **3 s** macht **+1,5 ♥**. Landet nie in Blöcken oder geschützten Zonen; ist hinter dem Ziel kein Platz, landet er seitlich, sonst scheitert die Kraft ohne Cooldown. |
| Cooldown | **18 s** |
| Aufladen 0–5 T | Am Anwender: Spirale nach innen aus `minecraft:portal` (Geschwindigkeit nach innen) + `minecraft:dust_color_transition` (#8E3BFF→#2A0B4F, 1,0). **Am Zielpunkt gleichzeitig eine Tintenpfütze**: Ring r 0,6 aus `minecraft:squid_ink` (1/Punkt, 8 Punkte) – das ist die Vorwarnung für das Ziel. |
| Auslösen T 6 | Abflug: `minecraft:large_smoke` (6), `minecraft:reverse_portal` (20, Kugel r 0,5). Spur Start → Ziel (`Shapes.wave`, Amplitude 0,3, 1,5 Wellen, 0,3 Abstand) aus `minecraft:dust_color_transition` (1,2) + jeder 3. Punkt `minecraft:witch`. Ankunft: `minecraft:reverse_portal` (24), `minecraft:dust` (#8E3BFF 1,5, Ring r 1). |
| Nachklingen 20 T | Spur verblasst von Start zum Ziel hin (je T ein Abschnitt weniger), am Ziel steigen `minecraft:sculk_soul` (1 alle 4 T) auf. Solange der Hinterhalt bereit ist: am Anwender alle 5 T `minecraft:dust` (#8E3BFF 0,7) an der Hand. |
| Displays | keine |
| Sounds | Aufladen: `minecraft:entity.illusioner.prepare_mirror (0,7; 1,2 ±0,1)` (am Ziel hörbar). Teleport: `minecraft:entity.enderman.teleport (1,0; 0,7 ±0,1)`, `minecraft:entity.illusioner.mirror_move (0,8; 0,9 ±0,1; +1)`, `minecraft:particle.soul_escape (1,0; 0,8 ±0,2; +3)`. Hinterhalt-Treffer: `minecraft:entity.phantom.swoop (0,6; 1,4)`. |
| Konter | Tintenpfütze + Sound 0,3 s vorher hinter dem Ziel → umdrehen, Schild heben. Keine Sichtlinie = kein Teleport. |

### 5.4 Sekundär – Seelenbann
| | |
|---|---|
| Wirkung | Nach **14 T** Aufladung Seelen-Schockwelle im Radius **7**: Gegner erhalten **1 ♥**, **Dunkelheit 6 s** und **Schwäche I 4 s**. Präzisiert: keine Blindheit (zu stark in Kombination). |
| Cooldown | **40 s** |
| Aufladen 0–13 T | 4 Seelen kreisen enger um den Anwender (r 2,5 → 0,8, Höhe 1,2): `minecraft:sculk_soul` (1 je Seele/T) + `minecraft:dust_color_transition` (#8E3BFF→#2A0B4F, 1,0) als Schweif. Bodenring r 7 aus `minecraft:dust` (#2A0B4F 1,2) erscheint (Telegraph). |
| Auslösen T 14 | Ring r 0,5 → 7 in 8 T aus `minecraft:sculk_soul` + `minecraft:soul` + `minecraft:dust` (#8E3BFF 1,6), `minecraft:sculk_charge_pop` (10) am Anwender, `minecraft:large_smoke` (8). Getroffene: `minecraft:squid_ink` (6) über dem Kopf. |
| Nachklingen 60 T | 4 Geister kreisen 3 s um den Anwender (`Shapes.spiral` r 1,2, 2 Windungen, `minecraft:sculk_soul` + `minecraft:dust` #8E3BFF 0,8) und steigen am Ende auf. |
| Displays | 4 Item-Displays `minecraft:echo_shard` (0,4, glow #8E3BFF, fullBright) als Seelenkern, kreisen mit und lösen sich am Ende auf. |
| Sounds | Aufladen: `minecraft:entity.evoker.prepare_attack (0,8; 0,7 ±0,05)`, `minecraft:block.sculk_catalyst.bloom (1,0; 0,8; +6)`. Auslösen: `minecraft:entity.warden.sonic_charge (0,5; 1,4)` **(V: Dauer)**, `minecraft:particle.soul_escape (1,2; 0,6 ±0,2)`, `minecraft:entity.wither.ambient (0,5; 1,6 ±0,1; +2)`. Nachklingen: `minecraft:entity.vex.ambient (0,5; 0,8 ±0,1)` alle 20 T. |
| Konter | 0,7 s Aufladung mit sichtbarem Ring → herauslaufen; Milch entfernt Dunkelheit und Schwäche. |

### 5.5 Ultimate (Tier 2) – Nachtfall *(neu)*
| | |
|---|---|
| Idee | Eine Schattendomäne fällt über das Gebiet. Darin ist der Anwender unsichtbar, schnell und kann ohne Cooldown durch die Schatten springen. Jeder Sprung brandmarkt ein Ziel; wer am Ende noch markiert in der Domäne steht, erleidet Schaden. |
| Wirkung | Domäne Radius **10** um den Startpunkt (fest), Dauer **8 s** nach **30 T** Aufladung. **Gegner im Inneren:** Dunkelheit (solange drin) und Langsamkeit I. **Anwender im Inneren:** Unsichtbarkeit (Rüstung sichtbar), Tempo I und **Schattenschritt ohne Cooldown, höchstens 3×** (eigene Zählung, verbraucht den normalen Cooldown nicht). Jeder Schattenschritt setzt ein **Seelenmal** auf das Ziel (max. 3 pro Ziel). Am Ende **Implosion:** jedes Ziel, das **noch in der Domäne** steht, erleidet **2 ♥ pro Mal** (max. **6 ♥**). Wer die Domäne verlässt, verliert alle Male sofort. Hinterhalt-Bonus des Schattenschritts gilt nicht zusätzlich. |
| Cooldown | **180 s** |
| Aufladen 0–29 T (Telegraph) | Am Rand steigen 16 Seelensäulen auf (Höhe 0 → 5): `minecraft:sculk_soul` + `minecraft:dust_color_transition` (#8E3BFF→#2A0B4F, 1,6, long distance **[E]**). Bodenring r 10 aus `minecraft:dust` (#8E3BFF 1,5). Vom Anwender breitet sich `minecraft:squid_ink` aus (Ring r 0 → 10). |
| Domäne 30–189 T | Kuppel (`Shapes.sphere` r 10, obere Hälfte, 80 Punkte, alle 4 T neu und gedreht) aus `minecraft:dust` (#2A0B4F 2,0) + jeder 4. Punkt `minecraft:reverse_portal`. Innen schweben `minecraft:sculk_soul` und `minecraft:ash` (3/T). Seelenmal am Ziel: rotierender Ring r 0,6 über dem Kopf aus `minecraft:dust` (#8E3BFF 0,8), je Mal ein zusätzlicher Ring (1/2/3 Ringe), alle 2 T. **Spur des unsichtbaren Anwenders:** alle 5 T `minecraft:squid_ink` (1) an den Füßen. |
| Schattenschritte | wie Primär (5.3), aber schneller: Aufladen 3 T, Tintenpfütze trotzdem sichtbar. |
| Implosion 190–199 T | Kuppel zieht sich in 10 T auf den Mittelpunkt zusammen (`EASE_IN`), Male leuchten auf (`minecraft:enchanted_hit` 6 je Mal). T 200: an jedem markierten Ziel `minecraft:sonic_boom` (1) + `minecraft:sculk_soul` (12) + `minecraft:large_smoke` (6). |
| Nachklingen 30 T | `minecraft:soul` steigt aus dem Boden im Radius 10 (3/T), `minecraft:portal` am Mittelpunkt. |
| Displays | 16 Item-Displays `minecraft:echo_shard` (0,5, glow #8E3BFF) an den Spitzen der Seelensäulen, kreisen langsam; je Seelenmal 1 Item-Display `minecraft:echo_shard` (0,3) über dem Kopf des Ziels (max. 6 gleichzeitig). |
| Sounds | Start (Warnung, 2,0): `minecraft:block.sculk_shrieker.shriek (2,0; 0,7)`, `minecraft:entity.warden.heartbeat (1,2; 0,8; +10)`, `minecraft:ambient.soul_sand_valley.mood (1,0; 1,0; +20)`. Domäne: alle 20 T `minecraft:entity.warden.heartbeat (1,0; 0,7 ±0,05)`. Mal gesetzt: `minecraft:entity.elder_guardian.curse (0,4; 1,6 ±0,1)` (nur für das Ziel laut). Implosion: `minecraft:entity.warden.sonic_boom (1,2; 0,8)`, `minecraft:block.respawn_anchor.deplete (1,0; 0,6 ±0,05; +2)`, `minecraft:particle.soul_escape (1,5; 0,5; +4)`. |
| Konter | 1,5 s Aufladung mit Säulen am Rand → Domäne verlassen. **Male verfallen beim Verlassen**, die Kuppel ist klar sichtbar. Der unsichtbare Anwender hinterlässt eine Tintenspur, seine Rüstung bleibt sichtbar, und jeder Schritt hat die Tintenpfütze als Vorwarnung. Milch entfernt Dunkelheit (aber nicht die Male). Unterbrechbar in der Aufladung. |

---

## 6. Natur (grün, Wealth-Textur)

Leitmotiv: Wachstum, Blüten, Heilung. Farbverlauf grün → rosa, dazu `cherry_leaves`, `spore_blossom_air`, `happy_villager`.

### 6.1 Passiv T1 – Wurzelkraft
- **Regeneration I**, solange der Spieler auf Gras, Moos, Laub, Erde mit Pflanzen o. Ä. steht (Block-Tag `xmassmp:nature_ground`, Standard: `minecraft:grass_block`, `minecraft:moss_block`, `minecraft:podzol`, `minecraft:mycelium`, `#minecraft:leaves`, `minecraft:rooted_dirt`, `minecraft:pale_moss_block`). Nicht im Kampf (**5 s** nach Schaden durch Spieler).
- Optik: alle 40 T `minecraft:happy_villager` (1) an den Füßen, nur solange aktiv.

### 6.2 Passiv T2 – Reiche Ernte
- Dauerhaft **Glück I** und **25 %** Chance, beim Abbau reifer Feldfrüchte die Ernte zu verdoppeln.
- Optik bei Verdopplung: `minecraft:composter` (6) + `minecraft:happy_villager` (3), Sound `minecraft:block.composter.fill_success (0,5; 1,4 ±0,1)`.

### 6.3 Primär – Rankenfessel
| | |
|---|---|
| Wirkung | Ziel per Blick (Spieler/Mob, Sichtlinie, bis **16** Blöcke). Eine Ranke kriecht in **10 T** am Boden zum **Punkt, an dem das Ziel beim Auslösen stand**. Steht das Ziel dann noch im Radius **1,5** um diesen Punkt, wird es **2 s** festgehalten (Bewegung und Springen auf 0 über Attribut-Modifier `minecraft:movement_speed` −100 % und `minecraft:jump_strength` −100 %; Umsehen und Angreifen bleiben möglich) und erhält **1 ♥**. Teleport (Enderperle, Schattenschritt) befreit sofort. |
| Cooldown | **18 s** |
| Aufladen/Kriechen 0–9 T | Linie Anwender → Zielpunkt (`Shapes.wave`, Amplitude 0,25, 3 Wellen) wächst mit `EASE_IN` mit: `minecraft:dust_color_transition` (#3FD45A→#1E7A2E, 1,0) + jeder 3. Punkt `minecraft:composter`. Am Zielpunkt ein Ring r 1,5 aus `minecraft:dust` (#3FD45A 0,8) – Vorwarnung. |
| Auslösen T 10 | Ranken schießen hoch: 3 Spiralen (`Shapes.spiral` r 0,6, Höhe 2, Phasen 0/120/240°) aus `minecraft:dust` (#3FD45A 1,2), `minecraft:happy_villager` (8), `minecraft:block` (`minecraft:moss_block`, 12) **[E]**. |
| Halten 10–49 T | Spiralen drehen sich langsam, alle 3 T `minecraft:spore_blossom_air` (1) und `minecraft:falling_spore_blossom` (1). |
| Nachklingen 15 T | Ranken welken: Spiralfarbe → `minecraft:dust_color_transition` (#3FD45A→#6B4423), Displays schrumpfen, `minecraft:composter` (6) fällt. |
| Displays | 4 Item-Displays `minecraft:vine` (0,8, um das Ziel gedreht, wachsen 0 → 0,8 in 6 T) + 2 Block-Displays `minecraft:mangrove_roots` (0,5) am Boden + 1 Block-Display `minecraft:flowering_azalea` (0,4) obenauf. |
| Sounds | Kriechen: `minecraft:block.cave_vines.place (0,8; 0,8 ±0,1)` alle 3 T, `minecraft:block.rooted_dirt.break (0,6; 1,2; +4)`. Fessel: `minecraft:block.mangrove_roots.place (1,0; 0,7 ±0,1)`, `minecraft:entity.evoker_fangs.attack (0,6; 1,2 ±0,1; +1)`, `minecraft:block.hanging_roots.place (0,8; 0,9 ±0,1; +3)`. Befreien: `minecraft:block.azalea_leaves.break (0,8; 1,2 ±0,1)`. |
| Konter | 0,5 s sichtbare Ranke am Boden → aus dem Zielkreis gehen (r 1,5 reicht ein Sprint-Schritt). Enderperle befreit. Diminishing Returns. |

### 6.4 Sekundär – Blütenheilung
| | |
|---|---|
| Wirkung | Heilkreis Radius **5** an der Position des Anwenders (fest), Dauer **5 s**. Anwender und Verbündete (siehe Frage 2) im Kreis heilen **1 ♥ pro Sekunde** (max. **5 ♥**); einmalig werden **Gift** und **Wither** entfernt. Gegner im Kreis: keine Wirkung. |
| Cooldown | **45 s** |
| Aufladen 0–9 T | Kreis zeichnet sich gegen den Uhrzeigersinn (Bogen 0 → 360°) aus `minecraft:dust_color_transition` (#3FD45A→#FF9FD6, 1,2). Knospen steigen: `minecraft:spore_blossom_air` (3/T). |
| Auslösen T 10 | Blütenburst aus der Mitte: `minecraft:cherry_leaves` (20, Streuung 2), `minecraft:totem_of_undying` (10, Geschwindigkeit 0,3), `minecraft:heart` (3). |
| Heilen 10–109 T | Von oben (+3) fallen `minecraft:cherry_leaves` (3/T im Radius). Vom Boden steigen 2 Spiralen (r 4, Höhe 2,5, gegenläufig) aus `minecraft:dust` (#3FD45A 1,0) und `minecraft:dust` (#FF9FD6 0,8). Bei jedem Heil-Tick über jedem Geheilten `minecraft:heart` (1) + `minecraft:happy_villager` (3). |
| Nachklingen 20 T | Blüten-Displays schließen sich, `minecraft:cherry_leaves` (10) wehen nach außen, Ring verblasst (`minecraft:dust` #FF9FD6 0,6). |
| Displays | 6 Block-Displays am Kreisrand, abwechselnd `minecraft:pink_petals` (Zustand `flower_amount=4` **[E]**) und `minecraft:flowering_azalea`, wachsen 0 → 0,7 in 10 T, drehen sich leicht; 1 Block-Display `minecraft:spore_blossom` (0,8, kopfüber, fullBright) schwebt 2,5 über der Mitte. |
| Sounds | Aufladen: `minecraft:item.bone_meal.use (0,8; 1,0 ±0,1)`, `minecraft:block.azalea_leaves.place (0,8; 1,2; +4)`. Auslösen: `minecraft:block.amethyst_block.chime (1,0; 1,4 ±0,1)`, `minecraft:block.cherry_leaves.place (1,0; 0,9 ±0,1; +1)`, `minecraft:entity.player.levelup (0,3; 1,8; +3)`. Heil-Tick: `minecraft:block.amethyst_block.chime (0,5; 1,6 ±0,15)`. |
| Konter | Fester Ort → Gegner in den Kreis drängen/stoßen (Wind, Erde) oder dort angreifen; geheilt wird nur 1 ♥/s. |

### 6.5 Ultimate (Tier 2) – Weltenbaum *(neu)*
| | |
|---|---|
| Idee | Ein mächtiger Baum wächst aus dem Boden. Er heilt Verbündete, schlägt mit Wurzeln nach Gegnern und vergeht am Ende in einem Blütensturm. Gegner können ihn fällen. |
| Wirkung | Baum an der Position des Anwenders (fest), wächst in **30 T**, steht **10 s**. Radius **9**. **Verbündete** (inkl. Anwender) im Radius: **1 ♥ pro Sekunde** Heilung. **Wurzelschlag:** alle **40 T** wählt der Baum den nächsten Gegner im Radius; **10 T Vorwarnung** (Ring am Boden), dann Fessel **1 s** (wie Primär, Diminishing Returns) und **1,5 ♥**; das Ziel wird dabei 1 Block zum Baum gezogen. Höchstens 4 Wurzelschläge, je Ziel höchstens 2 (max. **3 ♥** pro Ziel). **Ende:** Blütensturm – Verbündete im Radius bekommen **Absorption I (2 ♥) 15 s**. **Fällen:** Der Stamm hat eine Hitbox (Interaction-Entity **[E]**); **8 Nahkampftreffer** von Gegnern beenden den Baum sofort ohne Blütensturm. |
| Cooldown | **180 s** |
| Aufladen/Wachsen 0–29 T (Telegraph) | Bodenkreis r 9 aus `minecraft:dust_color_transition` (#3FD45A→#FF9FD6, 1,5, long distance **[E]**). Wurzellinien laufen vom Zentrum nach außen (8 × `Shapes.wave`, `minecraft:dust` #6B4423 1,0, jeder 3. Punkt `minecraft:composter`). Stamm wächst (Displays skalieren hoch, `EASE_OUT`), um den Stamm `minecraft:happy_villager` (4/T) und `minecraft:block` (`minecraft:rooted_dirt`, 4/T) **[E]**. |
| Stehen 30–229 T | Krone: alle 2 T `minecraft:cherry_leaves` (4) und `minecraft:falling_spore_blossom` (2) aus der Krone, `minecraft:firefly` **(V, ab 1.21.5)** (2/T) im Radius, Spirale um den Stamm (`minecraft:dust` #3FD45A 1,0 aufsteigend). Heil-Tick: Linie Baum → Verbündeter aus `minecraft:dust` (#FF9FD6 0,6) + `minecraft:heart` (1) am Ziel. Wurzelschlag: Vorwarnung Ring r 1,2 unter dem Ziel aus `minecraft:dust` (#6B4423 1,0) + `minecraft:block` (`minecraft:rooted_dirt`) **[E]**; Auslösen wie Rankenfessel mit Wurzel-Displays. Treffer auf den Stamm: `minecraft:block` (`minecraft:oak_log`, 8) **[E]** + Riss-Partikel `minecraft:crit` (4). |
| Ende 230–249 T | Krone platzt: `minecraft:cherry_leaves` (60, Streuung 4), `minecraft:totem_of_undying` (30, Geschwindigkeit 0,4), `minecraft:happy_villager` (20), Ring r 0 → 9 aus `minecraft:dust` (#FF9FD6 1,6). Displays schrumpfen in 20 T. |
| Nachklingen 30 T | `minecraft:cherry_leaves` rieseln im Radius 9 (3/T), `minecraft:spore_blossom_air` steigt (2/T). |
| Displays | Stamm: 4 Block-Displays `minecraft:oak_log` (1,0, gestapelt, leicht verdreht) + 4 `minecraft:mangrove_roots` (0,8) am Fuß; Krone: 6 Block-Displays `minecraft:flowering_azalea_leaves` (1,5) + 4 `minecraft:cherry_leaves` (1,2); Wurzelschläge je 2 `minecraft:mangrove_roots` (0,5). Höchstens **24** gleichzeitig. |
| Sounds | Start (Warnung, 2,0): `minecraft:block.beacon.activate (2,0; 1,4)`, `minecraft:item.bone_meal.use (1,2; 0,6; +4)`, `minecraft:block.wood.place (1,0; 0,6 ±0,1; +8)`, `minecraft:block.chorus_flower.grow (1,2; 0,7; +16)`. Stehen: alle 30 T `minecraft:block.azalea_leaves.step (0,6; 0,8 ±0,1)` **(V)** bzw. `minecraft:block.cherry_leaves.place`. Heil-Tick: `minecraft:block.amethyst_block.chime (0,5; 1,5 ±0,15)`. Wurzelschlag: Vorwarnung `minecraft:block.rooted_dirt.break (0,8; 0,7)`, Treffer `minecraft:block.mangrove_roots.place (1,0; 0,6 ±0,1)`, `minecraft:entity.evoker_fangs.attack (0,8; 0,9 ±0,1; +1)`. Stamm-Treffer: `minecraft:block.wood.hit (1,0; 0,7 ±0,1)`. Fällen: `minecraft:block.wood.break (1,5; 0,5)`, `minecraft:block.azalea_leaves.break (1,2; 0,7; +2)`. Ende: `minecraft:item.totem.use (0,6; 1,4)`, `minecraft:entity.player.levelup (0,5; 1,6; +2)`. |
| Konter | Fester Ort, großer Kreis → Radius verlassen; **Baum fällen** (8 Treffer, Hitbox sichtbar am Stamm); Wurzelschlag hat 0,5 s Vorwarnung; geheilt wird nur 1 ♥/s. |

---

## 7. Übersicht aller 25 Fähigkeiten

| Kristall | Slot | Name | ID | Tier | Energie | Kernwert | Cooldown |
|---|---|---|---|---|---|---|---|
| Feuer | Passiv T1 | Glutherz | `fire.ember_heart` | 1 | immer | Feuerresistenz | – |
| Feuer | Passiv T2 | Glutklinge | `fire.ember_blade` | 2 | immer | Brand 3 s bei Nahkampf | intern 4 s/Ziel |
| Feuer | Primär | Flammenstoß | `fire.flame_burst` | 1 | ≥ 1 | 2,5 ♥, r 5 | **18 s** |
| Feuer | Sekundär | Meteor | `fire.meteor` | 1 | ≥ 3 | 4,5 → 2 ♥, r 4 | **40 s** |
| Feuer | Ultimate | Inferno-Sonne | `fire.inferno_sun` | 2 | ≥ 3 | max. 6,5 ♥, r 12 | **180 s** |
| Erde | Passiv T1 | Standfest | `earth.steadfast` | 1 | immer | Rückstoß −50 % | – |
| Erde | Passiv T2 | Bergmannsblut | `earth.miners_blood` | 2 | immer | Eile II beim Abbau | – |
| Erde | Primär | Erdbeben | `earth.quake` | 1 | ≥ 1 | 2 ♥, r 6, Langsamkeit II | **18 s** |
| Erde | Sekundär | Felsbrocken | `earth.boulder` | 1 | ≥ 3 | 4 ♥ / Splash 2 ♥ | **35 s** |
| Erde | Ultimate | Titanenschlag | `earth.titan_slam` | 2 | ≥ 3 | max. 6 ♥, r 9 | **180 s** |
| Wind | Passiv T1 | Federleicht | `wind.featherfall` | 1 | immer | kein Fallschaden | – |
| Wind | Passiv T2 | Doppelsprung | `wind.double_jump` | 2 | immer | +3 Blöcke | intern 2 s |
| Wind | Primär | Böe | `wind.gust` | 1 | ≥ 1 | 1 ♥, Kegel 6, Dash | **15 s** |
| Wind | Sekundär | Tornado | `wind.tornado` | 1 | ≥ 3 | max. 2,5 ♥, 5 s | **40 s** |
| Wind | Ultimate | Sturmauge | `wind.storm_eye` | 2 | ≥ 3 | 3 ♥ + 2 s wehrlos, r 10 | **180 s** |
| Schatten | Passiv T1 | Nachtauge | `shadow.night_eye` | 1 | immer | Nachtsicht | – |
| Schatten | Passiv T2 | Schattenmantel | `shadow.cloak` | 2 | immer | Unsichtbar beim Schleichen im Dunkeln | Sperre 8 s |
| Schatten | Primär | Schattenschritt | `shadow.shadow_step` | 1 | ≥ 1 | Teleport 20, +1,5 ♥ | **18 s** |
| Schatten | Sekundär | Seelenbann | `shadow.soul_ban` | 1 | ≥ 3 | 1 ♥ + Dunkelheit 6 s, r 7 | **40 s** |
| Schatten | Ultimate | Nachtfall | `shadow.nightfall` | 2 | ≥ 3 | max. 6 ♥, r 10, 8 s | **180 s** |
| Natur | Passiv T1 | Wurzelkraft | `nature.root_strength` | 1 | immer | Regeneration I auf Gras | – |
| Natur | Passiv T2 | Reiche Ernte | `nature.rich_harvest` | 2 | immer | Glück I, 25 % doppelte Ernte | – |
| Natur | Primär | Rankenfessel | `nature.vine_snare` | 1 | ≥ 1 | 2 s Fessel + 1 ♥ | **18 s** |
| Natur | Sekundär | Blütenheilung | `nature.blossom_heal` | 1 | ≥ 3 | 5 ♥ Heilung in 5 s, r 5 | **45 s** |
| Natur | Ultimate | Weltenbaum | `nature.world_tree` | 2 | ≥ 3 | Heilung + 3 ♥ Wurzeln, r 9 | **180 s** |

---

## 8. Vorschlag für die Config (Ausschnitt `config.jsonc`)

Neuer Block `abilities`. Schaden in **Herzen**, Zeiten in **Sekunden** (Telegraph-Zeiten in Ticks, weil sie an die Animation gebunden sind). Alle Werte gelten bei strength 1,0. Die vorhandenen `crystals.<id>.cooldownMultiplier/damageMultiplier` bleiben als globale Regler darüber.

```jsonc
"abilities": {
  // ---------------------------------------------------------------- Gemeinsam
  "common": {
    // Mindestenergie je Slot (Owner-Regel: Primär ab 1, Sekundär und Ultimate ab 3).
    "minEnergy": { "primary": 1, "secondary": 3, "ultimate": 3 },
    // Radius-Skalierung: Radius × (radiusBase + (1 - radiusBase) × strength).
    "radiusBase": 0.8,
    // Kontroll-Effekte (Fessel, Hochwurf, Langsamkeit) innerhalb dieses Fensters abschwächen.
    "diminishingReturnsSeconds": 6,
    "diminishingReturnsFactor": 0.5,
    // Ultimate-Aufladung bricht ab, wenn der Anwender so viel Schaden (Herzen) von Spielern nimmt.
    "ultimateInterruptHearts": 3.0,
    "ultimateInterruptCooldownFactor": 0.5,
    // Sperre der Ultimate nach Respawn/Login.
    "ultimateLockAfterRespawnSeconds": 10,
    // Warnsound/Actionbar-Warnung für Spieler in diesem Umkreis.
    "ultimateWarningRange": 48,
    // Verbündete (gleiches Scoreboard-Team) werden nicht getroffen und von Heilungen erfasst.
    "friendlyFire": false,
    "alliesByScoreboardTeam": true,
    // Mobs ohne Besitzer werden getroffen; Haustiere, Dorfbewohner, Händler nicht.
    "hitHostileMobs": true,
    "hitPassiveMobs": false,
    // Schilde blocken Direkttreffer von Projektil-Kräften.
    "shieldsBlockProjectiles": true,
    // Weltschutz.
    "respectSpawnProtection": true,
    "respectClaims": true
  },

  // ---------------------------------------------------------------- Feuer
  "fire": {
    "ember_heart":  { "enabled": true },
    "ember_blade":  { "enabled": true, "burnSeconds": 3, "perTargetCooldownSeconds": 4 },
    "flame_burst":  { "enabled": true, "cooldownSeconds": 18, "damageHearts": 2.5, "radius": 5.0,
                      "ringTicks": 10, "burnSeconds": 4, "knockback": 0.6,
                      "jumpUp": 0.9, "jumpForward": 0.4, "maxAirTicks": 30 },
    "meteor":       { "enabled": true, "cooldownSeconds": 40, "range": 30, "warningTicks": 30,
                      "centerDamageHearts": 4.5, "edgeDamageHearts": 2.0, "centerRadius": 1.5,
                      "radius": 4.0, "burnSeconds": 5, "knockback": 0.8, "knockUp": 0.4 },
    "inferno_sun":  { "enabled": true, "cooldownSeconds": 180, "chargeTicks": 40, "height": 8,
                      "radius": 12, "beams": 6, "beamIntervalTicks": 20, "beamWarningTicks": 10,
                      "beamDamageHearts": 1.5, "beamBurnSeconds": 3, "maxBeamsPerTarget": 3,
                      "collapseRadius": 7, "collapseDamageHearts": 2.0, "collapseKnockback": 1.0 }
  },

  // ---------------------------------------------------------------- Erde
  "earth": {
    "steadfast":    { "enabled": true, "knockbackResistance": 0.5 },
    "miners_blood": { "enabled": true, "hasteLevel": 2, "durationSeconds": 4,
                      "blockTag": "xmassmp:earth_mineable" },
    "quake":        { "enabled": true, "cooldownSeconds": 18, "chargeTicks": 8, "waveTicks": 12,
                      "radius": 6.0, "damageHearts": 2.0, "knockUp": 0.6,
                      "slownessLevel": 2, "slownessSeconds": 3, "groundOnly": true },
    "boulder":      { "enabled": true, "cooldownSeconds": 35, "chargeTicks": 15, "speed": 1.3,
                      "gravity": 0.03, "maxRange": 28, "directDamageHearts": 4.0,
                      "splashDamageHearts": 2.0, "splashRadius": 2.5, "knockback": 1.0,
                      "slownessLevel": 1, "slownessSeconds": 2 },
    "titan_slam":   { "enabled": true, "cooldownSeconds": 180, "jumpHeight": 6, "hoverTicks": 20,
                      "waveRadii": [3, 6, 9], "waveDamageHearts": [4.0, 3.0, 2.0], "waveGapTicks": 8,
                      "waveSlownessSeconds": 3, "pillarMaxTargets": 6, "pillarWarningTicks": 12,
                      "pillarDamageHearts": 2.0, "pillarKnockUp": 1.1, "pillarSlownessLevel": 3,
                      "pillarSlownessSeconds": 2, "selfResistanceLevel": 2, "selfResistanceSeconds": 4 }
  },

  // ---------------------------------------------------------------- Wind
  "wind": {
    "featherfall":  { "enabled": true },
    "double_jump":  { "enabled": true, "power": 0.75, "cooldownSeconds": 2 },
    "gust":         { "enabled": true, "cooldownSeconds": 15, "dashSpeed": 1.8, "dashUp": 0.2,
                      "coneAngle": 60, "coneRange": 6.0, "damageHearts": 1.0,
                      "knockback": 1.6, "knockUp": 0.4 },
    "tornado":      { "enabled": true, "cooldownSeconds": 40, "durationSeconds": 5, "speed": 0.2,
                      "pullRadius": 4.0, "pullStrength": 0.12, "sneakPullFactor": 0.5,
                      "coreRadius": 1.5, "lift": 0.35, "damageHeartsPerSecond": 0.5,
                      "maxDamageHearts": 2.5, "endKnockback": 1.2 },
    "storm_eye":    { "enabled": true, "cooldownSeconds": 180, "chargeTicks": 35, "radius": 10,
                      "selfHover": 4, "liftHeight": 6, "holdTicks": 40, "openSkyCheck": 8,
                      "slamDamageHearts": 3.0, "slownessLevel": 2, "slownessSeconds": 2,
                      "roofedKnockback": 1.2 }
  },

  // ---------------------------------------------------------------- Schatten
  "shadow": {
    "night_eye":    { "enabled": true },
    "cloak":        { "enabled": true, "maxLightLevel": 5, "sneakSeconds": 2, "lockoutSeconds": 8 },
    "shadow_step":  { "enabled": true, "cooldownSeconds": 18, "chargeTicks": 6, "targetRange": 20,
                      "blockRange": 12, "behindDistance": 1.5, "ambushBonusHearts": 1.5,
                      "ambushWindowSeconds": 3 },
    "soul_ban":     { "enabled": true, "cooldownSeconds": 40, "chargeTicks": 14, "radius": 7.0,
                      "damageHearts": 1.0, "darknessSeconds": 6, "weaknessLevel": 1,
                      "weaknessSeconds": 4 },
    "nightfall":    { "enabled": true, "cooldownSeconds": 180, "chargeTicks": 30, "radius": 10,
                      "durationSeconds": 8, "freeSteps": 3, "stepChargeTicks": 3,
                      "markDamageHearts": 2.0, "maxMarksPerTarget": 3,
                      "selfSpeedLevel": 1, "enemySlownessLevel": 1 }
  },

  // ---------------------------------------------------------------- Natur
  "nature": {
    "root_strength":{ "enabled": true, "regenerationLevel": 1, "combatLockSeconds": 5,
                      "blockTag": "xmassmp:nature_ground" },
    "rich_harvest": { "enabled": true, "luckLevel": 1, "doubleHarvestChance": 0.25 },
    "vine_snare":   { "enabled": true, "cooldownSeconds": 18, "range": 16, "travelTicks": 10,
                      "hitRadius": 1.5, "rootSeconds": 2, "damageHearts": 1.0 },
    "blossom_heal": { "enabled": true, "cooldownSeconds": 45, "chargeTicks": 10, "radius": 5.0,
                      "durationSeconds": 5, "healHeartsPerSecond": 1.0,
                      "cleanse": ["minecraft:poison", "minecraft:wither"] },
    "world_tree":   { "enabled": true, "cooldownSeconds": 180, "growTicks": 30, "durationSeconds": 10,
                      "radius": 9, "healHeartsPerSecond": 1.0, "rootIntervalTicks": 40,
                      "rootWarningTicks": 10, "rootSeconds": 1, "rootDamageHearts": 1.5,
                      "maxRoots": 4, "maxRootsPerTarget": 2, "pullBlocks": 1.0,
                      "absorptionLevel": 1, "absorptionSeconds": 15, "hitsToFell": 8 }
  }
}
```

Zusätzlich im vorhandenen Block `effects` (Budget je Slot, siehe 1.6):

```jsonc
"effects": {
  "quality": "HIGH",
  "maxParticlesPerTick": 3000,
  "maxDisplays": 256,
  // Richtwerte, die der Effekt-Code pro Anwendung nicht überschreiten soll (bei HIGH).
  "perAbilityPeakParticles": { "primary": 180, "secondary": 260, "ultimate": 400 },
  "perAbilityDisplays":      { "primary": 8,   "secondary": 12,  "ultimate": 32 },
  // Telegraph-Ringe von Ultimates weit sichtbar schicken (force/long distance).
  "ultimateTelegraphLongDistance": true
}
```

---

## 9. Offene Fragen an den Owner

| # | Frage | Vorschlag |
|---|---|---|
| 1 | Passen die Ultimates **Inferno-Sonne, Titanenschlag, Sturmauge, Nachtfall, Weltenbaum** von der Idee her, und ist **180 s** Cooldown richtig? | Ja, 180 s; nach Testrunden über `cooldownSeconds` anpassen |
| 2 | Wer zählt als **Verbündeter** (nicht getroffen, wird von Blütenheilung/Weltenbaum geheilt)? | Spieler im selben **Scoreboard-Team**; ohne Teams nur der Anwender selbst. Später ggf. ein eigenes `/xmas team`-System |
| 3 | Taste für die **Ultimate ohne Client-Mod** (Plan Frage 3) | Kristall in der Haupthand + Schleichen + Rechtsklick |
| 4 | Schatten Passiv T2: **Unsichtbarkeit** beim Schleichen im Dunkeln **oder** **Lebensraub nachts** (10 % des Nahkampfschadens)? | Unsichtbarkeit (klarer Konter: Licht, Rüstung sichtbar) |
| 5 | Erde Passiv T1: **Rückstoß −50 %** statt „kein Rückstoß / Resistenz I“ in Ordnung? | Ja; dauerhafte Resistenz I wäre bei 0 Energie zu stark |
