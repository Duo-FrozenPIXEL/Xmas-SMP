package de.xmassmp.effect;

import de.xmassmp.util.Rgb;
import de.xmassmp.util.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapesTest {

    private static final double EPS = 1e-9;

    @Test
    void ringPointsLieOnHorizontalCircle() {
        List<Vec3> ring = Shapes.ring(2.5, 36, 0.3);
        assertEquals(36, ring.size());
        for (Vec3 p : ring) {
            assertEquals(2.5, Math.hypot(p.x(), p.z()), EPS);
            assertEquals(0, p.y(), EPS);
        }
    }

    @Test
    void sphereCoversFullRadius() {
        List<Vec3> sphere = Shapes.sphere(3, 200);
        sphere.forEach(p -> assertEquals(3, p.length(), EPS));
        assertEquals(3, sphere.get(0).y(), EPS);
        assertEquals(-3, sphere.get(199).y(), EPS);
    }

    @Test
    void spiralRisesToHeight() {
        List<Vec3> spiral = Shapes.spiral(1, 4, 3, 61, 0);
        assertEquals(0, spiral.get(0).y(), EPS);
        assertEquals(4, spiral.get(60).y(), EPS);
        spiral.forEach(p -> assertEquals(1, Math.hypot(p.x(), p.z()), EPS));
    }

    @Test
    void lineIncludesBothEnds() {
        Vec3 from = new Vec3(0, 1, 0);
        Vec3 to = new Vec3(10, 1, 0);
        List<Vec3> line = Shapes.line(from, to, 11);
        assertEquals(from, line.get(0));
        assertEquals(to, line.get(10));
        assertEquals(5, line.get(5).x(), EPS);
    }

    @Test
    void waveStaysWithinAmplitudeSideways() {
        List<Vec3> wave = Shapes.wave(Vec3.ZERO, new Vec3(10, 0, 0), 0.5, 2, 81, 0);
        double max = wave.stream().mapToDouble(p -> Math.abs(p.z())).max().orElseThrow();
        assertEquals(0.5, max, 1e-3);
        wave.forEach(p -> assertEquals(0, p.y(), EPS));
    }

    @Test
    void orientRotatesPlusXToDirection() {
        List<Vec3> oriented = Shapes.orient(List.of(new Vec3(1, 0, 0)), new Vec3(0, 0, 1));
        assertEquals(0, oriented.get(0).x(), EPS);
        assertEquals(1, oriented.get(0).z(), EPS);
    }

    @Test
    void easingHitsEndpoints() {
        for (Easing easing : Easing.values()) {
            assertEquals(0, easing.apply(0), 1e-9, easing.name());
            assertEquals(1, easing.apply(1), 1e-9, easing.name());
        }
        assertTrue(Easing.EASE_IN.apply(0.5) < 0.5);
        assertTrue(Easing.EASE_OUT.apply(0.5) > 0.5);
    }

    @Test
    void colorLerp() {
        Rgb mid = Rgb.hex("#000000").lerp(Rgb.hex("#FF8000"), 0.5);
        assertEquals("#804000", mid.toHex());
    }
}
