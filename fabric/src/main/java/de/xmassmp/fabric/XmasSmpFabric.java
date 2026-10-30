package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.bottle.BottleService;
import de.xmassmp.util.BlockPos;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Fabric-Einstiegspunkt. Server-seitig: Vanilla-Clients bekommen das Ressourcenpaket über Polymer. */
public final class XmasSmpFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Xmas-SMP");

    private static volatile XmasCore core;
    private static volatile FabricPlatform platform;
    /** Letzter Tick mit einer Client-Mod-Taste je Spieler (gegen Paket-Spam). */
    private static final Map<UUID, Integer> lastKey = new ConcurrentHashMap<>();

    /** Laufender Kern oder {@code null}, solange kein Server läuft. */
    public static XmasCore core() {
        return core;
    }

    /** Handle eines Spielers für den Kern; nur aufrufen, solange ein Server läuft. */
    static FabricPlayerHandle handle(ServerPlayer player) {
        return platform.handle(player);
    }

    @Override
    public void onInitialize() {
        XmasRegistry.register();
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
        registerGameplayEvents();
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
            lastKey.remove(handler.getPlayer().getUUID());
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
            ItemStack stack = player.getItemInHand(hand);
            Map<String, String> data = FabricItems.data(stack);
            if (data.isEmpty()) {
                // Energy Bottle abfüllen: gehaltenes Schleichen + Rechtsklick mit einer leeren Glasflasche.
                if (hand == InteractionHand.MAIN_HAND && stack.is(Items.GLASS_BOTTLE)
                        && instance.onUseVanillaItem(platform.handle(serverPlayer), BottleService.GLASS_BOTTLE)) {
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            // Xmas-Items wirken nur aus der Haupthand; der Kern verbraucht sie dort.
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            return instance.onUseItem(platform.handle(serverPlayer), data)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            XmasCore instance = core;
            if (instance != null && entity instanceof ServerPlayer victim && damageTaken > 0) {
                ServerPlayer attacker = source.getEntity() instanceof ServerPlayer p ? p : null;
                instance.onPlayerDamaged(platform.handle(victim), attacker == null ? null : platform.handle(attacker));
            }
        });
    }

    private static void registerGameplayEvents() {
        // Optionaler Client-Mod: Tasten für Primär/Sekundär/Ultimate. Vanilla-Clients senden das nie.
        PayloadTypeRegistry.playC2S().register(AbilityKeyPayload.TYPE, AbilityKeyPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AbilityKeyPayload.TYPE, (payload, context) -> {
            XmasCore instance = core;
            ServerPlayer player = context.player();
            if (instance == null) {
                return;
            }
            int now = context.server().getTickCount();
            Integer last = lastKey.put(player.getUUID(), now);
            if (last != null && now - last < 2) {
                return;
            }
            instance.onAbilityKey(platform.handle(player), payload.slot());
        });

        // Totem: Abbau-Fortschritt und Bruch.
        PolymerBlockUtils.BREAKING_PROGRESS_UPDATE.register((player, pos, state, stage) -> {
            XmasCore instance = core;
            if (instance != null && state.is(XmasRegistry.totem)) {
                instance.totem().onMiningProgress(platform.handle(player), FabricPlatform.worldRef(player.level()),
                        new BlockPos(pos.getX(), pos.getY(), pos.getZ()), stage);
            }
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            XmasCore instance = core;
            if (instance != null && state.is(XmasRegistry.totem) && player instanceof ServerPlayer serverPlayer) {
                instance.totem().onDestroyed(platform.handle(serverPlayer), FabricPlatform.worldRef(level),
                        new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
            }
        });
    }

    /**
     * Aufruf aus {@code ServerPlayerMixin}: Tier-2-Spieler stehen gelb in der Tab-Liste.
     *
     * @return {@code null}, wenn der Name unverändert bleibt
     */
    public static Component tabListName(ServerPlayer player) {
        XmasCore instance = core;
        if (instance == null || !instance.isTier2(player.getUUID())) {
            return null;
        }
        return Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.YELLOW);
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
