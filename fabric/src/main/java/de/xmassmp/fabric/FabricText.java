package de.xmassmp.fabric;

import de.xmassmp.text.LocalizedText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Wandelt Kern-Texte (§-Codes, {@link LocalizedText}) in Minecraft-Components um. */
final class FabricText {

    private FabricText() {
    }

    /** Legacy-Text mit §-Farbcodes. */
    static MutableComponent legacy(String text) {
        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            ChatFormatting format = c == '§' && i + 1 < text.length()
                    ? ChatFormatting.getByCode(text.charAt(i + 1)) : null;
            if (format == null) {
                segment.append(c);
                continue;
            }
            flush(root, segment, style);
            if (format == ChatFormatting.RESET) {
                style = Style.EMPTY;
            } else if (format.isColor()) {
                style = Style.EMPTY.withColor(format);
            } else {
                style = style.applyFormat(format);
            }
            i++;
        }
        flush(root, segment, style);
        return root;
    }

    /** Übersetzbarer Text; ohne Ressourcenpaket zeigt der Client den deutschen Fallback. */
    static MutableComponent localized(LocalizedText text) {
        MutableComponent component;
        if (text.isLiteral()) {
            component = legacy(text.fallback());
        } else {
            Object[] args = text.args().stream().map(FabricText::localized).toArray();
            component = Component.translatableWithFallback(text.key(), text.fallback(), args);
        }
        ChatFormatting color = text.color() == 0 ? null : ChatFormatting.getByCode(text.color());
        return color == null ? component : component.withStyle(color);
    }

    private static void flush(MutableComponent root, StringBuilder segment, Style style) {
        if (!segment.isEmpty()) {
            root.append(Component.literal(segment.toString()).setStyle(style));
            segment.setLength(0);
        }
    }
}
