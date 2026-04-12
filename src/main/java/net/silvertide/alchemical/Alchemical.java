package net.silvertide.alchemical;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.network.Networking;
import net.silvertide.alchemical.registry.*;
import org.slf4j.Logger;

@Mod(Alchemical.MODID)
public class Alchemical {
    public static final String MODID = "alchemical";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alchemical(IEventBus modEventBus, ModContainer modContainer) {
        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        DataComponentRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        TabRegistry.register(modEventBus);
        Networking.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, AlchemicalConfig.SERVER_SPEC);

        modEventBus.addListener(this::addPackFinders);
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MODID, "builtin_data_packs/alchemical_defaults");
            event.addPackFinders(location, PackType.SERVER_DATA,
                    Component.literal("Alchemical Defaults"),
                    PackSource.DEFAULT, false, Pack.Position.TOP);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
