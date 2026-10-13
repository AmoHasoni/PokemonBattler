package com.pokemonbattler.ui;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 En generisk textmeny. Varje val är en text plus en Runnable, så menyn
 behöver inte veta vad valen gör (istället för en stor switch-sats).
 */
public class Menu {
    private record Option(String label, Runnable action) {
    }
    // ramens inner bredd i tecken
    private static final int WIDTH = 34;

    private final List<Option> options = new ArrayList<>();

    // Rubriker som skrivs ut före ett visst val (nyckel = valets index).
    private final Map<Integer, String> sections = new HashMap<>();

    // Nästa val som läggs till hamnar under den här rubriken.
    public Menu section(String title) {
        sections.put(options.size(), title);
        return this;
    }

    public Menu add(String label, Runnable action) {
        options.add(new Option(label, action));
        return this;
    }

    // Skriver ut menyn i en ram. Rader i header (t.ex. titel och status) centreras överst.
    public void print(PrintStream out, String exitLabel, String... header) {
        String thick = "+" + "=".repeat(WIDTH) + "+";
        String thin = "+" + "-".repeat(WIDTH) + "+";
        out.println();
        out.println(thick);
        if (header.length > 0) {
            for (String line : header) {
                row(out, center(line));
            }
            out.println(thick);
        }
        for (int i = 0; i < options.size(); i++) {
            if (sections.containsKey(i)) {
                if (i > 0) {
                    row(out, "");
                }
                row(out, " " + sections.get(i).toUpperCase());
            }
            row(out, String.format("  %2d. %s", i + 1, options.get(i).label()));
        }
        out.println(thin);
        row(out, String.format("  %2d. %s", 0, exitLabel));
        out.println(thick);
    }

    // En rad inuti ramen, utfylld med mellanslag så att högerkanten blir rak.
    private static void row(PrintStream out, String text) {
        out.println("|" + text + " ".repeat(Math.max(0, WIDTH - text.length())) + "|");
    }

    private static String center(String text) {
        int left = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(left) + text;
    }

    // Läser ett val och kör det. Returnerar false om användaren valde 0.
    public boolean chooseAndRun(ConsoleInput input, String prompt) {
        int choice = input.readInt(prompt, 0, options.size());
        if (choice == 0) {
            return false;
        }
        options.get(choice - 1).action().run();
        return true;
    }
}
