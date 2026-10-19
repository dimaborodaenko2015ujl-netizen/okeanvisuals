package com.okean.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Plus Mini settings. All Free tools continue to work independently. */
public final class OkeanPlusMini {
    private OkeanPlusMini() {}

    public static boolean chineseHat = false;
    public static boolean gradientHud = false;
    public static int hudTransparency = 0; // 0=85%, 1=65%, 2=45%, 3=25%
    public static boolean roundedHud = false;
    public static boolean colorfulWasd = false;
    public static boolean animatedUi = true;
    // 0 = theme accent. Additional 10 colors are 1..10.
    public static int hudColor = 0;

    private static final String[] COLOR_NAMES = {
        "Theme default", "Coral", "Rose", "Lavender", "Ice", "Mint",
        "Gold", "Ruby", "Cyan", "Lime", "Royal Blue"
    };
    private static final int[] COLORS = {
        0, 0xFFFF7D69, 0xFFFF81C4, 0xFFBFA3FF, 0xFF92E7FF, 0xFF67E1BB,
        0xFFFFD468, 0xFFFF526E, 0xFF42CFFF, 0xFFA8F46B, 0xFF708CFF
    };
    private static final int[] OPACITIES = {217, 166, 115, 64};

    public static int hudAccent() {
        return hudColor == 0 ? OkeanVisuals.OkeanMenu.accent()
                : COLORS[MathHelper.clamp(hudColor, 1, 10)];
    }

    public static String hudColorName() {
        return COLOR_NAMES[MathHelper.clamp(hudColor, 0, 10)];
    }

    public static String hudTransparencyName() {
        return switch (MathHelper.clamp(hudTransparency, 0, 3)) {
            case 1 -> "65%";
            case 2 -> "45%";
            case 3 -> "25%";
            default -> "85%";
        };
    }

    private static int withAlpha(int color, int alpha) {
        return ((MathHelper.clamp(alpha, 0, 255) & 0xFF) << 24) | (color & 0xFFFFFF);
    }

    /** Draw rounded or square panel with optional vertical gradient and adjustable opacity. */
    public static void drawHudBackground(DrawContext ctx, int w, int h) {
        int accent = hudAccent();
        int alpha = OPACITIES[MathHelper.clamp(hudTransparency, 0, 3)];
        int rgb1 = 0x0C1325;
        int rgb2 = gradientHud ? ((accent & 0xFFFFFF) >>> 2) & 0x3F3F3F : rgb1;
        int top = withAlpha(rgb1, alpha);
        int bottom = withAlpha(rgb2, alpha);
        if (!roundedHud) {
            if (gradientHud) ctx.fillGradient(0, 0, w, h, top, bottom);
            else ctx.fill(0, 0, w, h, top);
            ctx.drawBorder(0, 0, w, h, accent);
            return;
        }
        // Rounded 4px corners; top and bottom horizontal strips soften Minecraft's pixels.
        if (gradientHud) ctx.fillGradient(0, 4, w, h - 4, top, bottom);
        else ctx.fill(0, 4, w, h - 4, top);
        ctx.fill(4, 0, w - 4, 1, top);
        ctx.fill(2, 1, w - 2, 2, top);
        ctx.fill(1, 2, w - 1, 4, top);
        ctx.fill(1, h - 4, w - 1, h - 2, bottom);
        ctx.fill(2, h - 2, w - 2, h - 1, bottom);
        ctx.fill(4, h - 1, w - 4, h, bottom);
        // Lightweight rounded outline.
        ctx.fill(4, 0, w - 4, 1, accent);
        ctx.fill(4, h - 1, w - 4, h, accent);
        ctx.fill(0, 4, 1, h - 4, accent);
        ctx.fill(w - 1, 4, w, h - 4, accent);
        ctx.fill(1, 2, 2, 4, accent);
        ctx.fill(w - 2, 2, w - 1, 4, accent);
        ctx.fill(1, h - 4, 2, h - 2, accent);
        ctx.fill(w - 2, h - 4, w - 1, h - 2, accent);
        ctx.fill(2, 1, 4, 2, accent);
        ctx.fill(w - 4, 1, w - 2, 2, accent);
        ctx.fill(2, h - 2, 4, h - 1, accent);
        ctx.fill(w - 4, h - 2, w - 2, h - 1, accent);
    }

    public static class SettingsScreen extends Screen {
        private final Screen parent;
        private int left, top, w, h;

        public SettingsScreen(Screen parent) {
            super(Text.literal("Okean Visuals Plus Mini"));
            this.parent = parent;
        }

        private void option(int row, String label, Runnable callback) {
            addDrawableChild(OkeanButton.of(label, left + 24, top + 39 + row * 28,
                    w - 48, 23, b -> {
                        callback.run();
                        OkeanVisuals.OkeanMenu.saveSettings();
                        clearAndInit();
                    }));
        }

        @Override
        protected void init() {
            w = Math.min(392, width - 20);
            h = Math.min(295, height - 20);
            left = (width - w) / 2;
            top = (height - h) / 2;
            option(0, "Chinese hat: " + (chineseHat ? "ON" : "OFF"),
                    () -> chineseHat = !chineseHat);
            option(1, "HUD color: " + hudColorName(),
                    () -> hudColor = (hudColor + 1) % COLORS.length);
            option(2, "HUD gradient: " + (gradientHud ? "ON" : "OFF"),
                    () -> gradientHud = !gradientHud);
            option(3, "HUD opacity: " + hudTransparencyName(),
                    () -> hudTransparency = (hudTransparency + 1) % 4);
            option(4, "Rounded HUD: " + (roundedHud ? "ON" : "OFF"),
                    () -> roundedHud = !roundedHud);
            option(5, "Colored WASD: " + (colorfulWasd ? "ON" : "OFF"),
                    () -> colorfulWasd = !colorfulWasd);
            option(6, "UI animations: " + (animatedUi ? "ON" : "OFF"),
                    () -> animatedUi = !animatedUi);
            addDrawableChild(OkeanButton.of("Done", left + (w - 120) / 2,
                    top + h - 30, 120, 22, b -> close()));
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xAD010615);
            ctx.fill(left, top, left + w, top + h, OkeanVisuals.OkeanMenu.panelColor());
            ctx.drawBorder(left, top, w, h, OkeanVisuals.OkeanMenu.accent());
            ctx.drawText(textRenderer, "PLUS MINI - VISUALS",
                    left + 18, top + 14, OkeanVisuals.OkeanMenu.accent(), true);
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            OkeanVisuals.OkeanMenu.saveSettings();
            MinecraftClient.getInstance().setScreen(parent);
        }

        @Override
        public boolean shouldPause() { return false; }
    }
}
