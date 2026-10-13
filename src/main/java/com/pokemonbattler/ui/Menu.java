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

    public void print(PrintStream out, String exitLabel) {
        out.println();
        for (int i = 0; i < options.size(); i++) {
            if (sections.containsKey(i)) {
                out.println(" -- " + sections.get(i) + " --");
            }
            out.printf("  %2d. %s%n", i + 1, options.get(i).label());
        }
        out.println();
        out.printf("  %2d. %s%n", 0, exitLabel);
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
