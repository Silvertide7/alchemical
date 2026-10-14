package net.silvertide.alchemical.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.ElixirItem;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.menu.AthanorMenu;
import net.silvertide.alchemical.menu.AthanorMenu.ValidationResult;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;
import net.silvertide.alchemical.util.ElixirModifiers;
import net.silvertide.alchemical.util.FormattingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * No background texture; everything is drawn with fills. Left panel shows what's in the
 * flask, right panel shows the staged ingredient. Both fade and slide in.
 */
public class AthanorScreen extends AbstractContainerScreen<AthanorMenu> {

    // --- Layout (pixels from the GUI's top-left) ---

    private static final int ELIXIR_SLOT_X = AthanorMenu.ELIXIR_SLOT_X;
    private static final int ELIXIR_SLOT_Y = AthanorMenu.ELIXIR_SLOT_Y;
    private static final int INGREDIENT_SLOT_X = AthanorMenu.INGREDIENT_SLOT_X;
    private static final int INGREDIENT_SLOT_Y = AthanorMenu.INGREDIENT_SLOT_Y;

    // Left panel: flask name, capacity dots, per-stone overview, [Empty Elixir]
    private static final int LP_X = 1;
    private static final int LP_RIGHT = 66;
    private static final int LP_CONTENT_X = LP_X + 2;

    private static final int PANEL_BOT = 130;                       // stay clear of the player inventory
    private static final int CONTENT_Y = 25;                        // first row under the title
    private static final int CLEAR_LABEL_Y = PANEL_BOT - 14;        // [Empty Elixir] at the bottom of the left panel
    private static final int ADD_LABEL_Y = INGREDIENT_SLOT_Y + 20;  // [Add] just under the ingredient slot
    private static final int RP_INFO_X = INGREDIENT_SLOT_X + 22;    // ingredient name/stats to the right of its slot

    private static final int CORNER = 4;            // length of the L-shaped slot corner marks
    private static final int DOTS_PER_ROW = 15;
    private static final int DOT_SPACING = 8;
    private static final int MAX_DOT_ROWS = 3;

    private static final float BUTTON_TEXT_SCALE = 0.8f;
    private static final int BUTTON_H = 13;         // 1px border + 2px padding around the scaled text

    // --- Colours (ARGB) ---

    private static final int C_ELIXIR_CORNER     = 0xFF886644;   // always drawn
    private static final int C_ELIXIR_CORNER_LIT = 0xFFFFDD77;   // fades in over it with the flask
    private static final int C_INGR_CORNER       = 0xFF446688;   // shown once the flask has room
    private static final int C_INGR_CORNER_LIT   = 0xFF88CCFF;   // fades in with a staged ingredient
    private static final int C_LINE_LIT          = 0xFFBB8844;   // flask-to-ingredient connector
    private static final int C_ELIXIR_NAME       = 0xFFCCBB88;   // also the info icon's hover colour
    private static final int C_LEFT_TEXT         = 0xFF999988;   // also the info icon's idle colour
    private static final int C_CLEAR_IDLE        = 0xFF994444;
    private static final int C_CLEAR_HOVER       = 0xFFCC6655;
    private static final int C_CLEAR_CONFIRM     = 0xFFDD4422;
    private static final int C_ADD_IDLE          = 0xFF7799AA;
    private static final int C_ADD_HOVER         = 0xFFCCEEFF;
    private static final int C_ADD_DISABLED      = 0xFF444444;
    private static final int C_TYPE_LABEL        = 0xFF7788AA;
    private static final int C_INGR_NAME         = 0xFFDDDDDD;
    private static final int C_STATS             = 0xFFCCCCBB;
    private static final int C_DOT_FILLED        = 0xFFCC8800;
    private static final int C_DOT_EMPTY         = 0xFF665544;
    private static final int C_DOT_PREVIEW       = 0xFF44AAFF;   // what the staged ingredient would fill
    private static final int C_DOT_OVERFLOW      = 0xFFCC2222;   // ...when it doesn't fit
    private static final int C_WARNING           = 0xFFDD4422;
    private static final int C_SLOT_GRID         = 0xFF667788;

    // --- Animation and confirm state ---

    private static final long ANIM_MS = 350L;
    private static final long CONFIRM_TTL_MS = 3000L;

    private long flaskAnimStart;        // 0 while there's no flask
    private long ingredientAnimStart;   // 0 while there's no staged ingredient
    private boolean confirmPending;
    private long confirmStart;

