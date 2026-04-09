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
import net.silvertide.alchemical.util.IngredientUtil;
import net.silvertide.alchemical.menu.AthanorMenu;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import com.mojang.math.Axis;
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

    // Left info panel (elixir name, vertical tabs, content)
    private static final int LP_X       = 1;
    private static final int LP_RIGHT   = 66;
    private static final int TAB_COL_W  = 11;   // width of the vertical tab sidebar
    private static final int LP_CONT_X  = LP_X + TAB_COL_W + 2;  // content column start (x=14)

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
    // [Clear] — anchored to bottom of left panel
    private static final int CLEAR_LABEL_GUI_X = LP_CONT_X;
    private static final int CLEAR_LABEL_GUI_Y = PANEL_BOT - 12;
    // [Add] — to the RIGHT of the ingredient slot, vertically centred on it
    private static final int ADD_LABEL_GUI_X   = INGREDIENT_SLOT_X + 20;  // 4px gap after slot
    private static final int ADD_LABEL_GUI_Y   = INGREDIENT_SLOT_Y + 5;   // (18-9)/2 ≈ centre

    // Right panel info — starts where [Add] used to sit (below the slot)
    private static final int RP_INFO_Y         = INGREDIENT_SLOT_Y + 22;

    // ── Colors ────────────────────────────────────────────────────────────────

    private static final int C_ELIXIR_CORNER     = 0xFF886644;   // dim — always drawn
    private static final int C_ELIXIR_CORNER_LIT = 0xFFFFDD77;   // bright gold when elixir present
    private static final int C_INGR_CORNER        = 0xFF446688;   // dim — fades in with elixir
    private static final int C_INGR_CORNER_LIT    = 0xFF88CCFF;   // bright cyan when ingredient placed
    private static final int C_LINE               = 0xFF443322;   // dim base
    private static final int C_LINE_LIT           = 0xFFBB8844;   // warm highlight when ingredient present
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
    private static final int C_STATS         = 0xFFCCCCBB;
    private static final int C_TAB_ACTIVE    = 0xFFCCBB88;
    private static final int C_TAB_INACTIVE  = 0xFF665544;
    private static final int C_DOT_FILLED    = 0xFFCC8800;
    private static final int C_DOT_EMPTY     = 0xFF332211;
    private static final int C_DOT_PREVIEW   = 0xFF44AAFF;   // staged-ingredient slot preview (fits)
    private static final int C_DOT_OVERFLOW  = 0xFFCC2222;   // staged-ingredient slot preview (no room)
    private static final int C_WARNING       = 0xFFDD4422;   // not-enough-room warning text
    private static final int C_SLOT_GRID     = 0xFF667788;   // inventory slot grid lines

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

    // ── Dot hover state (updated each render frame) ───────────────────────────

    private int lastDotsPerRow = 1;
    private int lastDotRows    = 0;
    private int lastDotScreenX = 0;
    private int lastDotScreenY = 0;

    // ── Left-panel hover lines (rebuilt each render frame) ───────────────────

    private final List<HoverLine> hoverLines = new ArrayList<>();
    private record HoverLine(int screenX, int screenY, int width, List<Component> tooltip) {}

    // ── Left panel tab ────────────────────────────────────────────────────────

    private enum LeftTab { INGREDIENTS, OVERVIEW }
    private LeftTab leftTab = LeftTab.OVERVIEW;

    // Screen-space positions for vertical tab column (set each render frame)
    private int tabColScreenX;
    private int ovTabScreenY,  ingTabScreenY;
    private int ovTabH,        ingTabH;

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

        // ── Elixir slot corners ────────────────────────────────────────────────
        // Dim base always visible; bright overlay fades in when elixir is placed
        float elixirP   = easeOut(elixirAnimStart);
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
            float ingP   = easeOut(ingredientAnimStart);
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
        // Slide in from the left as the elixir animation plays
        float lpSlide = (1f - elixirP) * 14f;
        g.pose().pushPose();
        g.pose().translate(-lpSlide, 0, 0);

        // Elixir name above the tab column, aligned to content column
        g.pose().pushPose();
        g.pose().translate(px + LP_CONT_X, py + CONTENT_Y, 0);
        g.pose().scale(0.875f, 0.875f, 1f);
        g.drawString(font, elixir.getHoverName(), 0, 0, withAlpha(C_ELIXIR_NAME, elixirAlpha), false);
        g.pose().popPose();

        // ── Vertical tab column ────────────────────────────────────────────────
        final float TAB_SCALE = 0.70f;
        final int   TAB_PAD   = 3;
        int tabTopY = py + CONTENT_Y + 11;
        int tabColX = px + LP_X;

        String ovLabel  = "Overview";
        String ingLabel = "Ingredients";
        int ovTextW  = (int)(font.width(ovLabel)  * TAB_SCALE);
        int ingTextW = (int)(font.width(ingLabel) * TAB_SCALE);
        ovTabH  = ovTextW  + TAB_PAD * 2;
        ingTabH = ingTextW + TAB_PAD * 2;

        int ovTabY  = tabTopY;
        int ingTabY = ovTabY + ovTabH + 3;

        tabColScreenX = tabColX;
        ovTabScreenY  = ovTabY;
        ingTabScreenY = ingTabY;

        int ovColor  = leftTab == LeftTab.OVERVIEW    ? C_TAB_ACTIVE : C_TAB_INACTIVE;
        int ingColor = leftTab == LeftTab.INGREDIENTS ? C_TAB_ACTIVE : C_TAB_INACTIVE;

        drawBorder(g, tabColX, ovTabY,  TAB_COL_W, ovTabH,  withAlpha(ovColor,  elixirAlpha));
        drawBorder(g, tabColX, ingTabY, TAB_COL_W, ingTabH, withAlpha(ingColor, elixirAlpha));

        // Text rotated -90° so it reads bottom-to-top (standard left-sidebar orientation).
        // After translate(tx,ty) + rotate(-90°) + scale(s):
        //   screen pos = (tx + localY*s, ty - localX*s)
        // Text (0..W at local y=0) spans screen x=[tx, tx+9s], y=[ty-W*s, ty]
        int textTx = tabColX + TAB_COL_W / 2 - (int)(9 * TAB_SCALE / 2);

        g.pose().pushPose();
        g.pose().translate(textTx, ovTabY + TAB_PAD + ovTextW, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90f));
        g.pose().scale(TAB_SCALE, TAB_SCALE, 1f);
        g.drawString(font, ovLabel, 0, 0, withAlpha(ovColor, elixirAlpha), false);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(textTx, ingTabY + TAB_PAD + ingTextW, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90f));
        g.pose().scale(TAB_SCALE, TAB_SCALE, 1f);
        g.drawString(font, ingLabel, 0, 0, withAlpha(ingColor, elixirAlpha), false);
        g.pose().popPose();

        // Vertical separator between tab column and content
        int tabSepX = tabColX + TAB_COL_W + 1;
        g.fill(tabSepX, tabTopY, tabSepX + 1, py + PANEL_BOT - 14,
               withAlpha(C_TAB_INACTIVE, elixirAlpha));

        // ── Tab content (right of tab column) ─────────────────────────────────
        hoverLines.clear();
        int contentStartY = tabTopY;
        int listBot       = py + CLEAR_LABEL_GUI_Y - 10;

        List<ItemStack> stones    = elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> tinctures = elixir.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> catalysts = elixir.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        if (leftTab == LeftTab.INGREDIENTS) {
            lastDotsPerRow = 0;
            lastDotRows    = 0;
            int maxW = LP_RIGHT - LP_CONT_X - 2;
            int curY = contentStartY;
            curY = drawSection(g, px, curY, listBot, elixirAlpha, maxW, "ESSENCE STONES", stones,  IngredientType.ESSENCE_STONE);
            if (!tinctures.isEmpty() && !stones.isEmpty() && curY < listBot) curY += 3;
            curY = drawSection(g, px, curY, listBot, elixirAlpha, maxW, "TINCTURES",      tinctures, IngredientType.TINCTURE);
            if (!catalysts.isEmpty() && (!stones.isEmpty() || !tinctures.isEmpty()) && curY < listBot) curY += 3;
            drawSection(g, px, curY, listBot, elixirAlpha, maxW, "CATALYSTS", catalysts, IngredientType.CATALYST);

        } else {
            // Capacity dots aligned to content column
            int dotStartX  = px + LP_CONT_X;
            int dotStartY  = contentStartY;
            int dotsPerRow = (LP_RIGHT - LP_CONT_X) / 7;
            int dotRows    = capacity > 0 ? ((capacity - 1) / dotsPerRow + 1) : 0;
            lastDotsPerRow = dotsPerRow;
            lastDotRows    = dotRows;
            lastDotScreenX = dotStartX;
            lastDotScreenY = dotStartY;

            for (int i = 0; i < capacity; i++) {
                int color = withAlpha(i < loadedCount ? C_DOT_FILLED : C_DOT_EMPTY, elixirAlpha);
                drawDiamond(g, dotStartX + (i % dotsPerRow) * 7,
                               dotStartY + (i / dotsPerRow) * 7, color);
            }

            if (!atCapacity) {
                float ingPrev  = easeOut(ingredientAnimStart);
                int   ingPrevA = (int)(ingPrev * 255);
                if (ingPrevA > 0 && !menu.getIngredientStack().isEmpty()) {
                    int potency      = IngredientUtil.getPotency(menu.getIngredientStack());
                    boolean fits     = loadedCount + potency <= capacity;
                    int previewColor = fits ? C_DOT_PREVIEW : C_DOT_OVERFLOW;
                    for (int i = loadedCount; i < Math.min(loadedCount + potency, capacity); i++) {
                        drawDiamond(g, dotStartX + (i % dotsPerRow) * 7,
                                       dotStartY + (i / dotsPerRow) * 7,
                                       withAlpha(previewColor, ingPrevA));
                    }
                }
            }

            int overviewStartY = dotStartY + dotRows * 7 + 5;
            drawOverviewTab(g, px, overviewStartY, listBot, elixirAlpha, elixir, iElixir,
                            tinctures, catalysts);
        }

        // [Clear] — only if elixir has content
        if (hasContent) {
            String clearText = confirmPending ? "Confirm?" : "Clear";
            boolean hoverClear = isOverButton(mouseX, mouseY, clearScreenX, clearScreenY,
                                              font.width(clearText));
            int clearColor = confirmPending ? C_CLEAR_CONFIRM
                           : hoverClear     ? C_CLEAR_HOVER
                           :                  C_CLEAR_IDLE;
            drawButton(g, clearText, clearScreenX, clearScreenY, clearColor, elixirAlpha);
        }

        g.pose().popPose(); // end left-panel slide

        // ── Right panel (only when not at capacity) ────────────────────────────
        if (!atCapacity) {
            // [Add] button
            boolean canAdd  = cachedValidation == AthanorMenu.ValidationResult.CAN_ADD;
            boolean hoverAdd = isOverButton(mouseX, mouseY, addScreenX, addScreenY, font.width("Add"));
            int addColor = !canAdd   ? C_ADD_DISABLED
                         : hoverAdd  ? C_ADD_HOVER
                         :             C_ADD_IDLE;
            drawButton(g, "Add", addScreenX, addScreenY, addColor, elixirAlpha);

            // Tooltip when [Add] is hovered but disabled
            if (!canAdd && hoverAdd) {
                Component reason = validationMessage(cachedValidation);
                if (reason != null) g.renderTooltip(font, reason, mouseX, mouseY);
            }

            // Ingredient info (fades + slides in when ingredient is placed)
            float ingP = easeOut(ingredientAnimStart);
            if (ingP > 0f) {
                int ingAlpha = (int)(ingP * 255);
                ItemStack ingredient = menu.getIngredientStack();
                if (!ingredient.isEmpty()) {
                    IngredientType type = IngredientType.of(ingredient);
                    int rpX   = px + INGREDIENT_SLOT_X - 1;
                    int infoY = py + RP_INFO_Y;

                    // Slide in from the left alongside the fade
                    float rpSlide = (1f - ingP) * 12f;
                    g.pose().pushPose();
                    g.pose().translate(-rpSlide, 0, 0);

                    // Ingredient name at 0.875 scale
                    g.pose().pushPose();
                    g.pose().translate(rpX, infoY, 0);
                    g.pose().scale(0.875f, 0.875f, 1f);
                    g.drawString(font, resolveDisplayName(ingredient, type), 0, 0,
                                 withAlpha(C_INGR_NAME, ingAlpha), false);
                    g.pose().popPose();

                    // Separator line under the name
                    int nameSepY = infoY + 9;
                    int sepRight = px + RP_RIGHT - 2;
                    g.fill(rpX, nameSepY, sepRight, nameSepY + 1, withAlpha(C_INGR_NAME, ingAlpha / 3));

                    // Type label — slightly smaller (75 % scale) beneath the name
                    String typeLbl = switch (type) {
                        case ESSENCE_STONE -> "ESSENCE STONE";
                        case TINCTURE      -> "TINCTURE";
                        case CATALYST      -> "CATALYST";
                        default            -> "";
                    };
                    int typeY = infoY + 12;
                    g.pose().pushPose();
                    g.pose().translate(rpX, typeY, 0);
                    g.pose().scale(0.75f, 0.75f, 1f);
                    g.drawString(font, typeLbl, 0, 0, withAlpha(C_TYPE_LABEL, ingAlpha), false);
                    g.pose().popPose();

                    // Stats — below the scaled type label, drawn at 75% scale
                    List<Component> stats = ingredientStats(ingredient, type);
                    int statY = typeY + 10;
                    for (Component stat : stats) {
                        if (statY + 8 > py + PANEL_BOT) break;
                        g.pose().pushPose();
                        g.pose().translate(rpX, statY, 0);
                        g.pose().scale(0.75f, 0.75f, 1f);
                        g.drawString(font, stat, 0, 0, withAlpha(C_STATS, ingAlpha), false);
                        g.pose().popPose();
                        statY += 8;
                    }

                    // Warning when the ingredient is too large to fit
                    boolean tooLarge = cachedValidation == AthanorMenu.ValidationResult.AT_CAPACITY;
                    if (tooLarge && statY + 9 <= py + PANEL_BOT) {
                        g.drawString(font,
                                Component.translatable("gui.alchemical.athanor.no_room"),
                                rpX, statY, withAlpha(C_WARNING, ingAlpha), false);
                    }

                    g.pose().popPose(); // end ingredient info slide
                }
            }
        }

        // ── Hover tooltips (rendered last so they sit on top) ──────────────────
        // Only show while animation is settled — positions are offset during slide
        boolean settled = elixirP >= 1f;

        // Dot-area tooltip: "Loaded: X / Y"
        if (settled && hasElixir && lastDotRows > 0) {
            if (mouseX >= lastDotScreenX && mouseX < lastDotScreenX + lastDotsPerRow * 7
                    && mouseY >= lastDotScreenY && mouseY < lastDotScreenY + lastDotRows * 7) {
                IElixir ie = (IElixir) menu.getElixirStack().getItem();
                g.renderTooltip(font, Component.translatable("gui.alchemical.athanor.capacity",
                        ie.getLoadedCount(menu.getElixirStack()), ie.getCapacity()), mouseX, mouseY);
            }
        }

        // Left-panel ingredient hover tooltips (truncated lines)
        if (settled) {
            for (HoverLine hl : hoverLines) {
                if (mouseX >= hl.screenX() && mouseX < hl.screenX() + hl.width()
                        && mouseY >= hl.screenY() && mouseY < hl.screenY() + 9) {
                    g.renderComponentTooltip(font, hl.tooltip(), mouseX, mouseY);
                    break;
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

                // Add button
                if (!atCap && isOverButton(mouseX, mouseY, addScreenX, addScreenY, font.width("Add"))
                        && cachedValidation == AthanorMenu.ValidationResult.CAN_ADD) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    return true;
                }

                // Clear button
                if (loadedCount > 0) {
                    String clearText = confirmPending ? "Confirm?" : "Clear";
                    if (isOverButton(mouseX, mouseY, clearScreenX, clearScreenY,
                                     font.width(clearText))) {
                        handleClearClick();
                        return true;
                    }
                }

                // Vertical tab clicks
                if (mouseX >= tabColScreenX && mouseX < tabColScreenX + TAB_COL_W) {
                    if (mouseY >= ovTabScreenY && mouseY < ovTabScreenY + ovTabH) {
                        leftTab = LeftTab.OVERVIEW;
                        return true;
                    }
                    if (mouseY >= ingTabScreenY && mouseY < ingTabScreenY + ingTabH) {
                        leftTab = LeftTab.INGREDIENTS;
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

    /**
     * Draws a bordered button with 0.8× scaled text.
     * Layout: 1px border + 2px horizontal pad, 2px top pad + 1px bottom pad.
     * Total size: (font.width(text) × 0.8 + 4) × 11.
     */
    private void drawButton(GuiGraphics g, String text, int x, int y, int color, int alpha) {
        int btnW = (int)(font.width(text) * 0.8f) + 4;
        drawBorder(g, x, y, btnW, 11, withAlpha(color, alpha));
        g.pose().pushPose();
        g.pose().translate(x + 2, y + 2, 0);
        g.pose().scale(0.8f, 0.8f, 1f);
        g.drawString(font, text, 0, 0, withAlpha(color, alpha), false);
        g.pose().popPose();
    }

    /** Hit-test for a bordered button drawn by drawButton(). */
    private boolean isOverButton(double mx, double my, int x, int y, int textWidth) {
        return mx >= x && mx < x + (int)(textWidth * 0.8f) + 4 && my >= y && my < y + 11;
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

    // ── Left panel section drawing ────────────────────────────────────────────

    /**
     * Draws the Overview tab — one block per essence stone showing its effective
     * output (effect + level, duration, cooldown) after applying all tincture and
     * catalyst modifiers. The active stone is highlighted in gold with "[Active]".
     */
    private void drawOverviewTab(GuiGraphics g, int px, int curY, int listBot, int alpha,
                                 ItemStack elixir, IElixir iElixir,
                                 List<ItemStack> tinctures, List<ItemStack> catalysts) {
        List<ItemStack> stones = elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        if (stones.isEmpty()) {
            if (curY + 9 <= listBot) {
                g.drawString(font, Component.translatable("gui.alchemical.athanor.none"),
                        px + LP_X + 2, curY, withAlpha(C_LEFT_TEXT, alpha), false);
            }
            return;
        }

        int activeIndex = iElixir.getActiveStoneIndex(elixir);
        int baseCooldown = iElixir.getCooldownSeconds();

        // Pre-compute tincture + catalyst modifier totals (shared across all stones)
        float sharedDurMult  = 1.0f;
        int   sharedDurFlat  = 0;
        int   sharedLevelMod = 0;
        float sharedCdMult   = 1.0f;
        int   sharedCdFlat   = 0;
        for (ItemStack ts : tinctures) {
            var def = ClientIngredientData.getTincture(ts.getItem());
            if (def.isPresent()) {
                var d = def.get();
                sharedDurMult  *= d.effectDurationMultiplier();
                sharedDurFlat  += d.effectDurationFlat();
                sharedLevelMod += d.effectLevelModifier();
                sharedCdMult   *= d.elixirCooldownMultiplier();
                sharedCdFlat   += d.elixirCooldownFlat();
            }
        }
        for (ItemStack cs : catalysts) {
            var def = ClientIngredientData.getCatalyst(cs.getItem());
            if (def.isPresent()) {
                var d = def.get();
                sharedDurMult  *= d.effectDurationMultiplier();
                sharedDurFlat  += d.effectDurationFlat();
                sharedLevelMod += d.effectLevelModifier();
                sharedCdMult   *= d.elixirCooldownMultiplier();
                sharedCdFlat   += d.elixirCooldownFlat();
            }
        }

        for (int i = 0; i < stones.size(); i++) {
            if (curY + 9 > listBot) break;
            ItemStack stoneStack = stones.get(i);
            var stoneType = stoneStack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
            if (stoneType == null) continue;
            var stoneDef = ClientIngredientData.getStone(stoneType);
            if (stoneDef.isEmpty()) continue;
            var def = stoneDef.get();

            boolean isActive = (i == activeIndex);

            // Compute effective stats for this stone
            int finalDuration  = Math.max(1, (int)((def.baseDuration() + sharedDurFlat) * sharedDurMult));
            int finalLevel     = Math.max(1, def.baseLevel() + sharedLevelMod);

            // Cooldown: stone's own modifier applies on top of shared modifiers
            float stoneCdMult  = def.elixirCooldownMultiplier() * sharedCdMult;
            int   stoneCdFlat  = def.elixirCooldownFlat() + sharedCdFlat;
            int   finalCooldown = Math.max(0, (int)((baseCooldown + stoneCdFlat) * stoneCdMult));

            // Effect + Level line (full size, name color)
            String effectLine = BuiltInRegistries.MOB_EFFECT.getOptional(def.effect())
                    .map(e -> e.getDisplayName().getString() + " " + toRoman(finalLevel))
                    .orElse(resolveDisplayName(stoneStack, IngredientType.ESSENCE_STONE).getString());

            // Effect + Level at 0.875 scale
            int nameColor = isActive ? C_TAB_ACTIVE : C_LEFT_TEXT;
            g.pose().pushPose();
            g.pose().translate(px + LP_CONT_X + 2, curY, 0);
            g.pose().scale(0.875f, 0.875f, 1f);
            g.drawString(font, effectLine, 0, 0, withAlpha(nameColor, alpha), false);
            g.pose().popPose();

            // "[Active]" suffix at 75% scale on the same line
            if (isActive) {
                int suffixX = px + LP_X + 2 + (int)(font.width(effectLine) * 0.875f) + 3;
                g.pose().pushPose();
                g.pose().translate(suffixX, curY + 1, 0);
                g.pose().scale(0.75f, 0.75f, 1f);
                g.drawString(font, "[Active]", 0, 0, withAlpha(C_TYPE_LABEL, alpha), false);
                g.pose().popPose();
            }
            curY += 9;

            // Duration + Cooldown at 75% scale
            if (curY + 8 <= listBot) {
                g.pose().pushPose();
                g.pose().translate(px + LP_CONT_X + 4, curY, 0);
                g.pose().scale(0.75f, 0.75f, 1f);
                g.drawString(font, "Duration: " + ticksToSeconds(finalDuration) + "s", 0, 0,
                        withAlpha(C_STATS, alpha), false);
                g.pose().popPose();
                curY += 8;
            }
            if (curY + 8 <= listBot) {
                g.pose().pushPose();
                g.pose().translate(px + LP_CONT_X + 4, curY, 0);
                g.pose().scale(0.75f, 0.75f, 1f);
                g.drawString(font, "Cooldown: " + finalCooldown + "s", 0, 0,
                        withAlpha(C_STATS, alpha), false);
                g.pose().popPose();
                curY += 8;
            }

            // Gap between stones
            if (i < stones.size() - 1) curY += 4;
        }
    }

    /**
     * Draws one labelled section (e.g. "ESSENCE STONES") with an item per line.
     * Each line tries to show "Name — summary"; falls back to just name + hover tooltip.
     * Returns the next available curY.
     */
    private int drawSection(GuiGraphics g, int px, int curY, int listBot,
                            int alpha, int maxW, String header,
                            List<ItemStack> items, IngredientType type) {
        if (items.isEmpty() || curY + 8 > listBot) return curY;

        // Section header at 75% scale
        g.pose().pushPose();
        g.pose().translate(px + LP_CONT_X, curY, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(font, header, 0, 0, withAlpha(C_TYPE_LABEL, alpha), false);
        g.pose().popPose();
        curY += 8;

        for (ItemStack stack : items) {
            if (curY + 8 > listBot) break;
            String name    = resolveDisplayName(stack, type).getString();
            String summary = buildInlineSummary(stack, type);
            String full    = summary.isEmpty() ? name : name + " \u2014 " + summary;

            int scaledMaxW = (int)(maxW / 0.875f);
            String display = font.width(full) <= scaledMaxW ? full : name;
            g.pose().pushPose();
            g.pose().translate(px + LP_CONT_X + 2, curY, 0);
            g.pose().scale(0.875f, 0.875f, 1f);
            g.drawString(font, display, 0, 0, withAlpha(C_LEFT_TEXT, alpha), false);
            g.pose().popPose();

            if (display == name) {
                List<Component> tooltip = ingredientStats(stack, type);
                if (!tooltip.isEmpty()) {
                    hoverLines.add(new HoverLine(leftPos + LP_CONT_X + 2, curY,
                                                 (int)(font.width(name) * 0.875f), tooltip));
                }
            }
            curY += 8;
        }
        return curY;
    }

    /**
     * Returns a short one-line summary of what the ingredient does.
     * For stones: the effect display name.
     * For tinctures/catalysts: the first non-default modifier as a compact string.
     */
    private static String buildInlineSummary(ItemStack stack, IngredientType type) {
        return switch (type) {
            case ESSENCE_STONE -> {
                var st = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                if (st == null) yield "";
                yield ClientIngredientData.getStone(st)
                        .flatMap(def -> BuiltInRegistries.MOB_EFFECT.getOptional(def.effect())
                                .map(e -> e.getDisplayName().getString()))
                        .orElse("");
            }
            case TINCTURE -> ClientIngredientData.getTincture(stack.getItem())
                    .map(d -> firstModifierSummary(d.effectDurationMultiplier(), d.effectDurationFlat(),
                                                   d.effectLevelModifier(),
                                                   d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse("");
            case CATALYST -> ClientIngredientData.getCatalyst(stack.getItem())
                    .map(d -> firstModifierSummary(d.effectDurationMultiplier(), d.effectDurationFlat(),
                                                   d.effectLevelModifier(),
                                                   d.elixirCooldownMultiplier(), d.elixirCooldownFlat()))
                    .orElse("");
            default -> "";
        };
    }

    /** Returns the first non-default modifier as a compact string (e.g. "Dur ×1.50"). */
    private static String firstModifierSummary(float durMult, int durFlat, int levelMod,
                                                float cdMult, int cdFlat) {
        if (durMult  != 1.0f) return String.format("Dur \u00d7%.2f", durMult);
        if (durFlat  != 0)    return "Dur +" + durFlat + "t";
        if (levelMod != 0)    return "Lvl +" + levelMod;
        if (cdMult   != 1.0f) return String.format("CD \u00d7%.2f", cdMult);
        if (cdFlat   != 0)    return "CD +" + cdFlat + "t";
        return "";
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
        lines.add(Component.literal("Potency: " + def.potency()));
        BuiltInRegistries.MOB_EFFECT.getOptional(def.effect()).ifPresent(effect ->
                lines.add(Component.literal(effect.getDisplayName().getString() + " " + toRoman(def.baseLevel()))));
        lines.add(Component.literal("Effect Duration: " + ticksToSeconds(def.baseDuration()) + "s"));
        if (def.elixirCooldownMultiplier() != 1.0f)
            lines.add(Component.literal(String.format("Elixir Cooldown \u00d7%.2f", def.elixirCooldownMultiplier())));
        if (def.elixirCooldownFlat() != 0) {
            String sign = def.elixirCooldownFlat() > 0 ? "+" : "";
            lines.add(Component.literal("Elixir Cooldown " + sign + def.elixirCooldownFlat() + "s"));
        }
        return lines;
    }

    private static List<Component> modifierStats(int potency, float durMult, int durFlat, int levelMod,
                                                  float cdMult, int cdFlat) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Potency: " + potency));
        if (durMult  != 1.0f) lines.add(Component.literal(String.format("Effect Duration \u00d7%.2f", durMult)));
        if (durFlat  != 0) {
            String sign = durFlat > 0 ? "+" : "";
            lines.add(Component.literal("Effect Duration " + sign + ticksToSeconds(durFlat) + "s"));
        }
        if (levelMod != 0) {
            String sign = levelMod > 0 ? "+" : "";
            lines.add(Component.literal("Effect Level " + sign + levelMod));
        }
        if (cdMult   != 1.0f) lines.add(Component.literal(String.format("Elixir Cooldown \u00d7%.2f", cdMult)));
        if (cdFlat   != 0) {
            String sign = cdFlat > 0 ? "+" : "";
            lines.add(Component.literal("Elixir Cooldown " + sign + cdFlat + "s"));
        }
        return lines;
    }

    /** Converts ticks to a seconds string — no decimal when whole (e.g. "15"), one decimal otherwise ("10.5"). */
    private static String ticksToSeconds(int ticks) {
        float s = ticks / 20f;
        return (s == (int) s) ? String.valueOf((int) s) : String.format("%.1f", s);
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
            case DUPLICATE_STONE    -> Component.translatable("gui.alchemical.athanor.duplicate_stone");
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
        g.drawString(font, this.title, titleX, -7, 0xFFCCBB88, false);
        // "Inventory" label intentionally omitted
    }
}
