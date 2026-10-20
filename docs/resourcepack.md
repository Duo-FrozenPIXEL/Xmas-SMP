# Ressourcenpaket

Quelle: `resourcepack/` (entpackt, korrigiert). Das Original `BlissGemsV4.1-fixed.zip` bleibt unverändert als Referenz im Repo.

```
python3 tools/resourcepack.py validate   # alle Modell-/Textur-/Font-Referenzen prüfen
python3 tools/resourcepack.py build      # prüfen + build/Xmas-SMP-resourcepack.zip (inkl. SHA-1)
```

Ziel-Versionen: `pack.mcmeta` mit `min_format` 75 (1.21.11) bis `max_format` 97 (26.3 laut einer Quelle, noch zu verifizieren).

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

``–``, je Gem zwei Icons in der Reihenfolge astra, fire, flux, life, puff, speed, strength, wealth.

## Korrekturen gegenüber dem Original

- Strength-Texturen auf das Namensschema der anderen Gems umbenannt (`strengthgemtierNpristine2.png` → `strengthgemtierN.png`, `strengthgemtierNpristine.png` → `strengthgemtierNpristine2.png`). Vorher fehlte die makellose Textur und Stufe 1 war makellos statt zerbrochen.
- All-for-One: fehlende Modelle `blissgems:item/allforone_gem_t1/_t2` angelegt, Verweise in `echo_shard.json` korrigiert (zeigten auf nicht existierende `minecraft:item/…`).
- Ungültiges `"parent": "generated"` aus `energy_bottle`, `gem_trader`, `trader`, `upgrader` entfernt (Modelle definieren eigene `elements`).
- `enchanted_book.json`: Schutz-Schwellen 2 und 3002 → nur CMD 1 und 3001 zeigen Custom-Modelle, andere Bücher bleiben normal.
- Energy Bottle nur noch auf `ghast_tear` (doppelte Belegung von `nether_star` entfernt).
- `white_dye-working.txt` (Arbeitsdatei im `items`-Ordner) entfernt.
- `pack.mcmeta` auf `min_format`/`max_format` für 1.21.11 – 26.3 umgestellt, Beschreibung auf Xmas-SMP.

## Offen

- `trader.json` (Modell) ist noch keinem Item zugeordnet → für eine spätere Trader-Station reserviert.
- `repairitem.png`, `empty.png`, `gem-breaking/*` werden nicht verwendet (Riss-Overlays für die Bruch-Animation vorgesehen).
- `strengthgemtier2pristine3/4.png` haben graue Randpixel links – optisch nachbessern.
