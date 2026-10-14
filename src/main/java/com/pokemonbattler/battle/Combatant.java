package com.pokemonbattler.battle;

import com.pokemonbattler.model.Attack;
import com.pokemonbattler.model.Pokemon;

import java.util.Optional;

/*
 En deltagare i en strid: den som styr en Pokémon.

 Detta är programmets centrala polymorfism. Battle vet inte om det är
 en människa eller datorn som väljer attack; den anropar bara
 chooseAttack(). HumanCombatant frågar via konsolen, CpuCombatant slumpar.
 */
public abstract class Combatant {
    private final String displayName;
    private final Pokemon pokemon;

    // protected: Bara subklasserna kan anropa konstruktorn.
    protected Combatant(String displayName, Pokemon pokemon) {
        this.displayName = displayName;
        this.pokemon = pokemon;
    }

    // Namnet som visas i loggen, t.ex. "Din Pikachu" eller "Vilda Vulpix".
    public String getDisplayName() {
        return displayName;
    }

    // Pokémon som den här deltagaren styr.
    public Pokemon getPokemon() {
        return pokemon;
    }

    // Väljer nästa attack. Optional.empty() betyder att deltagaren flyr.
    public abstract Optional<Attack> chooseAttack(Pokemon opponent);

    // Om true räknas deltagaren som spelaren i resultat och statistik.
    public abstract boolean isPlayer();
}