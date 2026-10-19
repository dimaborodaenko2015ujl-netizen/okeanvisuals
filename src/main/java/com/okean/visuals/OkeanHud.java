package com.okean.visuals;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * A lightweight, draggable HUD. No external services, scripts, or downloads.
 * HUD coordinates are in Minecraft GUI-scaled pixels and survive restarts.
 */
public final class OkeanHud {
    private OkeanHud() {}

    public static int x = 10;
    public static int y = 10;
    public static float scale = 1.0f;
    public static boolean showWasd = true;
    public static boolean plusCrosshair = false;
    // Visual Time applies to the local client-world only; the server clock is untouched.
    public static boolean visualTimeEnabled = false;
    public static int visualTimeHour = 12; // 1..24

    private static final Deque<Long> recentClicks = new ArrayDeque<>();
    private static final long ONE_SECOND = 1_000_000_000L;
    public static final int PANEL_WIDTH = 205;

    public static int panelHeight() {
        return showWasd ? 171 : 122;
    }

    public static void recordLeftClick() {
        long now = System.nanoTime();
        recentClicks.addLast(now);
        cleanClicks(now);
    }

    private static void cleanClicks(long now) {
        while (!recentClicks.isEmpty() && now - recentClicks.peekFirst() > ONE_SECOND) {
            recentClicks.removeFirst();
        }
    }

    public static int currentCps() {
        cleanClicks(System.nanoTime());
        return recentClicks.size();
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (!(client.currentScreen instanceof HudMoveScreen) && OkeanVisuals.hudEnabled) {
            drawPanel(ctx, client, x, y);
        }
        // Optional minimal '+' crosshair; vanilla crosshair remains underneath.
        if (plusCrosshair && client.currentScreen == null && !client.options.hudHidden) {
            int cx = client.getWindow().getScaledWidth() / 2;
            int cy = client.getWindow().getScaledHeight() / 2;
            int color = OkeanVisuals.OkeanMenu.accent();
            ctx.fill(cx - 5, cy, cx + 6, cy + 1, 0xFF071321);
            ctx.fill(cx, cy - 5, cx + 1, cy + 6, 0xFF071321);
            ctx.fill(cx - 4, cy, cx + 5, cy + 1, color);
            ctx.fill(cx, cy - 4, cx + 1, cy + 5, color);
        }
    }

