package net.silvertide.alchemical.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientIngredientData {
    private static final Map<ResourceLocation, EssenceStoneDefinition> STONES = new HashMap<>();
    private static final Map<Item, TinctureDefinition> TINCTURES = new HashMap<>();
    private static final Map<Item, CatalystDefinition> CATALYSTS = new HashMap<>();

    private ClientIngredientData() {}

    public static void sync(
            List<EssenceStoneDefinition> stones,
            List<TinctureDefinition> tinctures,
            List<CatalystDefinition> catalysts
    ) {
        STONES.clear();
        TINCTURES.clear();
        CATALYSTS.clear();
        stones.forEach(d -> STONES.put(d.id(), d));
        tinctures.forEach(d -> TINCTURES.put(d.item(), d));
        catalysts.forEach(d -> CATALYSTS.put(d.item(), d));
    }

    public static Optional<EssenceStoneDefinition> getStone(ResourceLocation id) {
        return Optional.ofNullable(STONES.get(id));
    }

    public static Optional<TinctureDefinition> getTincture(Item item) {
        return Optional.ofNullable(TINCTURES.get(item));
    }

    public static Optional<CatalystDefinition> getCatalyst(Item item) {
        return Optional.ofNullable(CATALYSTS.get(item));
    }
}
