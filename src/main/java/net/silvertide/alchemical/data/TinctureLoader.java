package net.silvertide.alchemical.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silvertide.alchemical.records.TinctureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TinctureLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TinctureLoader.class);
    public static final TinctureLoader INSTANCE = new TinctureLoader();

    private TinctureLoader() {
        super(new GsonBuilder().create(), "tincture");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        IngredientManager.clearTinctures();
        for (var entry : map.entrySet()) {
            TinctureDefinition.CODEC
                    .parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse tincture definition '{}': {}", entry.getKey(), err))
                    .ifPresent(IngredientManager::registerTincture);
        }
        LOGGER.info("[Alchemical] Loaded {} tincture definitions", IngredientManager.getAllTinctures().size());
    }
}
