package com.pokemonbattler.model;

import com.pokemonbattler.exception.InvalidAttackException;

import java.util.ArrayList;
import java.util.List;

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

    public String getName() {
        return name;
    }

    //Kastar InvalidAttackException om namnet är tomt eller för långt.
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

    public static class Pokemon {
        public static final int MIN_HP = 1;
        public static final int MAX_HP = 999;
        public static final int MIN_ATTACKS = 1;
        public static final int MAX_ATTACKS = 4;
        public static final int MAX_NAME_LENGTH = 30;

        private String name;
        private Type type;
        private int maxHp;
        private int currentHp;
        private final List<Attack> attacks = new ArrayList<>();

        // Tom konstruktor behövs när JSON laddas senare
        public Pokemon() {
        }

        // Vanlig konstruktor som används när en ny Pokémon skapas. Sätter aktuellt HP till max HP.
        public Pokemon(String name, Type type, int maxHp) {
            setName(name);
            setType(type);
            setMaxHp(maxHp);
            this.currentHp = this.maxHp;
        }

        public String getName() {
            return name;
        }

        // Kastar InvalidPokemonException om namnet är tomt eller för långt.
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

        // Kastar InvalidPokemonException om typen saknas (null).
        public void setType(Type type) {
            if (type == null) {
                throw new InvalidPokemonException("Typ måste anges. Tillåtna: " + Type.options());
            }
            this.type = type;
        }

        public int getMaxHp() {
            return maxHp;
        }

        /*
         Kastar InvalidPokemonException om värdet ligger utanför tillåtet intervall.
         Om aktuellt HP är högre än det nya max HP:t sänks det automatiskt.
         */
        public void setMaxHp(int maxHp) {
            if (maxHp < MIN_HP || maxHp > MAX_HP) {
                throw new InvalidPokemonException(
                        "Max HP måste vara mellan " + MIN_HP + " och " + MAX_HP + ".");
            }
            this.maxHp = maxHp;
            if (currentHp > maxHp) {
                currentHp = maxHp;
            }
        }

        public int getCurrentHp() {
            return currentHp;
        }

        // Kastar InvalidPokemonException om värdet är negativt eller större än max HP.
        public void setCurrentHp(int currentHp) {
            if (currentHp < 0) {
                throw new InvalidPokemonException("Aktuellt HP får inte vara negativt.");
            }
            if (maxHp > 0 && currentHp > maxHp) {
                throw new InvalidPokemonException("Aktuellt HP får inte överstiga max HP.");
            }
            this.currentHp = currentHp;
        }

        // Returnerar en kopia av attack-listan, så att den inte kan ändras utifrån förbi valideringen.
        public List<Attack> getAttacks() {
            return List.copyOf(attacks);
        }

        /*
         Ersätter hela attack-listan. Kräver 1–4 attacker.
         Används bland annat när Jackson läser in JSON.
         */
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

        // Lägger till en attack. Max 4 stycken tillåtna.
        public void addAttack(Attack attack) {
            if (attack == null) {
                throw new InvalidPokemonException("Attack får inte vara null.");
            }
            if (attacks.size() >= MAX_ATTACKS) {
                throw new InvalidPokemonException("En Pokémon kan ha högst " + MAX_ATTACKS + " attacker.");
            }
            attacks.add(attack);
        }

        // Tar bort attacken på angivet index. Minst 1 attack måste finnas kvar.
        public void removeAttackAt(int index) {
            if (index < 0 || index >= attacks.size()) {
                throw new InvalidPokemonException("Ogiltigt attackindex.");
            }
            if (attacks.size() <= MIN_ATTACKS) {
                throw new InvalidPokemonException("En Pokémon måste ha minst " + MIN_ATTACKS + " attack.");
            }
            attacks.remove(index);
        }

        public int getAttackCount() {
            return attacks.size();
        }

        // En rad i listan, t.ex. i menyn "Visa alla".
        public String formatSummary() {
            return name + "  " + type + "  HP " + currentHp + "/" + maxHp
                    + "  attacker: " + attacks.size();
        }

        // All info när användaren väljer "Visa detaljer".
        public String formatDetails() {
            StringBuilder sb = new StringBuilder();
            sb.append("Namn: ").append(name).append('\n');
            sb.append("Typ:  ").append(type).append('\n');
            sb.append("HP:   ").append(currentHp).append('/').append(maxHp).append('\n');
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

        // Skriver ut Pokémonen på samma sätt som formatSummary().
        @Override
        public String toString() {
            return formatSummary();
        }
    }
}