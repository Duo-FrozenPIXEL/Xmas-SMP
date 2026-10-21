package de.xmassmp.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistenter Zustand eines Spielers. Cooldowns werden als Ablaufzeitpunkt (Epoch-Millis)
 * gespeichert und überstehen so Neustarts.
 */
public final class PlayerData {

    static final int SCHEMA_VERSION = 1;

    private final UUID uuid;
    private String crystalId;
    private int tier;
    private int energy;
    private final Map<String, Long> cooldownUntil = new HashMap<>();
    private transient boolean dirty;

    public PlayerData(UUID uuid, int startEnergy) {
        this.uuid = uuid;
        this.tier = 1;
        this.energy = startEnergy;
        this.dirty = true;
    }

    PlayerData(UUID uuid, String crystalId, int tier, int energy, Map<String, Long> cooldowns) {
        this.uuid = uuid;
        this.crystalId = crystalId;
        this.tier = tier;
        this.energy = energy;
        this.cooldownUntil.putAll(cooldowns);
    }

    public UUID uuid() {
        return uuid;
    }

    /** ID des aktiven Kristalls oder {@code null}. Nur ein Kristall pro Spieler. */
    public String crystalId() {
        return crystalId;
    }

    public void setCrystalId(String crystalId) {
        this.crystalId = crystalId;
        this.dirty = true;
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
        this.dirty = true;
    }

    public int energy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        this.dirty = true;
    }

    public boolean isOnCooldown(String abilityId, long nowMillis) {
        return remainingCooldownMillis(abilityId, nowMillis) > 0;
    }

    public long remainingCooldownMillis(String abilityId, long nowMillis) {
        Long until = cooldownUntil.get(abilityId);
        return until == null ? 0 : Math.max(0, until - nowMillis);
    }

    public void setCooldownUntil(String abilityId, long untilMillis) {
        cooldownUntil.put(abilityId, untilMillis);
        dirty = true;
    }

    public void clearCooldowns() {
        if (!cooldownUntil.isEmpty()) {
            cooldownUntil.clear();
            dirty = true;
        }
    }

    /** Entfernt abgelaufene Cooldowns, damit die Datei nicht wächst. */
    public void pruneCooldowns(long nowMillis) {
        if (cooldownUntil.values().removeIf(until -> until <= nowMillis)) {
            dirty = true;
        }
    }

    public Map<String, Long> cooldowns() {
        return Collections.unmodifiableMap(cooldownUntil);
    }

    boolean isDirty() {
        return dirty;
    }

    void markClean() {
        dirty = false;
    }
}
