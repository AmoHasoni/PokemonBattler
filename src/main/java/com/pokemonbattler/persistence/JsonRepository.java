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
 Används både för
 Pokémon-listan och för statistiken.
 Saknad fil: standardvärdet returneras.
 Trasig fil: filen döps om till *.corrupt-<tid> så att datan inte
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

    /*
     file: var datan sparas. typeReference: vilken typ Jackson läser.
     DefaultValue: vad som returneras om filen saknas eller är trasig.
     */
    public JsonRepository(Path file, TypeReference<T> typeReference, Supplier<T> defaultValue) {
        this.file = file;
        this.typeReference = typeReference;
        this.defaultValue = defaultValue;
    }

    // Sant om filen finns på disk.
    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    /*
     Läser filen. Vid saknad eller trasig fil returneras standardvärdet,
     aldrig ett exception.
     */
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
            warning = "filen " + file.getFileName() + " är trasig (" + reason(e) + "). "
                    + backupCorruptFile();
            return defaultValue.get();
        } catch (IOException e) {
            warning = "kunde inte läsa " + file.getFileName() + ": " + firstLine(e);
            return defaultValue.get();
        }
    }

    // Sparar via en temporär fil. Returnerar false om det misslyckas.
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

    // Varning från senaste load/save, eller tom om allt gick bra.
    @Override
    public Optional<String> lastWarning() {
        return Optional.ofNullable(warning);
    }

    // Döper om en trasig fil till *.corrupt-<tid> så att den inte skrivs över.
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

    /*
 Ger en kort, begriplig förklaring i stället för Jacksons långa tekniska text.
 Om en setter i Pokemon/Attack/Stats vägrade ett värde används dess eget meddelande,
 t.ex. "Max HP måste vara mellan 1 och 999.". Annars är det felaktig JSON-syntax.
 */
    private static String reason(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof RuntimeException && !(cause instanceof JacksonException)
                    && cause.getMessage() != null) {
                String message = cause.getMessage();
                // Tar bort en avslutande punkt, eftersom texten hamnar inom parentes.
                return message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
            }
            cause = cause.getCause();
        }
        return "ogiltig JSON";
    }

    // Plockar ut första raden av ett felmeddelande så att utskriften blir kort.
    private static String firstLine(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int nl = msg.indexOf('\n');
        return nl >= 0 ? msg.substring(0, nl) : msg;
    }
}
