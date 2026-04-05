package net.silvertide.alchemical.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.attachments.ElixirCooldown;

import java.util.function.Supplier;

public final class AttachmentRegistry {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Alchemical.MODID);

    public static final Supplier<AttachmentType<ElixirCooldown>> ELIXIR_COOLDOWN = ATTACHMENT_TYPES.register(
            "elixir_cooldown", () -> AttachmentType.builder(() -> new ElixirCooldown(0L, 0))
                    .serialize(ElixirCooldown.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    private AttachmentRegistry() {}
}
