package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.menu.AthanorMenu;

import java.util.function.Supplier;

public final class MenuRegistry {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Alchemical.MODID);

    public static final Supplier<MenuType<AthanorMenu>> ATHANOR =
            MENUS.register("athanor", () -> IMenuTypeExtension.create(AthanorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private MenuRegistry() {}
}
