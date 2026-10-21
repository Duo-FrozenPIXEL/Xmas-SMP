package de.xmassmp.config;

import com.google.gson.JsonParseException;
import de.xmassmp.util.JsonFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lädt {@code config.jsonc}. Fehlt die Datei, wird die dokumentierte Vorlage geschrieben.
 * Ist sie fehlerhaft, bleibt die zuletzt gültige Konfiguration aktiv – ein Reload bringt
 * den Server also nie in einen kaputten Zustand.
 */
public final class ConfigManager {

    public static final String FILE_NAME = "config.jsonc";
    static final String TEMPLATE = "/xmassmp/config.default.jsonc";

    private final Path file;
    private final List<Consumer<XmasConfig>> listeners = new CopyOnWriteArrayList<>();
    private volatile XmasConfig current = new XmasConfig();

    public ConfigManager(Path configDirectory) {
        this.file = configDirectory.resolve(FILE_NAME);
    }

    public XmasConfig get() {
        return current;
    }

    public Path file() {
        return file;
    }

    /** Wird nach jedem erfolgreichen (Re-)Load aufgerufen. */
    public void onReload(Consumer<XmasConfig> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public LoadResult load() {
        List<String> messages = new ArrayList<>();
        try {
            if (Files.notExists(file)) {
                writeTemplate();
                messages.add(FILE_NAME + " mit Standardwerten angelegt");
            }
            XmasConfig parsed;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                parsed = JsonFiles.readLenient(reader, XmasConfig.class);
            }
            XmasConfig loaded = parsed != null ? parsed : new XmasConfig();
            messages.addAll(loaded.validate());
            current = loaded;
            listeners.forEach(listener -> listener.accept(loaded));
            return new LoadResult(true, messages);
        } catch (IOException | JsonParseException e) {
            messages.add(FILE_NAME + " konnte nicht gelesen werden, alte Konfiguration bleibt aktiv: "
                    + rootMessage(e));
            return new LoadResult(false, messages);
        }
    }

    private void writeTemplate() throws IOException {
        try (InputStream in = ConfigManager.class.getResourceAsStream(TEMPLATE)) {
            if (in == null) {
                throw new IOException("Vorlage " + TEMPLATE + " fehlt im JAR");
            }
            JsonFiles.writeAtomic(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    public record LoadResult(boolean success, List<String> messages) {
        public LoadResult {
            messages = List.copyOf(messages);
        }
    }
}
