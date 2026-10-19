package com.okean.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Plus Mini local cosmetic options. Existing Free features are untouched.
 * No servers, permissions or gameplay mechanics are affected.
 */
public final class OkeanPlusMini {
    private OkeanPlusMini() {}

    public static boolean chineseHat = false;
    public static boolean gradientHud = false;
    public static int hudTransparency = 0; // 85%, 65%, 45%, 25%
    public static boolean roundedHud = false;
    public static boolean colorfulWasd = false;
    public static boolean animatedUi = true;
    public static int hudColor = 0; // theme default + ten colors

    // 1.5.1: features which make Plus Mini visually distinct from Free.
    public static boolean rainbowHud = false;
    public static boolean animatedHud = true;
    public static int hudStyle = 0; // 0=classic, 1=compact, 2=minimal
    public static int hatColor = 0; // six selectable cosmetic colors

    private static final String[] COLOR_NAMES = {
        "Theme default", "Coral", "Rose", "Lavender", "Ice", "Mint",
        "Gold", "Ruby", "Cyan", "Lime", "Royal Blue"
    };
    private static final int[] COLORS = {
        0, 0xFFFF7D69, 0xFFFF81C4, 0xFFBFA3FF, 0xFF92E7FF, 0xFF67E1BB,
        0xFFFFD468, 0xFFFF526E, 0xFF42CFFF, 0xFFA8F46B, 0xFF708CFF
    };
    private static final String[] HAT_NAMES = {
        "Straw", "Sakura", "Violet", "Aqua", "Mint", "Black"
    };
    private static final int[] HAT_COLORS = {
        0xFFD8B878, 0xFFF79ABC, 0xFFB8A0FF, 0xFF5ED9E7, 0xFF8ADCB0, 0xFF3E4557
    };
    private static final String[] HUD_STYLES = {"Classic", "Compact", "Minimal"};
    private static final int[] OPACITIES = {217, 166, 115, 64};
    private static final String[] PRESETS = {
        "Ocean Glow", "Sakura", "Cyber RGB", "Midnight Minimal", "Sunset"
    };

    public static String hudColorName() {
        return COLOR_NAMES[MathHelper.clamp(hudColor, 0, COLORS.length - 1)];
    }

    public static String hatColorName() {
        return HAT_NAMES[MathHelper.clamp(hatColor, 0, HAT_NAMES.length - 1)];
    }

    public static int hatColor() {
        return HAT_COLORS[MathHelper.clamp(hatColor, 0, HAT_COLORS.length - 1)];
    }

    public static String hudStyleName() {
        return HUD_STYLES[MathHelper.clamp(hudStyle, 0, HUD_STYLES.length - 1)];
    }

