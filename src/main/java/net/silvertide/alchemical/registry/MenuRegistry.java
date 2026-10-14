package net.silvertide.alchemical.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.menu.AthanorMenu;

public final class MenuRegistry {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Alchemical.MODID);

    public static final RegistryObject<MenuType<AthanorMenu>> ATHANOR =
            MENUS.register("athanor", () -> IForgeMenuType.create(AthanorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private MenuRegistry() {}
}
