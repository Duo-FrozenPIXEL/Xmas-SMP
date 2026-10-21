package de.xmassmp.crystal;

/** Zuordnung Kristall/Tier/Stufe → Basis-Item, Custom-Model-Data und Modell (docs/resourcepack.md). */
public final class CrystalModels {

    public static final String BASE_ITEM = "minecraft:echo_shard";
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 2;

    private CrystalModels() {
    }

    /** {@code CMD = 1000 × Tier + Stufen-Offset + Gem-Index}. */
    public static int customModelData(Crystal crystal, int tier, PristineStage stage) {
        return 1000 * checkTier(tier) + stage.modelDataOffset() + crystal.gemIndex();
    }

    public static String modelId(Crystal crystal, int tier, PristineStage stage) {
        return "xmassmp:item/" + crystal.gemTexture() + "_gem_tier" + checkTier(tier) + stage.modelSuffix();
    }

    public static boolean isValidTier(int tier) {
        return tier >= MIN_TIER && tier <= MAX_TIER;
    }

    private static int checkTier(int tier) {
        if (!isValidTier(tier)) {
            throw new IllegalArgumentException("Tier muss " + MIN_TIER + "–" + MAX_TIER + " sein: " + tier);
        }
        return tier;
    }
}
