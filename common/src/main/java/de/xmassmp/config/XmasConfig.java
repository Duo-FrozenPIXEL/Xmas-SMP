package de.xmassmp.config;

import de.xmassmp.effect.EffectQuality;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inhalt von {@code config.jsonc}. Fehlende Felder behalten die hier gesetzten Standardwerte.
 * Die dokumentierte Vorlage liegt unter {@code resources/xmassmp/config.default.jsonc}.
 */
public final class XmasConfig {

    public General general = new General();
    public Map<String, CrystalSettings> crystals = new LinkedHashMap<>();
    public CrystalItem crystalItem = new CrystalItem();
    public Energy energy = new Energy();
    public Effects effects = new Effects();
    public Events events = new Events();
    public Audit audit = new Audit();

    public static final class General {
        public boolean enabled = true;
        public boolean abilitiesEnabled = true;
    }

    public static final class CrystalSettings {
        public boolean enabled = true;
        public double cooldownMultiplier = 1.0;
        public double damageMultiplier = 1.0;
    }

    public static final class CrystalItem {
        public boolean giveOnJoin = true;
        public boolean soulbound = true;
    }

    /** (V) Vorläufige Werte, bis das Original-Energiesystem recherchiert ist (todo.md, 2). */
    public static final class Energy {
        public int start = 5;
        public int min = 0;
        public int max = 10;
        public int killGain = 1;
        public int deathLoss = 1;
        /** Kein Gewinn, wenn derselbe Spieler innerhalb dieser Zeit erneut besiegt wird. 0 = aus. */
        public int sameVictimCooldownMinutes = 30;
        public Stages stages = new Stages();
        /** Stärke der Fähigkeiten bei der niedrigsten Energie über „zerbrochen“; bei max = 1.0. */
        public double minStrength = 0.6;
    }

    /** Ab welcher Energie eine Bruchstufe gilt; darunter ist der Kristall zerbrochen. */
    public static final class Stages {
        public int damaged = 1;
        public int scratched = 4;
        public int pristine = 7;
    }

    public static final class Effects {
        public EffectQuality quality = EffectQuality.HIGH;
        public int maxParticlesPerTick = 3000;
        public int maxDisplays = 256;
    }

    public static final class Events {
        public String timezone = "Europe/Berlin";
        public boolean catchUpMissed = true;
    }

    public static final class Audit {
        public boolean enabled = true;
    }

    /** Einstellungen eines Kristalls; unbekannte Kristalle bekommen Standardwerte. */
    public CrystalSettings crystal(String id) {
        CrystalSettings settings = crystals.get(id);
        return settings != null ? settings : new CrystalSettings();
    }

    /** Korrigiert ungültige Werte und liefert Warnungen dazu. */
    List<String> validate() {
        List<String> warnings = new ArrayList<>();
        if (general == null) general = new General();
        if (crystals == null) crystals = new LinkedHashMap<>();
        if (crystalItem == null) crystalItem = new CrystalItem();
        if (energy == null) energy = new Energy();
        if (effects == null) effects = new Effects();
        if (events == null) events = new Events();
        if (audit == null) audit = new Audit();

        if (energy.min > energy.max) {
            warnings.add("energy.min (" + energy.min + ") > energy.max (" + energy.max + ") – Werte getauscht");
            int tmp = energy.min;
            energy.min = energy.max;
            energy.max = tmp;
        }
        if (energy.stages == null) energy.stages = new Stages();
        Stages st = energy.stages;
        if (!(energy.min <= st.damaged && st.damaged <= st.scratched && st.scratched <= st.pristine
                && st.pristine <= energy.max)) {
            int range = energy.max - energy.min;
            st.damaged = Math.min(energy.max, energy.min + 1);
            st.scratched = Math.max(st.damaged, energy.min + (int) Math.round(range * 0.4));
            st.pristine = Math.max(st.scratched, energy.min + (int) Math.round(range * 0.7));
            warnings.add("energy.stages müssen min ≤ damaged ≤ scratched ≤ pristine ≤ max sein – verwendet: "
                    + st.damaged + "/" + st.scratched + "/" + st.pristine);
        }
        if (energy.killGain < 0 || energy.deathLoss < 0 || energy.sameVictimCooldownMinutes < 0) {
            warnings.add("energy.killGain/deathLoss/sameVictimCooldownMinutes dürfen nicht negativ sein – auf 0 gesetzt");
            energy.killGain = Math.max(0, energy.killGain);
            energy.deathLoss = Math.max(0, energy.deathLoss);
            energy.sameVictimCooldownMinutes = Math.max(0, energy.sameVictimCooldownMinutes);
        }
        if (energy.minStrength < 0 || energy.minStrength > 1) {
            warnings.add("energy.minStrength muss 0–1 sein – 0.6 verwendet");
            energy.minStrength = 0.6;
        }
        if (energy.start < energy.min || energy.start > energy.max) {
            int clamped = Math.max(energy.min, Math.min(energy.max, energy.start));
            warnings.add("energy.start " + energy.start + " liegt außerhalb min/max – auf " + clamped + " gesetzt");
            energy.start = clamped;
        }
        if (effects.quality == null) {
            warnings.add("effects.quality ungültig – HIGH verwendet");
            effects.quality = EffectQuality.HIGH;
        }
        if (effects.maxParticlesPerTick < 100) {
            warnings.add("effects.maxParticlesPerTick < 100 – auf 100 gesetzt");
            effects.maxParticlesPerTick = 100;
        }
        if (effects.maxDisplays < 0) {
            warnings.add("effects.maxDisplays < 0 – auf 0 gesetzt (keine Displays)");
            effects.maxDisplays = 0;
        }
        try {
            ZoneId.of(events.timezone);
        } catch (RuntimeException e) {
            warnings.add("events.timezone '" + events.timezone + "' unbekannt – Europe/Berlin verwendet");
            events.timezone = "Europe/Berlin";
        }
        crystals.forEach((id, settings) -> {
            if (settings.cooldownMultiplier < 0) {
                warnings.add("crystals." + id + ".cooldownMultiplier < 0 – auf 1.0 gesetzt");
                settings.cooldownMultiplier = 1.0;
            }
            if (settings.damageMultiplier < 0) {
                warnings.add("crystals." + id + ".damageMultiplier < 0 – auf 1.0 gesetzt");
                settings.damageMultiplier = 1.0;
            }
        });
        return warnings;
    }
}
