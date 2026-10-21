package de.xmassmp.crystal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Alle bekannten Kristalle. Erweiterbar, z. B. für den Christmas-Twist. */
public final class CrystalRegistry {

    private final Map<String, Crystal> crystals = new LinkedHashMap<>();

    public static CrystalRegistry withDefaults() {
        CrystalRegistry registry = new CrystalRegistry();
        Crystals.DEFAULTS.forEach(registry::register);
        return registry;
    }

    public void register(Crystal crystal) {
        if (crystals.containsKey(crystal.id())) {
            throw new IllegalArgumentException("Kristall bereits registriert: " + crystal.id());
        }
        for (Crystal existing : crystals.values()) {
            if (existing.gemTexture().equals(crystal.gemTexture())) {
                throw new IllegalArgumentException("Textur " + crystal.gemTexture()
                        + " wird schon von " + existing.id() + " verwendet");
            }
        }
        crystals.put(crystal.id(), crystal);
    }

    public Optional<Crystal> get(String id) {
        return Optional.ofNullable(id == null ? null : crystals.get(id));
    }

    public Collection<Crystal> all() {
        return Collections.unmodifiableCollection(crystals.values());
    }

    public List<String> ids() {
        return List.copyOf(crystals.keySet());
    }

    /** Zufälliger Kristall aus {@code candidates} (z. B. nur aktivierte). */
    public static Optional<Crystal> random(List<Crystal> candidates, RandomGenerator random) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }
}
