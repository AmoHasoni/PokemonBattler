package com.pokemonbattler.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pokemonbattler.model.Pokemon;

import java.io.File;
import java.io.IOException;
import java.util.List;


// Sköter läsning och skrivning av Pokemon Battlern till en JSON-fil (pokedex.json).
// Använder biblioteket Jackson för själva JSON-hanteringen (VG-krav).
// Klassen gör bara detta, och all annan logik ligger i PokemonbattlerService.


public class JsonStorage {

    private static final String FILE_NAME = "pokedex.json";

    /*
    INDENT_OUTPUT gör filen läsbar för människor. FAIL_ON_UNKNOWN_PROPERTIES
    är avstängt så att extra fält i filen (t.ex. attackCount) ignoreras.
    */
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Returnerar true om sparfilen finns i projektmappen.
    public boolean fileExists() {
        return new File(FILE_NAME).isFile();
    }

    /*
    Skriver hela listan som JSON till filen.
    Kastar IOException om filen inte kan skrivas.
    */
    public void save(List<Pokemon> pokemons) throws IOException {
        mapper.writeValue(new File(FILE_NAME), pokemons);
    }

    /*
      Läser in listan från filen. Kastar IOException om filen är trasig eller
      innehåller ogiltig data (Pokemon/Attack validerar sig själva vid inläsning).
     */
    public List<Pokemon> load() throws IOException {
        return mapper.readValue(new File(FILE_NAME), new TypeReference<List<Pokemon>>() {});
    }
}