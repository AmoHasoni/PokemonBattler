# Pokemon Battler

En konsolapplikation i Java där du samlar Pokémon och låter dem slåss
turbaserat mot datorstyrda, vilda Pokémon. Projektet bygger vidare på
Pokédexen från inlämning 1: all funktionalitet därifrån finns kvar, men koden
är omstrukturerad mot bättre OOP och utökad med ett stridssystem, JSON-lagring
och statistik.

Kurs: Programmering med Java, grund Inlämning 2.

## Krav

- Java 17 eller nyare
- Maven
- IntelliJ IDEA (rekommenderas)

## Köra programmet

```bash
mvn compile exec:java
```

I IntelliJ: öppna projektet och kör `main` i `com.pokemonbattler.App`.

Data sparas i mappen `data/`, som skapas vid första körningen:

- `data/pokemons.json` – din samling, inklusive aktuellt HP
- `data/stats.json` – stridsstatistik

Samlingen sparas med menyval 6, när du avslutar med 0 och automatiskt efter
varje strid. Statistiken sparas efter varje strid.

Finns ingen sparad fil startar programmet med samma sex Pokémon som i
inlämning 1: Charmander, Squirtle, Bulbasaur, Pikachu, Eevee och Growlithe.

## Meny

```
=== Pokémon Battler ===

1. Visa alla Pokémon
2. Visa detaljer
3. Lägg till Pokémon
4. Redigera Pokémon
5. Ta bort Pokémon
6. Spara till fil
7. Ladda från fil
8. Återställ seed-data
9. Starta strid
10. Stridsresultat
11. Pokémon Center (läk alla)
0. Avsluta
```

Under **4. Redigera Pokémon** finns en undermeny där man kan ändra namn, typ,
max HP, aktuellt HP och speed, samt lägga till och ta bort attacker (1–4 st).

## Stridsregler

- Spelaren väljer en av sina Pokémon. Bara Pokémon med HP > 0 och minst en attack kan väljas.
- Datorn slumpar en motståndare ur en pool av sex vilda Pokémon. Motståndaren har alltid fullt HP.
- **Turordning:** den Pokémon som har högst `speed` attackerar först varje runda.
  Vid lika speed börjar spelaren.
- Spelaren väljer attack i en meny, eller `0` för att fly. Datorn väljer slumpvis bland sina attacker.
- **Träff:** ett slumptal 1–100 måste vara ≤ attackens träffsäkerhet, annars missar attacken.
- HP minskar med skadan. Vid 0 HP är Pokémon besegrad och kan inte attackera mer.
- Striden slutar när någon Pokémon har 0 HP, eller när spelaren flyr.
- Varje runda visas i en stridslogg, och slutresultatet visas, t.ex.
  `Du vann! Din Charmander besegrade vilda Oddish efter 2 rundor.`
- Skadan på din Pokémon finns kvar efter striden. Läk den i Pokémon Center (val 11).

### Skadeformel

```
skada = avrunda( basskada × typeffektivitet × slumpfaktor × kritisk )
```

- `typeffektivitet` - 2.0, 1.0 eller 0.5 enligt tabellen nedan
- `slumpfaktor` - slumpat decimaltal mellan 0.85 och 1.00
- `kritisk` - 2.0 med chansen 1/16, annars 1.0. Visas i loggen som "Kritisk träff!"
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

## OOP-design

### Paketstruktur

```
com.pokemonbattler
├── App                   huvudmeny, kopplar ihop alla delar
├── PokemonCollection     spelarens samling (unika namn, stridsklara Pokémon, startdata)
├── model
│   ├── Pokemon           namn, typ, HP, speed, 1-4 attacker
│   ├── Attack            namn, basskada, träffsäkerhet, typ
│   └── Type              enum: FIRE, WATER, GRASS, ELECTRIC, NORMAL
├── battle
│   ├── Combatant         abstrakt klass: den som styr en Pokémon i strid
│   ├── HumanCombatant    spelaren - väljer attack via konsolmeny
│   ├── CpuCombatant      datorn - väljer attack slumpvis
│   ├── Battle            turordning, stridslogg och resultat
│   ├── DamageCalculator  skadeformel, träff/miss, kritiska träffar
│   ├── TypeChart         typeffektivitetstabell
│   ├── AttackResult      resultatet av en attack (record)
│   ├── BattleResult      resultatet av en strid (record)
│   ├── BattleOutcome     enum: WIN, LOSS, FLED
│   └── WildPokemonPool   vilda motståndare
├── persistence
│   ├── Repository        interface för load/save
│   └── JsonRepository    implementation med Jackson
├── statistics
│   ├── Stats             total statistik, mest använda attack/Pokémon
│   └── PokemonRecord     vinster/förluster per Pokémon
├── ui
│   ├── ConsoleInput      all säker inläsning från konsolen
│   └── Menu              generisk meny
└── exception
    ├── InvalidPokemonException
    ├── InvalidAttackException
    └── InputAbortedException
```

### Separation of Concerns (SOC)

Varje paket har ett tydligt ansvar och känner inte till mer än det behöver:

- `App` och `ui/` sköter bara in- och utmatning i konsolen.
- `model/` och `PokemonCollection` innehåller domänreglerna och validerar sig själva.
- `battle/` innehåller all stridslogik: turordning, skadeberäkning och typeffektivitet.
- `persistence/` sköter all läsning och skrivning av filer.
- `statistics/` räknar och sammanställer statistik.

