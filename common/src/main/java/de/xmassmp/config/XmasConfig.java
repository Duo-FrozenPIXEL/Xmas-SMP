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
    public EnergyBottle energyBottle = new EnergyBottle();
    public Upgrader upgrader = new Upgrader();
    public Trader trader = new Trader();
    public Totem totem = new Totem();
    public Map<String, RecipeSpec> recipes = RecipeSpec.defaults();
    public Controls controls = new Controls();
    public Hud hud = new Hud();
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

    /** Energiesystem, Regeln siehe docs/implementierungsplan.md, Abschnitt 1.1. */
    public static final class Energy {
        /** Energie-Gewinn und -Verlust durch Kills und Tode; aus = Energie ändert sich nur per Befehl. */
        public boolean pvpEnabled = true;
        public int start = 5;
        public int min = 0;
        public int max = 10;
        public int killGain = 1;
        public int deathLoss = 1;
        /** Kein Gewinn, wenn derselbe Spieler innerhalb dieser Zeit erneut besiegt wird. 0 = aus. */
        public int sameVictimCooldownMinutes = 30;
        /** Hat der Sieger schon {@code max}, fällt die Energie des Opfers als Energy Bottle auf den Boden. */
        public boolean dropBottleWhenKillerAtMax = true;
        public Stages stages = new Stages();
        public Access access = new Access();
        /** Stärke der Fähigkeiten bei der niedrigsten Energie über „zerbrochen“; bei max = 1.0. */
        public double minStrength = 0.6;
    }

    /** Ab welcher Energie eine Bruchstufe gilt; darunter ist der Kristall zerbrochen. */
    public static final class Stages {
        public int damaged = 1;
        public int scratched = 3;
        public int pristine = 5;
    }

    /** Mindest-Energie je aktiver Fähigkeit. Passive wirken immer, auch bei 0. */
    public static final class Access {
        public int primary = 1;
        public int secondary = 3;
        /** Gilt zusätzlich zu Tier 2. */
        public int ultimate = 3;
    }

    public static final class EnergyBottle {
        public boolean enabled = true;
        /** So lange muss Schleichen + Rechtsklick mit einer Glasflasche gehalten werden. */
        public int chargeSeconds = 5;
        /** Abfüllen nur, solange danach mindestens so viel Energie bleibt. */
        public int keepAtLeast = 1;
        /** Kein Abfüllen so lange nach Schaden durch einen anderen Spieler. 0 = aus. */
        public int combatTagSeconds = 15;
        /** Wartezeit zwischen zwei getrunkenen Flaschen. */
        public int drinkCooldownSeconds = 1;
    }

    public static final class Upgrader {
        public boolean enabled = true;
    }

    public static final class Trader {
        public boolean enabled = true;
        public boolean keepTier = true;
        public boolean keepEnergy = true;
        /** Wartezeit nach einem Tausch, bevor der nächste möglich ist. 0 = keine. */
        public int cooldownMinutes = 0;
    }

    public static final class Totem {
        public boolean enabled = true;
        public int radius = 15;
        public int durationMinutes = 10;
        /** Energie unter diesem Wert wird am Ende auf genau diesen Wert gesetzt. */
        public int restoreTo = 5;
        /** Abbauzeit, fest – unabhängig von Werkzeug, Effizienz und Eile. */
        public int breakSeconds = 10;
        /** Wer in dieser Zeit nach dem Aufstellen im Kreis steht, nimmt teil. */
        public int joinSeconds = 30;
        /** Koordinaten beim Aufstellen per Chat und Titel an alle. */
        public boolean announceCoordinates = true;
    }

    public static final class Controls {
        public boolean swapHandsActivates = true;
        /** Schleichen + Rechtsklick mit dem Kristall in der Haupthand löst die Ultimate aus. */
        public boolean sneakUseUltimate = true;
        /** Tasten aus dem optionalen Client-Mod annehmen. */
        public boolean clientKeys = true;
        /** Tier-2-Spieler stehen gelb in der Tab-Liste. */
        public boolean tier2TabColor = true;
    }

    public static final class Hud {
        public boolean enabled = true;
        public int updateTicks = 10;
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
        if (energy.access == null) energy.access = new Access();
        if (energyBottle == null) energyBottle = new EnergyBottle();
        if (upgrader == null) upgrader = new Upgrader();
        if (trader == null) trader = new Trader();
        if (totem == null) totem = new Totem();
        if (recipes == null) recipes = new LinkedHashMap<>();
        RecipeSpec.defaults().forEach(recipes::putIfAbsent);
        if (controls == null) controls = new Controls();
        if (hud == null) hud = new Hud();
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
        Access ac = energy.access;
        if (ac.primary < energy.min || ac.secondary < ac.primary || ac.ultimate < ac.primary) {
            warnings.add("energy.access: es muss min ≤ primary ≤ secondary/ultimate gelten – Standard 1/3/3 verwendet");
            energy.access = new Access();
        }
        if (energyBottle.chargeSeconds < 1 || energyBottle.keepAtLeast < energy.min
                || energyBottle.combatTagSeconds < 0 || energyBottle.drinkCooldownSeconds < 0) {
            warnings.add("energyBottle: ungültige Werte – Standardwerte verwendet");
            boolean enabled = energyBottle.enabled;
            energyBottle = new EnergyBottle();
            energyBottle.enabled = enabled;
        }
        if (trader.cooldownMinutes < 0) {
            warnings.add("trader.cooldownMinutes < 0 – auf 0 gesetzt");
            trader.cooldownMinutes = 0;
        }
        if (totem.radius < 1 || totem.durationMinutes < 1 || totem.breakSeconds < 1 || totem.joinSeconds < 0
                || totem.restoreTo < energy.min || totem.restoreTo > energy.max) {
            warnings.add("totem: ungültige Werte – Standardwerte verwendet");
            boolean enabled = totem.enabled;
            totem = new Totem();
            totem.enabled = enabled;
        }
        recipes.replaceAll((id, spec) -> {
            String problem = spec == null ? "leer" : spec.problem();
            if (problem == null) {
                return spec;
            }
            warnings.add("recipes." + id + ": " + problem + " – Rezept abgeschaltet");
            return RecipeSpec.disabled();
        });
        if (hud.updateTicks < 1 || hud.updateTicks > 40) {
            warnings.add("hud.updateTicks muss 1–40 sein – 10 verwendet");
            hud.updateTicks = 10;
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
        crystals.replaceAll((id, settings) -> {
            if (settings == null) {
                warnings.add("crystals." + id + " ist leer – Standardwerte verwendet");
                return new CrystalSettings();
            }
            if (settings.cooldownMultiplier < 0) {
                warnings.add("crystals." + id + ".cooldownMultiplier < 0 – auf 1.0 gesetzt");
                settings.cooldownMultiplier = 1.0;
            }
            if (settings.damageMultiplier < 0) {
                warnings.add("crystals." + id + ".damageMultiplier < 0 – auf 1.0 gesetzt");
                settings.damageMultiplier = 1.0;
            }
            return settings;
        });
        return warnings;
    }
}
