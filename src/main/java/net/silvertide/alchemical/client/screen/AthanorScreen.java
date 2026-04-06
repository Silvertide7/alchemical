package net.silvertide.alchemical.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.menu.AthanorMenu;
import org.jetbrains.annotations.NotNull;

public class AthanorScreen extends AbstractContainerScreen<AthanorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Alchemical.MODID, "textures/gui/athanor.png");

    // Button to confirm adding the ingredient — positioned below the ingredient slot
    private static final int ADD_BUTTON_X = 80;
    private static final int ADD_BUTTON_Y = 60;
    private static final int ADD_BUTTON_W = 54;
    private static final int ADD_BUTTON_H = 20;

    private Button addButton;

    public AthanorScreen(AthanorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        addButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.alchemical.athanor.add"),
                btn -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .pos(x + ADD_BUTTON_X, y + ADD_BUTTON_Y)
                .size(ADD_BUTTON_W, ADD_BUTTON_H)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Update button state every tick based on current validation result
        AthanorMenu.ValidationResult result = menu.canAddIngredient();
        addButton.active = (result == AthanorMenu.ValidationResult.CAN_ADD);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Capacity display — top-center of the GUI
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int loaded = menu.getLoadedCount();
        int capacity = menu.getCapacity();

        if (capacity > 0) {
            Component capacityText = Component.translatable("gui.alchemical.athanor.capacity", loaded, capacity);
            graphics.drawString(this.font, capacityText, x + 8, y + 8, 0x404040, false);
        }

        // Validation status text when button is hovered and inactive
        if (!addButton.active && addButton.isHovered()) {
            AthanorMenu.ValidationResult result = menu.canAddIngredient();
            Component reason = getValidationMessage(result);
            if (reason != null) {
                graphics.renderTooltip(this.font, reason, mouseX, mouseY);
            }
        }
    }

    private Component getValidationMessage(AthanorMenu.ValidationResult result) {
        return switch (result) {
            case NO_ELIXIR -> Component.translatable("gui.alchemical.athanor.no_elixir");
            case NO_INGREDIENT -> Component.translatable("gui.alchemical.athanor.no_ingredient");
            case INVALID_INGREDIENT -> Component.translatable("gui.alchemical.athanor.invalid_ingredient");
            case AT_CAPACITY -> Component.translatable("gui.alchemical.athanor.at_capacity");
            case CAN_ADD -> null;
        };
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw "Inventory" label above player inventory
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