    // --- Refreshed every tick in containerTick ---

    private ItemStack flask = ItemStack.EMPTY;
    private boolean hasFlask;
    private int loaded;
    private int capacity;
    private ValidationResult validation = ValidationResult.NO_ELIXIR;

    // --- Screen-space positions for hit tests ---

    private int addX, addY, clearX, clearY;         // set in init()
    private int dotAreaX, dotAreaY, dotAreaRows;    // set while rendering
    private int infoIconX, infoIconY;

    private final Component addLabel = Component.translatable("gui.alchemical.athanor.add");
    private final Component clearLabel = Component.translatable("gui.alchemical.athanor.clear");
    private final Component confirmLabel = Component.translatable("gui.alchemical.athanor.clear_confirm");
    private final Component activeLabel = Component.translatable("gui.alchemical.athanor.active");
    private final Component noneLabel = Component.translatable("gui.alchemical.athanor.none");
    private final Component noRoomLabel = Component.translatable("gui.alchemical.athanor.no_room");
    private final Component placeFlaskHint = Component.translatable("gui.alchemical.athanor.place_elixir");

    public AthanorScreen(AthanorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 230;
        imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        // [Add] centred under the ingredient slot (nudged 1px left); [Empty Elixir] centred in the left panel
        addX = leftPos + INGREDIENT_SLOT_X + 9 - buttonWidth(addLabel) / 2 - 1;
        addY = topPos + ADD_LABEL_Y;
        clearX = leftPos + LP_X + (LP_RIGHT - LP_X) / 2 - buttonWidth(clearLabel) / 2;
        clearY = topPos + CLEAR_LABEL_Y;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long now = System.currentTimeMillis();

        flask = menu.getElixirStack();
        hasFlask = flask.getItem() instanceof ElixirItem;
        loaded = hasFlask ? ElixirItem.loadedCount(flask) : 0;
        capacity = ElixirItem.capacity();
        validation = menu.canAddIngredient();

        flaskAnimStart = animStart(flaskAnimStart, hasFlask, now);
        ingredientAnimStart = animStart(ingredientAnimStart, !menu.getIngredientStack().isEmpty(), now);

        if (confirmPending && now - confirmStart >= CONFIRM_TTL_MS) confirmPending = false;
    }

    /** Starts the clock when something appears, resets it when it goes away. */
    private static long animStart(long current, boolean present, long now) {
        if (!present) return 0L;
        return current == 0L ? now : current;
    }

    // --- Rendering ---

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int px = leftPos;
        int py = topPos;

        // Flask slot: dim corners always, bright corners fade in with the flask
        float flaskP = easeOut(flaskAnimStart);
        int flaskAlpha = (int) (flaskP * 255);
        drawSlotCorners(g, px + ELIXIR_SLOT_X - 1, py + ELIXIR_SLOT_Y - 1, C_ELIXIR_CORNER);
        if (flaskAlpha > 0) {
            drawSlotCorners(g, px + ELIXIR_SLOT_X - 1, py + ELIXIR_SLOT_Y - 1, withAlpha(C_ELIXIR_CORNER_LIT, flaskAlpha));
        }

        // Player inventory grid. Slots sit at (35, 152) and (35, 210); the grid is 1px outside them.
        drawSlotGrid(g, px + 34, py + 151, 9, 3, C_SLOT_GRID);
        drawSlotGrid(g, px + 34, py + 209, 9, 1, C_SLOT_GRID);

