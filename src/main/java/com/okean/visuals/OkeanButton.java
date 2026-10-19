package com.okean.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Keyboard-friendly themed button with a lightweight optional hover animation. */
public class OkeanButton extends ButtonWidget {
    private float hoverProgress = 0f;
    private long lastFrameNanos = System.nanoTime();

    public OkeanButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public static OkeanButton of(String label, int x, int y, int width, int height,
                                  PressAction onPress) {
        return new OkeanButton(x, y, width, height, Text.literal(label), onPress);
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int bx = getX();
        int by = getY();
        int bw = getWidth();
        int bh = getHeight();
        int accent = OkeanVisuals.OkeanMenu.accent();

        long now = System.nanoTime();
        float seconds = Math.min(0.05f, Math.max(0f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;
        float target = active && isHovered() ? 1f : 0f;
        if (OkeanPlusMini.animatedUi) {
            hoverProgress += (target - hoverProgress) * Math.min(1f, seconds * 13f);
        } else {
            hoverProgress = target;
        }
        float glow = Math.min(1f, Math.max(0f, hoverProgress));
        int border = active ? accent : 0xFF4B5667;
        int blue = Math.min(255, 32 + (int)(31f * glow));
        int green = Math.min(255, 26 + (int)(30f * glow));
        int bg = active ? (0xEC << 24) | (20 << 16) | (green << 8) | blue : 0xC0101822;

        ctx.fill(bx, by, bx + bw, by + bh, bg);
        ctx.drawBorder(bx, by, bw, bh, border);
        if (active) {
            ctx.fill(bx + 2, by + 2, bx + bw - 2, by + 4,
                    0x56000000 | (accent & 0x00FFFFFF));
            int width = Math.max(0, Math.round((bw - 4) * glow));
            if (width > 0) {
                int start = bx + (bw - width) / 2;
                ctx.fill(start, by + bh - 3, start + width, by + bh - 2, accent);
            }
        }
        int textColor = active ? 0xFFF5FCFF : 0xFF8694A5;
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                getMessage(), bx + bw / 2, by + (bh - 8) / 2, textColor);
    }
}