    public static void drawPanel(DrawContext ctx, MinecraftClient client, int posX, int posY) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(posX, posY, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        int h = panelHeight();
        int accent = OkeanVisuals.OkeanMenu.accent();
        ctx.fill(0, 0, PANEL_WIDTH, h, 0xB40C1222);
        ctx.drawBorder(0, 0, PANEL_WIDTH, h, accent);
        ctx.drawText(client.textRenderer, "OKEAN FREE 1.4.5", 9, 9, accent, true);

        int line = 27;
        String xyz = String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                client.player.getX(), client.player.getY(), client.player.getZ());
        ctx.drawText(client.textRenderer, xyz, 9, line, 0xFFB5D9FF, true);
        line += 14;
        ctx.drawText(client.textRenderer, "FPS: " + client.getCurrentFps(), 9, line, 0xFFFFFFFF, true);
        line += 14;
        String ping = "—";
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) ping = entry.getLatency() + " ms";
        }
        ctx.drawText(client.textRenderer, "Ping: " + ping, 9, line, 0xFFFFFFFF, true);
        line += 14;
        ctx.drawText(client.textRenderer, "CPS: " + currentCps(), 9, line, 0xFFFFFFFF, true);
        line += 14;
        ctx.drawText(client.textRenderer, "Sprint: " + (OkeanVisuals.autoSprint ? "ON" : "OFF"),
                9, line, OkeanVisuals.autoSprint ? 0xFF55EF7A : 0xFFFF7B7B, true);
        line += 14;
        ctx.drawText(client.textRenderer, "Fullbright: " + (OkeanVisuals.fullbright ? "ON" : "OFF"),
                9, line, OkeanVisuals.fullbright ? 0xFF55EF7A : 0xFFFF7B7B, true);
        line += 14;
        if (showWasd) {
            int baseY = 131;
            drawKey(ctx, client, 31, baseY, "W", client.options.forwardKey.isPressed());
            drawKey(ctx, client, 9, baseY + 19, "A", client.options.leftKey.isPressed());
            drawKey(ctx, client, 31, baseY + 19, "S", client.options.backKey.isPressed());
            drawKey(ctx, client, 53, baseY + 19, "D", client.options.rightKey.isPressed());
        }
        ctx.getMatrices().pop();
    }

    private static void drawKey(DrawContext ctx, MinecraftClient client, int bx, int by,
                                String label, boolean down) {
        ctx.fill(bx, by, bx + 19, by + 18, down ? 0xCF2D805B : 0xB435435A);
        ctx.drawBorder(bx, by, 19, 18, down ? 0xFF55EF7A : 0xFF839AB5);
        ctx.drawText(client.textRenderer, label, bx + 6, by + 5,
                down ? 0xFFFFFFFF : 0xFFD6DFE7, true);
    }

    public static class VisualSettingsScreen extends Screen {
        private final Screen parent;
        private int left, top, panelW, panelH;

        public VisualSettingsScreen(Screen parent) {
            super(Text.literal("Okean Visuals - HUD"));
            this.parent = parent;
        }

        private void option(String text, int yOffset, Runnable action) {
            addDrawableChild(OkeanButton.of(text, left + 25, top + yOffset,
                    panelW - 50, 22, b -> {
                action.run();
                OkeanVisuals.OkeanMenu.saveSettings();
                if (MinecraftClient.getInstance().currentScreen == this) {
                    clearAndInit();
                }
            }));
        }

        @Override
        protected void init() {
            panelW = Math.min(375, width - 24);
            panelH = Math.min(300, height - 20);
            left = (width - panelW) / 2;
            top = (height - panelH) / 2;
            int row = 34;
            option("HUD: " + (OkeanVisuals.hudEnabled ? "ON" : "OFF"), row,
                    () -> OkeanVisuals.hudEnabled = !OkeanVisuals.hudEnabled);
            option(String.format(Locale.ROOT, "HUD size: %.0f%% (click to cycle)", scale * 100),
                    row + 27, () -> scale = scale >= 1.75f ? 0.75f : scale + 0.25f);
            option("WASD: " + (showWasd ? "ON" : "OFF"), row + 54,
                    () -> showWasd = !showWasd);
            option("Crosshair +: " + (plusCrosshair ? "ON" : "OFF"), row + 81,
                    () -> plusCrosshair = !plusCrosshair);
            option("Move HUD (drag with mouse)", row + 108,
                    () -> MinecraftClient.getInstance().setScreen(new HudMoveScreen(this)));
            option("Reset HUD position / size", row + 135, () -> {
                x = 10; y = 10; scale = 1.0f;
            });
            option("Visual Time: " + (visualTimeEnabled
                    ? (visualTimeHour + ":00") : "Minecraft time"), row + 162,
                    () -> MinecraftClient.getInstance().setScreen(new VisualTimeScreen(this)));
            addDrawableChild(OkeanButton.of("Done", left + (panelW - 115) / 2,
                    top + panelH - 29, 115, 22, b -> close()));
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xA5000514);
            ctx.fill(left, top, left + panelW, top + panelH,
                    OkeanVisuals.OkeanMenu.panelColor());
            ctx.drawBorder(left, top, panelW, panelH, OkeanVisuals.OkeanMenu.accent());
            ctx.drawText(textRenderer, "HUD SETTINGS", left + 16, top + 12,
                    OkeanVisuals.OkeanMenu.accent(), true);
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }

        @Override
        public boolean shouldPause() { return false; }
    }

    /** Dedicated control screen: no server /time command is ever sent. */
    public static class VisualTimeScreen extends Screen {
        private final Screen parent;
        private int left, top, panelW, panelH;

        public VisualTimeScreen(Screen parent) {
            super(Text.literal("Okean Visuals - Visual Time"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            panelW = Math.min(365, width - 24);
            panelH = Math.min(188, height - 20);
            left = (width - panelW) / 2;
            top = (height - panelH) / 2;

            addDrawableChild(OkeanButton.of("Use Minecraft time (OFF)",
                    left + 24, top + 43, panelW - 48, 22, b -> {
                        visualTimeEnabled = false;
                        OkeanVisuals.OkeanMenu.saveSettings();
                    }));
            addDrawableChild(new VisualTimeSlider(left + 24, top + 88, panelW - 48, 22));
            addDrawableChild(OkeanButton.of("Done", left + (panelW - 116) / 2,
                    top + panelH - 29, 116, 22, b -> close()));
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xB0000916);
            ctx.fill(left, top, left + panelW, top + panelH,
                    OkeanVisuals.OkeanMenu.panelColor());
            ctx.drawBorder(left, top, panelW, panelH, OkeanVisuals.OkeanMenu.accent());
            ctx.drawText(textRenderer, "VISUAL TIME", left + 16, top + 12,
                    OkeanVisuals.OkeanMenu.accent(), true);
            ctx.drawText(textRenderer, visualTimeEnabled
                            ? "Custom time: " + visualTimeHour + ":00 (local only)"
                            : "Following Minecraft world time",
                    left + 24, top + 32, 0xFFD5EAFF, true);
            ctx.drawText(textRenderer, "1:00", left + 24, top + 116, 0xFFC0DAEC, true);
            ctx.drawText(textRenderer, "24:00", left + panelW - 53, top + 116,
                    0xFFC0DAEC, true);
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

    /** Custom colored SliderWidget preserves vanilla drag and keyboard accessibility. */
    private static final class VisualTimeSlider extends SliderWidget {
        private VisualTimeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.literal("Time"),
                    (visualTimeHour - 1) / 23.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int hour = 1 + (int) Math.round(value * 23.0);
            setMessage(Text.literal("Time: " + hour + ":00"));
        }

        @Override
        protected void applyValue() {
            int hour = MathHelper.clamp(1 + (int) Math.round(value * 23.0), 1, 24);
            if (hour != visualTimeHour || !visualTimeEnabled) {
                visualTimeHour = hour;
                visualTimeEnabled = true;
                OkeanVisuals.OkeanMenu.saveSettings();
            }
            updateMessage();
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int accent = OkeanVisuals.OkeanMenu.accent();
            int bx = getX(), by = getY(), bw = getWidth(), bh = getHeight();
            ctx.fill(bx, by, bx + bw, by + bh, 0xE5122132);
            ctx.drawBorder(bx, by, bw, bh, accent);
            int barX = bx + 8, barY = by + bh - 6, track = Math.max(1, bw - 16);
            ctx.fill(barX, barY, barX + track, barY + 2, 0xFF5B6477);
            int filled = MathHelper.clamp((int)Math.round(value * track), 0, track);
            ctx.fill(barX, barY, barX + filled, barY + 2, accent);
            int handleX = barX + filled;
            ctx.fill(handleX - 2, barY - 3, handleX + 2, barY + 5, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    getMessage(), bx + bw / 2, by + 4, 0xFFFFFFFF);
        }
    }

    public static class HudMoveScreen extends Screen {
        private final Screen parent;
        private boolean dragging;
        private int grabX, grabY;

        public HudMoveScreen(Screen parent) {
            super(Text.literal("Drag the Okean HUD"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            addDrawableChild(OkeanButton.of("Save position", width / 2 - 60,
                    height - 31, 120, 22, b -> close()));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && mx >= x && mx < x + PANEL_WIDTH * scale
                    && my >= y && my < y + panelHeight() * scale) {
                dragging = true;
                grabX = (int) mx - x;
                grabY = (int) my - y;
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (dragging && button == 0) {
                x = MathHelper.clamp((int) mx - grabX, 0,
                        Math.max(0, width - Math.round(PANEL_WIDTH * scale)));
                y = MathHelper.clamp((int) my - grabY, 0,
                        Math.max(0, height - Math.round(panelHeight() * scale)));
                return true;
            }
            return super.mouseDragged(mx, my, button, dx, dy);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (button == 0 && dragging) {
                dragging = false;
                OkeanVisuals.OkeanMenu.saveSettings();
                return true;
            }
            return super.mouseReleased(mx, my, button);
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xB0000915);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) drawPanel(ctx, client, x, y);
            ctx.drawCenteredTextWithShadow(textRenderer, "Drag HUD, then click Save",
                    width / 2, 16, 0xFF55D8FF);
            super.render(ctx, mx, my, delta);
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
