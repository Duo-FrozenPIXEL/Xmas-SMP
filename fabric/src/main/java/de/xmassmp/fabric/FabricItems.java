package de.xmassmp.fabric;

import de.xmassmp.item.ItemSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

    /** Ins Inventar oder, wenn voll, vor die Füße. */
    static void give(ServerPlayer player, ItemSpec spec) {
        ItemStack stack = create(spec);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
