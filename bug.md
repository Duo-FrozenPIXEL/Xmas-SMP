# Xmas-SMP – Gefundene Fehler

Stand: 2026-10-22, Commit `c11be83`. Reine Bestandsaufnahme – **noch nichts behoben**.

Geprüft: `common/`, `fabric/`, Gradle-/CI-Dateien, `tools/resourcepack.py`, `resourcepack/`.
`python3 tools/resourcepack.py validate` → 0 Fehler, 8 Warnungen (ungenutzte `gem-breaking/*`, bekannt aus `todo.md`).
Die Kern-Tests (`./gradlew :common:test`) konnten hier nicht laufen: Maven Central antwortete mit HTTP 429 (Too Many Requests).

Legende: **Hoch** = Absturz oder Datenverlust möglich · **Mittel** = falsches Verhalten im normalen Betrieb · **Niedrig** = Randfall, Komfort oder Doku · **(V)** = vor dem Fix verifizieren

---

## Hoch

### 1. Ungültige Sound-ID in `events.jsonc → messages` lässt den Server abstürzen
- **Wo:** `EventScheduler.java:320-338` (`announce`, `countdown`), `FabricEventHost.java:42`, `XmasCore.java:88-100`
- **Problem:** `announceSound` und `countdownSound` werden beim Laden nicht geprüft (`EventLoader.parseMessages`, Z. 87-98) und anders als Event-Aktionen ohne `try/catch` ausgeführt. `Identifier.parse` wirft bei ungültigen Zeichen (z. B. `"minecraft:block.note_block.Bell"` – Großbuchstabe, Leerzeichen) eine Exception. Sie läuft durch `EventScheduler.tick` → `XmasCore.tick` → `END_SERVER_TICK` und beendet den Server.
- **Folge:** `lastProcessed` wird vorher nicht aktualisiert. Derselbe Auslöser würde also bei jedem Versuch erneut feuern, und Aktionen davor im selben Fenster liefen doppelt.
- **Gleiche Ursache:** Auch `Listener.started/ended` (Z. 303, 316) und `XmasCore.tick()` insgesamt haben keine Absicherung. Jede RuntimeException im Kern bringt so den Server zum Absturz.

### 2. `null`-Eintrag unter `crystals` in `config.jsonc` → NullPointerException beim Start bzw. Reload
- **Wo:** `XmasConfig.java:97-106`, `ConfigManager.java:65`
- **Problem:** Bei `"crystals": { "fire": null }` greift `validate()` auf `settings.cooldownMultiplier` zu. `ConfigManager.load()` fängt nur `IOException | JsonParseException`, die NPE fliegt also weiter.
- **Folge:** Beim Serverstart bricht `SERVER_STARTING` ab und der Server startet nicht. Bei `/xmas reload` gibt es nur „unerwarteter Fehler“, und `events.jsonc` wird ebenfalls nicht neu geladen. Das widerspricht dem dokumentierten Versprechen „ein Reload bringt den Server nie in einen kaputten Zustand“.

---

## Mittel

### 3. `simulate off` verwirft simulierte Events ohne deren `onEnd`-Aktionen
- **Wo:** `EventService.java:122-147`
- **Problem:** Im Testmodus ohne `dry` laufen `onStart`-Aktionen wirklich, auch `command`-Aktionen wie `weather`, `gamerule` oder `time`. `stopSimulation()` stellt nur den alten Zustand wieder her. Die zu diesem Zeitpunkt laufenden simulierten Events enden nie, ihre `onEnd`-Aktionen werden nicht ausgeführt.
- **Folge:** Änderungen aus dem Test bleiben auf dem echten Server bestehen. Dasselbe gilt für `/xmas event start` während des Testmodus.

### 4. Der echte Event-Plan pausiert im Testmodus, danach enden echte Events ohne `onEnd`
- **Wo:** `EventService.java:94-104` und `122-147`, `EventScheduler.java:121-129` und `184-213`
- **Problem:** Während der Simulation wird nur die simulierte Uhr getickt. Nach `simulate off` kommt der gesicherte Zustand mit dem alten `lastProcessed` zurück. Ist die Lücke größer als 30 s, wertet der Scheduler das als „Server war aus“ (`recover`).
  - Echte Events, die während des Tests hätten enden sollen, enden **still**, ohne `onEnd`.
  - Ankündigungen, Countdowns und punktuelle Events (Dauer 0) aus dieser Zeit entfallen komplett.
- **Folge:** Wer in der Weihnachtswoche einen Testlauf macht, kann echte Events „verschlucken“.

### 5. Auch ein Trockenlauf (`dry`) wirkt auf das echte Spiel
- **Wo:** `XmasCore.java:67-68`, `EventService.java:118-120`, `EventScheduler.java:248-257` und `303`
- **Problem:** `modifier()` liest die laufenden Events des Schedulers, und im Testmodus sind das die *simulierten*.
- **Folge:** Ein simuliertes Event mit `"cooldown_multiplier": 0.5` halbiert echte Fähigkeits-Cooldowns, auch im Trockenlauf. Umgekehrt fallen Modifikatoren echter laufender Events während des Tests weg. Listener (geplant für den Christmas-Twist) werden im Trockenlauf ebenfalls aufgerufen.

