package de.xmassmp.effect;

import java.util.ArrayList;
import java.util.List;

/**
 * Mehrere übereinandergelegte Sounds. Jede Ebene kann verzögert starten und eine zufällige
 * Tonhöhen-Variation haben, damit wiederholte Fähigkeiten nicht monoton klingen.
 */
public record SoundStack(List<Layer> layers) {

    public SoundStack {
        layers = List.copyOf(layers);
    }

    /**
     * @param sound          Sound-ID, z. B. {@code minecraft:entity.blaze.shoot}
     * @param pitchVariation ± zufällige Abweichung der Tonhöhe
     * @param delayTicks     Verzögerung relativ zum Abspielen des Stacks
     */
    public record Layer(String sound, float volume, float pitch, float pitchVariation, int delayTicks) {
        public Layer {
            if (delayTicks < 0) {
                throw new IllegalArgumentException("delayTicks muss ≥ 0 sein");
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Layer> layers = new ArrayList<>();

        public Builder layer(String sound, float volume, float pitch) {
            return layer(sound, volume, pitch, 0f, 0);
        }

        public Builder layer(String sound, float volume, float pitch, float pitchVariation, int delayTicks) {
            layers.add(new Layer(sound, volume, pitch, pitchVariation, delayTicks));
            return this;
        }

        public SoundStack build() {
            return new SoundStack(layers);
        }
    }
}
