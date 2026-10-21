package de.xmassmp.ability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fähigkeiten nach ID und nach Kristall. */
public final class AbilityRegistry {

    private final Map<String, Ability> byId = new LinkedHashMap<>();

    public void register(Ability ability) {
        if (byId.putIfAbsent(ability.id(), ability) != null) {
            throw new IllegalArgumentException("Fähigkeit bereits registriert: " + ability.id());
        }
        if (ability instanceof ActiveAbility active && active.slot() != 1 && active.slot() != 2) {
            throw new IllegalArgumentException(ability.id() + ": slot muss 1 oder 2 sein");
        }
    }

    public Optional<Ability> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Ability> forCrystal(String crystalId) {
        List<Ability> out = new ArrayList<>();
        for (Ability ability : byId.values()) {
            if (ability.crystalId().equals(crystalId)) {
                out.add(ability);
            }
        }
        return out;
    }

    /** Aktive Fähigkeit eines Kristalls in einem Slot, falls vorhanden. */
    public Optional<ActiveAbility> active(String crystalId, int slot) {
        for (Ability ability : byId.values()) {
            if (ability instanceof ActiveAbility active
                    && active.crystalId().equals(crystalId) && active.slot() == slot) {
                return Optional.of(active);
            }
        }
        return Optional.empty();
    }
}
