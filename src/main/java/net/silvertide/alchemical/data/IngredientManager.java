package net.silvertide.alchemical.data;

import net.minecraft.world.item.Item;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class IngredientManager {
    private static final Map<Item, EssenceStoneDefinition> STONES = new HashMap<>();
    private static final Map<Item, TinctureDefinition> TINCTURES = new HashMap<>();
    private static final Map<Item, CatalystDefinition> CATALYSTS = new HashMap<>();

    private IngredientManager() {}

    public static Optional<EssenceStoneDefinition> getStone(Item item) {
        return Optional.ofNullable(STONES.get(item));
    }

    public static Optional<TinctureDefinition> getTincture(Item item) {
        return Optional.ofNullable(TINCTURES.get(item));
    }

    public static Optional<CatalystDefinition> getCatalyst(Item item) {
        return Optional.ofNullable(CATALYSTS.get(item));
    }

    public static void registerStone(EssenceStoneDefinition def) {
        STONES.put(def.item(), def);
    }

    public static void registerTincture(TinctureDefinition def) {
        TINCTURES.put(def.item(), def);
    }

    public static void registerCatalyst(CatalystDefinition def) {
        CATALYSTS.put(def.item(), def);
    }

    public static void clearStones() {
        STONES.clear();
    }

    public static void clearTinctures() {
        TINCTURES.clear();
    }

    public static void clearCatalysts() {
        CATALYSTS.clear();
    }

    public static Collection<EssenceStoneDefinition> getAllStones() {
        return STONES.values();
    }

    public static Collection<TinctureDefinition> getAllTinctures() {
        return TINCTURES.values();
    }

    public static Collection<CatalystDefinition> getAllCatalysts() {
        return CATALYSTS.values();
    }
}
