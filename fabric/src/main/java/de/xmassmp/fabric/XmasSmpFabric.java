package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Fabric-Einstiegspunkt. Server-seitig: Vanilla-Clients bekommen das Ressourcenpaket über Polymer. */
public final class XmasSmpFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Xmas-SMP");

    private static volatile XmasCore core;
    private static volatile FabricPlatform platform;

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
            FabricPlatform adapter = new FabricPlatform(server, LOGGER);
            XmasCore instance = new XmasCore(adapter);
            instance.start();
            platform = adapter;
            core = instance;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            XmasCore instance = core;
            core = null;
            platform = null;
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
        registerPlayerEvents();
    }

    private static void registerPlayerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.onPlayerJoin(platform.handle(handler.getPlayer()));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.onPlayerQuit(handler.getPlayer().getUUID());
                platform.forget(handler.getPlayer().getUUID());
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            XmasCore instance = core;
            if (instance != null && entity instanceof ServerPlayer victim) {
                ServerPlayer killer = killer(victim, source);
                instance.onPlayerDeath(platform.handle(victim), killer == null ? null : platform.handle(killer));
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            XmasCore instance = core;
            if (instance != null) {
                instance.onPlayerRespawn(platform.handle(newPlayer));
            }
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            XmasCore instance = core;
            if (instance == null || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            Map<String, String> data = FabricItems.data(player.getItemInHand(hand));
            if (data.isEmpty()) {
                return InteractionResult.PASS;
            }
            return instance.onUseItem(platform.handle(serverPlayer), data)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    /** Direkter Täter, sonst wer zuletzt Schaden verursacht hat (z. B. bei Fallschaden nach einem Schlag). */
    private static ServerPlayer killer(ServerPlayer victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer direct) {
            return direct;
        }
        return victim.getKillCredit() instanceof ServerPlayer credited ? credited : null;
    }

    /**
     * Aufruf aus {@code ServerGamePacketListenerImplMixin}, wenn ein Spieler die Zweithand-Taste
     * drückt. Liefert {@code true}, wenn der Tausch abgefangen wurde.
     *
     * <p>Minecraft verarbeitet das Paket zuerst auf dem Netzwerk-Thread und plant es dann für den
     * Server-Thread neu ein; reagiert wird nur auf dem Server-Thread.
     */
    public static boolean onSwapHands(ServerPlayer player) {
        XmasCore instance = core;
        FabricPlatform adapter = platform;
        if (instance == null || adapter == null || !adapter.server().isSameThread()) {
            return false;
        }
        if (!instance.onSwapHands(adapter.handle(player))) {
            return false;
        }
        // Der Client zeigt den Tausch evtl. schon an – Inventar neu senden.
        player.containerMenu.sendAllDataToRemote();
        return true;
    }
}