Exempel: `Battle` skriver aldrig till fil, `JsonRepository` vet ingenting om
menyer, och `Pokemon` läser aldrig från konsolen. Därför kan en del bytas ut,
till exempel JSON mot CSV, utan att resten av programmet påverkas.

### Arv och polymorfism - `Combatant`

```
           Combatant (abstrakt)
           ├─ chooseAttack()   abstrakt
           └─ isPlayer()       abstrakt
               ▲                  ▲
       HumanCombatant        CpuCombatant
       frågar spelaren       slumpar attack
```

`Battle` känner bara till den abstrakta klassen `Combatant` och anropar
`chooseAttack()` på den. `HumanCombatant` frågar spelaren via en meny och
`CpuCombatant` slumpar en attack. Striden behöver därför inga
`if (ärSpelare)`-satser för att välja attack. En ny sorts motståndare, till
exempel en AI som alltid väljer den mest effektiva attacken, kan läggas till
som en ny subklass utan att `Battle` behöver ändras.

### Interface - `Repository`

`App` beror på interfacet `Repository` och vet inte att datan sparas som
JSON. `JsonRepository<T>` är generisk och används både för listan med Pokémon
och för statistiken. Om lagringen skulle bytas till till exempel CSV behövs
bara en ny implementation. Resten av programmet påverkas inte.

`JsonStorage` från inlämning 1, som bara kunde spara Pokémon till
`pokedex.json`, har refaktorerats till `JsonRepository<T>` så att samma kod
kan spara både Pokémon och statistik.

### Inkapsling

`Pokemon` och `Attack` kontrollerar alla värden i sina setters, till exempel
att namn inte är tomma, att HP ligger inom 1–999 och att det finns 1–4 attacker.
Därför kan ett objekt aldrig bli ogiltigt, vare sig det skapas via menyn eller
läses in från en handredigerad JSON-fil. `getAttacks()` returnerar en kopia av
listan så att den inte kan ändras utifrån. `PokemonCollection` kapslar in
listan och regeln om unika namn.

### Komposition

`Battle` får sin `DamageCalculator` via konstruktorn, och `DamageCalculator`
får i sin tur `TypeChart` och `Random`. Delarna är därför fristående och kan
bytas ut eller testas var för sig, till exempel med ett `Random` med fast frö.

### Övriga designval

- **Typtabellen** lagras som en `EnumMap` i stället för en lång if/switch-kedja.
  En ny relation är bara en rad i konstruktorn.
- **Menyn** är en lista av (text, `Runnable`) i stället för en stor switch-sats.
  Samma `Menu`-klass används för både huvudmenyn och redigeringsmenyn.
- **Records** (`AttackResult`, `BattleResult`) används för oföränderliga resultat.
- **`Optional`** används i stället för `null` när ett värde kan saknas, t.ex. när
  spelaren flyr eller avbryter ett val.

## VG-krav som uppfylls

- [x] **Typeffektivitet** - tabellen ovan, implementerad i `TypeChart`.
- [x] **JSON som persistensformat** – all lagring sker via Jackson
  (`data/pokemons.json` och `data/stats.json`).
- [x] **Kritiska träffar** - chans 1/16, dubbel skada, visas i stridsloggen.
- [x] **Persistent statistik** mellan sessioner, visas via menyval 10:
  - totala vinster och förluster
  - vinst/förlust-ratio per Pokémon
  - mest använda attack
  - mest använda Pokémon

## Felhantering

Programmet ska aldrig krascha:

- All inmatning går via `ConsoleInput`, som frågar om vid ogiltiga värden
  (`-1`, `99`, `abc`, tom rad).
- Avbruten inmatning (Ctrl+D / Ctrl+Z) kastar `InputAbortedException`, som
  fångas i `App`. Datan sparas och programmet avslutas snyggt, även mitt i en strid.
- Strid utan Pokémon, med en Pokémon som har 0 HP eller saknar attacker stoppas
  med ett tydligt meddelande.
- Saknad JSON-fil ger startdata.
- Korrupt JSON-fil (felaktig syntax eller ogiltiga värden) sparas undan som
  `*.corrupt-<tidpunkt>`, och programmet startar med startdata. Ingen data
  skrivs över i tysthet.
- Statistik utan tidigare strider visar "Inga strider har utkämpats ännu."
- Sparandet skriver först till en temporär fil, så en avbruten skrivning lämnar
  ingen trasig fil efter sig.
- Huvudloopen fångar dessutom alla oväntade `RuntimeException` som ett sista skyddsnät.

## Kända begränsningar

- Striderna är alltid en mot en. Det finns inga statuseffekter (gift, sömn osv.).
- Datorns motståndare väljer attack helt slumpvis, utan strategi.
- Startpokémonen har låg HP (35–55), så de flesta strider avgörs på 1–3 rundor.
- Flykt räknas som en strid i statistiken för Pokémon, men varken som vinst eller förlust.
- Statistik lagras per Pokémon-namn. En Pokémon som byter namn börjar om med ny statistik.
- Om en enda Pokémon i `pokemons.json` har ett ogiltigt värde räknas hela filen
  som korrupt. Filen sparas undan, så ingen data går förlorad, men den måste rättas för hand.
- Å, ä och ö kan visas fel i vissa Windows-terminaler beroende på teckenkodning.
  I IntelliJ visas de korrekt.