### 6. `general.enabled = false` stoppt auch das Autosave der Spielerdaten
- **Wo:** `XmasCore.java:88-100`
- **Problem:** Der frühe `return` überspringt neben Effekten und Events auch `players.saveDirty()`.
- **Folge:** Stürzt der Server in dieser Zeit ab, gehen Änderungen verloren (z. B. aus `/xmas set`). Ausnahme: `set` speichert sofort. Nach dem Wiedereinschalten greift außerdem die Pausenlogik aus Nr. 4: Events enden still.

### 7. `AbilityRegistry.register` lässt eine ungültige Fähigkeit registriert zurück
- **Wo:** `AbilityRegistry.java:14-21`
- **Problem:** `putIfAbsent` läuft *vor* der Slot-Prüfung. Eine aktive Fähigkeit mit Slot 3 wirft zwar eine Exception, steckt danach aber schon in der Map. Über `get()` und `forCrystal()` ist sie dann erreichbar, und ein korrigierter zweiter Versuch scheitert mit „bereits registriert“.

### 8. Verzögerte Sounds in der EffectEngine sind nicht abgesichert
- **Wo:** `EffectEngine.java:136-145`
- **Problem:** `playDueSounds()` läuft außerhalb des `try/catch` der Effekt-Schleife. Wirft der Sink, etwa wegen einer ungültigen Sound-ID in einem `SoundStack` mit `delayTicks > 0`, wird der Eintrag nicht entfernt (`it.remove()` kommt nach dem Aufruf).
- **Folge:** Die Exception kommt in jedem Tick wieder und bringt den Server zum Absturz (siehe Nr. 1). Sofortige Sounds sind dagegen abgesichert.

### 9. (V) Gson-Version zur Laufzeit unter Fabric
- **Wo:** `JsonFiles.java:6` und `38` (`Strictness`, erst ab Gson 2.11), `fabric/build.gradle.kts:30-31`
- **Problem:** Kompiliert wird gegen Gson 2.11.0 aus `common`. Zur Laufzeit gilt aber die Gson-Version, die Minecraft mitliefert, denn das Fabric-JAR enthält kein eigenes Gson.
- **Folge:** Liefert die Zielversion ein älteres Gson, scheitert jeder Config-/JSON-Zugriff mit `NoClassDefFoundError`. Für 1.21.11 und alle 26.x prüfen; beim ersten Testserver-Start fällt es sofort auf.

---

## Niedrig

### 10. `/xmas reload` meldet immer Erfolg
- **Wo:** `XmasCore.java:80-85`, `XmasCommands.java:108-113`
- **Problem:** `LoadResult.success()` wird ignoriert. Auch bei kaputter `config.jsonc` erscheint grün „Neu geladen“, der Fehler steht nur grau darunter.

### 11. Audit-Log schreibt Befehlsblöcke, Funktionen und RCON als „Konsole“
- **Wo:** `XmasCommands.java:144-147`
- **Problem:** Alles, was kein Spieler ist, wird zu `Konsole`. `getSource().getTextName()` würde die tatsächliche Quelle liefern.

### 12. Befehl vor dem Start des Kerns → „unerwarteter Fehler“
- **Wo:** `XmasCommands.java:136-142`
- **Problem:** `admin()` wirft eine `IllegalStateException` statt einer verständlichen Fehlermeldung. Das tritt z. B. ein, wenn `XmasCore.start()` in `SERVER_STARTING` fehlschlug.

### 13. Spielerdaten werden beim Laden nicht auf Plausibilität geprüft
- **Wo:** `PlayerDataStore.java:82-89`
- **Problem:** `tier` und `energy` werden ungeprüft aus der Datei übernommen. Ein `"tier": 3` (Handbearbeitung oder späteres Schema) führt erst später zu einer `IllegalArgumentException` in `CrystalModels.checkTier`.
- **Ähnlich:** Werden `energy.min` oder `energy.max` in der Config geändert, behalten bestehende Spieler Werte außerhalb des neuen Bereichs.

### 14. Fehlgeschlagenes Speichern verwirft Spielerdaten
- **Wo:** `PlayerDataStore.java:57-67` und `103-118`
- **Problem:** `unload()` und `saveAndUnloadAll()` entfernen die Daten auch dann aus dem Cache, wenn `write()` fehlschlug (nur geloggt). Es gibt keinen zweiten Versuch, die Änderungen sind weg.

### 15. Beim Start mit komplett kaputter `events.jsonc` ist die Meldung irreführend
- **Wo:** `EventService.java:79-83`
- **Problem:** Die Meldung lautet „vorherige Events bleiben aktiv“, beim Serverstart gibt es aber keine vorherigen. Aus `event-state.json` wiederhergestellte laufende Events haben dann keine Definition: Sie enden ohne `onEnd`, und ihre Modifikatoren wirken nicht.

