package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;

import java.util.List;
import java.util.Optional;
import java.util.Random;

// Datorn: väljer slumpvis bland sina egna attacker och flyr aldrig.
public class CpuCombatant extends Combatant {
    private final Random random;

    // Datorn får samma Random som resten av spelet.
    public CpuCombatant(Pokemon pokemon, Random random) {
        super("Vilda " + pokemon.getName(), pokemon);
        this.random = random;
    }

    // Väljer en slumpad attack bland sina egna. Tom Optional bara om attacker saknas.
    @Override
    public Optional<Attack> chooseAttack(Pokemon opponent) {
        List<Attack> attacks = getPokemon().getAttacks();
        if (attacks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(attacks.get(random.nextInt(attacks.size())));
    }

    // Datorn är inte spelaren.
    @Override
    public boolean isPlayer() {
        return false;
    }
}