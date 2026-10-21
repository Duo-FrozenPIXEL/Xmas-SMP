package de.xmassmp.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** JSON lesen (mit Kommentaren) und atomar schreiben. */
public final class JsonFiles {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Einzeilig, z. B. für Log-Dateien. */
    public static final Gson COMPACT = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private JsonFiles() {
    }

    /** Liest JSON nachsichtig: {@code //}-, {@code #}- und {@code /* *}{@code /}-Kommentare sind erlaubt. */
    public static <T> T readLenient(Reader reader, Type type) {
        JsonReader json = new JsonReader(reader);
        json.setStrictness(Strictness.LENIENT);
        return GSON.fromJson(json, type);
    }

    public static JsonElement parseLenient(String text) {
        return readLenient(new StringReader(text), JsonElement.class);
    }

    /** Schreibt über eine Temp-Datei, damit ein Absturz keine halbe Datei hinterlässt. */
    public static void writeAtomic(Path file, String content) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
