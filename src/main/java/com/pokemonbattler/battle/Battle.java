package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/*
 En turbaserad strid mellan två Combatants.
 Turordning: högst speed attackerar först varje runda, vid lika börjar spelaren.
 Striden slutar när en Pokémon har 0 HP, eller när spelaren flyr.
 */
public class Battle {
    private final Combatant player;
    private final Combatant opponent;
    private final DamageCalculator calculator;
    private final PrintStream log;
    private final Map<String, Integer> attacksUsed = new LinkedHashMap<>();

    // Tar emot båda deltagarna, skadekalkylatorn och var loggen ska skrivas.
    public Battle(Combatant player, Combatant opponent, DamageCalculator calculator, PrintStream log) {
        this.player = player;
        this.opponent = opponent;
        this.calculator = calculator;
        this.log = log;
    }

    // Kör striden runda för runda tills någon är besegrad eller spelaren flyr.
    public BattleResult run() {
        log.println();
        log.println("=== " + player.getDisplayName() + " mot " + opponent.getDisplayName() + " ===");

        int round = 0;
        while (true) {
            round++;
            log.println();
            log.println("--- Runda " + round + " ---");
            printStatus();

            Combatant first = firstToAct();
            Combatant second = (first == player) ? opponent : player;

            if (!takeTurn(first, second)) {
                return finish(BattleOutcome.FLED, round);
            }
            if (second.getPokemon().isFainted()) {
                break;
            }
            if (!takeTurn(second, first)) {
                return finish(BattleOutcome.FLED, round);
            }
            if (first.getPokemon().isFainted()) {
                break;
            }
        }

        BattleOutcome outcome = player.getPokemon().isFainted() ? BattleOutcome.LOSS : BattleOutcome.WIN;
        return finish(outcome, round);
    }

    // Snabbast först, spelaren vinner vid lika.
    Combatant firstToAct() {
        return opponent.getPokemon().getSpeed() > player.getPokemon().getSpeed() ? opponent : player;
    }

    // Kör en attack. Returnerar false om den som agerar valde att fly.
    private boolean takeTurn(Combatant attacker, Combatant defender) {
        Optional<Attack> choice = attacker.chooseAttack(defender.getPokemon());
        if (choice.isEmpty()) {
            log.println(attacker.getDisplayName() + " flyr från striden!");
            return false;
        }
        Attack attack = choice.get();
        if (attacker.isPlayer()) {
            attacksUsed.merge(attack.getName(), 1, Integer::sum);
        }

        log.println(attacker.getDisplayName() + " använder " + attack.getName() + "!");
        Pokemon target = defender.getPokemon();
        AttackResult result = calculator.calculate(attack, target);
        if (!result.hit()) {
            log.println("  ...men missade!");
            return true;
        }
        if (result.critical()) {
            log.println("  Kritisk träff!");
        }
        String effect = TypeChart.describe(result.effectiveness());
        if (!effect.isEmpty()) {
            log.println("  " + effect);
        }
        int dealt = target.takeDamage(result.damage());
        log.println("  " + defender.getDisplayName() + " tar " + dealt + " i skada ("
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP kvar).");
        if (target.isFainted()) {
            log.println("  " + defender.getDisplayName() + " är besegrad!");
        }
        return true;
    }

    // Skriver ut HP-status för båda Pokémon i början av varje runda.
    private void printStatus() {
        log.println(statusLine(player));
        log.println(statusLine(opponent));
    }

    // En statusrad: namn, typ, HP och HP-mätare.
    private static String statusLine(Combatant c) {
        Pokemon p = c.getPokemon();
        return String.format("  %-22s %-8s HP %3d/%-3d %s",
                c.getDisplayName(), p.getType(), p.getCurrentHp(), p.getMaxHp(), hpBar(p));
    }

    // Ritar en HP-mätare med 20 tecken, t.ex. [********..........].
    private static String hpBar(Pokemon p) {
        int width = 20;
        int filled = (int) Math.round((double) p.getCurrentHp() / p.getMaxHp() * width);
        return "[" + "*".repeat(filled) + ".".repeat(width - filled) + "]";
    }

    // Skapar resultatet och skriver ut slutmeddelandet.
    private BattleResult finish(BattleOutcome outcome, int rounds) {
        BattleResult result = new BattleResult(outcome, player.getPokemon().getName(),
                opponent.getPokemon().getName(), rounds, attacksUsed);
        log.println();
        log.println("*** " + result.summary() + " ***");
        return result;
    }
}