package com.pokemonbattler.battle;

import java.util.Map;

/*
 Sammanfattning av en avslutad strid, sett från spelarens håll.
 attacksUsed räknar hur många gånger spelaren använde varje attack.
 */
public record BattleResult(BattleOutcome outcome,
                           String playerPokemon,
                           String opponentPokemon,
                           int turns,
                           Map<String, Integer> attacksUsed) {

    public BattleResult {
        attacksUsed = Map.copyOf(attacksUsed);
    }

    public String summary() {
        return switch (outcome) {
            case WIN -> "Du vann! Din " + playerPokemon + " besegrade vilda " + opponentPokemon
                    + " efter " + turns + " rundor.";
            case LOSS -> "Du förlorade... Vilda " + opponentPokemon + " besegrade din " + playerPokemon + ".";
            case FLED -> "Du flydde från vilda " + opponentPokemon + ".";
        };
    }
}