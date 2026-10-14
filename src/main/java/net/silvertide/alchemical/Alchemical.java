package net.silvertide.alchemical;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.network.Networking;
import net.silvertide.alchemical.registry.BlockRegistry;
import net.silvertide.alchemical.registry.ItemRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;
import net.silvertide.alchemical.registry.TabRegistry;
import org.slf4j.Logger;

import java.nio.file.Path;

@Mod(Alchemical.MODID)
public class Alchemical {
    public static final String MODID = "alchemical";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alchemical() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BlockRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        TabRegistry.register(modEventBus);
        Networking.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AlchemicalConfig.SERVER_SPEC);

        modEventBus.addListener(Alchemical::addPackFinders);
    }

    // The default stones, tinctures and catalysts ship as a built-in datapack so packs can override them.
    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(consumer -> {
            Path packPath = ModList.get().getModFileById(MODID).getFile().findResource("builtin_data_packs/alchemical_defaults");
            Pack pack = Pack.readMetaAndCreate("alchemical/defaults", Component.literal("Alchemical Defaults"), false,
                    packId -> new PathPackResources(packId, packPath, true),
                    PackType.SERVER_DATA, Pack.Position.TOP, PackSource.DEFAULT);
            if (pack != null) consumer.accept(pack);
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
