package net.silvertide.alchemical.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

public class EssenceStoneLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EssenceStoneLoader.class);
    public static final EssenceStoneLoader INSTANCE = new EssenceStoneLoader();

    private EssenceStoneLoader() {
        super(new GsonBuilder().create(), "essence_stone");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        IngredientManager.clearStones();
        for (var entry : map.entrySet()) {
            EssenceStoneDefinition.CODEC
                    .parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse essence stone definition '{}': {}", entry.getKey(), err))
                    .map(def -> def.withId(entry.getKey()))
                    .ifPresent(IngredientManager::registerStone);
        }
        LOGGER.info("[Alchemical] Loaded {} essence stone definitions", IngredientManager.getAllStones().size());
    }
}
