package net.silvertide.alchemical.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record ElixirCooldown(long lastDrankAt, int cooldownSeconds) {
    public static final Codec<ElixirCooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("lastDrankAt").forGetter(ElixirCooldown::lastDrankAt),
            Codec.INT.fieldOf("cooldownSeconds").forGetter(ElixirCooldown::cooldownSeconds))
            .apply(instance, ElixirCooldown::new));

    public static final StreamCodec<FriendlyByteBuf, ElixirCooldown> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(@NotNull FriendlyByteBuf buf, @NotNull ElixirCooldown cooldown) {
            buf.writeLong(cooldown.lastDrankAt());
            buf.writeInt(cooldown.cooldownSeconds());
        }

        @Override
        public @NotNull ElixirCooldown decode(@NotNull FriendlyByteBuf buf) {
            return new ElixirCooldown(buf.readLong(), buf.readInt());
        }
    };

    public boolean isOnCooldown(long currentGameTime) {
        return getSecondsElapsed(currentGameTime) < cooldownSeconds;
    }

    public int getRemainingSeconds(long currentGameTime) {
        return Math.max(0, cooldownSeconds - (int) getSecondsElapsed(currentGameTime));
    }

    private long getSecondsElapsed(long currentGameTime) {
        return (currentGameTime - lastDrankAt) / 20;
    }

    public ElixirCooldown withLastDrankAt(long lastDrankAt) {
        return new ElixirCooldown(lastDrankAt, this.cooldownSeconds);
    }

    public ElixirCooldown withCooldownSeconds(int cooldownSeconds) {
        return new ElixirCooldown(this.lastDrankAt, cooldownSeconds);
    }
}
