# Alchemical — Claude Guide

## Project Overview

**Alchemical** is a NeoForge 1.21.1 Minecraft mod that adds reusable, customizable elixirs. Unlike potions, elixirs are not consumed on use — they regenerate over time. Drinking an elixir grants a prolonged effect and places the player on a cooldown (tracked via a player attachment). The effect granted depends on what components were used to craft the elixir.

- **Mod ID:** `alchemical`
- **Root Package:** `net.silvertide.alchemical`
- **NeoForge Version:** 21.0.167
- **Minecraft Version:** 1.21.1
- **Java Version:** 21

---

## Package Structure

Follow the same organizational pattern as the `homebound` mod:

```
net/silvertide/alchemical/
├── Alchemical.java              # Main mod class (@Mod entry point)
├── attachments/                 # Player data — elixir cooldown tracking
├── client/                      # Client-side rendering, HUD overlays
├── compat/                      # Third-party mod compatibility (Curios, JEI, etc.)
├── config/                      # Server/client config (TOML)
├── datagen/                     # Data generators (recipes, loot tables, lang)
├── events/                      # Event subscribers (NeoForge & Mod bus)
│   └── custom/                  # Custom mod events (e.g., DrinkElixirEvent)
├── item/                        # Item classes and interfaces
├── network/                     # Packets for client/server sync
├── records/                     # Immutable data records (codecs, stream codecs)
├── registry/                    # Centralized DeferredRegister classes
├── setup/                       # One-time initialization logic
└── util/                        # Utility helpers (attachment, effect, etc.)
```

---

## Naming Conventions

Match the conventions from `homebound`:

| Pattern | Convention | Example |
|---|---|---|
| Interfaces | `I` prefix | `IElixir`, `IAlchemicalComponent` |
| Registry classes | `Registry` suffix | `ItemRegistry`, `AttachmentRegistry` |
| Event handler classes | `Events` suffix | `ElixirEvents`, `CooldownEvents` |
| Custom events | `Event` suffix | `DrinkElixirEvent` |
| Utility classes | `Util` suffix | `ElixirAttachmentUtil`, `EffectUtil` |
| Records | Plain names | `ElixirCooldown`, `ElixirData` |

---

## Core Systems

### 1. Elixir Item

The elixir is a **reusable drink item** — not a one-time consumable like a vanilla potion.

- Right-clicking drinks it, granting a configurable potion effect for an extended duration
- Drinking starts a cooldown; the player cannot drink again until it expires
- The flask slowly regenerates charges (not an instant restock — tracked over real time or ticks)
- Multiple elixir item variants with different textures/models based on contents

**Relevant classes to create:**
- `item/ElixirItem.java` — base item class implementing drink logic
- `item/IElixir.java` — interface defining the contract (getEffect, getCooldown, etc.)

### 2. Cooldown Attachment

Track per-player cooldown state using a **NeoForge player attachment** (not capabilities — use the 1.21 attachment API).

```java
// Pattern from homebound:
AttachmentType.serializable(() -> new ElixirCooldown(0L, 0))
    .build()
```

- `ElixirCooldown` record: stores `lastDrankAt` (game time in ticks) and `cooldownTicks` (duration)
- Include an `isOnCooldown(Level level)` helper
- Use `ElixirAttachmentUtil` for get/set operations — never access attachments directly from item code
- Use `copyOnDeath()` — cooldown must persist through death and resolve naturally

### 3. Elixir Composition System

Elixirs are composed of alchemical parts. Use real alchemy terminology:

| Component | Term | Role |
|---|---|---|
| Base liquid(s) | **Tincture** | Contributes to duration and delivery |
| Core stone(s) | **Essence Stone** | Contributes to effect type |
| Modifier(s) | **Catalyst** | Modifies potency or adds secondary effects |

All three can have **multiple items** loaded simultaneously. The final effect is derived from the **combination** (mixture) of everything loaded — not from a single item.

Each `ElixirItem` has a **capacity** — a max total number of ingredient slots shared across all three types. Loading more stones leaves fewer slots for tinctures and catalysts. Requires at least 1 Essence Stone + 1 Tincture to be drinkable. Different registered elixir variants have different capacities.

If multiple Essence Stones are loaded, shift+use cycles which stone is currently active (the active stone determines the effect on drink). With only 1 stone loaded, shift does nothing.

### 4. Elixir Crafting — Alchemical Mini-Game (TBD)

The exact crafting mechanic is not yet finalized. Placeholder ideas:
- A custom crafting station block (Alchemical Table / Athanor)
- Multi-step recipe requiring sequential interactions (inspired by real alchemical processes: calcination, dissolution, distillation, etc.)
- Possibly a GUI with timed inputs or puzzle elements

**Do not implement the mini-game yet** — keep crafting as standard shaped/shapeless recipes until the design is nailed down.

---

## Registry Pattern

All registries use `DeferredRegister`. Register them in `Alchemical.java` constructor:

```java
public Alchemical(IEventBus modEventBus, ModContainer modContainer) {
    ItemRegistry.register(modEventBus);
    AttachmentRegistry.register(modEventBus);
    TabRegistry.register(modEventBus);
}
```

Each registry class has a static `register(IEventBus)` method and exposes `DeferredHolder` fields.

---

## Event Handler Pattern

Use `@EventBusSubscriber` with explicit bus declaration:

```java
// NeoForge game events (player interactions, ticks, etc.)
@EventBusSubscriber(modid = Alchemical.MODID, bus = Bus.GAME)
public class ElixirEvents { ... }

// Mod lifecycle events (registration, setup)
@EventBusSubscriber(modid = Alchemical.MODID, bus = Bus.MOD)
public class ModSetupEvents { ... }
```

---

## Utility Class Pattern

```java
public class ElixirAttachmentUtil {
    public static Optional<ElixirCooldown> getCooldown(Player player) { ... }
    public static void setCooldown(Player player, ElixirCooldown cooldown) { ... }
    public static boolean isOnCooldown(Player player) { ... }
}
```

Never let item classes or event handlers reach into attachments directly — always go through util.

---

## ResourceLocation Helper

Add a static helper to `Alchemical.java`:

```java
public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
}
```

---

## Data Generation

Use `datagen/` for:
- Item models (multiple elixir variants with predicate overrides or separate items)
- Language entries (`en_us.json`)
- Recipes (shaped crafting for elixir components, at minimum)
- Loot tables if needed

---

## Key Design Rules

1. **Never hardcode effect duration or cooldown** — drive these from item properties or config
2. **Elixir variants are separate items** registered individually, not a single item with NBT data
3. **No capability API** — use NeoForge 1.21 attachment API exclusively
4. **Codecs required** on all attachment records for serialization (persist through log out/in)
5. **StreamCodecs required** on any data sent over the network
6. **Do not implement the crafting mini-game** until the design is finalized — stub with standard recipes
