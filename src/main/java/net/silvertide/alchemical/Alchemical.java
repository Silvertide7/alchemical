package net.silvertide.alchemical;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.silvertide.alchemical.network.Networking;
import net.silvertide.alchemical.registry.AttachmentRegistry;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.registry.ItemRegistry;
import net.silvertide.alchemical.registry.TabRegistry;
import org.slf4j.Logger;

@Mod(Alchemical.MODID)
public class Alchemical {
    public static final String MODID = "alchemical";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alchemical(IEventBus modEventBus, ModContainer modContainer) {
        ItemRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        DataComponentRegistry.register(modEventBus);
        TabRegistry.register(modEventBus);
        Networking.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
