package de.xmassmp.crystal;

import de.xmassmp.util.Rgb;

import java.util.List;

/**
 * Die fünf Basis-Kristalle. Textur-Zuordnung laut Instructions.md / todo.md Abschnitt 0:
 * Feuer = Strength, Erde = Fire, Wind = Puff, Schatten = Astra, Natur = Wealth.
 */
public final class Crystals {

    public static final Crystal FIRE = new Crystal("fire", "Feuer", "strength", 7,
            Rgb.hex("#FF3B1F"), Rgb.hex("#FFB400"), 'c', "");

    public static final Crystal EARTH = new Crystal("earth", "Erde", "fire", 2,
            Rgb.hex("#E07A1F"), Rgb.hex("#6B4423"), '6', "");

    public static final Crystal WIND = new Crystal("wind", "Wind", "puff", 5,
            Rgb.hex("#F2F7FF"), Rgb.hex("#9FD8FF"), 'f', "");

    public static final Crystal SHADOW = new Crystal("shadow", "Schatten", "astra", 1,
            Rgb.hex("#8E3BFF"), Rgb.hex("#2A0B4F"), '5', "");

    public static final Crystal NATURE = new Crystal("nature", "Natur", "wealth", 8,
            Rgb.hex("#3FD45A"), Rgb.hex("#FF9FD6"), 'a', "");

    public static final List<Crystal> DEFAULTS = List.of(FIRE, EARTH, WIND, SHADOW, NATURE);

    private Crystals() {
    }
}
