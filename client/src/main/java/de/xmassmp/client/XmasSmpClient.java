package de.xmassmp.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Optionaler Client-Einstiegspunkt: drei frei belegbare Tasten (Optionen → Steuerung → „Xmas-SMP“)
 * für Primär-, Sekundär- und Ultimate-Fähigkeit.
 *
 * <p>Der Mod meldet nur „Taste gedrückt“; ob die Fähigkeit auslöst, entscheidet der Server.
 * Auf Servern ohne Xmas-SMP (Kanal fehlt) sendet er nichts.
 */
public final class XmasSmpClient implements ClientModInitializer {

    public static final String MOD_ID = "xmassmp_keys";

    // Übersetzung: key.category.xmassmp_keys.main
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private KeyMapping primary;
    private KeyMapping secondary;
    private KeyMapping ultimate;

    @Override
    public void onInitializeClient() {
        try {
            PayloadTypeRegistry.playC2S().register(AbilityKeyPayload.TYPE, AbilityKeyPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            // Liegt der Server-Mod im selben Client (Einzelspieler), hat er den Typ schon registriert.
        }

        primary = register("primary", GLFW.GLFW_KEY_R);
        secondary = register("secondary", GLFW.GLFW_KEY_G);
        ultimate = register("ultimate", GLFW.GLFW_KEY_V);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private static KeyMapping register(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, defaultKey, CATEGORY));
    }

    private void onTick(Minecraft client) {
        // consumeClick leert die Klicks immer – auch wenn nichts gesendet wird –,
        // damit sich keine Drücke aus Menüs oder fremden Servern aufstauen.
        boolean ready = client.player != null && client.screen == null
                && ClientPlayNetworking.canSend(AbilityKeyPayload.TYPE);
        drain(primary, AbilityKeyPayload.PRIMARY, ready);
        drain(secondary, AbilityKeyPayload.SECONDARY, ready);
        drain(ultimate, AbilityKeyPayload.ULTIMATE, ready);
    }

    /** Höchstens ein Paket pro Taste und Tick: gedrückt halten löst nicht dauerhaft aus. */
    private static void drain(KeyMapping key, int slot, boolean ready) {
        boolean pressed = false;
        while (key.consumeClick()) {
            pressed = true;
        }
        if (pressed && ready) {
            ClientPlayNetworking.send(new AbilityKeyPayload(slot));
        }
    }
}
