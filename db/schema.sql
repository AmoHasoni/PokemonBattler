CREATE TABLE "type" (
                        "type_id" serial PRIMARY KEY,
                        "name" varchar(20) UNIQUE NOT NULL
);

CREATE TABLE "trainer" (
                           "trainer_id" serial PRIMARY KEY,
                           "name" varchar(30) UNIQUE NOT NULL,
                           "created_at" timestamp NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "pokemon" (
                           "pokemon_id" serial PRIMARY KEY,
                           "trainer_id" int NOT NULL,
                           "type_id" int NOT NULL,
                           "name" varchar(30) NOT NULL,
                           "max_hp" int NOT NULL CHECK ("max_hp" BETWEEN 1 AND 999),
                           "current_hp" int NOT NULL,
                           "speed" int NOT NULL DEFAULT 50 CHECK ("speed" BETWEEN 1 AND 200),
                           "wins" int NOT NULL DEFAULT 0 CHECK ("wins" >= 0),
                           "losses" int NOT NULL DEFAULT 0 CHECK ("losses" >= 0),
                           CHECK ("current_hp" BETWEEN 0 AND "max_hp")
);

CREATE TABLE "attack" (
                          "attack_id" serial PRIMARY KEY,
                          "type_id" int NOT NULL,
                          "name" varchar(40) UNIQUE NOT NULL,
                          "base_damage" int NOT NULL CHECK ("base_damage" BETWEEN 1 AND 300),
                          "accuracy" int NOT NULL CHECK ("accuracy" BETWEEN 1 AND 100)
);

CREATE TABLE "pokemon_attack" (
                                  "pokemon_id" int NOT NULL,
                                  "attack_id" int NOT NULL,
                                  "slot" smallint NOT NULL CHECK ("slot" BETWEEN 1 AND 4),
                                  PRIMARY KEY ("pokemon_id", "attack_id")
);

CREATE UNIQUE INDEX "uq_pokemon_trainer_name" ON "pokemon" ("trainer_id", (lower(name)));

CREATE UNIQUE INDEX ON "pokemon_attack" ("pokemon_id", "slot");

COMMENT ON COLUMN "pokemon"."trainer_id" IS 'FK';
COMMENT ON COLUMN "pokemon"."type_id" IS 'FK';
COMMENT ON COLUMN "attack"."type_id" IS 'FK';
COMMENT ON TABLE "pokemon_attack" IS 'Kopplingstabell för N:M mellan pokemon och attack';
COMMENT ON COLUMN "pokemon_attack"."pokemon_id" IS 'PK + FK';
COMMENT ON COLUMN "pokemon_attack"."attack_id" IS 'PK + FK';

ALTER TABLE "pokemon" ADD FOREIGN KEY ("trainer_id") REFERENCES "trainer" ("trainer_id") ON DELETE CASCADE;
ALTER TABLE "pokemon" ADD FOREIGN KEY ("type_id") REFERENCES "type" ("type_id") ON DELETE RESTRICT;
ALTER TABLE "attack" ADD FOREIGN KEY ("type_id") REFERENCES "type" ("type_id") ON DELETE RESTRICT;
ALTER TABLE "pokemon_attack" ADD FOREIGN KEY ("pokemon_id") REFERENCES "pokemon" ("pokemon_id") ON DELETE CASCADE;
ALTER TABLE "pokemon_attack" ADD FOREIGN KEY ("attack_id") REFERENCES "attack" ("attack_id") ON DELETE CASCADE;