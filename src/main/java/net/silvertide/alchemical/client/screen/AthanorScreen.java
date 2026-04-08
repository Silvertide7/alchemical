package net.silvertide.alchemical.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.item.IElixir;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.menu.AthanorMenu;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AthanorScreen extends AbstractContainerScreen<AthanorMenu> {

    // ── Layout constants (GUI-relative pixels) ────────────────────────────────

    // Slot positions — must match AthanorMenu
    private static final int ELIXIR_SLOT_X     = 79;   // centred in 176px GUI
    private static final int ELIXIR_SLOT_Y     = 16;
    private static final int INGREDIENT_SLOT_X = 130;
    private static final int INGREDIENT_SLOT_Y = 16;

    // Left info panel (elixir name, dots, ingredient list, [Clear])
    private static final int LP_X      = 4;
    private static final int LP_RIGHT  = 74;

    // Right ingredient panel (ingredient slot + [Add] + stats)
    // Only rendered when elixir has room
    private static final int RP_X      = 101;
    private static final int RP_RIGHT  = 172;

    // Panel vertical extents
    private static final int PANEL_TOP = 8;
    private static final int PANEL_BOT = 130;  // above player inventory title (~140)

    // Content start Y (below title bar)
    private static final int CONTENT_Y = 20;

    // Corner marker size
    private static final int CORNER = 4;

    // Label positions (GUI-relative)
    // [Clear] — anchored to bottom of left panel
    private static final int CLEAR_LABEL_GUI_X = LP_X;
    private static final int CLEAR_LABEL_GUI_Y = PANEL_BOT - 12;
    // [Add] — to the RIGHT of the ingredient slot, vertically centred on it
    private static final int ADD_LABEL_GUI_X   = INGREDIENT_SLOT_X + 20;  // 4px gap after slot
    private static final int ADD_LABEL_GUI_Y   = INGREDIENT_SLOT_Y + 5;   // (18-9)/2 ≈ centre

    // Right panel info — starts where [Add] used to sit (below the slot)
    private static final int RP_INFO_Y         = INGREDIENT_SLOT_Y + 22;

    // Inventory panel
    private static final int INV_PANEL_X = 4;
    private static final int INV_PANEL_Y = 143;
    private static final int INV_PANEL_W = 168;
    private static final int INV_PANEL_H = 92;

    // ── Colors ────────────────────────────────────────────────────────────────

    private static final int C_ELIXIR_CORNER = 0xFFCCBB88;
    private static final int C_INGR_CORNER   = 0xFFAABBCC;
    private static final int C_LINE          = 0xFF665544;
    private static final int C_ELIXIR_NAME   = 0xFFCCBB88;
    private static final int C_LEFT_TEXT     = 0xFF999988;
    private static final int C_CLEAR_IDLE    = 0xFF885533;
    private static final int C_CLEAR_HOVER   = 0xFFCC7755;
    private static final int C_CLEAR_CONFIRM = 0xFFDD4422;
    private static final int C_ADD_IDLE      = 0xFF7799AA;
    private static final int C_ADD_HOVER     = 0xFFCCEEFF;
    private static final int C_ADD_DISABLED  = 0xFF444444;
    private static final int C_TYPE_LABEL    = 0xFF7788AA;
    private static final int C_INGR_NAME     = 0xFFDDDDDD;
    private static final int C_STATS         = 0xFF999988;
    private static final int C_DOT_FILLED    = 0xFFCC8800;
    private static final int C_DOT_EMPTY     = 0xFF332211;
    private static final int C_INV_FILL      = 0xAA000000;
    private static final int C_SLOT_GRID     = 0xFF333344;   // inventory slot grid lines

    // ── Animation ─────────────────────────────────────────────────────────────

    private static final long ANIM_MS = 350L;

    private boolean prevHasElixir      = false;
    private boolean prevHasIngredient  = false;
    private long elixirAnimStart       = 0L;
    private long ingredientAnimStart   = 0L;

    // ── Confirm-clear state ───────────────────────────────────────────────────

    private boolean confirmPending     = false;
    private long confirmAt             = 0L;
    private static final long CONFIRM_TTL = 3000L;

    // ── Cached state ──────────────────────────────────────────────────────────

    private AthanorMenu.ValidationResult cachedValidation = AthanorMenu.ValidationResult.NO_ELIXIR;

    // ── Computed screen-space label positions ─────────────────────────────────

    private int addScreenX, addScreenY;
    private int clearScreenX, clearScreenY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AthanorScreen(AthanorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        updateLabelPositions();
    }

    private void updateLabelPositions() {
        addScreenX   = leftPos + ADD_LABEL_GUI_X;
        addScreenY   = topPos  + ADD_LABEL_GUI_Y;
        clearScreenX = leftPos + CLEAR_LABEL_GUI_X;
        clearScreenY = topPos  + CLEAR_LABEL_GUI_Y;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    protected void containerTick() {
        super.containerTick();

        ItemStack elixir = menu.getElixirStack();
        boolean hasElixir = !elixir.isEmpty() && elixir.getItem() instanceof IElixir;

        if (hasElixir && !prevHasElixir) elixirAnimStart = System.currentTimeMillis();
        if (!hasElixir) elixirAnimStart = 0L;
        prevHasElixir = hasElixir;

        boolean hasIngredient = !menu.getIngredientStack().isEmpty();
        if (hasIngredient && !prevHasIngredient) ingredientAnimStart = System.currentTimeMillis();
        if (!hasIngredient) ingredientAnimStart = 0L;
        prevHasIngredient = hasIngredient;

        cachedValidation = menu.canAddIngredient();

        if (confirmPending && System.currentTimeMillis() - confirmAt >= CONFIRM_TTL) {
            confirmPending = false;
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int px = leftPos;
        int py = topPos;

        // ── Elixir slot corners — always visible ───────────────────────────────
        drawSlotCorners(g, px + ELIXIR_SLOT_X - 1, py + ELIXIR_SLOT_Y - 1, C_ELIXIR_CORNER);

        // ── Inventory panel fill + slot grid ──────────────────────────────────
        g.fill(px + INV_PANEL_X, py + INV_PANEL_Y,
               px + INV_PANEL_X + INV_PANEL_W, py + INV_PANEL_Y + INV_PANEL_H, C_INV_FILL);
        // 3×9 main inventory (slot positions: x=8, y=152 — grid offset 1px out)
        drawSlotGrid(g, px + 7, py + 151, 9, 3, C_SLOT_GRID);
        // 1×9 hotbar (x=8, y=210)
        drawSlotGrid(g, px + 7, py + 209, 9, 1, C_SLOT_GRID);

        // ── Right panel + line (only when elixir has room) ────────────────────
        boolean atCapacity = cachedValidation == AthanorMenu.ValidationResult.AT_CAPACITY
                          || menu.getElixirStack().isEmpty()
                          || !(menu.getElixirStack().getItem() instanceof IElixir);

        if (!atCapacity) {
            float elixirP = easeOut(elixirAnimStart);
            int elixirAlpha = (int)(elixirP * 255);
            if (elixirAlpha > 0) {
                // Connecting line (grows left→right as elixir anim plays)
                int lineY      = py + ELIXIR_SLOT_Y + 8;
                int lineStartX = px + ELIXIR_SLOT_X + 18;
                int lineEndX   = px + INGREDIENT_SLOT_X - 2;
                int lineCurX   = lineStartX + (int)((lineEndX - lineStartX) * elixirP);
                g.fill(lineStartX, lineY, lineCurX, lineY + 1, withAlpha(C_LINE, elixirAlpha));

                // Ingredient slot corners
                drawSlotCorners(g, px + INGREDIENT_SLOT_X - 1, py + INGREDIENT_SLOT_Y - 1,
                                withAlpha(C_INGR_CORNER, elixirAlpha));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int px = leftPos;
        int py = topPos;

        ItemStack elixir = menu.getElixirStack();
        boolean hasElixir = !elixir.isEmpty() && elixir.getItem() instanceof IElixir;
        if (!hasElixir) return;

        IElixir iElixir     = (IElixir) elixir.getItem();
        int capacity        = iElixir.getCapacity();
        int loadedCount     = iElixir.getLoadedCount(elixir);
        boolean atCapacity  = loadedCount >= capacity;
        boolean hasContent  = loadedCount > 0;

        float elixirP = easeOut(elixirAnimStart);
        if (elixirP <= 0f) return;
        int elixirAlpha = (int)(elixirP * 255);

        // ── Left panel ─────────────────────────────────────────────────────────

        // Elixir name
        g.drawString(font, elixir.getHoverName(),
                     px + LP_X, py + CONTENT_Y, withAlpha(C_ELIXIR_NAME, elixirAlpha), false);

        // Capacity dots
        int dotStartX = px + LP_X;
        int dotStartY = py + CONTENT_Y + 12;
        int dotsPerRow = (LP_RIGHT - LP_X) / 7;  // ~10 dots per row with 7px spacing
        for (int i = 0; i < capacity; i++) {
            int col    = i % dotsPerRow;
            int row    = i / dotsPerRow;
            int dotCX  = dotStartX + col * 7;
            int dotCY  = dotStartY + row * 7;
            int dotCol = withAlpha(i < loadedCount ? C_DOT_FILLED : C_DOT_EMPTY, elixirAlpha);
            drawDiamond(g, dotCX, dotCY, dotCol);
        }

        // Loaded ingredient list
        int dotRows    = capacity > 0 ? ((capacity - 1) / dotsPerRow + 1) : 0;
        int listStartY = dotStartY + dotRows * 7 + 5;

        List<ItemStack> loaded = new ArrayList<>();
        loaded.addAll(elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of()));
        loaded.addAll(elixir.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of()));
        loaded.addAll(elixir.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of()));

        int curY    = listStartY;
        int listBot = py + CLEAR_LABEL_GUI_Y - 10;
        for (ItemStack stack : loaded) {
            if (curY + 9 > listBot) break;
            IngredientType type = IngredientType.of(stack);
            Component name      = resolveDisplayName(stack, type);
            g.drawString(font, Component.literal("- ").append(name),
                         px + LP_X, curY, withAlpha(C_LEFT_TEXT, elixirAlpha), false);
            curY += 9;
        }

        // [Clear] — only if elixir has content
        if (hasContent) {
            Component clearLabel = confirmPending
                    ? Component.literal("[Confirm?]") : Component.literal("[Clear]");
            boolean hoverClear = isOverLabel(mouseX, mouseY, clearScreenX, clearScreenY,
                                             font.width(clearLabel.getString()));
            int clearColor = confirmPending ? C_CLEAR_CONFIRM
                           : hoverClear     ? C_CLEAR_HOVER
                           :                  C_CLEAR_IDLE;
            g.drawString(font, clearLabel, clearScreenX, clearScreenY,
                         withAlpha(clearColor, elixirAlpha), false);
        }

        // ── Right panel (only when not at capacity) ────────────────────────────
        if (!atCapacity) {
            // [Add] label
            boolean canAdd  = cachedValidation == AthanorMenu.ValidationResult.CAN_ADD;
            boolean hoverAdd = isOverLabel(mouseX, mouseY, addScreenX, addScreenY, font.width("[Add]"));
            int addColor = !canAdd   ? C_ADD_DISABLED
                         : hoverAdd  ? C_ADD_HOVER
                         :             C_ADD_IDLE;
            g.drawString(font, Component.literal("[Add]"), addScreenX, addScreenY,
                         withAlpha(addColor, elixirAlpha), false);

            // Tooltip when [Add] is hovered but disabled
            if (!canAdd && hoverAdd) {
                Component reason = validationMessage(cachedValidation);
                if (reason != null) g.renderTooltip(font, reason, mouseX, mouseY);
            }

            // Ingredient info (fades in when ingredient is placed)
            float ingP = easeOut(ingredientAnimStart);
            if (ingP > 0f) {
                int ingAlpha = (int)(ingP * 255);
                ItemStack ingredient = menu.getIngredientStack();
                if (!ingredient.isEmpty()) {
                    IngredientType type = IngredientType.of(ingredient);
                    int rpX  = px + RP_X;
                    int infoY = py + RP_INFO_Y;

                    // Ingredient name — prominent, first
                    g.drawString(font, resolveDisplayName(ingredient, type), rpX, infoY,
                                 withAlpha(C_INGR_NAME, ingAlpha), false);

                    // Type label — slightly smaller (75 % scale) beneath the name
                    String typeLbl = switch (type) {
                        case ESSENCE_STONE -> "ESSENCE STONE";
                        case TINCTURE      -> "TINCTURE";
                        case CATALYST      -> "CATALYST";
                        default            -> "";
                    };
                    int typeY = infoY + 11;
                    g.pose().pushPose();
                    g.pose().translate(rpX, typeY, 0);
                    g.pose().scale(0.75f, 0.75f, 1f);
                    g.drawString(font, typeLbl, 0, 0, withAlpha(C_TYPE_LABEL, ingAlpha), false);
                    g.pose().popPose();

                    // Stats — below the scaled type label (~8px tall at 0.75 scale)
                    List<Component> stats = ingredientStats(ingredient, type);
                    int statY = typeY + 10;
                    for (Component stat : stats) {
                        if (statY + 9 > py + PANEL_BOT) break;
                        g.drawString(font, stat, rpX, statY,
                                     withAlpha(C_STATS, ingAlpha), false);
                        statY += 9;
                    }
                }
            }
        }
    }

    // ── Mouse input ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ItemStack elixir   = menu.getElixirStack();
            boolean hasElixir  = !elixir.isEmpty() && elixir.getItem() instanceof IElixir;
            if (hasElixir) {
                int capacity    = ((IElixir) elixir.getItem()).getCapacity();
                int loadedCount = ((IElixir) elixir.getItem()).getLoadedCount(elixir);
                boolean atCap   = loadedCount >= capacity;

                // [Add]
                if (!atCap && isOverLabel(mouseX, mouseY, addScreenX, addScreenY, font.width("[Add]"))
                        && cachedValidation == AthanorMenu.ValidationResult.CAN_ADD) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    return true;
                }

                // [Clear]
                if (loadedCount > 0) {
                    Component clearLabel = confirmPending
                            ? Component.literal("[Confirm?]") : Component.literal("[Clear]");
                    if (isOverLabel(mouseX, mouseY, clearScreenX, clearScreenY,
                                   font.width(clearLabel.getString()))) {
                        handleClearClick();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Clear two-step ────────────────────────────────────────────────────────

    private void handleClearClick() {
        long now = System.currentTimeMillis();
        if (confirmPending && now - confirmAt < CONFIRM_TTL) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
            confirmPending = false;
        } else {
            confirmPending = true;
            confirmAt = now;
        }
    }

    @Override
    public void onClose() {
        confirmPending = false;
        super.onClose();
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    /** L-shaped corner markers on an 18×18 slot border area. (x,y) = top-left of border. */
    private void drawSlotCorners(GuiGraphics g, int x, int y, int color) {
        int s = CORNER;
        int e = 17; // 18px slot - 1
        // Top-left
        g.fill(x,         y,         x + s, y + 1,     color);
        g.fill(x,         y + 1,     x + 1, y + s,     color);
        // Top-right
        g.fill(x + e - s, y,         x + e + 1, y + 1, color);
        g.fill(x + e,     y + 1,     x + e + 1, y + s, color);
        // Bottom-left
        g.fill(x,         y + e,     x + s, y + e + 1, color);
        g.fill(x,         y + e - s + 1, x + 1, y + e, color);
        // Bottom-right
        g.fill(x + e - s, y + e,     x + e + 1, y + e + 1, color);
        g.fill(x + e,     y + e - s + 1, x + e + 1, y + e, color);
    }

    /** 3×3 pixel diamond centred at (cx, cy). */
    private void drawDiamond(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx + 1, cy,     cx + 2, cy + 1, color);
        g.fill(cx,     cy + 1, cx + 3, cy + 2, color);
        g.fill(cx + 1, cy + 2, cx + 2, cy + 3, color);
    }

    /**
     * Draws a 1-pixel grid of (cols × rows) slots, each 18×18 pixels.
     * Produces cols+1 vertical lines and rows+1 horizontal lines.
     */
    private void drawSlotGrid(GuiGraphics g, int startX, int startY, int cols, int rows, int color) {
        int totalW = cols * 18;
        int totalH = rows * 18;
        // Horizontal lines (top edge of each row + bottom edge of last row)
        for (int row = 0; row <= rows; row++) {
            g.fill(startX, startY + row * 18, startX + totalW, startY + row * 18 + 1, color);
        }
        // Vertical lines (left edge of each column + right edge of last column)
        for (int col = 0; col <= cols; col++) {
            g.fill(startX + col * 18, startY, startX + col * 18 + 1, startY + totalH, color);
        }
    }

    /** 1-pixel solid border around a rect. */
    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y + 1,     x + 1,     y + h - 1, color);
        g.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private float easeOut(long startMs) {
        if (startMs == 0L) return 0f;
        float t = Math.min(1f, (System.currentTimeMillis() - startMs) / (float) ANIM_MS);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    // ── Hit test ──────────────────────────────────────────────────────────────

    private boolean isOverLabel(double mx, double my, int lx, int ly, int lw) {
        return mx >= lx && mx < lx + lw && my >= ly && my < ly + 9;
    }

    // ── Ingredient stats ──────────────────────────────────────────────────────

    private static List<Component> ingredientStats(ItemStack stack, IngredientType type) {
        return switch (type) {
            case ESSENCE_STONE -> {
                ResourceLocation stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                if (stoneType == null) yield List.of();
                yield ClientIngredientData.getStone(stoneType)
                        .map(AthanorScreen::stoneStats)
                        .orElse(List.of());
            }
            case TINCTURE -> ClientIngredientData.getTincture(stack.getItem())
                    .map(d -> modifierStats(d.effectDurationMultiplier(), d.effectDurationFlat(),
                                           d.effectLevelModifier(),
                                           d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse(List.of());
            case CATALYST -> ClientIngredientData.getCatalyst(stack.getItem())
                    .map(d -> modifierStats(d.effectDurationMultiplier(), d.effectDurationFlat(),
                                           d.effectLevelModifier(),
                                           d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse(List.of());
            default -> List.of();
        };
    }

    private static List<Component> stoneStats(EssenceStoneDefinition def) {
        List<Component> lines = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.getOptional(def.effect()).ifPresent(effect ->
                lines.add(Component.literal("Effect: " + effect.getDisplayName().getString())));
        lines.add(Component.literal("Duration: " + def.baseDuration() + "t"));
        lines.add(Component.literal("Level: " + toRoman(def.baseLevel())));
        if (def.elixirCooldownMultiplier() != 1.0f)
            lines.add(Component.literal(String.format("Cooldown: \u00d7%.2f", def.elixirCooldownMultiplier())));
        if (def.elixirCooldownFlat() != 0)
            lines.add(Component.literal("Cooldown: +" + def.elixirCooldownFlat() + "t"));
        return lines;
    }

    private static List<Component> modifierStats(float durMult, int durFlat, int levelMod,
                                                  float cdMult, int cdFlat) {
        List<Component> lines = new ArrayList<>();
        if (durMult  != 1.0f) lines.add(Component.literal(String.format("Duration: \u00d7%.2f", durMult)));
        if (durFlat  != 0)    lines.add(Component.literal("Duration: +" + durFlat + "t"));
        if (levelMod != 0)    lines.add(Component.literal("Level: +" + levelMod));
        if (cdMult   != 1.0f) lines.add(Component.literal(String.format("Cooldown: \u00d7%.2f", cdMult)));
        if (cdFlat   != 0)    lines.add(Component.literal("Cooldown: +" + cdFlat + "t"));
        return lines;
    }

    // ── Display name resolution ────────────────────────────────────────────────

    private static Component resolveDisplayName(ItemStack stack, IngredientType type) {
        Optional<String> name = switch (type) {
            case TINCTURE      -> ClientIngredientData.getTincture(stack.getItem()).flatMap(d -> d.name());
            case ESSENCE_STONE -> {
                ResourceLocation st = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                yield st != null
                        ? ClientIngredientData.getStone(st).flatMap(EssenceStoneDefinition::name)
                        : Optional.empty();
            }
            case CATALYST      -> ClientIngredientData.getCatalyst(stack.getItem()).flatMap(d -> d.name());
            default            -> Optional.empty();
        };
        return name.<Component>map(Component::literal).orElseGet(stack::getHoverName);
    }

    // ── Validation tooltip ────────────────────────────────────────────────────

    private static Component validationMessage(AthanorMenu.ValidationResult r) {
        return switch (r) {
            case NO_ELIXIR          -> Component.translatable("gui.alchemical.athanor.no_elixir");
            case NO_INGREDIENT      -> Component.translatable("gui.alchemical.athanor.no_ingredient");
            case INVALID_INGREDIENT -> Component.translatable("gui.alchemical.athanor.invalid_ingredient");
            case AT_CAPACITY        -> Component.translatable("gui.alchemical.athanor.at_capacity");
            case CAN_ADD            -> null;
        };
    }

    // ── Roman numeral helper ──────────────────────────────────────────────────

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    // ── renderLabels override ─────────────────────────────────────────────────

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // Centre title above the elixir slot, nudged up slightly
        int titleX = ELIXIR_SLOT_X + 9 - font.width(this.title) / 2;
        g.drawString(font, this.title, titleX, 3, 0xFFCCBB88, false);
        // "Inventory" label intentionally omitted
    }
}
