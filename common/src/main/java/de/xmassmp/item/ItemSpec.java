package de.xmassmp.item;

import de.xmassmp.text.LocalizedText;

import java.util.List;
import java.util.Map;

/**
 * Plattformneutrale Beschreibung eines Custom-Items. Der Adapter baut daraus den Item-Stack.
 *
 * @param baseItem        Vanilla-Item, z. B. {@code minecraft:echo_shard}
 * @param customModelData Wert für die Komponente {@code custom_model_data} (floats[0])
 * @param name            Item-Name (nicht kursiv)
 * @param lore            Lore-Zeilen
 * @param data            Markierungen in {@code custom_data} unter dem Schlüssel {@code xmassmp}
 */
public record ItemSpec(
        String baseItem,
        int customModelData,
        LocalizedText name,
        List<LocalizedText> lore,
        Map<String, String> data) {

    public static final String DATA_ROOT = "xmassmp";
    public static final String DATA_TYPE = "type";

    public ItemSpec {
        lore = List.copyOf(lore);
        data = Map.copyOf(data);
    }
}