        // Ingredient slot corners and the connecting line, only while the flask has room.
        // Uses the real capacity rather than `validation` so these stay put when the staged
        // ingredient is merely too big to fit.
        boolean hasRoom = hasFlask && loaded < capacity;
        if (hasRoom && flaskAlpha > 0) {
            drawSlotCorners(g, px + INGREDIENT_SLOT_X - 1, py + INGREDIENT_SLOT_Y - 1, withAlpha(C_INGR_CORNER, flaskAlpha));

            int ingAlpha = (int) (easeOut(ingredientAnimStart) * 255);
            if (ingAlpha > 0) {
                boolean tooBig = validation == ValidationResult.AT_CAPACITY;
                int lineY = py + ELIXIR_SLOT_Y + 8;
                int lineStart = px + ELIXIR_SLOT_X + 18;
                int lineEnd = lineStart + (int) ((px + INGREDIENT_SLOT_X - 2 - lineStart) * flaskP);
                g.fill(lineStart, lineY, lineEnd, lineY + 1, withAlpha(tooBig ? C_DOT_OVERFLOW : C_LINE_LIT, ingAlpha));
                drawSlotCorners(g, px + INGREDIENT_SLOT_X - 1, py + INGREDIENT_SLOT_Y - 1,
                        withAlpha(tooBig ? C_DOT_OVERFLOW : C_INGR_CORNER_LIT, ingAlpha));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        if (!hasFlask) {
            int hintX = leftPos + ELIXIR_SLOT_X + 9 - (int) (font.width(placeFlaskHint) * 0.75f) / 2;
            drawScaledString(g, placeFlaskHint, hintX, topPos + ELIXIR_SLOT_Y + 22, 0.75f, C_LEFT_TEXT);
            return;
        }

        float flaskP = easeOut(flaskAnimStart);
        if (flaskP <= 0f) return;
        int alpha = (int) (flaskP * 255);

        renderLeftPanel(g, mouseX, mouseY, flaskP, alpha);
        if (loaded < capacity) renderRightPanel(g, mouseX, mouseY, alpha);
        if (flaskP >= 1f) renderHoverTooltips(g, mouseX, mouseY);
    }

    private void renderLeftPanel(GuiGraphics g, int mouseX, int mouseY, float flaskP, int alpha) {
        int px = leftPos;
        int py = topPos;
        boolean hasContent = loaded > 0;

        // Slides in from the left as the flask fades in
        g.pose().pushPose();
        g.pose().translate(-(1f - flaskP) * 14f, 0, 0);

        drawScaledString(g, flask.getHoverName(), px + LP_CONTENT_X, py + CONTENT_Y, 0.875f, withAlpha(C_ELIXIR_NAME, alpha));

        // Separator under the name, as wide as a full row of dots
        int sepY = py + CONTENT_Y + 9;
        int sepRight = px + LP_CONTENT_X + (DOTS_PER_ROW - 1) * DOT_SPACING + 5;
        g.fill(px + LP_CONTENT_X, sepY, sepRight, sepY + 1, withAlpha(C_ELIXIR_NAME, alpha / 3));

        if (hasContent) {
            infoIconX = px + LP_RIGHT - 8;
            infoIconY = py + CONTENT_Y;
            boolean hover = isOverInfoIcon(mouseX, mouseY);
            drawInfoIcon(g, infoIconX, infoIconY, withAlpha(hover ? C_ELIXIR_NAME : C_LEFT_TEXT, alpha));
        }

        int y = renderCapacityDots(g, px + LP_CONTENT_X, py + CONTENT_Y + 13, alpha);
        drawStoneOverview(g, px + LP_CONTENT_X, y, py + CLEAR_LABEL_Y - 3, alpha);

        if (hasContent) {
            Component label = confirmPending ? confirmLabel : clearLabel;
            int color = confirmPending ? C_CLEAR_CONFIRM
                    : isOverButton(mouseX, mouseY, clearX, clearY, label) ? C_CLEAR_HOVER
                    : C_CLEAR_IDLE;
            drawButton(g, label, clearX, clearY, color, alpha);
        }

        g.pose().popPose();
    }

    /** Capacity dots plus a preview of what the staged ingredient would fill. Returns the Y below them. */
    private int renderCapacityDots(GuiGraphics g, int x, int y, int alpha) {
        int rows = Math.min(MAX_DOT_ROWS, (capacity - 1) / DOTS_PER_ROW + 1);
        int maxDots = DOTS_PER_ROW * MAX_DOT_ROWS;
        dotAreaX = x;
        dotAreaY = y;
        dotAreaRows = rows;

        for (int i = 0; i < Math.min(capacity, maxDots); i++) {
            drawDot(g, x, y, i, withAlpha(i < loaded ? C_DOT_FILLED : C_DOT_EMPTY, alpha));
        }

        ItemStack ingredient = menu.getIngredientStack();
        int ingAlpha = (int) (easeOut(ingredientAnimStart) * 255);
        if (loaded < capacity && ingAlpha > 0 && !ingredient.isEmpty()) {
            int potency = IngredientManager.potency(ingredient);
            int color = withAlpha(loaded + potency <= capacity ? C_DOT_PREVIEW : C_DOT_OVERFLOW, ingAlpha);
            // Deliberately runs past `capacity` so an overflow spills visibly off the end
            for (int i = loaded; i < Math.min(loaded + potency, maxDots); i++) {
                drawDot(g, x, y, i, color);
            }
        }
        return y + rows * DOT_SPACING + 5;
    }

    private void drawDot(GuiGraphics g, int x, int y, int index, int color) {
        drawDiamond(g, x + (index % DOTS_PER_ROW) * DOT_SPACING, y + (index / DOTS_PER_ROW) * DOT_SPACING, color);
    }

    /** One block per stone: effect and level, duration, cooldown, all after modifiers. */
    private void drawStoneOverview(GuiGraphics g, int x, int y, int bottom, int alpha) {
        List<ItemStack> stones = ElixirItem.stones(flask);
        if (stones.isEmpty()) {
            if (y + 9 <= bottom) g.drawString(font, noneLabel, x, y, withAlpha(C_LEFT_TEXT, alpha), false);
            return;
        }

        int active = ElixirItem.activeStoneIndex(flask);
        ElixirModifiers mods = ElixirModifiers.of(flask);

        for (int i = 0; i < stones.size() && y + 9 <= bottom; i++) {
            Optional<EssenceStoneDefinition> def = IngredientManager.getStone(stones.get(i));
            if (def.isEmpty()) continue;
            EssenceStoneDefinition stone = def.get();
            boolean isActive = i == active;

            Component effect = FormattingUtil.effectName(stone.effect(), mods.level(stone));
            drawScaledString(g, effect, x + 2, y, 0.75f, withAlpha(isActive ? C_ELIXIR_NAME : C_LEFT_TEXT, alpha));
            if (isActive) {
                int suffixX = x + 2 + (int) (font.width(effect) * 0.75f) + 3;
                drawScaledString(g, activeLabel, suffixX, y + 1, 0.625f, withAlpha(C_TYPE_LABEL, alpha));
            }
            y += 7;

            if (y + 6 <= bottom) {
                Component duration = Component.translatable("gui.alchemical.athanor.stat.duration",
                        FormattingUtil.ticksToTime(mods.duration(stone)));
                drawScaledString(g, duration, x + 4, y, 0.625f, withAlpha(C_STATS, alpha));
                y += 6;
            }
            if (y + 6 <= bottom) {
                Component cooldown = Component.translatable("gui.alchemical.athanor.stat.cooldown",
                        FormattingUtil.secondsToTime(mods.cooldown(stone)));
                drawScaledString(g, cooldown, x + 4, y, 0.625f, withAlpha(C_STATS, alpha));
                y += 6;
            }
            if (i < stones.size() - 1) y += 2;
        }
    }

    private void renderRightPanel(GuiGraphics g, int mouseX, int mouseY, int alpha) {
        int px = leftPos;
        int py = topPos;

        boolean canAdd = validation == ValidationResult.CAN_ADD;
        boolean hoverAdd = isOverButton(mouseX, mouseY, addX, addY, addLabel);
        int addColor = !canAdd ? C_ADD_DISABLED : hoverAdd ? C_ADD_HOVER : C_ADD_IDLE;
        drawButton(g, addLabel, addX, addY, addColor, alpha);
        if (!canAdd && hoverAdd) {
            Component reason = validation.message();
            if (reason != null) g.renderTooltip(font, reason, mouseX, mouseY);
        }

        // Staged ingredient's name, type and stats, sliding in beside its slot
        ItemStack ingredient = menu.getIngredientStack();
        float ingP = easeOut(ingredientAnimStart);
        if (ingP <= 0f || ingredient.isEmpty()) return;
        int ingAlpha = (int) (ingP * 255);
        int x = px + RP_INFO_X;

        g.pose().pushPose();
        g.pose().translate(-(1f - ingP) * 12f, 0, 0);

        int y = py + INGREDIENT_SLOT_Y + 5;
        drawScaledString(g, IngredientManager.displayName(ingredient), x, y, 0.875f, withAlpha(C_INGR_NAME, ingAlpha));
        y += 10;

        Component typeLabel = Component.translatable("gui.alchemical.athanor.type." + IngredientType.of(ingredient).key());
        drawScaledString(g, typeLabel, x, y, 0.75f, withAlpha(C_TYPE_LABEL, ingAlpha));
        y += 10;

        for (Component stat : ingredientStats(ingredient)) {
            if (y + 8 > py + PANEL_BOT) break;
            drawScaledString(g, stat, x, y, 0.75f, withAlpha(C_STATS, ingAlpha));
            y += 8;
        }

        if (validation == ValidationResult.AT_CAPACITY && y + 9 <= py + PANEL_BOT) {
            g.drawString(font, noRoomLabel, x, y, withAlpha(C_WARNING, ingAlpha), false);
        }

        g.pose().popPose();
    }

    /** Drawn last so they sit on top of everything. */
    private void renderHoverTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX >= dotAreaX && mouseX < dotAreaX + DOTS_PER_ROW * DOT_SPACING
                && mouseY >= dotAreaY && mouseY < dotAreaY + dotAreaRows * DOT_SPACING) {
            g.renderTooltip(font, Component.translatable("gui.alchemical.athanor.capacity", loaded, capacity), mouseX, mouseY);
        }
        if (loaded > 0 && isOverInfoIcon(mouseX, mouseY)) {
            g.renderComponentTooltip(font, ingredientTooltip(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title centred over the flask slot; the "Inventory" label is left out on purpose
        g.drawString(font, title, ELIXIR_SLOT_X + 9 - font.width(title) / 2, -7, C_ELIXIR_NAME, false);
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasFlask) {
            if (loaded < capacity && validation == ValidationResult.CAN_ADD
                    && isOverButton(mouseX, mouseY, addX, addY, addLabel)) {
                clickMenuButton(AthanorMenu.BUTTON_ADD);
                return true;
            }
            if (loaded > 0 && isOverButton(mouseX, mouseY, clearX, clearY, confirmPending ? confirmLabel : clearLabel)) {
                if (confirmPending) {
                    clickMenuButton(AthanorMenu.BUTTON_CLEAR);
                    confirmPending = false;
                } else {
                    confirmPending = true;
                    confirmStart = System.currentTimeMillis();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickMenuButton(int id) {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean isOverButton(double mx, double my, int x, int y, Component label) {
        return mx >= x && mx < x + buttonWidth(label) && my >= y && my < y + BUTTON_H;
    }

    private boolean isOverInfoIcon(int mx, int my) {
        return mx >= infoIconX && mx < infoIconX + 7 && my >= infoIconY && my < infoIconY + 7;
    }

    // --- Tooltip text ---

    /** Everything in the flask grouped by kind, for the info icon. */
    private List<Component> ingredientTooltip() {
        List<Component> lines = new ArrayList<>();
        addSection(lines, "gui.alchemical.athanor.section.stones", ElixirItem.stones(flask));
        addSection(lines, "gui.alchemical.athanor.section.tinctures", ElixirItem.tinctures(flask));
        addSection(lines, "gui.alchemical.athanor.section.catalysts", ElixirItem.catalysts(flask));
        return lines;
    }

    private static void addSection(List<Component> lines, String headerKey, List<ItemStack> stacks) {
        if (stacks.isEmpty()) return;
        if (!lines.isEmpty()) lines.add(Component.empty());
        lines.add(Component.translatable(headerKey).withStyle(s -> s.withColor(C_TYPE_LABEL)));
        for (ItemStack stack : stacks) {
            lines.add(Component.literal("  ").append(IngredientManager.displayName(stack)));
            for (Component stat : ingredientStats(stack)) {
                lines.add(Component.literal("    ").append(stat.copy().withStyle(s -> s.withColor(C_STATS))));
            }
        }
    }

    private static List<Component> ingredientStats(ItemStack stack) {
        return switch (IngredientType.of(stack)) {
            case ESSENCE_STONE -> IngredientManager.getStone(stack).map(AthanorScreen::stoneStats).orElse(List.of());
            case TINCTURE, CATALYST -> IngredientManager.getModifier(stack).map(AthanorScreen::modifierStats).orElse(List.of());
            case NONE -> List.of();
        };
    }

    private static List<Component> stoneStats(EssenceStoneDefinition def) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.alchemical.athanor.stat.potency", def.potency()));
        lines.add(FormattingUtil.effectName(def.effect(), def.baseLevel()));
        lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration", FormattingUtil.ticksToTime(def.baseDuration())));
        if (def.cooldownMultiplier() != 1f) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_mult", multiplier(def.cooldownMultiplier())));
        }
        if (def.cooldownFlat() != 0) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_flat", signed(def.cooldownFlat()) + "s"));
        }
        return lines;
    }

    private static List<Component> modifierStats(ModifierDefinition def) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.alchemical.athanor.stat.potency", def.potency()));
        if (def.durationMultiplier() != 1f) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration_mult", multiplier(def.durationMultiplier())));
        }
        if (def.durationFlat() != 0) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_duration_flat", signedTicks(def.durationFlat())));
        }
        if (def.levelModifier() != 0) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.effect_level", signed(def.levelModifier())));
        }
        if (def.cooldownMultiplier() != 1f) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_mult", multiplier(def.cooldownMultiplier())));
        }
        if (def.cooldownFlat() != 0) {
            lines.add(Component.translatable("gui.alchemical.athanor.stat.cooldown_flat", signed(def.cooldownFlat()) + "s"));
        }
        return lines;
    }

    private static String multiplier(float f) {
        return String.format("%.2f", f);
    }

    private static String signed(int n) {
        return n > 0 ? "+" + n : Integer.toString(n);
    }

    private static String signedTicks(int ticks) {
        return (ticks < 0 ? "-" : "+") + FormattingUtil.ticksToTime(Math.abs(ticks));
    }

    // --- Drawing primitives ---

    /** L-shaped marks on the four corners of an 18x18 slot border. (x, y) is the border's top-left. */
    private void drawSlotCorners(GuiGraphics g, int x, int y, int color) {
        int s = CORNER;
        int e = 17;
        g.fill(x,         y,             x + s,     y + 1,     color);  // top-left
        g.fill(x,         y + 1,         x + 1,     y + s,     color);
        g.fill(x + e - s, y,             x + e + 1, y + 1,     color);  // top-right
        g.fill(x + e,     y + 1,         x + e + 1, y + s,     color);
        g.fill(x,         y + e,         x + s,     y + e + 1, color);  // bottom-left
        g.fill(x,         y + e - s + 1, x + 1,     y + e,     color);
        g.fill(x + e - s, y + e,         x + e + 1, y + e + 1, color);  // bottom-right
        g.fill(x + e,     y + e - s + 1, x + e + 1, y + e,     color);
    }

    /** 5x5 diamond with its top-left at (x, y). */
    private void drawDiamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 2, y,     x + 3, y + 1, color);
        g.fill(x + 1, y + 1, x + 4, y + 2, color);
        g.fill(x,     y + 2, x + 5, y + 3, color);
        g.fill(x + 1, y + 3, x + 4, y + 4, color);
        g.fill(x + 2, y + 4, x + 3, y + 5, color);
    }

    /** 1px grid lines around a cols x rows block of 18px slots. */
    private void drawSlotGrid(GuiGraphics g, int x, int y, int cols, int rows, int color) {
        int w = cols * 18 + 1;
        int h = rows * 18 + 1;
        for (int row = 0; row <= rows; row++) g.fill(x, y + row * 18, x + w, y + row * 18 + 1, color);
        for (int col = 0; col <= cols; col++) g.fill(x + col * 18, y, x + col * 18 + 1, y + h, color);
    }

    /** 7x7 circle outline with an "i" in it. */
    private void drawInfoIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 2, y,     x + 5, y + 1, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color);
        g.fill(x + 5, y + 1, x + 6, y + 2, color);
        g.fill(x,     y + 2, x + 1, y + 5, color);
        g.fill(x + 6, y + 2, x + 7, y + 5, color);
        g.fill(x + 1, y + 5, x + 2, y + 6, color);
        g.fill(x + 5, y + 5, x + 6, y + 6, color);
        g.fill(x + 2, y + 6, x + 5, y + 7, color);
        g.fill(x + 3, y + 2, x + 4, y + 3, color);  // the dot
        g.fill(x + 3, y + 3, x + 4, y + 5, color);  // the stem
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y + 1,     x + 1,     y + h - 1, color);
        g.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
    }

    /** Bordered text button. isOverButton() and buttonWidth() must agree with this. */
    private void drawButton(GuiGraphics g, Component text, int x, int y, int color, int alpha) {
        drawBorder(g, x, y, buttonWidth(text), BUTTON_H, withAlpha(color, alpha));
        drawScaledString(g, text, x + 3, y + 3, BUTTON_TEXT_SCALE, withAlpha(color, alpha));
    }

    private int buttonWidth(Component text) {
        return (int) (font.width(text) * BUTTON_TEXT_SCALE) + 6;
    }

    private void drawScaledString(GuiGraphics g, Component text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** 0 when nothing has started, 1 once ANIM_MS have passed, cubic ease-out in between. */
    private static float easeOut(long startMs) {
        if (startMs == 0L) return 0f;
        float t = Math.min(1f, (System.currentTimeMillis() - startMs) / (float) ANIM_MS);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
