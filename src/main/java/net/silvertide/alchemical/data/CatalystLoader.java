package net.silvertide.alchemical.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silvertide.alchemical.records.CatalystDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CatalystLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalystLoader.class);
    public static final CatalystLoader INSTANCE = new CatalystLoader();

    private CatalystLoader() {
        super(new GsonBuilder().create(), "catalyst");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        IngredientManager.clearCatalysts();
        for (var entry : map.entrySet()) {
            CatalystDefinition.CODEC
                    .parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse catalyst definition '{}': {}", entry.getKey(), err))
                    .ifPresent(IngredientManager::registerCatalyst);
        }
        LOGGER.info("[Alchemical] Loaded {} catalyst definitions", IngredientManager.getAllCatalysts().size());
    }
}
