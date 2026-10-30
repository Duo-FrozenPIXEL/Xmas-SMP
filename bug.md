# Xmas-SMP – Gefundene Fehler

Stand: 2026-10-29, Branch `fix/bugs-und-grundlagen`. Behobene Einträge sind entfernt; die Nummern der übrigen bleiben unverändert.

Legende: **Hoch** = Absturz oder Datenverlust möglich · **Mittel** = falsches Verhalten im normalen Betrieb · **Niedrig** = Randfall, Komfort oder Doku · **(V)** = vor dem Fix verifizieren

## Behoben am 2026-10-29

- **`main` kompilierte nicht** (neu gefunden, *Hoch*): Beim Merge von PR #3 gingen in `XmasCore` die Felder `crystalService`, `energy`, `hud`, `playerListeners` sowie die Ticks von Kristall-Items und HUD verloren. Die CI ist seitdem rot. Wiederhergestellt.
- Nr. 3–8 und 10–22, 24–26. Kurz:
  - Testmodus (3–5): Die Simulation läuft in einem eigenen Plan, der echte Plan läuft in echter Zeit weiter. `simulate off` beendet simulierte Events mit End-Aktionen. Im Trockenlauf wirken simulierte Modifikatoren und Listener nicht.
  - Pausen (6, 15): Autosave läuft auch bei `general.enabled = false`. Nach einer Pause (Plugin aus, `events.jsonc` defekt) enden abgelaufene Events mit End-Aktionen. Bei defekter Datei beim Start bleiben wiederhergestellte Events erhalten, und die Meldung stimmt.
  - Scheduler (16–19): keine Ankündigung, solange das Event noch läuft. Eine zurückgestellte Uhr wiederholt nichts. `nextStart` schaut 8 Jahre voraus. Platzhalter werden in einem Durchgang ersetzt, `{duration}` behält Bruchteile.
  - Robustheit (7, 8, 12, 13, 14, 20, 22): Prüfung vor dem Eintragen in die Registry, verzögerte Sounds werfen nur einmal, verständliche Fehler vor dem Start, Spielerdaten auf gültige Werte begrenzt, kein Datenverlust bei Schreibfehlern, klare Typfehler in `events.jsonc`, `%` im Fallback-Text.
  - Sonstiges (10, 11, 21, 24, 25, 26): `/xmas reload` meldet Fehler, das Audit-Log nennt die echte Quelle, `WorldRef` nutzt die Dimensions-ID, die CI-Matrix setzt alle Versionen, Lücken im Pack-Validator geschlossen, README korrigiert.
- Nr. 9 (Gson): geprüft, kein Fehler. 1.21.11 und 26.1 liefern Gson 2.13.2, 26.2 und 26.3 Gson 2.14.0 (`docs/versionen.md`). Paper und NeoForge sind noch zu prüfen, sobald es die Adapter gibt.

Kern-Tests: alle grün. Wegen fehlendem Gradle-Zugriff liefen sie mit `javac` und einem kleinen JUnit-Ersatz. Die Fabric-Quellen sind nicht kompiliert, das übernimmt die CI.

---

## Niedrig

### 23. Custom-Items behalten ihre Vanilla-Funktion
Bekannt als offener Punkt (todo 4.1 „seelengebunden“). Das wird zum echten Fehler, sobald Items verteilt werden:
- Kristall auf Basis `echo_shard`: lässt sich zum Bergungskompass verarbeiten. Die Stapelgröße ist inzwischen 1.
- Reparatur-Set (`beacon`): lässt sich als Leuchtfeuer-Block platzieren
- Energieflasche (`ghast_tear`): funktioniert als Brau-Zutat
- Kristall-Händler (`emerald`): gilt als Währung beim Dorfbewohner-Handel
- Kristall-Splitter (`prismarine_shard`): lässt sich craften

**Behoben am 2026-10-30:** Alle Xmas-Items sind jetzt eigene Polymer-Server-Items (`xmassmp:<typ>`) und haben dadurch keine Vanilla-Funktion mehr (im Testserver geprüft: Crafter, TNT). Ursprüngliche Einschätzung: Sauber lösen ließe sich das mit eigenen Server-Items über Polymer (`PolymerItem`, Anzeige als Vanilla-Item) oder mit dem Umstieg auf die `item_model`-Komponente (todo 1.3). Die Alternative wären mehrere Mixins in Crafting, Brauen, Handel und Platzieren. Beides ändert die Item-Architektur und muss im Spiel getestet werden.
