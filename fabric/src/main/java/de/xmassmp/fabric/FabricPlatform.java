package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.event.EventHost;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.Vec3;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private final Map<UUID, FabricPlayerHandle> handles = new ConcurrentHashMap<>();

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

    /** Welt zu einer {@link WorldRef}; die ID ist {@code String.valueOf(level.dimension())}. */
    ServerLevel level(WorldRef ref) {
        for (ServerLevel level : server.getAllLevels()) {
            if (worldRef(level).equals(ref)) {
                return level;
            }
        }
        return null;
    }

    static WorldRef worldRef(Level level) {
        return new WorldRef(String.valueOf(level.dimension()));
    }

    static Vec3 vec(net.minecraft.world.phys.Vec3 v) {
        return new Vec3(v.x, v.y, v.z);
    }
}
