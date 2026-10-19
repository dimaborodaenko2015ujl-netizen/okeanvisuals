package com.okean.visuals;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class OkeanVisuals implements ClientModInitializer {

    public static boolean hudEnabled = true;
    public static boolean autoSprint = false;
    public static boolean fullbright = false;
    public static boolean aimbot = false;

    private boolean zoomEnabled = false;
    private int previousFov = 70;
    private boolean menuRequested = false;

    @Override
    public void onInitializeClient() {

        System.out.println("🌊 Okean Visuals loaded!");

        // Клавиша меню — Right Shift
        KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.okeanvisuals.menu",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        "category.okeanvisuals"
                )
        );

        // Клавиша зума — C
        KeyBinding zoomKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.okeanvisuals.zoom",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_C,
                        "category.okeanvisuals"
                )
        );

        // HUD
        HudRenderCallback.EVENT.register(this::renderHUD);

        // Команды
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerCommands(dispatcher)
        );

        // Игровой тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.world == null) {
                return;
            }

            if (menuRequested) {
                menuRequested = false;
                client.setScreen(new OkeanMenu());
            }

            // Автоспринт
            if (autoSprint
                    && client.player.forwardSpeed > 0
                    && !client.player.isSneaking()) {

                client.player.setSprinting(true);
            }

            // Fullbright
            client.options.getGamma().setValue(
                    fullbright ? 100.0 : 0.5
            );

            // Открытие меню через Right Shift
            while (menuKey.wasPressed()) {
                client.setScreen(new OkeanMenu());
            }

            // Zoom
            if (zoomKey.isPressed()) {

                if (!zoomEnabled) {
                    previousFov = client.options.getFov().getValue();
                    zoomEnabled = true;
                }

                client.options.getFov().setValue(
                        MathHelper.clamp((int) Math.round(previousFov * 0.35), 30, 110)
                );

            } else if (zoomEnabled) {

                client.options.getFov().setValue(previousFov);
                zoomEnabled = false;
            }

            // Aimbot
            if (aimbot) {

                LivingEntity target = findTarget(client);

                if (target != null) {
                    aimAt(client, target);
                }
            }
        });
    }

    // =========================
    // КОМАНДЫ
    // =========================

    private void registerCommands(
            CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {

        dispatcher.register(
                ClientCommandManager.literal("okean")
                        .executes(context -> {
                            menuRequested = true;
                            return 1;
                        })

                        // /okean menu
                        .then(
                                ClientCommandManager.literal("menu")
                                        .executes(context -> {

                                            menuRequested = true;

                                            return 1;
                                        })
                        )

                        // /okean sprint
                        .then(
                                ClientCommandManager.literal("sprint")
                                        .executes(context -> {

                                            autoSprint = !autoSprint;

                                            return 1;
                                        })
                        )

                        // /okean hud
                        .then(
                                ClientCommandManager.literal("hud")
                                        .executes(context -> {

                                            hudEnabled = !hudEnabled;

                                            return 1;
                                        })
                        )

                        // /okean bright
                        .then(
                                ClientCommandManager.literal("bright")
                                        .executes(context -> {

                                            fullbright = !fullbright;

                                            return 1;
                                        })
                        )

                        // /okean aimbot
                        .then(
                                ClientCommandManager.literal("aimbot")
                                        .executes(context -> {

                                            aimbot = !aimbot;

                                            return 1;
                                        })
                        )
        );
    }

    // =========================
    // HUD
    // =========================

    private void renderHUD(
            DrawContext ctx,
            net.minecraft.client.render.RenderTickCounter tickCounter) {

        if (!hudEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        int x = 10;
        int y = 10;
        int w = 180;
        int h = 100;

        // Фон
        ctx.fill(
                x,
                y,
                x + w,
                y + h,
                0xB4000014
        );

        // Рамка
        ctx.drawBorder(
                x,
                y,
                w,
                h,
                0xFF00D4FF
        );

        int tx = x + 8;
        int ty = y + 10;

        // Название
        ctx.drawText(
                client.textRenderer,
                "OKEAN VISUALS",
                tx,
                ty,
                0xFF00D4FF,
                true
        );

        ty += 16;

        // Координаты
        ctx.drawText(
                client.textRenderer,
                String.format(
                        "XYZ: %.1f / %.1f / %.1f",
                        client.player.getX(),
                        client.player.getY(),
                        client.player.getZ()
                ),
                tx,
                ty,
                0xFF88CCFF,
                true
        );

        ty += 12;

        // FPS
        ctx.drawText(
                client.textRenderer,
                "FPS: " + client.getCurrentFps(),
                tx,
                ty,
                0xFFFFFFFF,
                true
        );

        ty += 12;

        // Sprint
        ctx.drawText(
                client.textRenderer,
                "Sprint: " + (autoSprint ? "ON" : "OFF"),
                tx,
                ty,
                autoSprint ? 0xFF44FF44 : 0xFFFF4444,
                true
        );

        ty += 12;

        // Aimbot
        ctx.drawText(
                client.textRenderer,
                "Aimbot: " + (aimbot ? "ON" : "OFF"),
                tx,
                ty,
                aimbot ? 0xFF44FF44 : 0xFFFF4444,
                true
        );
    }

    // =========================
    // AIMBOT
    // =========================

    private LivingEntity findTarget(MinecraftClient client) {

        LivingEntity best = null;
        double bestDist = 30.0;

        for (LivingEntity entity :
                client.world.getEntitiesByClass(
                        LivingEntity.class,
                        client.player.getBoundingBox().expand(30),
                        e -> e != client.player && e.isAlive()
                )) {

            double distance =
                    client.player.distanceTo(entity);

            if (distance < bestDist) {

                bestDist = distance;
                best = entity;
            }
        }

        return best;
    }

    private void aimAt(
            MinecraftClient client,
            LivingEntity target) {

        Vec3d diff =
                target.getPos()
                        .subtract(client.player.getEyePos());

        float yaw =
                (float) Math.toDegrees(
                        Math.atan2(diff.z, diff.x)
                ) - 90.0f;

        float pitch =
                (float) -Math.toDegrees(
                        Math.atan2(
                                diff.y,
                                Math.sqrt(
                                        diff.x * diff.x
                                                + diff.z * diff.z
                                )
                        )
                );

        client.player.setYaw(yaw);

        client.player.setPitch(
                MathHelper.clamp(
                        pitch,
                        -90.0f,
                        90.0f
                )
        );
    }

    // =========================
    // МЕНЮ
    // =========================

    public static class OkeanMenu extends Screen {
        private enum Tab { MISC, HUB, SETTINGS }
        private enum Language { RUSSIAN, ENGLISH, UKRAINIAN }
        private enum Theme { OCEAN_BLUE, DARK_PURPLE, MIDNIGHT }

        private static Language language = Language.RUSSIAN;
        private static Theme theme = Theme.OCEAN_BLUE;
        private static int opacityIndex = 0;

        private Tab tab = Tab.MISC;
        private int px;
        private int py;
        private int pw;
        private int ph;

        public OkeanMenu() {
            super(Text.literal("Okean Visuals"));
        }

        private static String tr(String ru, String en, String uk) {
            return switch (language) {
                case RUSSIAN -> ru;
                case ENGLISH -> en;
                case UKRAINIAN -> uk;
            };
        }

        private static int accent() {
            return switch (theme) {
                case OCEAN_BLUE -> 0xFF40D7FF;
                case DARK_PURPLE -> 0xFFBB88FF;
                case MIDNIGHT -> 0xFF9AB4D6;
            };
        }

        private static int panelColor() {
            int alpha = switch (opacityIndex) {
                case 1 -> 0xB8;
                case 2 -> 0xF5;
                default -> 0xDF;
            };
            int rgb = switch (theme) {
                case OCEAN_BLUE -> 0x0C1222;
                case DARK_PURPLE -> 0x180E29;
                case MIDNIGHT -> 0x0C1119;
            };
            return (alpha << 24) | rgb;
        }

        private static String themeName() {
            return switch (theme) {
                case OCEAN_BLUE -> "Ocean Blue";
                case DARK_PURPLE -> "Dark Purple";
                case MIDNIGHT -> "Midnight";
            };
        }

        private static String languageName() {
            return switch (language) {
                case RUSSIAN -> "Русский";
                case ENGLISH -> "English";
                case UKRAINIAN -> "Українська";
            };
        }

        private static String transparencyName() {
            return switch (opacityIndex) {
                case 1 -> "72%";
                case 2 -> "96%";
                default -> "87%";
            };
        }

        private void addAction(String label, int x, int y, int w, int h,
                               ButtonWidget.PressAction action) {
            addDrawableChild(
                    ButtonWidget.builder(Text.literal(label), action)
                            .dimensions(x, y, w, h)
                            .build()
            );
        }

        private void addPlanned(String label, int x, int y, int w, int h) {
            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(label + "  [" + tr("скоро", "soon", "скоро") + "]"),
                    ignored -> {}
            ).dimensions(x, y, w, h).build();
            button.active = false;
            addDrawableChild(button);
        }

        @Override
        protected void init() {
            pw = Math.min(440, Math.max(220, width - 24));
            ph = Math.min(275, Math.max(185, height - 24));
            px = (width - pw) / 2;
            py = (height - ph) / 2;

            int pad = 18;
            int gap = 6;
            int tabW = (pw - 2 * pad - 2 * gap) / 3;
            int tabY = py + 37;

            addAction(tr("Разное", "Misc", "Різне"), px + pad, tabY, tabW, 23,
                    b -> { tab = Tab.MISC; clearAndInit(); });
            addAction(tr("Хаб", "Hub", "Хаб"), px + pad + tabW + gap, tabY, tabW, 23,
                    b -> { tab = Tab.HUB; clearAndInit(); });
            addAction(tr("Настройки", "Settings", "Налаштування"),
                    px + pad + 2 * (tabW + gap), tabY, tabW, 23,
                    b -> { tab = Tab.SETTINGS; clearAndInit(); });

            int rowX = px + 35;
            int rowW = pw - 70;
            int rowY = tabY + 37;
            int rowGap = 28;

            if (tab == Tab.MISC) {
                addPlanned(tr("Расположение рук", "Hand position", "Положення рук"),
                        rowX, rowY, rowW, 22);
                addAction("Fullbright: " + (fullbright ? "ON" : "OFF"),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            fullbright = !fullbright;
                            clearAndInit();
                        });
                addPlanned("Kill Aura", rowX, rowY + 2 * rowGap, rowW, 22);
                addAction("AutoSprint: " + (autoSprint ? "ON" : "OFF"),
                        rowX, rowY + 3 * rowGap, rowW, 22, b -> {
                            autoSprint = !autoSprint;
                            clearAndInit();
                        });
            } else if (tab == Tab.SETTINGS) {
                addAction(tr("Язык: ", "Language: ", "Мова: ") + languageName(),
                        rowX, rowY, rowW, 22, b -> {
                            language = Language.values()[(language.ordinal() + 1)
                                    % Language.values().length];
                            clearAndInit();
                        });
                addAction(tr("Стиль: ", "Theme: ", "Стиль: ") + themeName(),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            theme = Theme.values()[(theme.ordinal() + 1)
                                    % Theme.values().length];
                            clearAndInit();
                        });
                addAction(tr("Непрозрачность: ", "Opacity: ", "Непрозорість: ")
                                + transparencyName(),
                        rowX, rowY + 2 * rowGap, rowW, 22, b -> {
                            opacityIndex = (opacityIndex + 1) % 3;
                            clearAndInit();
                        });
            }

            addAction(tr("Закрыть", "Close", "Закрити"),
                    px + (pw - 135) / 2, py + ph - 32, 135, 23,
                    b -> close());
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xA0000614);
            ctx.fill(px, py, px + pw, py + ph, panelColor());
            ctx.drawBorder(px, py, pw, ph, accent());
            ctx.fill(px + 1, py + 1, px + pw - 1, py + 4, accent());

            ctx.drawText(textRenderer, "🌊 OKEAN VISUALS",
                    px + 18, py + 16, accent(), true);

            int pad = 18;
            int gap = 6;
            int tabW = (pw - 2 * pad - 2 * gap) / 3;
            int selection = switch (tab) {
                case MISC -> 0;
                case HUB -> 1;
                case SETTINGS -> 2;
            };
            int activeX = px + pad + selection * (tabW + gap);
            ctx.fill(activeX, py + 63, activeX + tabW, py + 65, accent());

            if (tab == Tab.HUB) {
                int tx = px + 33;
                int ty = py + 83;
                ctx.drawText(textRenderer, tr("Главная страница Okean Visuals",
                        "Okean Visuals main page", "Головна сторінка Okean Visuals"),
                        tx, ty, accent(), true);
                ctx.drawText(textRenderer, tr("Версия: 1.0.1 UI Test",
                        "Version: 1.0.1 UI Test", "Версія: 1.0.1 UI Test"),
                        tx, ty + 24, 0xFFFFFFFF, true);
                ctx.drawText(textRenderer, tr("Новости: новое меню с 3 разделами",
                        "News: new menu with 3 sections", "Новини: нове меню з 3 розділами"),
                        tx, ty + 46, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Помощь: Right Shift — открыть меню",
                        "Help: Right Shift — open menu", "Допомога: Right Shift — відкрити меню"),
                        tx, ty + 68, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Другие функции появятся позже",
                        "More features coming later", "Інші функції з'являться пізніше"),
                        tx, ty + 90, 0xFFB8D6EB, true);
            } else if (tab == Tab.SETTINGS) {
                ctx.drawText(textRenderer, tr("Нажми на кнопку, чтобы сменить настройку",
                        "Click a button to change a setting",
                        "Натисни кнопку, щоб змінити налаштування"),
                        px + 30, py + ph - 55, 0xFFB8D6EB, true);
            }

            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
