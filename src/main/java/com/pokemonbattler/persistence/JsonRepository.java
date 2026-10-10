package com.pokemonbattler.persistence;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Supplier;

/*
 Generisk JSON-lagring med Jackson (VG-krav). Används både för
 Pokémon-listan och för statistiken.

 - Saknad fil:  standardvärdet returneras.
 - Trasig fil:  filen döps om till *.corrupt-<tid> så att datan inte
   skrivs över i tysthet, och standardvärdet returneras med en varning.
 Inget undantag lämnar klassen, så programmet kraschar aldrig på grund av filer.
 */
public class JsonRepository<T> implements Repository<T> {
    /*
     INDENT_OUTPUT gör filen läsbar för människor. FAIL_ON_UNKNOWN_PROPERTIES
     är avstängt så att extra fält i filen ignoreras.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Path file;
    private final TypeReference<T> typeReference;
    private final Supplier<T> defaultValue;
    private String warning;

    public JsonRepository(Path file, TypeReference<T> typeReference, Supplier<T> defaultValue) {
        this.file = file;
        this.typeReference = typeReference;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    @Override
    public T load() {
        warning = null;
        if (!exists()) {
            return defaultValue.get();
        }
        try {
            T data = MAPPER.readValue(file.toFile(), typeReference);
            if (data == null) {
                throw new IOException("Filen är tom.");
            }
            return data;
        } catch (JacksonException | RuntimeException e) {
            // Ogiltig JSON, eller värden som Pokemon/Attack vägrar ta emot i sina setters.
            warning = "filen " + file.getFileName() + " är trasig: " + firstLine(e) + ". "
                    + backupCorruptFile();
            return defaultValue.get();
        } catch (IOException e) {
            warning = "kunde inte läsa " + file.getFileName() + ": " + firstLine(e);
            return defaultValue.get();
        }
    }

    @Override
    public boolean save(T data) {
        warning = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Skriv först till en temporär fil och byt sedan namn, så att en
            // avbruten skrivning inte lämnar en halvskriven (trasig) fil.
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            MAPPER.writerFor(typeReference).writeValue(tmp.toFile(), data);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | RuntimeException e) {
            warning = "Kunde inte spara " + file.getFileName() + ": " + firstLine(e);
            return false;
        }
    }

    @Override
    public Optional<String> lastWarning() {
        return Optional.ofNullable(warning);
    }

    private String backupCorruptFile() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + stamp);
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            return "Sparade en kopia som " + backup.getFileName() + ".";
        } catch (IOException e) {
            return "Kunde inte säkerhetskopiera filen.";
        }
    }

    private static String firstLine(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int nl = msg.indexOf('\n');
        return nl >= 0 ? msg.substring(0, nl) : msg;
    }
}
