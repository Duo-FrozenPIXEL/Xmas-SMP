# Paper (geplant)

Kommt nach Fabric (todo.md, Abschnitt 9). Der Adapter implementiert dieselben Schnittstellen
wie `fabric/`:

- `de.xmassmp.platform.Platform` – Ordner, Logger
- `de.xmassmp.effect.EffectSink` – Partikel/Sounds über die Bukkit-API
- `de.xmassmp.event.EventHost` – Broadcast, Titel, Sound, Konsolenbefehl
- `/xmas` über Brigadier (Paper Command API) → `de.xmassmp.admin.AdminService`
- `XmasCore#tick()` jeden Server-Tick (Scheduler)

Das Modul wird erst in `settings.gradle.kts` eingetragen, wenn es Code enthält.
