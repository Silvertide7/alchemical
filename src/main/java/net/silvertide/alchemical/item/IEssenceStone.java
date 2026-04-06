package net.silvertide.alchemical.item;

/**
 * Marker interface for Essence Stone items.
 * Any item implementing this interface can be loaded into an Elixir as an Essence Stone,
 * contributing to the effect type. Multiple stones can be loaded; shift+use cycles which is active.
 *
 * Future: may add getAssociatedEffect() -> Holder<MobEffect>
 */
public interface IEssenceStone {}
