package de.xmassmp.crystal;

import de.xmassmp.text.LocalizedText;

import java.util.Locale;
import java.util.Optional;

/**
 * Sichtbare Bruchstufe eines Kristalls (Breaking-Pristine-System).
 *
 * <p>Modell- und Texturnamen sind im Original um eine Stufe versetzt, siehe docs/resourcepack.md.
 * Die Zuordnung Energie → Stufe folgt, sobald das Energiesystem recherchiert ist (todo.md, 2).
 */
public enum PristineStage {
    BROKEN(0, "", "zerbrochen", '8'),
    DAMAGED(20, "_pristine2", "beschädigt", '7'),
    SCRATCHED(30, "_pristine3", "zerkratzt", 'f'),
    PRISTINE(40, "_pristine4", "makellos", 'b');

    private final int modelDataOffset;
    private final String modelSuffix;
    private final String nameFallback;
    private final char color;

    PristineStage(int modelDataOffset, String modelSuffix, String nameFallback, char color) {
        this.modelDataOffset = modelDataOffset;
        this.modelSuffix = modelSuffix;
        this.nameFallback = nameFallback;
        this.color = color;
    }

    public int modelDataOffset() {
        return modelDataOffset;
    }

    public String modelSuffix() {
        return modelSuffix;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public LocalizedText displayName() {
        return LocalizedText.of("stage.xmassmp." + id(), nameFallback).withColor(color);
    }

    public static Optional<PristineStage> byId(String id) {
        for (PristineStage stage : values()) {
            if (stage.id().equals(id)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
