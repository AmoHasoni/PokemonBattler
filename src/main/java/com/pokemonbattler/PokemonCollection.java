package com.pokemonbattler;

import com.pokemonbattler.exception.InvalidPokemonException;
import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.model.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 Spelarens samling av Pokémon. Kapslar in listan så att regler som
 "unika namn" bara finns på ett ställe.
 */
public class PokemonCollection {
    private final List<Pokemon> pokemons = new ArrayList<>();

    public PokemonCollection() {
    }

    public PokemonCollection(List<Pokemon> initial) {
        if (initial != null) {
            for (Pokemon p : initial) {
                // Hoppar över null och dubbletter från en handredigerad fil.
                if (p != null && p.getName() != null && find(p.getName()).isEmpty()) {
                    pokemons.add(p);
                }
            }
        }
    }

    public void add(Pokemon pokemon) {
        if (pokemon == null) {
            throw new InvalidPokemonException("Pokémon får inte vara null.");
        }
        if (find(pokemon.getName()).isPresent()) {
            throw new InvalidPokemonException("Det finns redan en Pokémon som heter " + pokemon.getName() + ".");
        }
        pokemons.add(pokemon);
    }

    public boolean remove(Pokemon pokemon) {
        return pokemons.remove(pokemon);
    }

    // Byter namn och ser till att namnet fortfarande är unikt.
    public void rename(Pokemon pokemon, String newName) {
        Optional<Pokemon> existing = find(newName);
        if (existing.isPresent() && existing.get() != pokemon) {
            throw new InvalidPokemonException("Det finns redan en Pokémon som heter " + newName.trim() + ".");
        }
        pokemon.setName(newName);
    }

    public Optional<Pokemon> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.trim();
        return pokemons.stream().filter(p -> p.getName().equalsIgnoreCase(wanted)).findFirst();
    }

    // Oföränderlig kopia, i samma ordning som de lades till.
    public List<Pokemon> getAll() {
        return List.copyOf(pokemons);
    }

    // Pokémon som kan slåss: minst 1 HP och minst en attack.
    public List<Pokemon> getBattleReady() {
        return getAll().stream()
                .filter(p -> !p.isFainted() && p.getAttackCount() > 0)
                .toList();
    }

    public int size() {
        return pokemons.size();
    }

    public boolean isEmpty() {
        return pokemons.isEmpty();
    }

    public void healAll() {
        pokemons.forEach(Pokemon::heal);
    }

    // Används när samlingen ska sparas.
    public List<Pokemon> toList() {
        return List.copyOf(pokemons);
    }

    // Startdata (samma sex Pokémon som i inlämning 1, nu med speed för turordningen).
    public static PokemonCollection starter() {
        PokemonCollection collection = new PokemonCollection();
        collection.add(seed("Charmander", Type.FIRE, 39, 65, new Attack("Ember", 40, 100, Type.FIRE)));
        collection.add(seed("Squirtle", Type.WATER, 44, 43, new Attack("Water Gun", 40, 100, Type.WATER)));
        collection.add(seed("Bulbasaur", Type.GRASS, 45, 45, new Attack("Vine Whip", 45, 100, Type.GRASS)));
        collection.add(seed("Pikachu", Type.ELECTRIC, 35, 90, new Attack("Thunder Shock", 40, 100, Type.ELECTRIC)));
        collection.add(seed("Eevee", Type.NORMAL, 55, 55, new Attack("Tackle", 40, 100, Type.NORMAL)));
        collection.add(seed("Growlithe", Type.FIRE, 55, 60, new Attack("Flame Wheel", 60, 90, Type.FIRE)));
        return collection;
    }

    private static Pokemon seed(String name, Type type, int hp, int speed, Attack attack) {
        Pokemon p = new Pokemon(name, type, hp, speed);
        p.addAttack(attack);
        return p;
    }
}