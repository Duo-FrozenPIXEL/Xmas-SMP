package de.xmassmp.fabric;

import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.util.Vec3;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * {@link PlayerHandle} für Fabric. Löst den {@link ServerPlayer} bei jedem Zugriff neu auf, weil
 * Minecraft beim Respawn ein neues Spieler-Objekt anlegt.
 */
final class FabricPlayerHandle implements PlayerHandle {

    private final MinecraftServer server;
    private final UUID uuid;
    private ServerPlayer last;

    FabricPlayerHandle(MinecraftServer server, ServerPlayer player) {
        this.server = server;
        this.uuid = player.getUUID();
        this.last = player;
    }

    /** Aktuelles Spieler-Objekt; offline der zuletzt bekannte Stand. */
    ServerPlayer player() {
        ServerPlayer current = server.getPlayerList().getPlayer(uuid);
        if (current != null) {
            last = current;
        }
        return last;
    }

    private boolean online() {
        return server.getPlayerList().getPlayer(uuid) != null;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public String name() {
        return player().getName().getString();
    }

    @Override
    public WorldRef world() {
        return FabricPlatform.worldRef(player().level());
    }

    @Override
    public Vec3 position() {
        return FabricPlatform.vec(player().position());
    }

    @Override
    public Vec3 eyePosition() {
        return FabricPlatform.vec(player().getEyePosition());
    }

    @Override
    public Vec3 lookDirection() {
        return FabricPlatform.vec(player().getLookAngle()).normalize();
    }

    @Override
    public boolean isSneaking() {
        return player().isShiftKeyDown();
    }

    @Override
    public void sendActionBar(String legacyText) {
        if (online()) {
            player().connection.send(new ClientboundSetActionBarTextPacket(FabricText.legacy(legacyText)));
        }
    }

    @Override
    public void sendMessage(String legacyText) {
        if (online()) {
            player().sendSystemMessage(FabricText.legacy(legacyText));
        }
    }
}