### 16. Ankündigung und Countdown, obwohl das Event schon läuft
- **Wo:** `EventScheduler.java:138-158` und `291-295`
- **Problem:** Läuft ein Event bereits, etwa weil es manuell gestartet wurde, wird trotzdem „beginnt in X Minuten“ samt Countdown gesendet. Der eigentliche Start wird dann mit „läuft bereits“ übersprungen.

### 17. Zeitsprung rückwärts startet bereits gelaufene Events erneut
- **Wo:** `EventScheduler.java:121-124` und `199-200`
- **Problem:** Bei `now < lastProcessed` (Systemuhr zurückgestellt) ruft der Scheduler `recover(null, now)` auf. Mit `last == null` gilt jedes Event, dessen Zeitfenster `now` enthält und das gerade nicht läuft, als „verpasst“ und wird nachgeholt. Das gilt auch, wenn es in diesem Fenster schon lief und beendet wurde.

### 18. `nextStart` sucht nur 400 Tage voraus
- **Wo:** `EventScheduler.java:237-245`
- **Problem:** Ein jährliches Event am `02-29` zeigt in `/xmas event list` meist „kein weiterer Termin“, obwohl es stattfinden wird.

### 19. Platzhalter-Ersetzung
- **Wo:** `ActionContext.java:20-26`, `EventScheduler.java:348`
- **Ersetzung nacheinander:** Enthält ein eingesetzter Wert selbst einen Platzhalter (z. B. Eventname `"Bonus {id}"`), wird dieser erneut ersetzt. Die Reihenfolge hängt von der `HashMap` ab.
- **`{duration}` rundet ab:** Der Wert nutzt `toMinutes()`, Bruchteile gehen verloren (`durationMinutes: 0.5` → „0 Minuten“).

### 20. Unverständliche Fehlermeldungen bei falschen Typen in `events.jsonc`
- **Wo:** `EventLoader.java:110, 123, 137, 153, 164`
- **Problem:** Zum Beispiel ergibt `"dates": "12-24"` statt `["12-24"]` eine `ClassCastException`-Meldung wie „class com.google.gson.JsonPrimitive cannot be cast …“ statt eines Hinweises auf das richtige Format.

### 21. `WorldRef`-ID unter Fabric hängt an `toString()`
- **Wo:** `FabricPlatform.java:72-80`
- **Problem:** Die ID ist `String.valueOf(level.dimension())`, also `ResourceKey[minecraft:dimension / minecraft:overworld]`. Ein naheliegendes `new WorldRef("minecraft:overworld")` findet nie eine Welt, und das Format kann sich mit der Minecraft-Version ändern.
- **Nebenbei:** Die Suche baut den String für jede Welt bei *jedem* Partikel neu.

### 22. `LocalizedText.toPlainString` und `%` im Text
- **Wo:** `LocalizedText.java:42-45`
- **Problem:** Ein Fallback mit `%` (z. B. „50% schneller“) plus Argumenten wirft eine `IllegalFormatException`.

### 23. Custom-Items behalten ihre Vanilla-Funktion
Bekannt als offener Punkt (todo 4.1 „seelengebunden“), wird aber zum echten Fehler, sobald Items verteilt werden:
- Kristall auf Basis `echo_shard`:
  - lässt sich zum Bergungskompass verarbeiten
  - stapelt bis 64, zweimal `/xmas give` ergibt einen Stapel aus zwei Kristallen
- Reparatur-Set (`beacon`): lässt sich als Leuchtfeuer-Block platzieren
- Energieflasche (`ghast_tear`): funktioniert als Brau-Zutat
- Kristall-Händler (`emerald`): gilt als Währung beim Dorfbewohner-Handel
- Kristall-Splitter (`prismarine_shard`): lässt sich craften

### 24. CI-Matrix überschreibt nur `minecraft_version`
- **Wo:** `.github/workflows/build.yml:64`
- **Problem:** `fabric_api_version`, `polymer_version` und `sgui_version` sind fest auf 1.21.11 gesetzt. Kommt eine zweite Version in die Matrix, bricht der Build bzw. lädt falsche Abhängigkeiten.

### 25. Pack-Validator: Lücken
- **Wo:** `tools/resourcepack.py:85` und `104-105`
- Texturen im `minecraft`-Namespace, die im Pack fehlen, gelten automatisch als Vanilla. Ein Tippfehler (`minecraft:item/echo_shrad`) fällt also nicht auf.
- Fehlt bei einem Eintrag `threshold`, stürzt das Script mit einem `TypeError` bei `sorted()` ab, statt einen Fehler zu melden.

### 26. README: CI-Beschreibung stimmt nicht
- **Wo:** `README.md` (Abschnitt Entwicklung)
- **Problem:** Dort steht „bei jedem Push“. Der Workflow läuft aber nur bei Pushes auf `main`, bei Pull Requests und manuell.
