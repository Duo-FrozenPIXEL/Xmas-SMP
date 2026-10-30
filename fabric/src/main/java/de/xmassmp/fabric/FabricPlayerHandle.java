package de.xmassmp.fabric;

import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.util.Vec3;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
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

    @Override
    public void giveItem(ItemSpec item) {
        if (online()) {
            FabricItems.give(player(), item);
        }
    }

    @Override
    public void syncCrystalItem(ItemSpec expected, boolean soulbound) {
        if (online()) {
            FabricItems.syncCrystal(player(), expected, soulbound);
        }
    }

    @Override
    public void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (online()) {
            ServerPlayer p = player();
            p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            p.connection.send(new ClientboundSetSubtitleTextPacket(FabricText.legacy(subtitle)));
            p.connection.send(new ClientboundSetTitleTextPacket(FabricText.legacy(title)));
        }
    }

    @Override
    public String mainHandItem() {
        return FabricItems.id(player().getMainHandItem());
    }

    @Override
    public Map<String, String> mainHandData() {
        return FabricItems.data(player().getMainHandItem());
    }

    @Override
    public int selectedSlot() {
        return player().getInventory().getSelectedSlot();
    }

    @Override
    public boolean consumeMainHand(String itemId, Map<String, String> requiredData) {
        return online() && FabricItems.consumeMainHand(player(), itemId, requiredData);
    }

    @Override
    public void dropItem(ItemSpec item) {
        ServerPlayer p = player();
        ItemEntity entity = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), FabricItems.create(item));
        entity.setDefaultPickUpDelay();
        p.level().addFreshEntity(entity);
    }

    @Override
    public boolean isAlive() {
        return online() && player().isAlive();
    }

    @Override
    public boolean isCreative() {
        return player().isCreative() || player().isSpectator();
    }

    @Override
    public void refreshTabListName() {
        if (online()) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player()));
        }
    }

    @Override
    public void removeDroppedCrystals(double radius) {
        // Auch direkt nach dem Tod, solange der Spieler noch an der Todesstelle ist.
        FabricItems.removeDroppedCrystals(player(), radius);
    }
}
