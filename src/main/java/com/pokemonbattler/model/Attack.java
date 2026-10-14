package com.pokemonbattler.model;

import com.pokemonbattler.exception.InvalidAttackException;

/* En attack som en Pokémon kan använda. Klassen validerar sina egna fält
 i sina "setters", så att ett Attack-objekt aldrig kan hamna i ett
 ogiltigt tillstånd, vare sig det skapas manuellt eller läses in från JSON.
 */
public class Attack {
    public static final int MIN_DAMAGE = 1;
    public static final int MAX_DAMAGE = 300;
    public static final int MIN_ACCURACY = 1;
    public static final int MAX_ACCURACY = 100;
    public static final int MAX_NAME_LENGTH = 40;

    private String name;
    private int baseDamage;
    private int accuracy;
    private Type type;

    // Tom konstruktor krävs av Jackson (JSON-biblioteket) vid inläsning.
    public Attack() {
    }

    // Vanlig konstruktor som används när en ny attack skapas via menyn.
    public Attack(String name, int baseDamage, int accuracy, Type type) {
        setName(name);
        setBaseDamage(baseDamage);
        setAccuracy(accuracy);
        setType(type);
    }

    // Attackens namn.
    public String getName() {
        return name;
    }

    // Kastar InvalidAttackException om namnet är tomt eller för långt.
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidAttackException("Attacknamn får inte vara tomt.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidAttackException("Attacknamn får vara max " + MAX_NAME_LENGTH + " tecken.");
        }
        this.name = trimmed;
    }

    // Basskada som används i skadeformeln.
    public int getBaseDamage() {
        return baseDamage;
    }

    // Kastar InvalidAttackException om skadan ligger utanför tillåtet intervall.
    public void setBaseDamage(int baseDamage) {
        if (baseDamage < MIN_DAMAGE || baseDamage > MAX_DAMAGE) {
            throw new InvalidAttackException(
                    "Basskada måste vara mellan " + MIN_DAMAGE + " och " + MAX_DAMAGE + ".");
        }
        this.baseDamage = baseDamage;
    }

    // Träffsäkerhet i procent (1–100).
    public int getAccuracy() {
        return accuracy;
    }

    // Kastar InvalidAttackException om träffsäkerheten ligger utanför tillåtet intervall.
    public void setAccuracy(int accuracy) {
        if (accuracy < MIN_ACCURACY || accuracy > MAX_ACCURACY) {
            throw new InvalidAttackException(
                    "Träffsäkerhet måste vara mellan " + MIN_ACCURACY + " och " + MAX_ACCURACY + ".");
        }
        this.accuracy = accuracy;
    }

    // Attackens typ, används för typeffektivitet.
    public Type getType() {
        return type;
    }

    // Kastar InvalidAttackException om typen saknas (null).
    public void setType(Type type) {
        if (type == null) {
            throw new InvalidAttackException("Attacktyp måste anges. Tillåtna: " + Type.options());
        }
        this.type = type;
    }

    // Skriver ut attacken snyggt i menyerna, t.ex. "Ember [FIRE] dmg=40 acc=100%".
    @Override
    public String toString() {
        return name + " [" + type + "] dmg=" + baseDamage + " acc=" + accuracy + "%";
    }
}