package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.ui.ConsoleInput;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

// Spelaren: väljer attack via en meny i konsolen.
public class HumanCombatant extends Combatant {
    private final ConsoleInput input;
    private final PrintStream out;

    public HumanCombatant(Pokemon pokemon, ConsoleInput input, PrintStream out) {
        super("Din " + pokemon.getName(), pokemon);
        this.input = input;
        this.out = out;
    }

    @Override
    public Optional<Attack> chooseAttack(Pokemon opponent) {
        List<Attack> attacks = getPokemon().getAttacks();
        out.println("Välj attack:");
        for (int i = 0; i < attacks.size(); i++) {
            out.println("  " + (i + 1) + ". " + attacks.get(i));
        }
        out.println("  0. Fly från striden");
        // readInt frågar om vid "abc", -1, 99 osv. så ogiltigt index kan aldrig nå hit.
        int choice = input.readInt("> ", 0, attacks.size());
        if (choice == 0) {
            return Optional.empty();
        }
        return Optional.of(attacks.get(choice - 1));
    }

    @Override
    public boolean isPlayer() {
        return true;
    }
}
