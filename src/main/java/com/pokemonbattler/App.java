package com.pokemonbattler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pokemonbattler.battle.Battle;
import com.pokemonbattler.battle.BattleResult;
import com.pokemonbattler.battle.CpuCombatant;
import com.pokemonbattler.battle.DamageCalculator;
import com.pokemonbattler.battle.HumanCombatant;
import com.pokemonbattler.battle.TypeChart;
import com.pokemonbattler.battle.WildPokemonPool;
import com.pokemonbattler.exception.InputAbortedException;
import com.pokemonbattler.exception.InvalidAttackException;
import com.pokemonbattler.exception.InvalidPokemonException;
import com.pokemonbattler.persistence.JsonRepository;
import com.pokemonbattler.persistence.Repository;
import com.pokemonbattler.statistics.Stats;
import com.pokemonbattler.ui.ConsoleInput;
import com.pokemonbattler.ui.Menu;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/*
 Startpunkt och konsolmeny för Pokemon Battler. Klassen sköter bara in- och
 utmatning; reglerna finns i modellklasserna, striden i Battle och lagringen
 bakom Repository.
 */
public class App {
    private static final Path DATA_DIR = Path.of("data");

    private final ConsoleInput input;
    private final PrintStream out;
    private final Repository<List<Pokemon>> pokemonRepo;
    private final Repository<Stats> statsRepo;
    private final Random random = new Random();
    private final DamageCalculator calculator = new DamageCalculator(new TypeChart(), random);
    private final WildPokemonPool wildPool = new WildPokemonPool(random);

    private PokemonCollection collection;
    private Stats stats;

    public App(ConsoleInput input, PrintStream out,
               Repository<List<Pokemon>> pokemonRepo, Repository<Stats> statsRepo) {
        this.input = input;
        this.out = out;
        this.pokemonRepo = pokemonRepo;
        this.statsRepo = statsRepo;
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        Repository<List<Pokemon>> pokemonRepo = new JsonRepository<>(
                DATA_DIR.resolve("pokemons.json"), new TypeReference<>() {},
                () -> PokemonCollection.starter().toList());
        Repository<Stats> statsRepo = new JsonRepository<>(
                DATA_DIR.resolve("stats.json"), new TypeReference<>() {}, Stats::new);
        new App(new ConsoleInput(System.in, out), out, pokemonRepo, statsRepo).run();
    }

    public void run() {
        loadAtStartup();

        Menu menu = new Menu()
                .add("Visa alla Pokémon", this::listAll)
                .add("Visa detaljer", this::showDetails)
                .add("Lägg till Pokémon", this::addPokemon)
                .add("Redigera Pokémon", this::editPokemon)
                .add("Ta bort Pokémon", this::removePokemon)
                .add("Spara till fil", this::saveToFile)
                .add("Ladda från fil", this::loadFromFile)
                .add("Återställ seed-data", this::resetSeed)
                .add("Starta strid", this::startBattle)
                .add("Stridsresultat", this::showStatistics)
                .add("Pokémon Center (läk alla)", this::healAll);

        out.println("=== Pokémon Battler ===");
        try {
            boolean running = true;
            while (running) {
                menu.print(out, "Avsluta");
                try {
                    running = menu.chooseAndRun(input, "Välj: ");
                } catch (InvalidPokemonException | InvalidAttackException e) {
                    out.println(e.getMessage());
                } catch (InputAbortedException e) {
                    throw e;
                } catch (RuntimeException e) {
                    // Ett oväntat fel ska aldrig krascha programmet.
                    out.println("Ett oväntat fel uppstod: " + e.getMessage() + ". Fortsätter.");
                }
            }
            saveAll();
            out.println("Hejdå! :)");
        } catch (InputAbortedException e) {
            out.println();
            out.println("Ingen mer indata tillgänglig. Avslutar och sparar automatiskt...");
            saveAll();
        }
    }

    // ---------- Fil ----------

