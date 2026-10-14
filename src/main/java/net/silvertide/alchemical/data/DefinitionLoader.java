package net.silvertide.alchemical.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.silvertide.alchemical.Alchemical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Reads every JSON in one datapack folder with a codec and hands the results to a store. */
public final class DefinitionLoader<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    private final String folder;
    private final Codec<T> codec;
    private final BiFunction<T, ResourceLocation, T> stampId;
    private final Consumer<List<T>> store;

    public DefinitionLoader(String folder, Codec<T> codec, Consumer<List<T>> store) {
        this(folder, codec, (def, id) -> def, store);
    }

    /** {@code stampId} gets the file name, for definitions that need to remember it. */
    public DefinitionLoader(String folder, Codec<T> codec, BiFunction<T, ResourceLocation, T> stampId, Consumer<List<T>> store) {
        super(GSON, folder);
        this.folder = folder;
        this.codec = codec;
        this.stampId = stampId;
        this.store = store;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<T> defs = new ArrayList<>();
        files.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> Alchemical.LOGGER.error("Bad {} definition {}: {}", folder, id, err))
                .ifPresent(def -> defs.add(stampId.apply(def, id))));
        store.accept(defs);
        Alchemical.LOGGER.info("Loaded {} {} definitions", defs.size(), folder);
    }
}
