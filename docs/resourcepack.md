# Ressourcenpaket

Quelle: `resourcepack/` (entpackt, korrigiert). Das Original `BlissGemsV4.1-fixed.zip` bleibt unverändert als Referenz im Repo.

```
python3 tools/resourcepack.py validate   # alle Modell-/Textur-/Font-Referenzen prüfen
python3 tools/resourcepack.py build      # prüfen + build/Xmas-SMP-resourcepack.zip (inkl. SHA-1)
```

Für Bitmap-Fonts prüft der Validator zusätzlich: ascent ≤ height, Bildgröße durch das `chars`-Raster teilbar, gleich lange `chars`-Zeilen und keine doppelt belegten Zeichen.

Ziel-Versionen: `pack.mcmeta` mit `min_format` 75 (1.21.11) bis `max_format` 97 (26.3 laut einer Quelle, noch zu verifizieren).

## Sprachdateien

`assets/xmassmp/lang/de_de.json` (Referenz) und `en_us.json`. Der Code nutzt übersetzbare Texte mit deutschem Fallback, d. h. ohne Pack sehen Spieler die deutschen Namen. Der Validator prüft, dass alle Sprachen dieselben Schlüssel haben; ein Test im Kern prüft, dass jeder im Code verwendete Schlüssel existiert.

## Kristalle

| Kristall | Gem-Textur | Gem-Index |
|---|---|---|
| Feuer | strength | 7 |
| Erde | fire | 2 |
| Wind | puff | 5 |
| Schatten | astra | 1 |
| Natur | wealth | 8 |

Nicht genutzt, aber im Pack: flux (3), life (4), speed (6), All-for-One (9001/9002).

## Custom-Model-Data

Basis-Item für alle Gems: `minecraft:echo_shard`.

`CMD = 1000 × Tier + Stufen-Offset + Gem-Index`

| Stufe (Modell) | Offset | Textur | Optik |
|---|---|---|---|
| `*_tierN` | 0 | `*tierNpristine2` | zerbrochen |
| `*_tierN_pristine2` | 20 | `*tierNpristine3` | beschädigt |
| `*_tierN_pristine3` | 30 | `*tierNpristine4` | zerkratzt |
| `*_tierN_pristine4` | 40 | `*tierN` | makellos |

Achtung: Modellname und Texturname sind um eine Stufe versetzt (aus dem Original übernommen).
Beispiel: Feuer (strength), Tier 2, makellos → `2047`.

| Item | Basis-Item | CMD |
|---|---|---|
| Revival Tome | `enchanted_book` | 1 |
| Upgrader | `enchanted_book` | 3001 |
| Energy Bottle | `ghast_tear` | 4001 |
| Gem Trader | `emerald` | 4002 |
| Repair Kit | `beacon` | 4003 |
| Gem Fragment | `prismarine_shard` | 4004 |
| All-for-One Tier 1 / 2 | `echo_shard` | 9001 / 9002 |

`range_dispatch` wählt den höchsten Schwellenwert ≤ CMD. Werte zwischen den Einträgen zeigen daher das darunterliegende Modell – das Plugin setzt immer exakte Werte.

## HUD-Icons (Font `minecraft:default`)

| Zeichen (Java `\uXXXX`) | Datei | ascent / height | Inhalt |
|---|---|---|---|
| `U+E010`–`U+E01F` | `icons/<gem>1.png`, `icons/<gem>2.png` | 8 / 16 | je Gem zwei Icons in der Reihenfolge astra, fire, flux, life, puff, speed, strength, wealth |
| `U+E020` | `icons/ultimate_fire.png` | 8 / 16 | Ultimate Feuer (rot, Flamme) |
| `U+E021` | `icons/ultimate_earth.png` | 8 / 16 | Ultimate Erde (orange, Fels) |
| `U+E022` | `icons/ultimate_wind.png` | 8 / 16 | Ultimate Wind (weiß, Wirbel) |
| `U+E023` | `icons/ultimate_shadow.png` | 8 / 16 | Ultimate Schatten (lila, Auge) |
| `U+E024` | `icons/ultimate_nature.png` | 8 / 16 | Ultimate Natur (grün, Blatt) |
| `U+E030`–`U+E03A` | `hud/bottle_progress.png` | 6 / 6 | Abfüll-Ladebalken der Energy Bottle: 0 %, 10 %, … 100 % (`U+E030` + Prozent / 10) |