    public static int hudAccent() {
        if (rainbowHud) {
            // Smooth loop every 10 seconds, computed locally and without background tasks.
            float hue = (System.currentTimeMillis() % 10000L) / 10000f;
            return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.78f, 1f) & 0xFFFFFF);
        }
        return hudColor == 0 ? OkeanVisuals.OkeanMenu.accent()
                : COLORS[MathHelper.clamp(hudColor, 1, 10)];
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

    public static int fadeColor(int color, float fade) {
        return withAlpha(color, Math.round(((color >>> 24) & 0xFF)
                * MathHelper.clamp(fade, 0f, 1f)));
    }

    /** Draw a theme-tinted gradient or solid panel, respecting fade-in progress. */
    public static void drawHudBackground(DrawContext ctx, int w, int h, float fade) {
        int accent = hudAccent();
        int alpha = Math.round(OPACITIES[MathHelper.clamp(hudTransparency, 0, 3)]
                * MathHelper.clamp(fade, 0f, 1f));
        int rgb1 = 0x0C1325;
        int red = 12 + (((accent >>> 16) & 0xFF) / 7);
        int green = 19 + (((accent >>> 8) & 0xFF) / 7);
        int blue = 37 + ((accent & 0xFF) / 7);
        int rgb2 = gradientHud ? (red << 16) | (green << 8) | blue : rgb1;
        int top = withAlpha(rgb1, alpha);
        int bottom = withAlpha(rgb2, alpha);
        int border = fadeColor(accent, fade);

        if (!roundedHud) {
            if (gradientHud) ctx.fillGradient(0, 0, w, h, top, bottom);
            else ctx.fill(0, 0, w, h, top);
            ctx.drawBorder(0, 0, w, h, border);
            return;
        }

        if (gradientHud) ctx.fillGradient(0, 4, w, h - 4, top, bottom);
        else ctx.fill(0, 4, w, h - 4, top);
        ctx.fill(4, 0, w - 4, 1, top);
        ctx.fill(2, 1, w - 2, 2, top);
        ctx.fill(1, 2, w - 1, 4, top);
        ctx.fill(1, h - 4, w - 1, h - 2, bottom);
        ctx.fill(2, h - 2, w - 2, h - 1, bottom);
        ctx.fill(4, h - 1, w - 4, h, bottom);
        ctx.fill(4, 0, w - 4, 1, border);
        ctx.fill(4, h - 1, w - 4, h, border);
        ctx.fill(0, 4, 1, h - 4, border);
        ctx.fill(w - 1, 4, w, h - 4, border);
        ctx.fill(1, 2, 2, 4, border);
        ctx.fill(w - 2, 2, w - 1, 4, border);
        ctx.fill(1, h - 4, 2, h - 2, border);
        ctx.fill(w - 2, h - 4, w - 1, h - 2, border);
        ctx.fill(2, 1, 4, 2, border);
        ctx.fill(w - 4, 1, w - 2, 2, border);
        ctx.fill(2, h - 2, 4, h - 1, border);
        ctx.fill(w - 4, h - 2, w - 2, h - 1, border);
    }

    public static void applyPreset(int selection) {
        switch (MathHelper.clamp(selection, 0, PRESETS.length - 1)) {
            case 0 -> { // bright ocean
                OkeanVisuals.OkeanMenu.selectMiniTheme("OCEAN_BLUE");
                hudColor = 0; hatColor = 3; rainbowHud = false;
                gradientHud = true; hudTransparency = 0;
                roundedHud = true; colorfulWasd = true; animatedUi = true;
                animatedHud = true; hudStyle = 0;
            }
            case 1 -> {
                OkeanVisuals.OkeanMenu.selectMiniTheme("SAKURA");
                hudColor = 2; hatColor = 1; rainbowHud = false;
                gradientHud = true; hudTransparency = 1;
                roundedHud = true; colorfulWasd = true; animatedUi = true;
                animatedHud = true; hudStyle = 1;
            }
            case 2 -> {
                OkeanVisuals.OkeanMenu.selectMiniTheme("NEON");
                hudColor = 0; hatColor = 2; rainbowHud = true;
                gradientHud = true; hudTransparency = 0;
                roundedHud = true; colorfulWasd = true; animatedUi = true;
                animatedHud = true; hudStyle = 0;
            }
            case 3 -> {
                OkeanVisuals.OkeanMenu.selectMiniTheme("MIDNIGHT");
                hudColor = 4; hatColor = 5; rainbowHud = false;
                gradientHud = false; hudTransparency = 1;
                roundedHud = true; colorfulWasd = false; animatedUi = true;
                animatedHud = true; hudStyle = 2;
            }
            default -> {
                OkeanVisuals.OkeanMenu.selectMiniTheme("SUNSET");
                hudColor = 1; hatColor = 0; rainbowHud = false;
                gradientHud = true; hudTransparency = 0;
                roundedHud = true; colorfulWasd = true; animatedUi = true;
                animatedHud = true; hudStyle = 1;
            }
        }
        // Presets change the appearance. They never force the hat or HUD on/off.
        OkeanHud.restartAnimation();
    }

    public static class SettingsScreen extends Screen {
        private final Screen parent;
        private int left, top, w, h;
        private int page = 0;
        private int presetSelection = 0;

        public SettingsScreen(Screen parent) {
            super(Text.literal("Okean Visuals Plus Mini"));
            this.parent = parent;
        }

        private void option(int row, String label, Runnable callback) {
            addDrawableChild(OkeanButton.of(label, left + 24, top + 40 + row * 29,
                    w - 48, 23, b -> {
                        callback.run();
                        OkeanVisuals.OkeanMenu.saveSettings();
                        if (MinecraftClient.getInstance().currentScreen == this) clearAndInit();
                    }));
        }

        @Override
        protected void init() {
            w = Math.min(395, width - 20);
            h = Math.min(295, height - 20);
            left = (width - w) / 2;
            top = (height - h) / 2;

            if (page == 0) {
                option(0, "Chinese hat: " + (chineseHat ? "ON" : "OFF"),
                        () -> chineseHat = !chineseHat);
                option(1, "Hat color: " + hatColorName(),
                        () -> hatColor = (hatColor + 1) % HAT_COLORS.length);
                option(2, "RGB HUD: " + (rainbowHud ? "ON" : "OFF"),
                        () -> rainbowHud = !rainbowHud);
                option(3, "HUD color: " + hudColorName(),
                        () -> { hudColor = (hudColor + 1) % COLORS.length; rainbowHud = false; });
                option(4, "HUD gradient: " + (gradientHud ? "ON" : "OFF"),
                        () -> gradientHud = !gradientHud);
                option(5, "HUD transparency: " + hudTransparencyName(),
                        () -> hudTransparency = (hudTransparency + 1) % OPACITIES.length);
            } else {
                option(0, "Rounded HUD: " + (roundedHud ? "ON" : "OFF"),
                        () -> roundedHud = !roundedHud);
                option(1, "HUD shape: " + hudStyleName(),
                        () -> { hudStyle = (hudStyle + 1) % HUD_STYLES.length;
                            OkeanHud.restartAnimation(); });
                option(2, "HUD fade-in: " + (animatedHud ? "ON" : "OFF"),
                        () -> { animatedHud = !animatedHud; OkeanHud.restartAnimation(); });
                option(3, "Colored WASD: " + (colorfulWasd ? "ON" : "OFF"),
                        () -> colorfulWasd = !colorfulWasd);
                option(4, "UI animations: " + (animatedUi ? "ON" : "OFF"),
                        () -> animatedUi = !animatedUi);
                option(5, "Preset: " + PRESETS[presetSelection] + " (next)",
                        () -> presetSelection = (presetSelection + 1) % PRESETS.length);
                addDrawableChild(OkeanButton.of("APPLY PRESET", left + 24,
                        top + h - 59, w - 48, 22, b -> {
                    applyPreset(presetSelection);
                    OkeanVisuals.OkeanMenu.saveSettings();
                    clearAndInit();
                }));
            }
            addDrawableChild(OkeanButton.of(page == 0 ? "NEXT >" : "< BACK",
                    left + 24, top + h - 29, (w - 56) / 2, 22, b -> {
                page = 1 - page;
                clearAndInit();
            }));
            addDrawableChild(OkeanButton.of("DONE",
                    left + 32 + (w - 56) / 2, top + h - 29,
                    (w - 56) / 2, 22, b -> close()));
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xAD010615);
            ctx.fill(left, top, left + w, top + h, OkeanVisuals.OkeanMenu.panelColor());
            ctx.drawBorder(left, top, w, h, OkeanVisuals.OkeanMenu.accent());
            ctx.drawText(textRenderer, "PLUS MINI - " + (page == 0 ? "COLORS" : "LAYOUT / PRESETS"),
                    left + 16, top + 13, OkeanVisuals.OkeanMenu.accent(), true);
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
