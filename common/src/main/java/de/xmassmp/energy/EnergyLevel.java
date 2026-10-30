package de.xmassmp.energy;

import de.xmassmp.text.LocalizedText;

/**
 * Die 11 Energie-Zustände des Originals (Broken … Pristine +5) für Lore und HUD. Die Optik des
 * Kristalls kennt nur 4 Stufen ({@link de.xmassmp.crystal.PristineStage}).
 */
public final class EnergyLevel {

    /** Ab hier heißt der Zustand „Pristine“; darüber zählt „+1 … +5“. */
    public static final int PRISTINE = 5;

    private static final String[] FALLBACK = {
            "Zerbrochen", "Ruiniert", "Beschädigt", "Gerissen", "Zerkratzt", "Makellos"};
    private static final String[] KEYS = {
            "broken", "ruined", "damaged", "cracked", "scratched", "pristine"};

    private EnergyLevel() {
    }

    /** Name des Zustands, z. B. „Makellos +2“ bei 7 Energie. */
    public static LocalizedText name(int energy) {
        if (energy <= 0) {
            return LocalizedText.of("energy.xmassmp.broken", FALLBACK[0]).withColor('8');
        }
        if (energy < PRISTINE) {
            return LocalizedText.of("energy.xmassmp." + KEYS[energy], FALLBACK[energy]).withColor(color(energy));
        }
        if (energy == PRISTINE) {
            return LocalizedText.of("energy.xmassmp.pristine", FALLBACK[PRISTINE]).withColor('b');
        }
        return LocalizedText.of("energy.xmassmp.pristine_plus", "Makellos +%s")
                .withArgs(LocalizedText.literal(Integer.toString(energy - PRISTINE)))
                .withColor('b');
    }

    private static char color(int energy) {
        return energy <= 2 ? 'c' : 'e';
    }
}