`U+E025`–`U+E02F` sind frei (Reserve für weitere Kristall-Icons).

**Ultimate-Icons** (Platzhalter): Kristall-Raute mit Facetten, vier Strahlen und Funken in der Kristallfarbe (Original-Gem-Farbe, siehe „Kristalle“), in der Mitte ein Emblem je Kristall. Format wie die übrigen Icons (16×16, ascent 8, height 16).

**Ladebalken:** Spritesheet `hud/bottle_progress.png`, 32×66 px = 11 Zellen à 32×6 px untereinander (eine `chars`-Zeile pro Zelle). Dunkler Rahmen mit abgerundeten Ecken, innen 30×4 px; die türkise Füllung (Verlauf oben hell → unten dunkel, leicht links → rechts) wächst um 3 px pro Stufe. Alle Stufen haben denselben Rahmen und damit dieselbe Breite (Vorschub 33), der Untertitel springt also nicht.

Position (ascent 6, height 6): Vanilla zeichnet den Untertitel 2× skaliert bei y = 5 relativ zur Bildschirmmitte, also 10 GUI-px darunter. Eine Bitmap-Glyphe beginnt `7 − ascent` Pixel unter der Textzeile; die Oberkante liegt damit bei `Mitte + 2 · (5 + 7 − ascent)` = `Mitte + 24 − 2 · ascent`. `height` 6 = Zellenhöhe, d. h. Maßstab 1 ohne Verzerrung; da ascent nicht größer als height sein darf, ist 6 der höchste mögliche Wert. Der Balken liegt so bei Mitte + 12 bis Mitte + 24 GUI-px (64 px breit), das 15 px große Fadenkreuz endet bei Mitte + 7 – rund 4 px Abstand. Jeder Schritt kleineres (bzw. negatives) ascent schiebt den Balken 2 GUI-px tiefer.

## Korrekturen gegenüber dem Original

- Strength-Texturen auf das Namensschema der anderen Gems umbenannt (`strengthgemtierNpristine2.png` → `strengthgemtierN.png`, `strengthgemtierNpristine.png` → `strengthgemtierNpristine2.png`). Vorher fehlte die makellose Textur und Stufe 1 war makellos statt zerbrochen.
- All-for-One: fehlende Modelle `xmassmp:item/allforone_gem_t1/_t2` angelegt, Verweise in `echo_shard.json` korrigiert (zeigten auf nicht existierende `minecraft:item/…`).
- Ungültiges `"parent": "generated"` aus `energy_bottle`, `gem_trader`, `trader`, `upgrader` entfernt (Modelle definieren eigene `elements`).
- `enchanted_book.json`: Schutz-Schwellen 2 und 3002 → nur CMD 1 und 3001 zeigen Custom-Modelle, andere Bücher bleiben normal.
- Energy Bottle nur noch auf `ghast_tear` (doppelte Belegung von `nether_star` entfernt).
- `white_dye-working.txt` (Arbeitsdatei im `items`-Ordner) entfernt.
- `pack.mcmeta` auf `min_format`/`max_format` für 1.21.11 – 26.3 umgestellt, Beschreibung auf Xmas-SMP.
- Namespace `blissgems` → `xmassmp` (Ordner `assets/xmassmp/`, alle Verweise in Items, Modellen und Font).
- Ungenutzte Dateien `empty.png` (magenta Platzhalter) und `repairitem.png` (UI-Atlas-Fragment) entfernt; im Original-ZIP weiterhin enthalten.

## Offen

- `trader.json` (Modell) ist noch keinem Item zugeordnet → für eine spätere Trader-Station reserviert.
- `gem-breaking/*` werden nicht verwendet (Riss-Overlays für die Bruch-Animation vorgesehen).
- `strengthgemtier2pristine3/4.png` haben graue Randpixel links – optisch nachbessern.
