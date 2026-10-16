package com.pokemonbattler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pokemonbattler.battle.Battle;
import com.pokemonbattler.battle.BattleOutcome;
import com.pokemonbattler.battle.BattleResult;
import com.pokemonbattler.battle.CpuCombatant;
import com.pokemonbattler.battle.DamageCalculator;
import com.pokemonbattler.battle.HumanCombatant;
import com.pokemonbattler.battle.TypeChart;
import com.pokemonbattler.battle.WildPokemonPool;
import com.pokemonbattler.exception.InputAbortedException;
import com.pokemonbattler.exception.InvalidAttackException;
import com.pokemonbattler.exception.InvalidPokemonException;
import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;
import com.pokemonbattler.model.Type;
import com.pokemonbattler.persistence.JsonRepository;
import com.pokemonbattler.persistence.Repository;
import com.pokemonbattler.statistics.Stats;
import com.pokemonbattler.ui.Box;
import com.pokemonbattler.ui.ConsoleInput;
import com.pokemonbattler.ui.Menu;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /*
     Tar emot alla beroenden utifrån (dependency injection),
     så att App inte skapar dem själv.
     */
    public App(ConsoleInput input, PrintStream out,
               Repository<List<Pokemon>> pokemonRepo, Repository<Stats> statsRepo) {
        this.input = input;
        this.out = out;
        this.pokemonRepo = pokemonRepo;
        this.statsRepo = statsRepo;
    }

    // Startpunkt: kopplar ihop repositories för Pokémon och statistik och startar appen.
    public static void main(String[] args) {
        PrintStream out = System.out;
        Repository<List<Pokemon>> pokemonRepo = new JsonRepository<>(
                DATA_DIR.resolve("pokemons.json"), new TypeReference<>() {},
                () -> PokemonCollection.starter().toList());
        Repository<Stats> statsRepo = new JsonRepository<>(
                DATA_DIR.resolve("stats.json"), new TypeReference<>() {}, Stats::new);
        new App(new ConsoleInput(System.in, out), out, pokemonRepo, statsRepo).run();
    }

    // Huvudloopen: visar menyn tills användaren väljer 0. Fångar fel så att programmet aldrig kraschar.
    public void run() {
        loadAtStartup();
        Menu menu = new Menu()
                .section("Pokémon")
                .add("Visa alla Pokémon", this::listAll)
                .add("Visa detaljer", this::showDetails)
                .add("Lägg till Pokémon", this::addPokemon)
                .add("Redigera Pokémon", this::editPokemon)
                .add("Ta bort Pokémon", this::removePokemon)
                .section("Strid")
                .add("Starta strid", this::startBattle)
                .add("Stridsresultat", this::showStatistics)
                .add("Pokémon Center (läk alla)", this::healAll)
                .section("Fil")
                .add("Spara till fil", this::saveToFile)
                .add("Ladda från fil", this::loadFromFile)
                .add("Återställ seed-data", this::resetSeed);


        try {
            boolean running = true;
            while (running) {
                String status = "Pokémon: " + collection.size()
                        + "   Stridsredo: " + collection.getBattleReady().size();
                menu.print(out, "Avsluta", "POKÉMON BATTLER", status);
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

    // Läser in sparad data vid start, eller startdata om filen saknas eller är trasig.
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

    // Menyval: sparar allt och bekräftar för användaren.
    private void saveToFile() {
        if (saveAll()) {
            out.println("Sparat till fil.");
        }
    }

    /*
     Sparar både samlingen och statistiken. Returnerar false om något misslyckades.
     */
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

    // Menyval: ersätter samlingen med de sex startpokémonen.
    private void resetSeed() {
        collection = PokemonCollection.starter();
        out.println("Återställd till seedad data.");
    }

    // ---------- Pokémon ----------

    // Menyval: visar alla Pokémon i en ram.
    private void listAll() {
        if (collection.isEmpty()) {
            out.println("Du har inga Pokémon.");
            return;
        }
        printNumbered("Dina Pokémons", collection.getAll());
    }

    // Menyval: låter användaren välja en Pokémon och visar all info om den.
    private void showDetails() {
        selectPokemon(collection.getAll(), "Vilken vill du se detaljer för? (0 för avbryt): ")
                .ifPresent(p -> Box.print(out, p.getName(), p.formatDetails()));
    }

    // Menyval: läser in namn, typ, HP, speed och en första attack och skapar en ny Pokémon.
    private void addPokemon() {
        Box.open(out, "Lägg till Pokémon");
        String name = input.readLine("| Namn: ");
        Type type = input.readType("| Typ");
        int maxHp = input.readInt("| Max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                Pokemon.MIN_HP, Pokemon.MAX_HP);
        int speed = input.readInt("| Speed (" + Pokemon.MIN_SPEED + "-" + Pokemon.MAX_SPEED + "): ",
                Pokemon.MIN_SPEED, Pokemon.MAX_SPEED);
        Box.divider(out, "Första attacken");
        String attackName = input.readLine("| Namn: ");
        int baseDamage = input.readInt("| Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
        int accuracy = input.readInt("| Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);
        Box.close(out);

        // Pokemon och Attack kastar exceptions vid ogiltig data, t.ex. tomt namn.
        try {
            Pokemon pokemon = new Pokemon(name, type, maxHp, speed);
            pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, type));
            collection.add(pokemon);
            Box.print(out, pokemon.getName() + " har lagts till!", pokemon.formatDetails());
        } catch (InvalidPokemonException | InvalidAttackException e) {
            Box.print(out, "Kunde inte lägga till Pokémon", List.of(e.getMessage()));
        }
    }

    // Menyval: tar bort en vald Pokémon ur samlingen.
    private void removePokemon() {
        Optional<Pokemon> selected = selectPokemon(collection.getAll(),
                "Vilken vill du ta bort? (0 för avbryt): ");
        if (selected.isEmpty()) {
            out.println("Avbrutet.");
            return;
        }
        Pokemon pokemon = selected.get();
        Box.print(out, "Ta bort " + pokemon.getName() + "?", pokemon.formatDetails());
        int answer = input.readInt("Är du säker? (1 = ja, 0 = nej): ", 0, 1);
        if (answer == 0) {
            out.println("Avbrutet.");
            return;
        }
        collection.remove(pokemon);
        confirm(pokemon.getName() + " har tagits bort.");
    }

    // Menyval: undermeny för att ändra en Pokémons värden och attacker.
    private void editPokemon() {
        Optional<Pokemon> selected = selectPokemon(collection.getAll(),
                "Vilken vill du redigera? (0 för avbryt): ");
        if (selected.isEmpty()) {
            out.println("Avbrutet.");
            return;
        }
        Pokemon pokemon = selected.get();

        // Varje ändring sparar det gamla värdet först, så att bekräftelsen kan visa före -> efter.
        Menu editMenu = new Menu()
                .add("Ändra namn", () -> {
                    String before = pokemon.getName();
                    collection.rename(pokemon, input.readLine("Nytt namn: "));
                    confirm("Namn: " + before + " -> " + pokemon.getName());
                })
                .add("Ändra typ", () -> {
                    Type before = pokemon.getType();
                    pokemon.setType(input.readType("Typ"));
                    confirm("Typ: " + before + " -> " + pokemon.getType());
                })
                .add("Ändra max HP", () -> {
                    int before = pokemon.getMaxHp();
                    pokemon.setMaxHp(input.readInt("Nytt max HP (" + Pokemon.MIN_HP + "-" + Pokemon.MAX_HP + "): ",
                            Pokemon.MIN_HP, Pokemon.MAX_HP));
                    confirm("Max HP: " + before + " -> " + pokemon.getMaxHp());
                })
                .add("Ändra aktuellt HP", () -> {
                    int before = pokemon.getCurrentHp();
                    pokemon.setCurrentHp(input.readInt("Nytt aktuellt HP (0-" + pokemon.getMaxHp() + "): ",
                            0, pokemon.getMaxHp()));
                    confirm("Aktuellt HP: " + before + " -> " + pokemon.getCurrentHp());
                })
                .add("Ändra speed", () -> {
                    int before = pokemon.getSpeed();
                    pokemon.setSpeed(input.readInt("Ny speed (" + Pokemon.MIN_SPEED + "-" + Pokemon.MAX_SPEED + "): ",
                            Pokemon.MIN_SPEED, Pokemon.MAX_SPEED));
                    confirm("Speed: " + before + " -> " + pokemon.getSpeed());
                })
                .add("Lägg till attack", () -> addAttack(pokemon))
                .add("Ta bort attack", () -> removeAttack(pokemon));

        boolean editing = true;
        while (editing) {
            editMenu.print(out, "Klar", "REDIGERAR " + pokemon.getName().toUpperCase(),
                    pokemon.getType() + "   HP " + pokemon.getCurrentHp() + "/" + pokemon.getMaxHp()
                            + "   SPD " + pokemon.getSpeed());
            try {
                editing = editMenu.chooseAndRun(input, "Val: ");
            } catch (InvalidPokemonException | InvalidAttackException e) {
                Box.print(out, "Kunde inte ändra", List.of(e.getMessage()));
            }
        }
        Box.print(out, pokemon.getName() + " är sparad", pokemon.formatDetails());
    }

    // Läser in en ny attack. Pokemon kastar exception om den redan har 4 attacker.
    private void addAttack(Pokemon pokemon) {
        Box.open(out, "Ny attack för " + pokemon.getName());
        String attackName = input.readLine("| Namn: ");
        int baseDamage = input.readInt("| Basskada (" + Attack.MIN_DAMAGE + "-" + Attack.MAX_DAMAGE + "): ",
                Attack.MIN_DAMAGE, Attack.MAX_DAMAGE);
        int accuracy = input.readInt("| Träffsäkerhet (" + Attack.MIN_ACCURACY + "-" + Attack.MAX_ACCURACY + "): ",
                Attack.MIN_ACCURACY, Attack.MAX_ACCURACY);
        Type type = input.readType("| Attacktyp");
        Box.close(out);
        pokemon.addAttack(new Attack(attackName, baseDamage, accuracy, type));
        confirm(attackName + " har lagts till.");
    }

    /*
    Visar attackerna och tar bort den användaren väljer.
    Minst en attack måste finnas kvar.
     */
    private void removeAttack(Pokemon pokemon) {
        List<Attack> attacks = pokemon.getAttacks();
        if (attacks.isEmpty()) {
            out.println("Inga attacker att ta bort.");
            return;
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < attacks.size(); i++) {
            lines.add((i + 1) + ". " + attacks.get(i));
        }
        Box.print(out, "Attacker", lines);

        int index = input.readInt("Vilken attack? (0 för avbryt): ", 0, attacks.size());
        if (index != 0) {
            pokemon.removeAttackAt(index - 1);
            out.println("Attack borttagen.");
        }
    }

    // Menyval (Pokémon Center): ger alla Pokémon fullt HP.
    private void healAll() {
        collection.healAll();
        out.println("Alla dina Pokémon har fullt HP igen.");
    }

    // ---------- Strid ----------

    /*
 Menyval: stoppar strid utan stridsredo Pokémon,
 låter spelaren välja och kör en strid mot en vild Pokémon.
 Efter en vinst kan spelaren fortsätta mot en ny vild Pokémon eller återgå till basen.
 */
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
        Pokemon pokemon = chosen.get();

        // En strid per varv. Loopen fortsätter bara om spelaren vann och vill strida igen.
        boolean fighting = true;
        while (fighting) {
            Pokemon wild = wildPool.randomOpponent();
            out.println("En vild " + wild.getName() + " (" + wild.getType() + ") dyker upp!");

            Battle battle = new Battle(
                    new HumanCombatant(pokemon, input, out),
                    new CpuCombatant(wild, random),
                    calculator, out);
            boolean won;
            try {
                BattleResult result = battle.run();
                stats.record(result);
                won = result.outcome() == BattleOutcome.WIN;
            } finally {
                // Sparar efter varje strid, även om den avbryts (t.ex. Ctrl+D).
                saveAll();
            }
            fighting = won && askContinue(pokemon);
        }
    }

    // Frågar efter en vunnen strid om spelaren vill strida igen. Skadan finns kvar mellan striderna.
    private boolean askContinue(Pokemon pokemon) {
        Box.print(out, "Vad vill du göra?", List.of(
                pokemon.getName() + " har " + pokemon.getCurrentHp() + "/" + pokemon.getMaxHp() + " HP kvar.",
                "",
                "1. Fortsätt strida mot en ny vild Pokémon",
                "0. Återgå till basen"));
        return input.readInt("Välj: ", 0, 1) == 1;
    }

    // Menyval: visar sparad stridsstatistik i en ram.
    private void showStatistics() {
        Box.print(out, "Stridsresultat", stats.format());
    }

    // ---------- Hjälpmetoder ----------

    // Visar ett kort bekräftelsemeddelande i en ram.
    private void confirm(String message) {
        Box.print(out, "Klart", List.of(message));
    }

    // Skriver ut en numrerad lista med Pokémon i en ram.
    private void printNumbered(String title, List<Pokemon> list) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            lines.add(String.format("%2d. %s", i + 1, list.get(i)));
        }
        Box.print(out, title, lines);
    }
    // Visar en numrerad lista och låter användaren välja. 0 avbryter.
    private Optional<Pokemon> selectPokemon(List<Pokemon> list, String prompt) {
        if (list.isEmpty()) {
            out.println("Du har inga Pokémon.");
            return Optional.empty();
        }
        printNumbered("Välj Pokémon", list);
        int choice = input.readInt(prompt, 0, list.size());
        return choice == 0 ? Optional.empty() : Optional.of(list.get(choice - 1));
    }
}