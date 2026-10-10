package com.pokemonbattler.persistence;

import java.util.Optional;

/*
 Abstraktion för att läsa och spara data. Resten av programmet beror
 bara på detta interface, inte på att det är JSON på disk.
 */
public interface Repository<T> {

    // Finns det någon sparad data att läsa?
    boolean exists();

    // Läser data. Returnerar alltid ett giltigt värde (standardvärde om filen saknas eller är trasig).
    T load();

    // Sparar data. Returnerar false om det inte gick att skriva.
    boolean save(T data);

    // Meddelande om något gick snett vid senaste load/save.
    Optional<String> lastWarning();
}