    private void loadAtStartup() {
        if (!pokemonRepo.exists()) {
            collection = PokemonCollection.starter();
            out.println("Ingen sparad fil hittades. Fyller med startdata.");
        } else {
            collection = new PokemonCollection(pokemonRepo.load());
            Optional<String> warning = pokemonRepo.lastWarning();
            if (warning.isPresent()) {
                out.println("Kunde inte läsa sparad data (" + warning.get() + "). Fyller med startdata.");
            } else {
                out.println("Sparad data inläst.");
            }
        }
        stats = statsRepo.load();
        statsRepo.lastWarning().ifPresent(w ->
                out.println("Kunde inte läsa statistiken (" + w + "). Börjar om från noll."));
    }

    private void saveToFile() {
        if (saveAll()) {
            out.println("Sparat till fil.");
        }
    }

    // Sparar både samlingen och statistiken. Returnerar false om något misslyckades.
    private boolean saveAll() {
        boolean ok = true;
        if (!pokemonRepo.save(collection.toList())) {
            out.println("Kunde inte spara: " + pokemonRepo.lastWarning().orElse("okänt fel"));
            ok = false;
        }
        if (!statsRepo.save(stats)) {
            out.println("Kunde inte spara statistik: " + statsRepo.lastWarning().orElse("okänt fel"));
            ok = false;
        }
        return ok;
    }

    // Datan i minnet lämnas orörd om filen saknas eller är trasig.
    private void loadFromFile() {
        if (!pokemonRepo.exists()) {
            out.println("Ingen sparad fil hittades.");
            return;
        }
        List<Pokemon> loaded = pokemonRepo.load();
        Optional<String> warning = pokemonRepo.lastWarning();
        if (warning.isPresent()) {
            out.println("Kunde inte läsa filen: " + warning.get());
            return;
        }
        collection = new PokemonCollection(loaded);
        out.println("Data inläst från fil.");
    }

    private void resetSeed() {
        collection = PokemonCollection.starter();
        out.println("Återställd till seedad data.");
    }

    // ---------- Pokémon ----------

    private void listAll() {
        if (collection.isEmpty()) {
            out.println("Du har inga Pokémon.");
            return;
        }
        printNumbered(collection.getAll());
    }

    private void showDetails() {
        selectPokemon(collection.getAll(), "Vilken vill du se detaljer för? (0 för avbryt): ")
                .ifPresent(p -> out.println(p.formatDetails()));
    }

