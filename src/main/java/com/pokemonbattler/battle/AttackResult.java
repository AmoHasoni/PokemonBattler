package com.pokemonbattler.battle;

/*
 Resultatet av en attack: om den träffade, hur mycket skada den gjorde,
 typeffektivitet och om det var en kritisk träff.
 */
public record AttackResult(boolean hit, int damage, double effectiveness, boolean critical) {

    // Skapar ett resultat för en miss: ingen skada.
    public static AttackResult miss() {
        return new AttackResult(false, 0, TypeChart.NEUTRAL, false);
    }
}
