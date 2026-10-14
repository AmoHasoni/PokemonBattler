package com.pokemonbattler.battle;

import com.pokemonbattler.model.Type;

import java.util.EnumMap;
import java.util.Map;

/*
 Typeffektivitetstabell. Kombinationer som inte finns i tabellen ger 1x.
 Lagras som en EnumMap istället för en lång if/switch-kedja, så att nya
 relationer bara är en rad i konstruktorn.
 */
public class TypeChart {
    public static final double SUPER_EFFECTIVE = 2.0;
    public static final double NOT_VERY_EFFECTIVE = 0.5;
    public static final double NEUTRAL = 1.0;

    private final Map<Type, Map<Type, Double>> chart = new EnumMap<>(Type.class);

    // Fyller tabellen med relationerna från uppgiften.
    public TypeChart() {
        set(Type.FIRE, Type.GRASS, SUPER_EFFECTIVE);
        set(Type.WATER, Type.FIRE, SUPER_EFFECTIVE);
        set(Type.GRASS, Type.WATER, SUPER_EFFECTIVE);
        set(Type.ELECTRIC, Type.WATER, SUPER_EFFECTIVE);

        set(Type.FIRE, Type.WATER, NOT_VERY_EFFECTIVE);
        set(Type.WATER, Type.GRASS, NOT_VERY_EFFECTIVE);
        set(Type.GRASS, Type.FIRE, NOT_VERY_EFFECTIVE);
        // NORMAL mot alla, och övriga kombinationer, blir NEUTRAL (1x).
    }

    // Sparar en multiplikator för en kombination av anfallande och försvarande typ.
    private void set(Type attacking, Type defending, double multiplier) {
        chart.computeIfAbsent(attacking, t -> new EnumMap<>(Type.class)).put(defending, multiplier);
    }

    // Returnerar multiplikatorn för en attack av typen attacking mot en Pokémon av typen defending.
    public double getMultiplier(Type attacking, Type defending) {
        if (attacking == null || defending == null) {
            return NEUTRAL;
        }
        Map<Type, Double> row = chart.get(attacking);
        if (row == null) {
            return NEUTRAL;
        }
        return row.getOrDefault(defending, NEUTRAL);
    }

    // Text till stridsloggen.
    public static String describe(double multiplier) {
        if (multiplier > NEUTRAL) {
            return "Det är supereffektivt!";
        }
        if (multiplier < NEUTRAL) {
            return "Det är inte särskilt effektivt...";
        }
        return "";
    }
}