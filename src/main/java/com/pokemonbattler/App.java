package com.pokemonbattler;

import com.pokemonbattler.exception.InvalidAttackException;
import com.pokemonbattler.exception.InvalidPokemonException;
import com.pokemonbattler.exception.PokemonNotFoundException;
import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.model.Type;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 Startpunkt och konsolmeny för Pokédexen. Klassen sköter bara in- och utmatning;
 själva datan och reglerna hanteras av PokedexService och modellklasserna.
 */
public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        // Vid start: läs in sparad fil om den finns, annars fyller man med startdata.
        PokedexService service = new PokedexService();
        if (service.savedFileExists()) {
            try {
                service.load();
                System.out.println("Sparad data inläst.");
            } catch (IOException e) {   // Trasig fil ska aldrig krascha programmet: använd startdata istället.
                System.out.println("Kunde inte läsa sparad data (" + e.getMessage() + "). Fyller med startdata.");
                service.seed();
            }
        } else {
            service.seed();
            System.out.println("Ingen sparad fil hittades. Fyller med startdata.");
        }


        System.out.println("=== Pokédex ===");
        // Loopen kör tills användaren väljer 0
        while (running) {
            try {
                printMenu();

                int choice = readIntInRange(scanner, "Välj: ", 0, 8);

                // 0–8. Bokstäver, tom rad och 999 ger felmeddelande, ingen krasch
                switch (choice) {
                    case 1 -> {  // 1: visa alla Pokémon som en numrerad lista.
                        List<Pokemon> all = service.getAll();
                        if (all.isEmpty()) {
                            System.out.println("Pokédexen är tom.");
                        } else {
                            for (int i = 0; i < all.size(); i++) {
                                System.out.println((i + 1) + ". " + all.get(i));
                            }
                        }
                    }
                    case 2 -> {  // 2: visa alla detaljer (inklusive attacker) för en vald Pokémon.
                        List<Pokemon> all = service.getAll();
                        if (all.isEmpty()) {
                            System.out.println("Pokédexen är tom.");
                        } else {
                            for (int i = 0; i < all.size(); i++) {
                                System.out.println((i + 1) + ". " + all.get(i));
                            }
                            int index = readIntInRange(scanner,
                                    "Vilken vill du se detaljer för? (0 för avbryt): ", 0, all.size());
                            if (index != 0) {
                                System.out.println(all.get(index - 1).formatDetails());
                            }
                        }
                    }
                    // 3-5: lägg till, redigera och ta bort (egna metoder nedan)
                    case 3 -> addPokemon(scanner, service);
                    case 4 -> editPokemon(scanner, service);
                    case 5 -> removePokemon(scanner, service);
                    case 6 -> {  // 6: spara manuellt till fil
                        try {
                            service.save();
                            System.out.println("Sparat till fil.");
                        } catch (IOException e) {
                            System.out.println("Kunde inte spara: " + e.getMessage());
                        }
                    }
                    case 7 -> {  // 7: ladda från fil (datan i minnet lämnas orörd om filen är trasig)
                        if (!service.savedFileExists()) {
                            System.out.println("Ingen sparad fil hittades.");
                        } else {
                            try {
                                service.load();
                                System.out.println("Data inläst från fil.");
                            } catch (IOException e) {
                                System.out.println("Kunde inte läsa filen: " + e.getMessage());
                            }
                        }
                    }
                    case 8 -> {  // 8: återställ till de 6 fördefinierade Pokémonen
                        service.seed();
                        System.out.println("Återställd till seedad data.");
                    }
                    case 0 -> {  // 0: avsluta, och spara automatiskt först
                        try {
                            service.save();
                        } catch (IOException e) {
                            System.out.println("Kunde inte spara vid avslut: " + e.getMessage());
                        }
                        System.out.println("Hejdå! :)");
                        running = false;
                    }
                }
            } catch (NoSuchElementException | IllegalStateException e) {
                // Indatan tog slut mitt i programmet: spara och avsluta snyggt istället för att krascha.
                System.out.println();
                System.out.println("Ingen mer indata tillgänglig. Avslutar och sparar automatiskt...");
                try {
                    service.save();
                } catch (IOException ex) {
                    System.out.println("Kunde inte spara: " + ex.getMessage());
                }
                running = false;
            } catch (RuntimeException e) {  //  Ett oväntat fel ska aldrig krascha programmet.
                System.out.println("Ett oväntat fel uppstod: " + e.getMessage() + ". Fortsätter.");
            }
        }
        scanner.close();
    }

    // Skriver ut huvudmeny
    private static void printMenu() {
        System.out.println();
        System.out.println("1. Visa alla Pokémon");
        System.out.println("2. Visa detaljer");
        System.out.println("3. Lägg till Pokémon");
        System.out.println("4. Redigera Pokémon");
        System.out.println("5. Ta bort Pokémon");
        System.out.println("6. Spara till fil");
        System.out.println("7. Ladda från fil");
        System.out.println("8. Återställ seed-data");
        System.out.println("0. Avsluta");
    }

    /*
     Läser ett heltal mellan min och max.
     Kraschar aldrig på bokstäver, tom rad, 999, -1 osv.
     */
    public static int readIntInRange(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.printf("Ange ett tal mellan %d och %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Det där var inget heltal. Försök igen.");
            }
        }
    }

    /*
      Guidad inmatning för en ny Pokémon (namn, typ, HP och en första attack).
      Ogiltig data (t.ex. tomt namn) fångas och visas som felmeddelande, och
      då läggs ingen Pokémon till.
     */
    private static void addPokemon(Scanner scanner, PokedexService service) {
        System.out.print("Namn: ");
        String name = scanner.nextLine().trim();

        Type type = readType(scanner);

        int maxHp = readIntInRange(scanner, "Max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                Pokemon.MIN_HP, Pokemon.MAX_HP);

        System.out.print("Attackens namn: ");
        String attackName = scanner.nextLine().trim();
        int baseDamage = readIntInRange(scanner,
                "Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
        int accuracy = readIntInRange(scanner,
                "Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);

        // Här valideras namnen. Pokemon och Attack kastar exceptions vid ogiltig data.
        try {
            Pokemon pokemon = new Pokemon(name, type, maxHp);
            pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, type));
            service.add(pokemon);
            System.out.println("Tillagd: " + pokemon);
        } catch (InvalidPokemonException | InvalidAttackException e) {
            System.out.println("Kunde inte lägga till Pokémon: " + e.getMessage());
        }
    }

    // Frågar efter en typ (FIRE, WATER, ...) tills användaren skriver en giltig.
    private static Type readType(Scanner scanner) {
        while (true) {
            System.out.print("Typ (" + Type.options() + "): ");
            Type type = Type.fromInput(scanner.nextLine());
            if (type != null) {
                return type;
            }
            System.out.println("Ogiltig typ. Försök igen.");
        }
    }

    /*
     Visar listan och tar bort vald Pokémon.
     Numret är begränsat av readIntInRange, 0 avbryter.
     */
    private static void removePokemon(Scanner scanner, PokedexService service) {
        List<Pokemon> all = service.getAll();
        if (all.isEmpty()) {
            System.out.println("Pokédexen är tom, inget att ta bort.");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            System.out.println((i + 1) + ". " + all.get(i));
        }
        int index = App.readIntInRange(scanner, "Vilken vill du ta bort? (0 för avbryt): ", 0, all.size());
        if (index == 0) {
            System.out.println("Avbrutet.");
            return;
        }
        try {
            Pokemon removed = service.get(index - 1);
            service.remove(index - 1);
            System.out.println("Borttagen: " + removed);
        } catch (PokemonNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     Låter användaren välja en Pokémon och redigera den i en undermeny:
     ändra namn, typ, max HP, aktuellt HP, samt lägga till och ta bort attacker.
     */
    private static void editPokemon(Scanner scanner, PokedexService service) {
        List<Pokemon> all = service.getAll();
        if (all.isEmpty()) {
            System.out.println("Pokédexen är tom, inget att redigera.");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            System.out.println((i + 1) + ". " + all.get(i));
        }
        int index = App.readIntInRange(scanner, "Vilken vill du redigera? (0 för avbryt): ", 0, all.size());
        if (index == 0) {
            System.out.println("Avbrutet.");
            return;
        }

        // Hämtar det riktiga objektet i listan, så ändringarna nedan slår igenom direkt.
        Pokemon pokemon;
        try {
            pokemon = service.get(index - 1);
        } catch (PokemonNotFoundException e) {
            System.out.println(e.getMessage());
            return;
        }

        boolean editing = true;
        while (editing) {
            System.out.println();
            System.out.println("Redigerar: " + pokemon);
            System.out.println("1. Ändra namn");
            System.out.println("2. Ändra typ");
            System.out.println("3. Ändra max HP");
            System.out.println("4. Ändra aktuellt HP");
            System.out.println("5. Lägg till attack");
            System.out.println("6. Ta bort attack");
            System.out.println("0. Klar");

            int choice = App.readIntInRange(scanner, "Val: ", 0, 6);
            switch (choice) {
                case 1 -> {  // 1: nytt namn (kan vara ogiltigt, så det fångas)
                    System.out.print("Nytt namn: ");
                    String newName = scanner.nextLine().trim();
                    try {
                        pokemon.setName(newName);
                        System.out.println("Namn uppdaterat.");
                    } catch (InvalidPokemonException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 2 -> {  // 2: ny typ (readType ger alltid en giltig typ)
                    pokemon.setType(App.readType(scanner));
                    System.out.println("Typ uppdaterad.");
                }
                case 3 -> {  // 3: nytt max HP (readIntInRange begränsar redan till tillåtet intervall)
                    int newMaxHp = App.readIntInRange(scanner,
                            "Nytt max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                            Pokemon.MIN_HP, Pokemon.MAX_HP);
                    pokemon.setMaxHp(newMaxHp);
                    System.out.println("Max HP uppdaterat.");
                }
                case 4 -> {  // 4: nytt aktuellt HP (0 till max HP)
                    int newCurrentHp = App.readIntInRange(scanner,
                            "Nytt aktuellt HP (0-" + pokemon.getMaxHp() + "): ",
                            0, pokemon.getMaxHp());
                    pokemon.setCurrentHp(newCurrentHp);
                    System.out.println("Aktuellt HP uppdaterat.");
                }
                case 5 -> {  // 5: lägg till en attack (misslyckas om Pokémonen redan har 4 eller namnet är ogiltigt)
                    System.out.print("Attackens namn: ");
                    String attackName = scanner.nextLine().trim();
                    int baseDamage = App.readIntInRange(scanner,
                            "Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                            Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
                    int accuracy = App.readIntInRange(scanner,
                            "Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                            Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);
                    try {
                        pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, pokemon.getType()));
                        System.out.println("Attack tillagd.");
                    } catch (InvalidPokemonException | InvalidAttackException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 6 -> {  // 6: ta bort en attack (minst 1 måste finnas kvar, annars fångas felet)
                    List<Attack> attacks = pokemon.getAttacks();
                    if (attacks.isEmpty()) {
                        System.out.println("Inga attacker att ta bort.");
                        break;
                    }
                    for (int i = 0; i < attacks.size(); i++) {
                        System.out.println((i + 1) + ". " + attacks.get(i));
                    }
                    int attackIndex = App.readIntInRange(scanner, "Vilken attack? (0 för avbryt): ", 0, attacks.size());
                    if (attackIndex == 0) {
                        break;
                    }
                    try {
                        pokemon.removeAttackAt(attackIndex - 1);
                        System.out.println("Attack borttagen.");
                    } catch (InvalidPokemonException e) {
                        System.out.println(e.getMessage());
                    }
                }

                // 0: klar, tillbaka till huvudmenyn
                case 0 -> editing = false;
            }
        }
    }
}

