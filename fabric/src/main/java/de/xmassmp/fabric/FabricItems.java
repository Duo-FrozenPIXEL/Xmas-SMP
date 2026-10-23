package de.xmassmp.fabric;

import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Baut Item-Stacks aus {@link ItemSpec}: Vanilla-Basis-Item + Komponenten, kein Client-Mod nötig. */
final class FabricItems {

    private FabricItems() {
    }

    static ItemStack create(ItemSpec spec) {
        Item item = BuiltInRegistries.ITEM.getValue(McIds.parse(spec.baseItem()));
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of((float) spec.customModelData()), List.of(), List.of(), List.of()));
        stack.set(DataComponents.ITEM_NAME, FabricText.localized(spec.name()));
        if (!spec.lore().isEmpty()) {
            List<Component> lore = spec.lore().stream()
                    .map(line -> (Component) FabricText.localized(line).withStyle(s -> s.withItalic(false)))
                    .toList();
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        if (ItemCatalog.TYPE_CRYSTAL.equals(spec.data().get(ItemSpec.DATA_TYPE))) {
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        CompoundTag data = new CompoundTag();
        spec.data().forEach(data::putString);
        CompoundTag root = new CompoundTag();
        root.put(ItemSpec.DATA_ROOT, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    /** Markierungen aus {@code custom_data.xmassmp}; leer bei Vanilla-Items. */
    static Map<String, String> data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return Map.of();
        }
        CompoundTag data = custom.copyTag().getCompoundOrEmpty(ItemSpec.DATA_ROOT);
        Map<String, String> out = new HashMap<>();
        for (String key : data.keySet()) {
            data.getString(key).ifPresent(value -> out.put(key, value));
        }
        return out;
    }

    /** Regeln siehe {@link de.xmassmp.platform.PlayerHandle#syncCrystalItem}. */
    static void syncCrystal(ServerPlayer player, ItemSpec expected, boolean soulbound) {
        String owner = player.getUUID().toString();
        boolean kept = false;

        ItemStack carried = player.containerMenu.getCarried();
        Map<String, String> carriedData = data(carried);
        if (ItemCatalog.isOwnedCrystal(carriedData)) {
            boolean mine = owner.equals(carriedData.get(ItemCatalog.DATA_OWNER));
            if (mine && expected != null) {
                kept = true;
                if (!carriedData.equals(expected.data())) {
                    player.containerMenu.setCarried(create(expected));
                }
            } else if (soulbound) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            Map<String, String> data = data(inventory.getItem(i));
            if (!ItemCatalog.isOwnedCrystal(data)) {
                continue;
            }
            boolean mine = owner.equals(data.get(ItemCatalog.DATA_OWNER));
            boolean outdated = expected != null && !data.equals(expected.data());
            if (mine && expected != null && !kept) {
                kept = true;
                if (outdated) {
                    inventory.setItem(i, create(expected));
                }
            } else if (soulbound) {
                inventory.setItem(i, ItemStack.EMPTY);
            } else if (mine && outdated) {
                inventory.setItem(i, create(expected));
            }
        }

        if (!soulbound) {
            return;
        }
        // Geöffnete Behälter und Crafting-Felder: dort hat ein gebundener Kristall nichts verloren.
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != inventory && ItemCatalog.isOwnedCrystal(data(slot.getItem()))) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (!kept && expected != null) {
            inventory.add(create(expected));
        }
    }

    static void removeDroppedCrystals(ServerPlayer player, double radius) {
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(radius), e -> ItemCatalog.isOwnedCrystal(data(e.getItem())))) {
            item.discard();
        }
    }

    /** Ins Inventar oder, wenn voll, vor die Füße. */
    static void give(ServerPlayer player, ItemSpec spec) {
        ItemStack stack = create(spec);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
