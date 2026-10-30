package de.xmassmp.fabric;

import de.xmassmp.XmasCore;
import de.xmassmp.config.RecipeSpec;
import de.xmassmp.recipe.RecipeService;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Werkbank-Rezept, dessen Muster aus {@code config.jsonc} kommt ({@link RecipeService}). Für jede
 * Rezept-ID gibt es eine Datei {@code data/xmassmp/recipe/<id>.json} mit {@code "type": "xmassmp:<id>"}.
 *
 * <p>{@link #getRemainingItems} läuft genau einmal beim Herausnehmen des Ergebnisses (auch bei
 * Shift-Klick je Durchgang und beim Crafter-Block) und schließt dabei eine zerlegte Energy Bottle im
 * Register. Ihre leere Glasflasche bleibt im Feld liegen.
 */
final class XmasRecipe extends CustomRecipe {

    private final String id;

    XmasRecipe(CraftingBookCategory category, String id) {
        super(category);
        this.id = id;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        XmasCore core = XmasSmpFabric.core();
        return core != null && core.recipes().matches(id, input.width(), input.height(), cells(input));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        XmasCore core = XmasSmpFabric.core();
        Optional<RecipeService.Result> result = core == null ? Optional.empty() : core.recipes().result(id);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = FabricItems.create(result.get().item());
        stack.setCount(result.get().count());
        return stack;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        XmasCore core = XmasSmpFabric.core();
        if (core == null || !RecipeSpec.GEM_FRAGMENTS.equals(id)) {
            return remaining;
        }
        List<RecipeService.Cell> cells = cells(input);
        if (core.recipes().onCrafted(id, cells, "crafting")) {
            for (int i = 0; i < cells.size(); i++) {
                if (!cells.get(i).isEmpty()) {
                    remaining.set(i, new ItemStack(Items.GLASS_BOTTLE));
                }
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        @SuppressWarnings("unchecked")
        RecipeSerializer<? extends CustomRecipe> serializer = (RecipeSerializer<? extends CustomRecipe>)
                BuiltInRegistries.RECIPE_SERIALIZER.getValue(XmasRegistry.id(id));
        return serializer;
    }

    static List<RecipeService.Cell> cells(CraftingInput input) {
        List<RecipeService.Cell> cells = new ArrayList<>(input.size());
        for (ItemStack stack : input.items()) {
            cells.add(stack.isEmpty() ? RecipeService.Cell.EMPTY
                    : new RecipeService.Cell(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    FabricItems.data(stack)));
        }
        return cells;
    }
}
