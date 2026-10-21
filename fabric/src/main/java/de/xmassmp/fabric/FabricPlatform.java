package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.effect.EffectSink;
import de.xmassmp.event.EventHost;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Path;

/** Fabric-Adapter des Kerns. Lebt so lange wie der Server. */
final class FabricPlatform implements Platform {

    private final MinecraftServer server;
    private final XmasLogger logger;
    private final FabricEffectSink effectSink;
    private final FabricEventHost eventHost;

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

    /** Welt zu einer {@link WorldRef}; die ID ist {@code String.valueOf(level.dimension())}. */
    ServerLevel level(WorldRef ref) {
        for (ServerLevel level : server.getAllLevels()) {
            if (String.valueOf(level.dimension()).equals(ref.id())) {
                return level;
            }
        }
        return null;
    }
}
