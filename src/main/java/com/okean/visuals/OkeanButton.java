package com.okean.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Minecraft-native clickable button with Okean Visuals theme colors.
 * Keyboard focus, narration, click behavior are inherited from ButtonWidget.
 */
public class OkeanButton extends ButtonWidget {

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
        int accent = OkeanVisuals.OkeanMenu.accent();
        int border = active ? accent : 0xFF4B5667;
        int bg = active ? (isHovered() ? 0xF02A4055 : 0xE5122132) : 0xC0101822;

        // Narrow highlight and colored border: no gray vanilla texture.
        ctx.fill(bx, by, bx + getWidth(), by + getHeight(), bg);
        ctx.drawBorder(bx, by, getWidth(), getHeight(), border);
        if (active) {
            ctx.fill(bx + 2, by + 2, bx + getWidth() - 2, by + 4,
                    (isHovered() ? 0x99000000 : 0x55000000) | (accent & 0x00FFFFFF));
        }
        int color = active ? 0xFFF5FCFF : 0xFF8694A5;
        ctx.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer, getMessage(),
                bx + getWidth() / 2, by + (getHeight() - 8) / 2, color);
    }
}
