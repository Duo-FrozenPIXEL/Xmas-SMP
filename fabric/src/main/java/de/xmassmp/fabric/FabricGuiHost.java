package de.xmassmp.fabric;

import de.xmassmp.gui.GuiButton;
import de.xmassmp.gui.GuiClick;
import de.xmassmp.gui.GuiHost;
import de.xmassmp.gui.GuiView;
import de.xmassmp.platform.XmasLogger;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Zeigt {@link GuiView}s als Kisten-GUIs über sgui an – Vanilla-Clients brauchen nichts. */
final class FabricGuiHost implements GuiHost {

    private final MinecraftServer server;
    private final XmasLogger logger;

    FabricGuiHost(MinecraftServer server, XmasLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public boolean open(UUID uuid, GuiView view) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) {
            return false;
        }
        SimpleGui gui = new SimpleGui(menuType(view.rows()), player, false);
        gui.setTitle(FabricText.legacy(view.title()));
        view.buttons().forEach((slot, button) -> gui.setSlot(slot, element(button)));
        return gui.open();
    }

    @Override
    public void close(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            player.closeContainer();
        }
    }

    private GuiElementBuilder element(GuiButton button) {
        Item item = BuiltInRegistries.ITEM.getValue(McIds.parse(button.icon()));
        GuiElementBuilder builder = new GuiElementBuilder(item == Items.AIR ? Items.PAPER : item)
                .setName(plain(button.name()));
        button.lore().forEach(line -> builder.addLoreLine(plain(line)));
        if (button.glow()) {
            builder.glow();
        }
        if (button.onClick() != null) {
            builder.setCallback((index, type, action) -> {
                GuiClick click = click(type);
                if (click == null) {
                    return;
                }
                try {
                    button.click(click);
                } catch (RuntimeException e) {
                    // Ein Fehler in einer Aktion darf den Spieler nicht vom Server werfen.
                    logger.error("Klick auf \"" + button.name() + "\" fehlgeschlagen", e);
                }
            });
        }
        return builder;
    }

    /** Nur Links- und Rechtsklicks (mit oder ohne Shift); Mittelklick, Zifferntasten usw. werden ignoriert. */
    private static GuiClick click(ClickType type) {
        if (type.isRight) {
            return type.shift ? GuiClick.SHIFT_RIGHT : GuiClick.RIGHT;
        }
        if (type.isLeft) {
            return type.shift ? GuiClick.SHIFT_LEFT : GuiClick.LEFT;
        }
        return null;
    }

    private static Component plain(String legacy) {
        return FabricText.legacy(legacy).withStyle(s -> s.withItalic(false));
    }

    private static MenuType<?> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
