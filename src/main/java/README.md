# Pokemon Battler

En konsolapplikation i Java där du samlar Pokémon och låter dem slåss
turbaserat mot datorstyrda, vilda Pokémon. Projektet bygger vidare på
Pokédexen från inlämning 1: all funktionalitet därifrån finns kvar, men koden
är omstrukturerad mot bättre OOP och utökad med ett stridssystem, JSON-lagring
och statistik.

## Krav
- Java 17 eller nyare
- Maven
- IntelliJ IDEA


## Köra programmet


```bash
mvn compile exec:java      # starta programmet
mvn test                   # kör enhetstesterna
```

I IntelliJ: öppna projektet och kör `main` i `com.pokemonbattler.App`.

Data sparas automatiskt i mappen `data/` (skapas vid första körningen):

- `data/pokemons.json` – din samling, inklusive aktuellt HP
- `data/stats.json` – stridsstatistik

Finns ingen `pokemons.json` startar du med fem Pokémon
(Charmander, Squirtle, Bulbasaur, Pikachu och Eevee).

## Meny

1. Visa alla Pokémon
2. Visa detaljer
3. Lägg till Pokémon
4. Redigera Pokémon (namn, typ, max HP, speed)
5. Hantera attacker (lägg till/ta bort, 1–4 st)
6. Ta bort Pokémon
7. Starta strid
8. Stridsresultat / statistik
9. Pokémon Center (läker alla till fullt HP)
0. Spara och avsluta

## Stridsregler

- Spelaren väljer en av sina Pokémon. Bara Pokémon med HP > 0 och minst en attack kan väljas.
- Datorn slumpar en motståndare ur en pool av vilda Pokémon. Motståndaren har alltid fullt HP.
- **Turordning:** den Pokémon som har högst `speed` attackerar först varje runda.
  Vid lika speed börjar spelaren.
- Spelaren väljer attack i en meny, eller `0` för att fly. Datorn väljer slumpvis bland sina attacker.
- **Träff:** ett slumptal 1–100 måste vara ≤ attackens träffsäkerhet, annars missar attacken.
- Striden slutar när någon Pokémon har 0 HP, eller när spelaren flyr.
- Skadan på din Pokémon sparas efter striden. Läk den i Pokémon Center.

### Skadeformel

```
skada = avrunda( basskada × typeffektivitet × slumpfaktor × kritisk )
```

- `typeffektivitet` – 2.0, 1.0 eller 0.5 enligt tabellen nedan
- `slumpfaktor` – slumpat decimaltal mellan 0.85 och 1.00
- `kritisk` – 2.0 med chansen 1/16, annars 1.0. Visas i loggen som "Kritisk träff!"
- En träff gör alltid minst 1 i skada.

Exempel: Ember (40 i basskada, FIRE) mot en GRASS-Pokémon med slumpfaktor 0.9
och ingen kritisk träff ger `40 × 2.0 × 0.9 × 1.0 = 72` i skada.

### Typeffektivitet

| Anfallande | Försvarande | Effekt |
|------------|-------------|--------|
| Fire       | Grass       | 2x     |
| Water      | Fire        | 2x     |
| Grass      | Water       | 2x     |
| Electric   | Water       | 2x     |
| Fire       | Water       | 0.5x   |
| Water      | Grass       | 0.5x   |
| Grass      | Fire        | 0.5x   |
| Normal     | (alla)      | 1x     |

Alla övriga kombinationer ger 1x.


### Arv och polymorfism – `Combatant`

`Battle` känner bara till den abstrakta klassen `Combatant` och anropar
`chooseAttack()` på den. `HumanCombatant` frågar spelaren via en meny och
`CpuCombatant` slumpar en attack. Striden behöver därför inga
`if (ärSpelare)`-satser för att välja attack. En ny sorts motståndare, till
exempel en AI som alltid väljer den mest effektiva attacken, kan läggas till
som en ny subklass utan att `Battle` behöver ändras. Testerna använder också en
egen `Combatant`-subklass för att köra strider med förutsägbart resultat.

### Interface – `Repository<T>`

`App` beror på interfacet `Repository<T>` och vet inte att datan sparas som
JSON. `JsonRepository<T>` är generisk och används både för listan med Pokémon
och för statistiken. Om lagringen skulle bytas till till exempel CSV behövs
bara en ny implementation. Resten av programmet påverkas inte.

### Inkapsling

`Pokemon` och `Attack` kontrollerar alla värden i sina setters, till exempel
att namn inte är tomma, att HP ligger inom 1–999 och att det finns 1–4 attacker.
Därför kan ett objekt aldrig bli ogiltigt, vare sig det skapas via menyn eller
läses in från en handredigerad JSON-fil. `getAttacks()` returnerar en kopia av
listan så att den inte kan ändras utifrån. `PokemonCollection` kapslar in
listan och regeln om unika namn.

### Komposition och testbarhet

`Battle` får sin `DamageCalculator` via konstruktorn, och `DamageCalculator`
får i sin tur `TypeChart` och `Random`. Därför kan testerna skicka in ett
`Random` med fast frö och få förutsägbara resultat.

### Övriga designval

- **Typtabellen** lagras som en `EnumMap` i stället för en lång if/switch-kedja.
  En ny relation är bara en rad i konstruktorn.
- **Menyn** är en lista av (text, `Runnable`) i stället för en stor switch-sats.
  Samma `Menu`-klass används för både huvudmenyn och attackmenyn.
- **Records** (`AttackResult`, `BattleResult`) används för oföränderliga resultat.

## Felhantering

Programmet ska aldrig krascha:

- All inmatning går via `ConsoleInput`, som frågar om vid ogiltiga värden
  (`-1`, `99`, `abc`, tom rad).
- Avbruten inmatning (Ctrl+D / Ctrl+Z) kastar `InputAbortedException`, som
  fångas i `App`. Datan sparas och programmet avslutas snyggt, även mitt i en strid.
- Strid utan Pokémon, med en Pokémon som har 0 HP eller saknar attacker stoppas
  med ett tydligt meddelande.
- Saknad JSON-fil ger standarddata.
- Korrupt JSON-fil (felaktig syntax eller ogiltiga värden) sparas undan som
  `*.corrupt-<tidpunkt>`, och programmet startar med standarddata. Ingen data
  skrivs över i tysthet.
- Statistik utan tidigare strider visar "Inga strider har utkämpats ännu."
- Sparandet skriver först till en temporär fil, så en avbruten skrivning lämnar
  ingen trasig fil efter sig.
- Huvudloopen fångar dessutom alla oväntade `RuntimeException` som ett sista skyddsnät.


## Kända begränsningar

- Striderna är alltid en mot en. Det finns inga statuseffekter (gift, sömn osv.).
- Datorns motståndare väljer attack helt slumpvis, utan strategi.
- Flykt räknas som en strid i statistiken för Pokémon, men varken som vinst eller förlust.
- Statistik lagras per Pokémon-namn. En Pokémon som byter namn börjar om med ny statistik.
- Om en enda Pokémon i `pokemons.json` har ett ogiltigt värde räknas hela filen
  som korrupt. Filen sparas undan, så ingen data går förlorad, men den måste rättas för hand.
- Å, ä och ö kan visas fel i vissa Windows-terminaler beroende på teckenkodning.
  I IntelliJ visas de korrekt.