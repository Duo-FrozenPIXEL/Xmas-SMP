package de.xmassmp.fabric;

import de.xmassmp.event.EventHost;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Führt Event-Aktionen auf dem Fabric-Server aus. */
final class FabricEventHost implements EventHost {

    private final MinecraftServer server;

    FabricEventHost(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void broadcast(String legacyText) {
        server.getPlayerList().broadcastSystemMessage(FabricText.legacy(legacyText), false);
    }

    @Override
    public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        var animation = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
        var subtitlePacket = new ClientboundSetSubtitleTextPacket(FabricText.legacy(subtitle));
        var titlePacket = new ClientboundSetTitleTextPacket(FabricText.legacy(title));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(animation);
            player.connection.send(subtitlePacket);
            player.connection.send(titlePacket);
        }
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(McIds.parse(sound));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(event, SoundSource.MASTER, volume, pitch);
        }
    }

    @Override
    public void runCommand(String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
}
