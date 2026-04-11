package net.silvertide.alchemical.records;

/**
 * Shared interface for ingredient definitions that contribute modifier values
 * (tinctures and catalysts). Allows generic code to read modifier fields without
 * caring about the concrete type.
 */
public interface ModifierDefinition {
    float elixirCooldownMultiplier();
    int elixirCooldownFlat();
    float effectDurationMultiplier();
    int effectDurationFlat();
    int effectLevelModifier();
    int potency();
}
