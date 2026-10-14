package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.model.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 Poolen av "vilda" Pokémon som datorn slumpar sin motståndare ifrån.
 randomOpponent() returnerar alltid en ny kopia med fullt HP.
 */
public class WildPokemonPool {
    private final List<Pokemon> templates = new ArrayList<>();
    private final Random random;

    // Skapar de sex vilda Pokémon som datorn kan slumpa fram.
    public WildPokemonPool(Random random) {
        this.random = random;
        // HP i samma storleksordning som startdatan, så att striderna blir jämna.
        templates.add(create("Vulpix", Type.FIRE, 38, 65,
                new Attack("Ember", 25, 100, Type.FIRE),
                new Attack("Quick Attack", 20, 100, Type.NORMAL)));
        templates.add(create("Psyduck", Type.WATER, 50, 55,
                new Attack("Water Gun", 25, 100, Type.WATER),
                new Attack("Scratch", 20, 100, Type.NORMAL)));
        templates.add(create("Oddish", Type.GRASS, 45, 30,
                new Attack("Absorb", 20, 100, Type.GRASS),
                new Attack("Razor Leaf", 30, 90, Type.GRASS)));
        templates.add(create("Voltorb", Type.ELECTRIC, 40, 100,
                new Attack("Spark", 25, 95, Type.ELECTRIC),
                new Attack("Tackle", 20, 95, Type.NORMAL)));
        templates.add(create("Rattata", Type.NORMAL, 30, 72,
                new Attack("Tackle", 20, 95, Type.NORMAL),
                new Attack("Hyper Fang", 35, 80, Type.NORMAL)));
        templates.add(create("Ponyta", Type.FIRE, 50, 90,
                new Attack("Flame Wheel", 30, 90, Type.FIRE),
                new Attack("Stomp", 25, 100, Type.NORMAL)));
    }

    // Hjälpmetod som skapar en vild Pokémon med sina attacker.
    private static Pokemon create(String name, Type type, int hp, int speed, Attack... attacks) {
        Pokemon p = new Pokemon(name, type, hp, speed);
        for (Attack a : attacks) {
            p.addAttack(a);
        }
        return p;
    }

    // Returnerar en slumpad vild Pokémon som en ny kopia med fullt HP.
    public Pokemon randomOpponent() {
        Pokemon copy = templates.get(random.nextInt(templates.size())).copy();
        copy.heal();
        return copy;
    }
}