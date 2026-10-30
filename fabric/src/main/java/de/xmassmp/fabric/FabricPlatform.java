package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.event.EventHost;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.TotemHost;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.BlockPos;
import de.xmassmp.util.Vec3;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Fabric-Adapter des Kerns. Lebt so lange wie der Server. */
final class FabricPlatform implements Platform {

    private final MinecraftServer server;
    private final XmasLogger logger;
    private final FabricEffectSink effectSink;
    private final FabricEventHost eventHost;
    private final FabricGuiHost guiHost;
    private final Map<UUID, FabricPlayerHandle> handles = new ConcurrentHashMap<>();
    private final Map<WorldRef, ServerLevel> levels = new ConcurrentHashMap<>();

    FabricPlatform(MinecraftServer server, Logger slf4j) {
        this.server = server;
        this.logger = new XmasLogger() {
            @Override
            public void info(String message) {
                slf4j.info(message);
            }

            @Override
            public void warn(String message) {
                slf4j.warn(message);
            }

            @Override
            public void error(String message, Throwable error) {
                slf4j.error(message, error);
            }
        };
        this.effectSink = new FabricEffectSink(this, slf4j);
        this.eventHost = new FabricEventHost(server);
        this.guiHost = new FabricGuiHost(server, this.logger);
    }

    MinecraftServer server() {
        return server;
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(XmasCore.MOD_ID);
    }

    @Override
    public Path dataDirectory() {
        return server.getWorldPath(LevelResource.ROOT).resolve(XmasCore.MOD_ID).normalize();
    }

    @Override
    public XmasLogger logger() {
        return logger;
    }

    @Override
    public EffectSink effectSink() {
        return effectSink;
    }

    @Override
    public EventHost eventHost() {
        return eventHost;
    }

    @Override
    public GuiHost guiHost() {
        return guiHost;
    }

    @Override
    public TotemHost totemHost() {
        return totemHost;
    }

    @Override
    public void refreshCrafting() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refresh(player.containerMenu);
            if (player.containerMenu != player.inventoryMenu) {
                refresh(player.inventoryMenu);
            }
        }
    }

    private static void refresh(AbstractContainerMenu menu) {
        if (menu instanceof AbstractCraftingMenu crafting && !crafting.getInputGridSlots().isEmpty()) {
            crafting.slotsChanged(crafting.getInputGridSlots().get(0).container);
        }
    }

    /** Totem-Block über {@link XmasRegistry#totem}; ungeladene Chunks werden nicht angefasst. */
    private final TotemHost totemHost = new TotemHost() {
        @Override
        public boolean place(WorldRef world, BlockPos pos) {
            ServerLevel level = level(world);
            net.minecraft.core.BlockPos at = mc(pos);
            if (level == null || !level.isInWorldBounds(at) || !level.getWorldBorder().isWithinBounds(at)
                    || !level.getBlockState(at).canBeReplaced()) {
                return false;
            }
            return level.setBlock(at, XmasRegistry.totem.defaultBlockState(), 3);
        }

        @Override
        public boolean isPresent(WorldRef world, BlockPos pos) {
            ServerLevel level = level(world);
            net.minecraft.core.BlockPos at = mc(pos);
            if (level == null) {
                return false;
            }
            // Ungeladen: nichts kann es dort gerade zerstören – als vorhanden werten, statt den Chunk zu laden.
            return !level.isLoaded(at) || level.getBlockState(at).is(XmasRegistry.totem);
        }

        @Override
        public void remove(WorldRef world, BlockPos pos) {
            ServerLevel level = level(world);
            net.minecraft.core.BlockPos at = mc(pos);
            if (level != null && level.getBlockState(at).is(XmasRegistry.totem)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    static net.minecraft.core.BlockPos mc(BlockPos pos) {
        return new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
    }

    @Override
    public List<PlayerHandle> onlinePlayers() {
        return server.getPlayerList().getPlayers().stream().map(p -> (PlayerHandle) handle(p)).toList();
    }

    @Override
    public Optional<PlayerHandle> player(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        return player == null ? Optional.empty() : Optional.of(handle(player));
    }

    /** Ein Handle pro Spieler, damit Effekt-Anker über Respawns hinweg gültig bleiben. */
    FabricPlayerHandle handle(ServerPlayer player) {
        return handles.computeIfAbsent(player.getUUID(), uuid -> new FabricPlayerHandle(server, player));
    }

    void forget(UUID uuid) {
        handles.remove(uuid);
    }

    /** Welt zu einer {@link WorldRef} (ID der Dimension, z. B. {@code minecraft:overworld}); zwischengespeichert. */
    ServerLevel level(WorldRef ref) {
        ServerLevel cached = levels.get(ref);
        if (cached != null) {
            return cached;
        }
        // Unbekannte ID oder neu geladene Dimension: einmal alle Welten eintragen.
        for (ServerLevel level : server.getAllLevels()) {
            levels.putIfAbsent(worldRef(level), level);
        }
        return levels.get(ref);
    }

    static WorldRef worldRef(Level level) {
        return new WorldRef(McIds.id(level.dimension()));
    }

    static Vec3 vec(net.minecraft.world.phys.Vec3 v) {
        return new Vec3(v.x, v.y, v.z);
    }
}
