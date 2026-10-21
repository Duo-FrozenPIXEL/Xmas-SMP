package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric-Einstiegspunkt. Server-seitig: Vanilla-Clients bekommen das Ressourcenpaket über Polymer. */
public final class XmasSmpFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Xmas-SMP");

    private static volatile XmasCore core;

    /** Laufender Kern oder {@code null}, solange kein Server läuft. */
    public static XmasCore core() {
        return core;
    }

    @Override
    public void onInitialize() {
        // Modelle, Texturen und Sprachdateien aus assets/ ins Server-Ressourcenpaket.
        PolymerResourcePackUtils.addModAssets(XmasCore.MOD_ID);
        PolymerResourcePackUtils.markAsRequired();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                XmasCommands.register(dispatcher, XmasSmpFabric::core));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            XmasCore instance = new XmasCore(new FabricPlatform(server, LOGGER));
            instance.start();
            core = instance;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            XmasCore instance = core;
            core = null;
            if (instance != null) {
                instance.shutdown();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.tick();
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.onPlayerJoin(handler.getPlayer().getUUID());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.onPlayerQuit(handler.getPlayer().getUUID());
            }
        });
    }
}
