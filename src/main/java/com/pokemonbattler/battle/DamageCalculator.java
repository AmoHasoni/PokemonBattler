package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;

import java.util.Random;

/*
 Beräknar om en attack träffar och hur mycket skada den gör.

 Formel:
   skada = avrunda( basskada × typeffektivitet × slumpfaktor × kritisk )
   - slumpfaktor: slumpat decimaltal mellan 0.85 och 1.00
   - kritisk:     2.0 med chansen 1/16, annars 1.0
   - en träff gör alltid minst 1 i skada

 Träff: ett slumptal 1–100 jämförs med attackens träffsäkerhet (accuracy).

 Random skickas in via konstruktorn så att tester kan använda ett fast
 frö (seed) och få förutsägbara resultat.
 */
public class DamageCalculator {
    public static final double MIN_RANDOM_FACTOR = 0.85;
    public static final double MAX_RANDOM_FACTOR = 1.00;
    public static final int CRITICAL_CHANCE = 16; // 1 på 16
    public static final double CRITICAL_MULTIPLIER = 2.0;

    private final TypeChart typeChart;
    private final Random random;

    public DamageCalculator(TypeChart typeChart, Random random) {
        this.typeChart = typeChart;
        this.random = random;
    }

    // Kör en hel attack: träff/miss, typeffektivitet, slump och kritisk träff.
    public AttackResult calculate(Attack attack, Pokemon defender) {
        int roll = random.nextInt(100) + 1; // 1–100
        if (roll > attack.getAccuracy()) {
            return AttackResult.miss();
        }
        double effectiveness = typeChart.getMultiplier(attack.getType(), defender.getType());
        double randomFactor = MIN_RANDOM_FACTOR
                + random.nextDouble() * (MAX_RANDOM_FACTOR - MIN_RANDOM_FACTOR);
        boolean critical = random.nextInt(CRITICAL_CHANCE) == 0;
        int damage = computeDamage(attack.getBaseDamage(), effectiveness, randomFactor, critical);
        return new AttackResult(true, damage, effectiveness, critical);
    }

    // Själva formeln, utan slump, så att den går att testa direkt.
    public static int computeDamage(int baseDamage, double effectiveness, double randomFactor, boolean critical) {
        double critMultiplier = critical ? CRITICAL_MULTIPLIER : 1.0;
        long damage = Math.round(baseDamage * effectiveness * randomFactor * critMultiplier);
        return (int) Math.max(1, damage);
    }
}