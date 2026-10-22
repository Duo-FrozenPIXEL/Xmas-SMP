package de.xmassmp.effect;

import de.xmassmp.util.Vec3;

/**
 * Lage eines Display-Entities relativ zu seiner Position: Verschiebung, Größe und Drehung.
 *
 * <p>Drehwinkel in Grad, angewendet in der Reihenfolge Gieren (Y) → Nicken (X) → Rollen (Z).
 *
 * @param translation Verschiebung in Blöcken
 * @param scale       Größe je Achse (1 = normal)
 */
public record DisplayTransform(Vec3 translation, Vec3 scale, float yaw, float pitch, float roll) {

    public static final DisplayTransform IDENTITY = new DisplayTransform(Vec3.ZERO, new Vec3(1, 1, 1), 0, 0, 0);

    public static DisplayTransform scaled(double scale) {
        return IDENTITY.withScale(scale);
    }

    public DisplayTransform withScale(double all) {
        return withScale(new Vec3(all, all, all));
    }

    public DisplayTransform withScale(Vec3 newScale) {
        return new DisplayTransform(translation, newScale, yaw, pitch, roll);
    }

    public DisplayTransform withTranslation(Vec3 newTranslation) {
        return new DisplayTransform(newTranslation, scale, yaw, pitch, roll);
    }

    public DisplayTransform withRotation(float newYaw, float newPitch, float newRoll) {
        return new DisplayTransform(translation, scale, newYaw, newPitch, newRoll);
    }

    /** Linearer Übergang, z. B. für eigene Keyframes. */
    public DisplayTransform lerp(DisplayTransform to, double t) {
        return new DisplayTransform(
                translation.lerp(to.translation, t),
                scale.lerp(to.scale, t),
                (float) (yaw + (to.yaw - yaw) * t),
                (float) (pitch + (to.pitch - pitch) * t),
                (float) (roll + (to.roll - roll) * t));
    }
}