    private void addPokemon() {
        String name = input.readLine("Namn: ");
        Type type = input.readType("Typ");
        int maxHp = input.readInt("Max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                Pokemon.MIN_HP, Pokemon.MAX_HP);
        int speed = input.readInt("Speed (" + Pokemon.MIN_SPEED + "-" + Pokemon.MAX_SPEED + "): ",
                Pokemon.MIN_SPEED, Pokemon.MAX_SPEED);
        String attackName = input.readLine("Attackens namn: ");
        int baseDamage = input.readInt("Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
        int accuracy = input.readInt("Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);

        // Pokemon och Attack kastar exceptions vid ogiltig data, t.ex. tomt namn.
        try {
            Pokemon pokemon = new Pokemon(name, type, maxHp, speed);
            pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, type));
            collection.add(pokemon);
            out.println("Tillagd: " + pokemon);
        } catch (InvalidPokemonException | InvalidAttackException e) {
            out.println("Kunde inte lägga till Pokémon: " + e.getMessage());
        }
    }

    private void removePokemon() {
        Optional<Pokemon> selected = selectPokemon(collection.getAll(),
                "Vilken vill du ta bort? (0 för avbryt): ");
        if (selected.isEmpty()) {
            out.println("Avbrutet.");
            return;
        }
        collection.remove(selected.get());
        out.println("Borttagen: " + selected.get());
    }

    private void editPokemon() {
        Optional<Pokemon> selected = selectPokemon(collection.getAll(),
                "Vilken vill du redigera? (0 för avbryt): ");
        if (selected.isEmpty()) {
            out.println("Avbrutet.");
            return;
        }
        Pokemon pokemon = selected.get();

        Menu editMenu = new Menu()
                .add("Ändra namn", () -> {
                    collection.rename(pokemon, input.readLine("Nytt namn: "));
                    out.println("Namn uppdaterat.");
                })
                .add("Ändra typ", () -> {
                    pokemon.setType(input.readType("Typ"));
                    out.println("Typ uppdaterad.");
                })
                .add("Ändra max HP", () -> {
                    pokemon.setMaxHp(input.readInt("Nytt max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                            Pokemon.MIN_HP, Pokemon.MAX_HP));
                    out.println("Max HP uppdaterat.");
                })
                .add("Ändra aktuellt HP", () -> {
                    pokemon.setCurrentHp(input.readInt("Nytt aktuellt HP (0-" + pokemon.getMaxHp() + "): ",
                            0, pokemon.getMaxHp()));
                    out.println("Aktuellt HP uppdaterat.");
                })
                .add("Ändra speed", () -> {
                    pokemon.setSpeed(input.readInt("Ny speed (" + Pokemon.MIN_SPEED + "-" + Pokemon.MAX_SPEED + "): ",
                            Pokemon.MIN_SPEED, Pokemon.MAX_SPEED));
                    out.println("Speed uppdaterad.");
                })
                .add("Lägg till attack", () -> addAttack(pokemon))
                .add("Ta bort attack", () -> removeAttack(pokemon));

        boolean editing = true;
        while (editing) {
            out.println();
            out.println("Redigerar: " + pokemon);
            editMenu.print(out, "Klar");
            try {
                editing = editMenu.chooseAndRun(input, "Val: ");
            } catch (InvalidPokemonException | InvalidAttackException e) {
                out.println(e.getMessage());
            }
        }
    }

    private void addAttack(Pokemon pokemon) {
        String attackName = input.readLine("Attackens namn: ");
        int baseDamage = input.readInt("Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
        int accuracy = input.readInt("Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);
        Type type = input.readType("Attacktyp");
        pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, type));
        out.println("Attack tillagd.");
    }

    private void removeAttack(Pokemon pokemon) {
        List<Attack> attacks = pokemon.getAttacks();
        if (attacks.isEmpty()) {
            out.println("Inga attacker att ta bort.");
            return;
        }
        for (int i = 0; i < attacks.size(); i++) {
            out.println((i + 1) + ". " + attacks.get(i));
        }
        int index = input.readInt("Vilken attack? (0 för avbryt): ", 0, attacks.size());
        if (index != 0) {
            pokemon.removeAttackAt(index - 1);
            out.println("Attack borttagen.");
        }
    }

    private void healAll() {
        collection.healAll();
        out.println("Alla dina Pokémon har fullt HP igen.");
    }

    // ---------- Strid ----------

    private void startBattle() {
        if (collection.isEmpty()) {
            out.println("Du äger inga Pokémon. Lägg till en först.");
            return;
        }
        List<Pokemon> ready = collection.getBattleReady();
        if (ready.isEmpty()) {
            out.println("Ingen av dina Pokémon kan slåss (0 HP eller inga attacker).");
            out.println("Besök Pokémon Center eller lägg till en attack.");
            return;
        }
        if (ready.size() < collection.size()) {
            out.println("(Pokémon med 0 HP eller utan attacker kan inte väljas.)");
        }
        Optional<Pokemon> chosen = selectPokemon(ready, "Välj din Pokémon (0 för avbryt): ");
        if (chosen.isEmpty()) {
            out.println("Avbrutet.");
            return;
        }
        Pokemon wild = wildPool.randomOpponent();
        out.println("En vild " + wild.getName() + " (" + wild.getType() + ") dyker upp!");

        Battle battle = new Battle(
                new HumanCombatant(chosen.get(), input, out),
                new CpuCombatant(wild, random),
                calculator, out);
        try {
            BattleResult result = battle.run();
            stats.record(result);
        } finally {
            // Sparar efter varje strid, även om den avbryts (t.ex. Ctrl+D).
            saveAll();
        }
    }

    private void showStatistics() {
        out.println(stats.format().stripTrailing());
    }

    // ---------- Hjälpmetoder ----------

    private void printNumbered(List<Pokemon> list) {
        for (int i = 0; i < list.size(); i++) {
            out.println((i + 1) + ". " + list.get(i));
        }
    }

    // Visar en numrerad lista och låter användaren välja. 0 avbryter.
    private Optional<Pokemon> selectPokemon(List<Pokemon> list, String prompt) {
        if (list.isEmpty()) {
            out.println("Du har inga Pokémon.");
            return Optional.empty();
        }
        printNumbered(list);
        int choice = input.readInt(prompt, 0, list.size());
        return choice == 0 ? Optional.empty() : Optional.of(list.get(choice - 1));
    }
}