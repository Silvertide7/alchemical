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
import net.silvertide.alchemical.util.ElixirCalcUtil;
import net.silvertide.alchemical.util.FormattingUtil;
import net.silvertide.alchemical.util.IngredientUtil;
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

    // Left info panel (elixir name + unified overview/ingredient list)
    private static final int LP_X       = 1;
    private static final int LP_RIGHT   = 66;
    private static final int LP_CONT_X  = LP_X + 2;  // content column start (x=3)

    // Right ingredient panel (ingredient slot + [Add] + stats)
    // Only rendered when elixir has room
    private static final int RP_X      = 101;
    private static final int RP_RIGHT  = 172;

    // Panel vertical extents
    private static final int PANEL_TOP = 8;
    private static final int PANEL_BOT = 130;  // above player inventory title (~140)

    // Content start Y (below title bar)
    private static final int CONTENT_Y = 25;

    // Corner marker size
    private static final int CORNER = 4;

    // Label positions (GUI-relative)
    // [Empty Elixir] — anchored to bottom of left panel, centered
    private static final int CLEAR_LABEL_GUI_Y = PANEL_BOT - 14;
    // [Add] — centred below the ingredient slot
    private static final int ADD_LABEL_GUI_Y   = INGREDIENT_SLOT_Y + 20;  // 2px gap below slot

    // Right-of-slot info area (ingredient name, type, stats) — left-aligned
    private static final int RP_INFO_X         = INGREDIENT_SLOT_X + 22;  // 4px gap after slot right edge (slot ends at +18)

    // ── Colors ────────────────────────────────────────────────────────────────

    private static final int C_ELIXIR_CORNER     = 0xFF886644;   // dim — always drawn
    private static final int C_ELIXIR_CORNER_LIT = 0xFFFFDD77;   // bright gold when elixir present
    private static final int C_INGR_CORNER        = 0xFF446688;   // dim — fades in with elixir
    private static final int C_INGR_CORNER_LIT    = 0xFF88CCFF;   // bright cyan when ingredient placed
    private static final int C_LINE               = 0xFF443322;   // dim base
    private static final int C_LINE_LIT           = 0xFFBB8844;   // warm highlight when ingredient present
    private static final int C_ELIXIR_NAME   = 0xFFCCBB88;   // also used for info icon hover
    private static final int C_LEFT_TEXT     = 0xFF999988;   // also used for info icon default
    private static final int C_CLEAR_IDLE    = 0xFF994444;
    private static final int C_CLEAR_HOVER   = 0xFFCC6655;
    private static final int C_CLEAR_CONFIRM = 0xFFDD4422;
    private static final int C_ADD_IDLE      = 0xFF7799AA;
    private static final int C_ADD_HOVER     = 0xFFCCEEFF;
    private static final int C_ADD_DISABLED  = 0xFF444444;
    private static final int C_TYPE_LABEL    = 0xFF7788AA;
    private static final int C_INGR_NAME     = 0xFFDDDDDD;
    private static final int C_STATS         = 0xFFCCCCBB;
    // C_LEFT_TEXT = C_LEFT_TEXT, C_ELIXIR_NAME = C_ELIXIR_NAME (deduplicated)
    private static final int C_DOT_FILLED    = 0xFFCC8800;
    private static final int C_DOT_EMPTY     = 0xFF665544;
    private static final int C_DOT_PREVIEW   = 0xFF44AAFF;   // staged-ingredient slot preview (fits)
    private static final int C_DOT_OVERFLOW  = 0xFFCC2222;   // staged-ingredient slot preview (no room)
    private static final int C_WARNING       = 0xFFDD4422;   // not-enough-room warning text
    private static final int C_SLOT_GRID     = 0xFF667788;   // inventory slot grid lines

    // ── Animation ─────────────────────────────────────────────────────────────

    private static final long ANIM_MS = 350L;

    private boolean prevHasElixir      = false;
    private boolean prevHasIngredient  = false;
    private long elixirAnimStartMs       = 0L;
    private long ingredientAnimStartMs   = 0L;

    // ── Confirm-clear state ───────────────────────────────────────────────────

    private boolean confirmPending     = false;
    private long confirmStartMs             = 0L;
    private static final long CONFIRM_TTL = 3000L;

    // ── Dot hover state (updated each render frame) ───────────────────────────

    private int dotAreaCols = 1;
    private int dotAreaRows    = 0;
    private int dotAreaX = 0;
    private int dotAreaY = 0;

    // ── Info icon position (updated each render frame) ────────────────────────

    private int infoIconX, infoIconY;

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
        // Shift the GUI a bit to the left — there's extra room on the right
        leftPos -= 10;
        updateLabelPositions();
    }

    private void updateLabelPositions() {
        // [Add] centred under the ingredient slot (18px wide), nudged 1px left
        String addText = Component.translatable("gui.alchemical.athanor.add").getString();
        int addBtnW  = (int)(font.width(addText) * 0.8f) + 6;
        addScreenX   = leftPos + INGREDIENT_SLOT_X + 9 - addBtnW / 2 - 1;
        addScreenY   = topPos  + ADD_LABEL_GUI_Y;
        // [Empty Elixir] centred in the left panel
        String clearText = Component.translatable("gui.alchemical.athanor.clear").getString();
        int clearBtnW = (int)(font.width(clearText) * 0.8f) + 6;
        int lpCenter  = leftPos + LP_X + (LP_RIGHT - LP_X) / 2;
        clearScreenX  = lpCenter - clearBtnW / 2;
        clearScreenY  = topPos  + CLEAR_LABEL_GUI_Y;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    protected void containerTick() {
        super.containerTick();

        ItemStack elixir = menu.getElixirStack();
        boolean hasElixir = !elixir.isEmpty() && elixir.getItem() instanceof IElixir;

        if (hasElixir && !prevHasElixir) elixirAnimStartMs = System.currentTimeMillis();
        if (!hasElixir) elixirAnimStartMs = 0L;
        prevHasElixir = hasElixir;

        boolean hasIngredient = !menu.getIngredientStack().isEmpty();
        if (hasIngredient && !prevHasIngredient) ingredientAnimStartMs = System.currentTimeMillis();
        if (!hasIngredient) ingredientAnimStartMs = 0L;
        prevHasIngredient = hasIngredient;

        cachedValidation = menu.canAddIngredient();

        if (confirmPending && System.currentTimeMillis() - confirmStartMs >= CONFIRM_TTL) {
            confirmPending = false;
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int px = leftPos;
        int py = topPos;

        // ── Elixir slot corners ────────────────────────────────────────────────
        // Dim base always visible; bright overlay fades in when elixir is placed
        float elixirP   = easeOut(elixirAnimStartMs);
        int   elixirLit = (int)(elixirP * 255);
        drawSlotCorners(g, px + ELIXIR_SLOT_X - 1, py + ELIXIR_SLOT_Y - 1, C_ELIXIR_CORNER);
        if (elixirLit > 0) {
            drawSlotCorners(g, px + ELIXIR_SLOT_X - 1, py + ELIXIR_SLOT_Y - 1,
                            withAlpha(C_ELIXIR_CORNER_LIT, elixirLit));
        }

        // ── Inventory slot grid (no fill background) ──────────────────────────
        // 3×9 main inventory (slot positions: x=8, y=152 — grid offset 1px out)
        drawSlotGrid(g, px + 7, py + 151, 9, 3, C_SLOT_GRID);
        // 1×9 hotbar (x=8, y=210)
        drawSlotGrid(g, px + 7, py + 209, 9, 1, C_SLOT_GRID);

        // ── Right panel + line (only when elixir slot is truly full or absent) ──
        // Use loadedCount >= capacity rather than cachedValidation so the line
        // and corners stay visible when an ingredient is staged but too large to fit.
        ItemStack elixirBg = menu.getElixirStack();
        boolean atCapacity = elixirBg.isEmpty()
                          || !(elixirBg.getItem() instanceof IElixir bgElixir)
                          || bgElixir.getLoadedCount(elixirBg) >= bgElixir.getCapacity();

        if (!atCapacity && elixirLit > 0) {
            int lineY      = py + ELIXIR_SLOT_Y + 8;
            int lineStartX = px + ELIXIR_SLOT_X + 18;
            int lineEndX   = px + INGREDIENT_SLOT_X - 2;
            int lineCurX   = lineStartX + (int)((lineEndX - lineStartX) * elixirP);

            // Dim base ingredient corners (always visible when slot is available)
            drawSlotCorners(g, px + INGREDIENT_SLOT_X - 1, py + INGREDIENT_SLOT_Y - 1,
                            withAlpha(C_INGR_CORNER, elixirLit));

            // Line + bright corner overlays only appear once an ingredient is staged
            float ingP   = easeOut(ingredientAnimStartMs);
            int   ingLit = (int)(ingP * 255);
            if (ingLit > 0) {
                boolean tooLargeBg = cachedValidation == AthanorMenu.ValidationResult.AT_CAPACITY;
                int litLine   = tooLargeBg ? C_DOT_OVERFLOW : C_LINE_LIT;
                int litCorner = tooLargeBg ? C_DOT_OVERFLOW : C_INGR_CORNER_LIT;
                g.fill(lineStartX, lineY, lineCurX, lineY + 1, withAlpha(litLine, ingLit));
                drawSlotCorners(g, px + INGREDIENT_SLOT_X - 1, py + INGREDIENT_SLOT_Y - 1,
                                withAlpha(litCorner, ingLit));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        ItemStack elixir = menu.getElixirStack();
        boolean hasElixir = !elixir.isEmpty() && elixir.getItem() instanceof IElixir;
        if (!hasElixir) {
            // Hint text below the empty elixir slot
            Component hint = Component.translatable("gui.alchemical.athanor.place_elixir");
            int hintX = leftPos + ELIXIR_SLOT_X + 9 - (int)(font.width(hint) * 0.75f) / 2;
            int hintY = topPos + ELIXIR_SLOT_Y + 22;
            drawScaledString(g, hint, hintX, hintY, 0.75f, C_LEFT_TEXT);
            return;
        }

        IElixir iElixir     = (IElixir) elixir.getItem();
        int loadedCount     = iElixir.getLoadedCount(elixir);
        boolean atCapacity  = loadedCount >= iElixir.getCapacity();
        boolean hasContent  = loadedCount > 0;

        float elixirP = easeOut(elixirAnimStartMs);
        if (elixirP <= 0f) return;
        int elixirAlpha = (int)(elixirP * 255);

        renderLeftPanel(g, mouseX, mouseY, elixir, iElixir, elixirP, elixirAlpha, hasContent);

        if (!atCapacity) {
            renderRightPanel(g, mouseX, mouseY, elixirAlpha);
        }

        renderHoverTooltips(g, mouseX, mouseY, elixirP, elixir, hasElixir, hasContent);
    }

    // ── Left panel rendering ─────────────────────────────────────────────────

    private void renderLeftPanel(GuiGraphics g, int mouseX, int mouseY,
                                  ItemStack elixir, IElixir iElixir,
                                  float elixirP, int elixirAlpha, boolean hasContent) {
        int px = leftPos;
        int py = topPos;

        // Slide in from the left as the elixir animation plays
        float lpSlide = (1f - elixirP) * 14f;
        g.pose().pushPose();
        g.pose().translate(-lpSlide, 0, 0);

        // Elixir name — left-aligned in the left panel
        drawScaledString(g, elixir.getHoverName(), px + LP_CONT_X, py + CONTENT_Y,
                0.875f, withAlpha(C_ELIXIR_NAME, elixirAlpha));

        // Separator line beneath the name
        int nameSepY = py + CONTENT_Y + 9;
        g.fill(px + LP_CONT_X, nameSepY, px + LP_RIGHT - 2, nameSepY + 1,
               withAlpha(C_ELIXIR_NAME, elixirAlpha / 3));

        // ⓘ info icon — right edge of left panel, below the name
        if (hasContent) {
            infoIconX = px + LP_RIGHT - 8;
            infoIconY = py + CONTENT_Y;
            boolean hoverInfo = mouseX >= infoIconX && mouseX < infoIconX + 7
                             && mouseY >= infoIconY && mouseY < infoIconY + 7;
            int iconColor = withAlpha(hoverInfo ? C_ELIXIR_NAME : C_LEFT_TEXT, elixirAlpha);
            drawInfoIcon(g, infoIconX, infoIconY, iconColor);
        }

        // Capacity dots + overview
        int contentStartY = py + CONTENT_Y + 13;
        int listBot       = py + CLEAR_LABEL_GUI_Y - 3;

        List<ItemStack> tinctures = elixir.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> catalysts = elixir.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        int curY = renderCapacityDots(g, px, contentStartY, elixirAlpha, iElixir, elixir);

        curY = drawOverviewTab(g, px, curY, listBot, elixirAlpha, elixir, iElixir,
                        tinctures, catalysts);

        // [Empty Elixir] / [Confirm?] — only if elixir has content
        if (hasContent) {
            String clearText = confirmPending
                    ? Component.translatable("gui.alchemical.athanor.clear_confirm").getString()
                    : Component.translatable("gui.alchemical.athanor.clear").getString();
            boolean hoverClear = isOverButton(mouseX, mouseY, clearScreenX, clearScreenY,
                                              font.width(clearText));
            int clearColor = confirmPending ? C_CLEAR_CONFIRM
                           : hoverClear     ? C_CLEAR_HOVER
                           :                  C_CLEAR_IDLE;
            drawButton(g, clearText, clearScreenX, clearScreenY, clearColor, elixirAlpha);
        }

        g.pose().popPose(); // end left-panel slide
    }

    // ── Capacity dots ────────────────────────────────────────────────────────

    /**
     * Draws the capacity dot grid and staged-ingredient preview dots.
     * Returns the Y position below the dots for subsequent content.
     */
    private int renderCapacityDots(GuiGraphics g, int px, int contentStartY,
                                    int elixirAlpha, IElixir iElixir, ItemStack elixir) {
        int capacity    = iElixir.getCapacity();
        int loadedCount = iElixir.getLoadedCount(elixir);
        boolean atCap   = loadedCount >= capacity;

        int dotStartX  = px + LP_CONT_X;
        int dotStartY  = contentStartY;
        int dotsPerRow = 15;
        int dotSpacing = 8;
        int maxRows    = 3;
        int dotRows    = capacity > 0 ? Math.min(maxRows, (capacity - 1) / dotsPerRow + 1) : 0;
        dotAreaCols = dotsPerRow;
        dotAreaRows    = dotRows;
        dotAreaX = dotStartX;
        dotAreaY = dotStartY;

        int maxDots   = dotsPerRow * maxRows;
        int drawCount = Math.min(capacity, maxDots);
        for (int i = 0; i < drawCount; i++) {
            int color = withAlpha(i < loadedCount ? C_DOT_FILLED : C_DOT_EMPTY, elixirAlpha);
            drawDiamond(g, dotStartX + (i % dotsPerRow) * dotSpacing,
                           dotStartY + (i / dotsPerRow) * dotSpacing, color);
        }

        if (!atCap) {
            float ingPrev  = easeOut(ingredientAnimStartMs);
            int   ingPrevA = (int)(ingPrev * 255);
            if (ingPrevA > 0 && !menu.getIngredientStack().isEmpty()) {
                int potency      = IngredientUtil.getPotency(menu.getIngredientStack());
                boolean fits     = loadedCount + potency <= capacity;
                int previewColor = fits ? C_DOT_PREVIEW : C_DOT_OVERFLOW;
                for (int i = loadedCount; i < Math.min(loadedCount + potency, maxDots); i++) {
                    drawDiamond(g, dotStartX + (i % dotsPerRow) * dotSpacing,
                                   dotStartY + (i / dotsPerRow) * dotSpacing,
                                   withAlpha(previewColor, ingPrevA));
                }
            }
        }

        return dotStartY + dotRows * dotSpacing + 5;
    }

    // ── Right panel rendering ────────────────────────────────────────────────

    private void renderRightPanel(GuiGraphics g, int mouseX, int mouseY, int elixirAlpha) {
        int px = leftPos;
        int py = topPos;

        // [Add] button — centred below the ingredient slot
        String addText = Component.translatable("gui.alchemical.athanor.add").getString();
        boolean canAdd  = cachedValidation == AthanorMenu.ValidationResult.CAN_ADD;
        boolean hoverAdd = isOverButton(mouseX, mouseY, addScreenX, addScreenY, font.width(addText));
        int addColor = !canAdd   ? C_ADD_DISABLED
                     : hoverAdd  ? C_ADD_HOVER
                     :             C_ADD_IDLE;
        drawButton(g, addText, addScreenX, addScreenY, addColor, elixirAlpha);

        // Tooltip when [Add] is hovered but disabled
        if (!canAdd && hoverAdd) {
            Component reason = validationMessage(cachedValidation);
            if (reason != null) g.renderTooltip(font, reason, mouseX, mouseY);
        }

        // Ingredient info — to the right of the slot (fades + slides in)
        float ingP = easeOut(ingredientAnimStartMs);
        if (ingP <= 0f) return;

        int ingAlpha = (int)(ingP * 255);
        ItemStack ingredient = menu.getIngredientStack();
        if (ingredient.isEmpty()) return;

        IngredientType type = IngredientType.of(ingredient);
        int infoX = px + RP_INFO_X;

        // Slide in from the left alongside the fade
        float rpSlide = (1f - ingP) * 12f;
        g.pose().pushPose();
        g.pose().translate(-rpSlide, 0, 0);

        // Ingredient name
        Component displayName = resolveDisplayName(ingredient, type);
        int nameY = py + INGREDIENT_SLOT_Y + 5;
        drawScaledString(g, displayName, infoX, nameY, 0.875f, withAlpha(C_INGR_NAME, ingAlpha));

        // Type label
        String typeLbl = switch (type) {
            case ESSENCE_STONE -> Component.translatable("gui.alchemical.athanor.type.essence_stone").getString();
            case TINCTURE      -> Component.translatable("gui.alchemical.athanor.type.tincture").getString();
            case CATALYST      -> Component.translatable("gui.alchemical.athanor.type.catalyst").getString();
            default            -> "";
        };
        int typeY = nameY + 10;
        drawScaledString(g, typeLbl, infoX, typeY, 0.75f, withAlpha(C_TYPE_LABEL, ingAlpha));

        // Stats
        List<Component> stats = ingredientStats(ingredient, type);
        int statY = typeY + 10;
        for (Component stat : stats) {
            if (statY + 8 > py + PANEL_BOT) break;
            drawScaledString(g, stat, infoX, statY, 0.75f, withAlpha(C_STATS, ingAlpha));
            statY += 8;
        }

        // Warning when the ingredient is too large to fit
        boolean tooLarge = cachedValidation == AthanorMenu.ValidationResult.AT_CAPACITY;
        if (tooLarge && statY + 9 <= py + PANEL_BOT) {
            g.drawString(font,
                    Component.translatable("gui.alchemical.athanor.no_room"),
                    infoX, statY, withAlpha(C_WARNING, ingAlpha), false);
        }

        g.pose().popPose(); // end ingredient info slide
    }

    // ── Hover tooltips (rendered last so they sit on top) ─────────────────────

    private void renderHoverTooltips(GuiGraphics g, int mouseX, int mouseY,
                                      float elixirP, ItemStack elixir,
                                      boolean hasElixir, boolean hasContent) {
        boolean settled = elixirP >= 1f;

        // Dot-area tooltip: "Potency: X / Y"
        if (settled && hasElixir && dotAreaRows > 0) {
            if (mouseX >= dotAreaX && mouseX < dotAreaX + dotAreaCols * 8
                    && mouseY >= dotAreaY && mouseY < dotAreaY + dotAreaRows * 8) {
                IElixir ie = (IElixir) menu.getElixirStack().getItem();
                g.renderTooltip(font, Component.translatable("gui.alchemical.athanor.capacity",
                        ie.getLoadedCount(menu.getElixirStack()), ie.getCapacity()), mouseX, mouseY);
            }
        }

        // ⓘ info icon tooltip — full ingredient breakdown
        if (settled && hasContent) {
            if (mouseX >= infoIconX && mouseX < infoIconX + 7
                    && mouseY >= infoIconY && mouseY < infoIconY + 7) {
                g.renderComponentTooltip(font, buildIngredientTooltip(elixir), mouseX, mouseY);
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

                // Add button
                String addLabel = Component.translatable("gui.alchemical.athanor.add").getString();
                if (!atCap && isOverButton(mouseX, mouseY, addScreenX, addScreenY, font.width(addLabel))
                        && cachedValidation == AthanorMenu.ValidationResult.CAN_ADD) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    return true;
                }

                // Clear button
                if (loadedCount > 0) {
                    String clearText = confirmPending
                            ? Component.translatable("gui.alchemical.athanor.clear_confirm").getString()
                            : Component.translatable("gui.alchemical.athanor.clear").getString();
                    if (isOverButton(mouseX, mouseY, clearScreenX, clearScreenY,
                                     font.width(clearText))) {
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
        if (confirmPending && now - confirmStartMs < CONFIRM_TTL) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
            confirmPending = false;
        } else {
            confirmPending = true;
            confirmStartMs = now;
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

    /** 5×5 pixel diamond centred at (cx+2, cy+2). */
    private void drawDiamond(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx + 2, cy,     cx + 3, cy + 1, color);   // top
        g.fill(cx + 1, cy + 1, cx + 4, cy + 2, color);   // row 2
        g.fill(cx,     cy + 2, cx + 5, cy + 3, color);    // middle (widest)
        g.fill(cx + 1, cy + 3, cx + 4, cy + 4, color);   // row 4
        g.fill(cx + 2, cy + 4, cx + 3, cy + 5, color);   // bottom
    }

    /**
     * Draws a 1-pixel grid of (cols × rows) slots, each 18×18 pixels.
     * Produces cols+1 vertical lines and rows+1 horizontal lines.
     */
    private void drawSlotGrid(GuiGraphics g, int startX, int startY, int cols, int rows, int color) {
        int totalW = cols * 18 + 1;  // +1 to include the rightmost vertical line pixel
        int totalH = rows * 18 + 1;  // +1 to include the bottommost horizontal line pixel
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

    /**
     * Draws a bordered button with 0.8× scaled text.
     * Layout: 1px border + 2px pad on all sides.
     * Total size: (font.width(text) × 0.8 + 6) × 13.
     */
    private void drawButton(GuiGraphics g, String text, int x, int y, int color, int alpha) {
        int btnW = (int)(font.width(text) * 0.8f) + 6;
        drawBorder(g, x, y, btnW, 13, withAlpha(color, alpha));
        g.pose().pushPose();
        g.pose().translate(x + 3, y + 3, 0);
        g.pose().scale(0.8f, 0.8f, 1f);
        g.drawString(font, text, 0, 0, withAlpha(color, alpha), false);
        g.pose().popPose();
    }

    /** Draws a string at (x, y) scaled by the given factor. */
    private void drawScaledString(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Draws a Component at (x, y) scaled by the given factor. */
    private void drawScaledString(GuiGraphics g, Component text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Hit-test for a bordered button drawn by drawButton(). */
    private boolean isOverButton(double mx, double my, int x, int y, int textWidth) {
        return mx >= x && mx < x + (int)(textWidth * 0.8f) + 6 && my >= y && my < y + 13;
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

    // ── Left panel section drawing ────────────────────────────────────────────

    /**
     * Draws the Overview tab — one block per essence stone showing its effective
     * output (effect + level, duration, cooldown) after applying all tincture and
     * catalyst modifiers. The active stone is highlighted in gold with "[Active]".
     */
    private int drawOverviewTab(GuiGraphics g, int px, int curY, int listBot, int alpha,
                                 ItemStack elixir, IElixir iElixir,
                                 List<ItemStack> tinctures, List<ItemStack> catalysts) {
        List<ItemStack> stones = elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        if (stones.isEmpty()) {
            if (curY + 9 <= listBot) {
                g.drawString(font, Component.translatable("gui.alchemical.athanor.none"),
                        px + LP_CONT_X, curY, withAlpha(C_LEFT_TEXT, alpha), false);
                curY += 9;
            }
            return curY;
        }

        int activeIndex = iElixir.getActiveStoneIndex(elixir);
        int baseCooldown = iElixir.getCooldownSeconds();

        // Pre-compute tincture + catalyst modifier totals (shared across all stones)
        ElixirCalcUtil.ModifierResult mods = ElixirCalcUtil.computeSharedModifiers(tinctures, catalysts);

        for (int i = 0; i < stones.size(); i++) {
            if (curY + 9 > listBot) break;
            ItemStack stoneStack = stones.get(i);
            var stoneDef = ElixirCalcUtil.resolveStone(stoneStack);
            if (stoneDef.isEmpty()) continue;
            var def = stoneDef.get();

            boolean isActive = (i == activeIndex);

            // Compute effective stats for this stone
            int finalDuration  = ElixirCalcUtil.computeEffectiveDuration(def, mods);
            int finalLevel     = ElixirCalcUtil.computeEffectiveLevel(def, mods);
            int finalCooldown  = ElixirCalcUtil.computeEffectiveCooldown(baseCooldown, def, mods);

            // Effect + Level line (full size, name color)
            String effectLine = BuiltInRegistries.MOB_EFFECT.getOptional(def.effect())
                    .map(e -> Component.translatable("gui.alchemical.athanor.stat.effect_name",
                            e.getDisplayName().getString(), FormattingUtil.toRoman(finalLevel)).getString())
                    .orElse(resolveDisplayName(stoneStack, IngredientType.ESSENCE_STONE).getString());

            // Effect + Level at 0.75 scale
            int nameColor = isActive ? C_ELIXIR_NAME : C_LEFT_TEXT;
            drawScaledString(g, effectLine, px + LP_CONT_X + 2, curY, 0.75f, withAlpha(nameColor, alpha));

            // "[Active]" suffix at 0.625 scale on the same line
            if (isActive) {
                String activeSuffix = Component.translatable("gui.alchemical.athanor.active").getString();
                int suffixX = px + LP_CONT_X + 2 + (int)(font.width(effectLine) * 0.75f) + 3;
                drawScaledString(g, activeSuffix, suffixX, curY + 1, 0.625f, withAlpha(C_TYPE_LABEL, alpha));
            }
            curY += 7;

            // Duration at 0.625 scale
            if (curY + 6 <= listBot) {
                String durationText = Component.translatable("gui.alchemical.athanor.stat.duration",
                        FormattingUtil.ticksToTime(finalDuration)).getString();
                drawScaledString(g, durationText,
                        px + LP_CONT_X + 4, curY, 0.625f, withAlpha(C_STATS, alpha));
                curY += 6;
            }
            // Cooldown at 0.625 scale
            if (curY + 6 <= listBot) {
                String cooldownText = Component.translatable("gui.alchemical.athanor.stat.cooldown",
                        FormattingUtil.secondsToTime(finalCooldown)).getString();
                drawScaledString(g, cooldownText,
                        px + LP_CONT_X + 4, curY, 0.625f, withAlpha(C_STATS, alpha));
                curY += 6;
            }

            // Gap between stones
            if (i < stones.size() - 1) curY += 2;
        }
        return curY;
    }

    /** Draws a small "i" inside a 7×7 pixel circle outline. */
    private void drawInfoIcon(GuiGraphics g, int x, int y, int color) {
        // 7×7 circle (rough pixel circle)
        g.fill(x + 2, y,     x + 5, y + 1, color);  // top edge
        g.fill(x + 1, y + 1, x + 2, y + 2, color);  // top-left
        g.fill(x + 5, y + 1, x + 6, y + 2, color);  // top-right
        g.fill(x,     y + 2, x + 1, y + 5, color);   // left edge
        g.fill(x + 6, y + 2, x + 7, y + 5, color);   // right edge
        g.fill(x + 1, y + 5, x + 2, y + 6, color);  // bottom-left
        g.fill(x + 5, y + 5, x + 6, y + 6, color);  // bottom-right
        g.fill(x + 2, y + 6, x + 5, y + 7, color);  // bottom edge
        // "i" character inside — dot at (3,2), stem at (3,3)-(3,4)
        g.fill(x + 3, y + 2, x + 4, y + 3, color);
        g.fill(x + 3, y + 3, x + 4, y + 5, color);
    }

    /**
     * Builds the full ingredient tooltip for the ⓘ icon.
     * Groups ingredients by type with headers, names, and stat lines.
     */
    private List<Component> buildIngredientTooltip(ItemStack elixir) {
        List<Component> lines = new ArrayList<>();
        List<ItemStack> stones    = elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> tinctures = elixir.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> catalysts = elixir.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        if (!stones.isEmpty()) {
            lines.add(Component.translatable("gui.alchemical.athanor.section.stones").withStyle(s -> s.withColor(C_TYPE_LABEL)));
            for (ItemStack stack : stones) {
                lines.add(Component.literal("  " + resolveDisplayName(stack, IngredientType.ESSENCE_STONE).getString()));
                for (Component stat : ingredientStats(stack, IngredientType.ESSENCE_STONE)) {
                    lines.add(Component.literal("    ").append(stat.copy().withStyle(s -> s.withColor(C_STATS))));
                }
            }
        }

        if (!tinctures.isEmpty()) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.add(Component.translatable("gui.alchemical.athanor.section.tinctures").withStyle(s -> s.withColor(C_TYPE_LABEL)));
            for (ItemStack stack : tinctures) {
                lines.add(Component.literal("  " + resolveDisplayName(stack, IngredientType.TINCTURE).getString()));
                for (Component stat : ingredientStats(stack, IngredientType.TINCTURE)) {
                    lines.add(Component.literal("    ").append(stat.copy().withStyle(s -> s.withColor(C_STATS))));
                }
            }
        }

        if (!catalysts.isEmpty()) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.add(Component.translatable("gui.alchemical.athanor.section.catalysts").withStyle(s -> s.withColor(C_TYPE_LABEL)));
            for (ItemStack stack : catalysts) {
                lines.add(Component.literal("  " + resolveDisplayName(stack, IngredientType.CATALYST).getString()));
                for (Component stat : ingredientStats(stack, IngredientType.CATALYST)) {
                    lines.add(Component.literal("    ").append(stat.copy().withStyle(s -> s.withColor(C_STATS))));
                }
            }
        }

        return lines;
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
                    .map(d -> modifierStats(d.potency(), d.effectDurationMultiplier(), d.effectDurationFlat(),
                                           d.effectLevelModifier(),
                                           d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse(List.of());
            case CATALYST -> ClientIngredientData.getCatalyst(stack.getItem())
                    .map(d -> modifierStats(d.potency(), d.effectDurationMultiplier(), d.effectDurationFlat(),
                                           d.effectLevelModifier(),
                                           d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse(List.of());
            default -> List.of();
        };
    }

    private static List<Component> stoneStats(EssenceStoneDefinition def) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.alchemical.athanor.stat.potency", def.potency()));
        BuiltInRegistries.MOB_EFFECT.getOptional(def.effect()).ifPresent(effect ->
                lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_name",
                        effect.getDisplayName().getString(), FormattingUtil.toRoman(def.baseLevel()))));
        lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration",
                FormattingUtil.ticksToTime(def.baseDuration())));
        if (def.elixirCooldownMultiplier() != 1.0f)
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_mult",
                    String.format("%.2f", def.elixirCooldownMultiplier())));
        if (def.elixirCooldownFlat() != 0) {
            String sign = def.elixirCooldownFlat() > 0 ? "+" : "";
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_flat",
                    sign + def.elixirCooldownFlat() + "s"));
        }
        return lines;
    }

    private static List<Component> modifierStats(int potency, float durMult, int durFlat, int levelMod,
                                                  float cdMult, int cdFlat) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.alchemical.athanor.stat.potency", potency));
        if (durMult  != 1.0f) lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration_mult",
                String.format("%.2f", durMult)));
        if (durFlat  != 0) {
            String sign = durFlat > 0 ? "+" : "";
            lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration_flat",
                    sign + FormattingUtil.ticksToTime(durFlat)));
        }
        if (levelMod != 0) {
            String sign = levelMod > 0 ? "+" : "";
            lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_level", sign + levelMod));
        }
        if (cdMult   != 1.0f) lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_mult",
                String.format("%.2f", cdMult)));
        if (cdFlat   != 0) {
            String sign = cdFlat > 0 ? "+" : "";
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_flat",
                    sign + cdFlat + "s"));
        }
        return lines;
    }

    // Time/Roman formatting delegated to FormattingUtil

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
            case DUPLICATE_STONE    -> Component.translatable("gui.alchemical.athanor.duplicate_stone");
            case NEEDS_STONE        -> Component.translatable("gui.alchemical.athanor.needs_stone");
            case NEEDS_TINCTURE     -> Component.translatable("gui.alchemical.athanor.needs_tincture");
            case MAX_STONES         -> Component.translatable("gui.alchemical.athanor.max_stones");
            case CAN_ADD            -> null;
        };
    }


    // ── renderLabels override ─────────────────────────────────────────────────

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // Centre title above the elixir slot, nudged up slightly
        int titleX = ELIXIR_SLOT_X + 9 - font.width(this.title) / 2;
        g.drawString(font, this.title, titleX, -7, 0xFFCCBB88, false);
        // "Inventory" label intentionally omitted
    }
}
