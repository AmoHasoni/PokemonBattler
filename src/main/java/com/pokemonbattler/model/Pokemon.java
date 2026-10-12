package com.pokemonbattler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.pokemonbattler.exception.InvalidPokemonException;

import java.util.ArrayList;
import java.util.List;

/* En Pokémon: namn, typ, HP, speed och 1–4 attacker.
 Klassen validerar sina egna fält (i sina setters), så att ett
 Pokemon-objekt aldrig kan hamna i ett ogiltigt tillstånd. */

// MaxHp skrivs före currentHp i JSON så att valideringen i setCurrentHp fungerar vid inläsning.
@JsonPropertyOrder({"name", "type", "maxHp", "currentHp", "speed", "attacks"})
public class Pokemon {
    public static final int MIN_HP = 1;
    public static final int MAX_HP = 999;
    public static final int MIN_ATTACKS = 1;
    public static final int MAX_ATTACKS = 4;
    public static final int MAX_NAME_LENGTH = 30;
    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 200;
    public static final int DEFAULT_SPEED = 50;

    private String name;
    private Type type;
    private int maxHp;
    private int currentHp;
    private int speed = DEFAULT_SPEED;
    private final List<Attack> attacks = new ArrayList<>();

    // Tom konstruktor krävs av Jackson vid inläsning.
    public Pokemon() {
    }

    // Anropar nu konstruktorn nedan med standard-speed.
    public Pokemon(String name, Type type, int maxHp) {
        this(name, type, maxHp, DEFAULT_SPEED);
    }

    // Konstruktor med speed, som avgör turordningen i strid.
    public Pokemon(String name, Type type, int maxHp, int speed) {
        setName(name);
        setType(type);
        setMaxHp(maxHp);
        setSpeed(speed);
        this.currentHp = this.maxHp;
    }

    // Fristående kopia (även av attackerna). Används för vilda Pokémon.
    public Pokemon copy() {
        Pokemon copy = new Pokemon(name, type, maxHp, speed);
        for (Attack a : attacks) {
            copy.addAttack(new Attack(a.getName(), a.getBaseDamage(), a.getAccuracy(), a.getType()));
        }
        copy.setCurrentHp(currentHp);
        return copy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidPokemonException("Namn får inte vara tomt.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidPokemonException("Namn får vara max " + MAX_NAME_LENGTH + " tecken.");
        }
        this.name = trimmed;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type == null) {
            throw new InvalidPokemonException("Typ måste anges. Tillåtna: " + Type.options());
        }
        this.type = type;
    }

    public int getMaxHp() {
        return maxHp;
    }

    // Om aktuellt HP är högre än det nya max HP:t sänks det automatiskt.
    public void setMaxHp(int maxHp) {
        if (maxHp < MIN_HP || maxHp > MAX_HP) {
            throw new InvalidPokemonException("Max HP måste vara mellan " + MIN_HP + " och " + MAX_HP + ".");
        }
        this.maxHp = maxHp;
        if (currentHp > maxHp) {
            currentHp = maxHp;
        }
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        if (currentHp < 0) {
            throw new InvalidPokemonException("Aktuellt HP får inte vara negativt.");
        }
        if (maxHp > 0 && currentHp > maxHp) {
            throw new InvalidPokemonException("Aktuellt HP får inte överstiga max HP.");
        }
        this.currentHp = currentHp;
    }

    public int getSpeed() {
        return speed;
    }

    // Samma typ av validering som dina andra setters.
    public void setSpeed(int speed) {
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new InvalidPokemonException("Speed måste vara mellan " + MIN_SPEED + " och " + MAX_SPEED + ".");
        }
        this.speed = speed;
    }

    // En Pokémon med 0 HP är besegrad och kan inte slåss.
    @JsonIgnore
    public boolean isFainted() {
        return currentHp <= 0;
    }

    // Drar av skada, men aldrig under 0. Returnerar den skada som faktiskt drogs av.
    public int takeDamage(int damage) {
        if (damage < 0) {
            throw new InvalidPokemonException("Skada får inte vara negativ.");
        }
        int dealt = Math.min(damage, currentHp);
        currentHp -= dealt;
        return dealt;
    }

    // Återställer HP till max (Pokémon Center).
    public void heal() {
        currentHp = maxHp;
    }

    // Returnerar en kopia, så att listan inte kan ändras utifrån förbi valideringen.
    public List<Attack> getAttacks() {
        return List.copyOf(attacks);
    }

    public void setAttacks(List<Attack> newAttacks) {
        if (newAttacks == null || newAttacks.isEmpty()) {
            throw new InvalidPokemonException("En Pokémon måste ha minst " + MIN_ATTACKS + " attack.");
        }
        if (newAttacks.size() > MAX_ATTACKS) {
            throw new InvalidPokemonException("En Pokémon kan ha högst " + MAX_ATTACKS + " attacker.");
        }
        attacks.clear();
        attacks.addAll(newAttacks);
    }

    public void addAttack(Attack attack) {
        if (attack == null) {
            throw new InvalidPokemonException("Attack får inte vara null.");
        }
        if (attacks.size() >= MAX_ATTACKS) {
            throw new InvalidPokemonException("En Pokémon kan ha högst " + MAX_ATTACKS + " attacker.");
        }
        attacks.add(attack);
    }

    public void removeAttackAt(int index) {
        if (index < 0 || index >= attacks.size()) {
            throw new InvalidPokemonException("Ogiltigt attackindex.");
        }
        if (attacks.size() <= MIN_ATTACKS) {
            throw new InvalidPokemonException("En Pokémon måste ha minst " + MIN_ATTACKS + " attack.");
        }
        attacks.remove(index);
    }

    @JsonIgnore   // Hindrar att "attackCount" skrivs in i JSON-filen
    public int getAttackCount() {
        return attacks.size();
    }

    // En rad i listan, t.ex. "Charmander  FIRE  HP 39/39  attacker: 1".
    public String formatSummary() {
        return name + "  " + type + "  HP " + currentHp + "/" + maxHp
                + "  attacker: " + attacks.size()
                + (isFainted() ? "  (besegrad)" : "");
    }

    // All info när användaren väljer "Visa detaljer".
    public String formatDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Namn: ").append(name).append('\n');
        sb.append("Typ:  ").append(type).append('\n');
        sb.append("HP:   ").append(currentHp).append('/').append(maxHp).append('\n');
        sb.append("SPD:  ").append(speed).append('\n');
        sb.append("Attacker:\n");
        if (attacks.isEmpty()) {
            sb.append("  (inga)\n");
        } else {
            for (int i = 0; i < attacks.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(attacks.get(i)).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatSummary();
    }
}