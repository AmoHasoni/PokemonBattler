package com.pokemonbattler.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.pokemonbattler.exception.PokemonNotFoundException;
import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.model.Type;
import com.pokemonbattler.persistence.JsonRepository;
import com.pokemonbattler.persistence.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PokemonbattlerService {

    private final List<Pokemon> pokedex = new ArrayList<>();
    private final Repository<List<Pokemon>> storage = new JsonRepository<>(
            Path.of("data", "pokemons.json"), new TypeReference<>() {
    }, List::of);


    /*
     Affärslogiken för Pokédexen: håller listan med Pokémon, hanterar
     lägg till/hämta/ta bort, seed-data samt spara/ladda via JsonStorage.
     Menyn (App) pratar bara med den här klassen, aldrig direkt med filen.
     */

    public void seed() {
        pokedex.clear();

        Pokemon charmander = new Pokemon("Charmander", Type.FIRE, 39);
        charmander.addAttack(new Attack("Ember", 40, 100, Type.FIRE));
        pokedex.add(charmander);

        Pokemon squirtle = new Pokemon("Squirtle", Type.WATER, 44);
        squirtle.addAttack(new Attack("Water Gun", 40, 100, Type.WATER));
        pokedex.add(squirtle);

        Pokemon bulbasaur = new Pokemon("Bulbasaur", Type.GRASS, 45);
        bulbasaur.addAttack(new Attack("Vine Whip", 45, 100, Type.GRASS));
        pokedex.add(bulbasaur);

        Pokemon pikachu = new Pokemon("Pikachu", Type.ELECTRIC, 35);
        pikachu.addAttack(new Attack("Thunder Shock", 40, 100, Type.ELECTRIC));
        pokedex.add(pikachu);

        Pokemon eevee = new Pokemon("Eevee", Type.NORMAL, 55);
        eevee.addAttack(new Attack("Tackle", 40, 100, Type.NORMAL));
        pokedex.add(eevee);

        Pokemon growlithe = new Pokemon("Growlithe", Type.FIRE, 55);
        growlithe.addAttack(new Attack("Flame Wheel", 60, 90, Type.FIRE));
        pokedex.add(growlithe);
    }

    // Returnerar en kopia av listan, så att den inte kan ändras utifrån förbi tjänsten.
    public List<Pokemon> getAll() {
        return List.copyOf(pokedex);
    }

    // Lägger till Pokémon sist i listan.
    public void add(Pokemon pokemon) {
        pokedex.add(pokemon);
    }

    /* Hämtar Pokémon på ett index (0-baserat).
    Kastar PokemonNotFoundException om indexet ligger utanför listan.
    */
    public Pokemon get(int index) {
        if (index < 0 || index >= pokedex.size()) {
            throw new PokemonNotFoundException("Det finns ingen Pokémon med det numret.");
        }
        return pokedex.get(index);
    }

    /*
    Tar bort Pokémon på ett index (0-baserat).
    Kastar PokemonNotFoundException om indexet ligger utanför listan.
    */
    public void remove(int index) {
        if (index < 0 || index >= pokedex.size()) {
            throw new PokemonNotFoundException("Det finns ingen Pokémon med det numret.");
        }
        pokedex.remove(index);
    }

    public int size() {
        return pokedex.size();
    }

    // Returnerar true om det finns en sparad fil att ladda från.
    public boolean savedFileExists() throws IOException {
        return storage.exists();

        // Sparar hela listan till filen.
        public void save () throws IOException {
            storage.save(pokedex);
        }

    /*
     Läser in listan från filen och ersätter nuvarande data. Kastar IOException
     om filen är trasig, tom eller innehåller null, och då lämnas listan orörd.
     */
        public void load() throws IOException {
            List<Pokemon> loaded = storage.load();
            if (storage.lastWarning().isPresent()) {
                throw new IOException(storage.lastWarning().get());
            }
            if (loaded == null || loaded.contains(null)) {
                throw new IOException("Filen innehåller ogiltig data.");
            }
            for (Pokemon p : loaded) {
                if (p.getName() == null || p.getType() == null || p.getMaxHp() < Pokemon.MIN_HP
                        || p.getAttackCount() < Pokemon.MIN_ATTACKS) {
                    throw new IOException("Filen innehåller en ofullständig Pokémon.");
                }
            }
            pokedex.clear();
            pokedex.addAll(loaded);
        }
    }
