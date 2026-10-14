# Alchemical

Reusable, customisable elixirs for Minecraft 1.20.1 on Forge.

Craft an Elixir flask once, load it with ingredients at an Athanor, and drink it as often as the cooldown allows. What the elixir does is decided by data: every stone, tincture and catalyst is a datapack JSON, so packs can reskin, rebalance or replace the whole set.

## How it works

**Elixir** – a flask that holds up to 9 potency worth of ingredients (configurable). Right-click to drink the active stone's effect. Shift + right-click to switch which stone is active. Hold Shift over the item for the full breakdown.

**Essence Stones** – decide the effect. One stone item exists; which effect it gives comes from its definition. The default pack ships ten: Bastion (Resistance), Crimson (Regeneration), Ember (Fire Resistance), Leaping (Jump Boost), Might (Strength), Nightseer (Night Vision), Phantom (Invisibility), Swift (Speed), Tidal (Water Breathing) and Zephyr (Slow Falling). Up to three can be loaded at once (configurable); only the active one is drunk.

**Tinctures** – vanilla items that act as the base solution and tune duration, level or cooldown. Defaults: Honey Bottle, Water Bottle, Bottle o' Enchanting, Dragon's Breath.

**Catalysts** – vanilla items that push the numbers harder, usually trading cooldown for strength. Defaults: Blaze Powder, Glowstone Dust, Gunpowder, Redstone.

**Athanor** – the brewing station. Place a flask, stage one ingredient, hit **Add**. The order is stone first, then tincture, then catalyst. **Empty Elixir** clears the flask: tinctures and catalysts are lost, each stone has a 50% chance (configurable) to come back.

Every drink starts a global cooldown (30 minutes by default) that the stone, tinctures and catalysts can shorten or lengthen.

## Recipes

| Item | Pattern |
|------|---------|
| Athanor | Blackstone around a Cauldron, Furnace and Brewing Stand (top to bottom, centre column) |
| Elixir | Glass Bottles around an Amethyst Shard (top) and a Heart of the Sea (centre) |

Essence Stones have no recipe by default; add your own via datapack, loot or the creative tab.

## Configuration

`<world>/serverconfig/alchemical-server.toml` (per world, synced to clients):

| Key | Default | Meaning |
|-----|---------|---------|
| `essenceStoneBreakChance` | 0.5 | Chance each stone is destroyed when emptying a flask |
| `elixirCooldownSeconds` | 1800 | Base cooldown before modifiers |
| `elixirCapacity` | 9 | Total potency a flask can hold |
| `maxEssenceStones` | 3 | Stones per flask. The Athanor UI only lists the first three |

Admins can clear a player's cooldown with `/alchemical clearcooldown <player>`.

## Datapacks

The defaults live in the built-in `alchemical_defaults` pack and can be overridden file-by-file. Three folders under `data/<namespace>/`:

**`essence_stone/<id>.json`** – the file name is the stone's id.

```json
{
  "name": "Crimson Stone",
  "color": "#FF6699",
  "effect": "minecraft:regeneration",
  "base_duration": 4800,
  "base_level": 1,
  "elixir_cooldown_multiplier": 1.0,
  "elixir_cooldown_flat": 0,
  "potency": 2
}
```

Only `effect` is required. `base_duration` is in ticks, `elixir_cooldown_flat` in seconds. `color` tints the stone's overlay layer.

**`tincture/<id>.json`** and **`catalyst/<id>.json`** share one shape; the folder decides which it is.

```json
{
  "item": "minecraft:blaze_powder",
  "name": "Blaze Catalyst",
  "effect_level_modifier": 1,
  "effect_duration_multiplier": 0.70,
  "effect_duration_flat": 0,
  "elixir_cooldown_multiplier": 1.30,
  "elixir_cooldown_flat": 0,
  "potency": 2
}
```

Only `item` is required. Multipliers from every loaded ingredient are multiplied together, flat values and level modifiers are summed, then applied to the active stone's base numbers. `name` overrides the item's display name wherever the mod shows it.

An unknown `effect` id or a malformed file is logged and skipped.

## Building

Requires Java 17 to run the game; Gradle itself runs on Java 17 or 21.

```bash
./gradlew build
```

The jar lands in `build/libs/` (already reobfuscated for Forge). `./gradlew publishMods` uploads it to CurseForge using the `CF_TOKEN` environment variable; set `PUBLISH_DRY_RUN=true` to rehearse without uploading. `./gradlew buildAndPublishMods` does both in one go.

## Licence

All Rights Reserved.
