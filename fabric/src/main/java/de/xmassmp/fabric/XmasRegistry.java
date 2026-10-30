package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.crystal.CrystalModels;
import de.xmassmp.item.CustomItem;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.recipe.RecipeService;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Registriert die eigenen Server-Items, den Totem-Block und die Werkbank-Rezepte. Alles ist über
 * Polymer server-seitig: Vanilla-Clients sehen das Basis-Item mit {@code custom_model_data} aus dem
 * Pack, Vanilla-Rezepte, Brauen, Handel und Platzieren greifen nicht.
 */
final class XmasRegistry {

    /** Item je Typ aus {@code custom_data.xmassmp.type} ({@code crystal}, {@code energy_bottle} …). */
    private static final Map<String, Item> ITEMS = new HashMap<>();
    static TotemBlock totem;

    private XmasRegistry() {
    }

    static void register() {
        registerItem(ItemCatalog.TYPE_CRYSTAL, CrystalModels.BASE_ITEM, 1);
        for (CustomItem item : CustomItem.values()) {
            registerItem(item.id(), item.baseItem(), item.maxStackSize());
        }
        Identifier totemId = id("repair_totem");
        totem = Registry.register(BuiltInRegistries.BLOCK, totemId, new TotemBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, totemId))
                // Abbauzeit bestimmt TotemBlock selbst; Explosionen, Kolben und Feuer wirken nicht.
                .strength(50f, 3_600_000f)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> 10)
                .sound(SoundType.VAULT)));
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.BLOCK, totem);
        for (String recipe : RecipeService.ids()) {
            Identifier recipeId = id(recipe);
            RecipeSerializer<XmasRecipe> serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, recipeId,
                    new CustomRecipe.Serializer<>(category -> new XmasRecipe(category, recipe)));
            RegistrySyncUtils.setServerEntry(BuiltInRegistries.RECIPE_SERIALIZER, serializer);
        }
    }

    private static void registerItem(String type, String baseItem, int maxStack) {
        Identifier itemId = id(type);
        Item base = BuiltInRegistries.ITEM.getValue(McIds.parse(baseItem));
        XmasItem item = Registry.register(BuiltInRegistries.ITEM, itemId, new XmasItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, itemId))
                .stacksTo(maxStack), base, type));
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.ITEM, item);
        ITEMS.put(type, item);
    }

    /** Server-Item zu einem Typ oder {@code null}. */
    static Item item(String type) {
        return type == null ? null : ITEMS.get(type);
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(XmasCore.MOD_ID, path);
    }
}
