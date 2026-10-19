package com.okean.visuals;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class OkeanVisuals implements ClientModInitializer {

    public static boolean hudEnabled = true;
    public static boolean autoSprint = false;
    public static boolean fullbright = false;
    public static boolean killAura = false;
    private static AuraTarget targetMode = AuraTarget.MONSTERS;
    private enum AuraTarget { MONSTERS, ANIMALS, PLAYERS, ALL }

    private boolean zoomEnabled = false;
    private int previousFov = 70;
    private boolean menuRequested = false;
    private boolean gammaOverridden = false;
    private double storedGamma = 0.5;
    private int auraTicksSinceAttack = 20;
    private boolean appliedNightVision = false;
    public static boolean showRings = true;

    @Override
    public void onInitializeClient() {

        System.out.println("🌊 Okean Visuals loaded!");
        OkeanMenu.loadSettings();

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

        KeyBinding auraKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.okeanvisuals.aura",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R, "category.okeanvisuals")
        );

        // HUD and world-space target indicators
        HudRenderCallback.EVENT.register(this::renderHUD);
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderTargetRings);

        // Команды
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerCommands(dispatcher)
        );

        // Игровой тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.world == null) {
                appliedNightVision = false;
                restoreGamma(client);
                if (zoomEnabled) {
                    client.options.getFov().setValue(previousFov);
                    zoomEnabled = false;
                }
                auraTicksSinceAttack = 20;
                return;
            }

            while (auraKey.wasPressed()) {
                killAura = !killAura;
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

            // Night Vision is actually visible with Minecraft 1.21.4's modern lightmap.
            // Only remove the effect if this mod itself added it.
            if (fullbright) {
                if (!client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    client.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION, 300, 0, true, false, false));
                    appliedNightVision = true;
                }
            } else if (appliedNightVision) {
                client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                appliedNightVision = false;
            }
            if (gammaOverridden) restoreGamma(client);

            // Открытие меню через Right Shift
            while (menuKey.wasPressed()) {
                client.setScreen(new OkeanMenu());
            }

            // Zoom
            if (zoomKey.isPressed() && client.currentScreen == null) {

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

            // Kill Aura: target mobs/players in reach without moving the camera.
            // Default mode is hostile mobs only. Respect normal attack cooldown.
            auraTicksSinceAttack++;
            if (killAura && client.currentScreen == null && client.interactionManager != null) {
                LivingEntity target = findAuraTarget(client);
                if (target != null) {
                    turnBodyToward(client, target);
                    if (auraTicksSinceAttack >= 12
                            && client.player.getAttackCooldownProgress(0.0f) >= 0.95f) {
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                        auraTicksSinceAttack = 0;
                    }
                }
            }
        });
    }

    private void restoreGamma(MinecraftClient client) {
        if (gammaOverridden) {
            client.options.getGamma().setValue(storedGamma);
            gammaOverridden = false;
        }
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

                        // /okean aura
                        .then(
                                ClientCommandManager.literal("aura")
                                        .executes(context -> {
                                            killAura = !killAura;
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

        // Kill Aura
        ctx.drawText(
                client.textRenderer,
                "Kill Aura: " + (killAura ? "ON" : "OFF"),
                tx,
                ty,
                killAura ? 0xFF44FF44 : 0xFFFF4444,
                true
        );
    }

    // =========================
    // KILL AURA
    // =========================

    private LivingEntity findAuraTarget(MinecraftClient client) {
        LivingEntity nearest = null;
        double nearestSquared = 3.0 * 3.0;

        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(3.0),
                e -> e != client.player && e.isAlive() && !e.isSpectator())) {

            boolean eligible = switch (targetMode) {
                case MONSTERS -> entity instanceof HostileEntity;
                case PLAYERS -> entity instanceof PlayerEntity;
                case ANIMALS -> entity instanceof AnimalEntity;
                case PLAYERS -> entity instanceof PlayerEntity;
                case ALL -> entity instanceof HostileEntity || entity instanceof AnimalEntity
                        || entity instanceof PlayerEntity;
            };
            if (!eligible || !client.player.canSee(entity)) {
                continue;
            }
            double d = client.player.squaredDistanceTo(entity);
            if (d < nearestSquared) {
                nearestSquared = d;
                nearest = entity;
            }
        }
        return nearest;
    }

    private void turnBodyToward(MinecraftClient client, LivingEntity target) {
        Vec3d diff = target.getEyePos().subtract(client.player.getEyePos());
        float desiredYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        // Third-person model orientation only. Never call setYaw/setPitch (camera).
        client.player.setBodyYaw(desiredYaw);
        client.player.setHeadYaw(desiredYaw);
    }

    private void renderTargetRings(WorldRenderContext context) {
        if (!showRings || context.consumers() == null || context.matrixStack() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, client.player.getBoundingBox().expand(16.0),
                e -> e != client.player && e.isAlive()
                        && (e instanceof AnimalEntity || e instanceof PlayerEntity))) {
            double distanceSq = client.player.squaredDistanceTo(entity);
            if (distanceSq > 16.0 * 16.0) continue;
            boolean inReach = distanceSq <= 9.0 && client.player.canSee(entity);
            boolean ready = client.player.getAttackCooldownProgress(0.0f) >= 0.95f;
            // Green means the attack is in reach and ready; red means not yet.
            int cr = inReach && ready ? 60 : 255;
            int cg = inReach && ready ? 235 : 70;
            int cb = 75;
            matrices.push();
            matrices.translate(entity.getX() - camera.x, entity.getY() - camera.y + 0.05,
                    entity.getZ() - camera.z);
            float radius = Math.max(0.45f, entity.getWidth() * 0.7f);
            final int segments = 32;
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                double b = Math.PI * 2.0 * (i + 1) / segments;
                lines.vertex(matrices.peek(), (float) (Math.cos(a) * radius), 0.0f,
                                (float) (Math.sin(a) * radius))
                        .color(cr, cg, cb, 240).normal(matrices.peek(), 0.0f, 1.0f, 0.0f);
                lines.vertex(matrices.peek(), (float) (Math.cos(b) * radius), 0.0f,
                                (float) (Math.sin(b) * radius))
                        .color(cr, cg, cb, 240).normal(matrices.peek(), 0.0f, 1.0f, 0.0f);
            }
            matrices.pop();
        }
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

        private static Arm clientArm() {
            return MinecraftClient.getInstance().options.getMainArm().getValue();
        }

        private static String targetName() {
            return switch (targetMode) {
                case MONSTERS -> tr("Монстры", "Monsters", "Монстри");
                case ANIMALS -> tr("Животные", "Animals", "Тварини");
                case PLAYERS -> tr("Игроки", "Players", "Гравці");
                case ALL -> tr("Все", "All", "Усі");
            };
        }

        private static String languageName() {
            return switch (language) {
                case RUSSIAN -> "Русский";
                case ENGLISH -> "English";
                case UKRAINIAN -> "Українська";
            };
        }

        // Keep UI preferences between Minecraft launches.
        private static final Path CONFIG_PATH =
                FabricLoader.getInstance().getConfigDir().resolve("okeanvisuals-ui.properties");

        private static void loadSettings() {
            if (!Files.exists(CONFIG_PATH)) return;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                try { language = Language.valueOf(props.getProperty("language", "RUSSIAN")); }
                catch (IllegalArgumentException ignored) { language = Language.RUSSIAN; }
                try { theme = Theme.valueOf(props.getProperty("theme", "OCEAN_BLUE")); }
                catch (IllegalArgumentException ignored) { theme = Theme.OCEAN_BLUE; }
                try { targetMode = AuraTarget.valueOf(props.getProperty("targets", "MONSTERS")); }
                catch (IllegalArgumentException ignored) { targetMode = AuraTarget.MONSTERS; }
                try {
                    opacityIndex = MathHelper.clamp(
                            Integer.parseInt(props.getProperty("opacity", "0")), 0, 2);
                } catch (NumberFormatException ignored) {
                    opacityIndex = 0;
                }
            } catch (IOException e) {
                System.err.println("Okean Visuals: could not load settings: " + e.getMessage());
            }
        }

        private static void saveSettings() {
            Properties props = new Properties();
            props.setProperty("language", language.name());
            props.setProperty("theme", theme.name());
            props.setProperty("targets", targetMode.name());
            props.setProperty("opacity", Integer.toString(opacityIndex));
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                    props.store(out, "Okean Visuals UI settings");
                }
            } catch (IOException e) {
                System.err.println("Okean Visuals: could not save settings: " + e.getMessage());
            }
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
            ph = Math.min(315, Math.max(215, height - 24));
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
            int rowGap = 26;

            if (tab == Tab.MISC) {
                Arm selectedArm = clientArm();
                addAction(tr("Рука: ", "Main hand: ", "Рука: ")
                                + (selectedArm == Arm.RIGHT
                                    ? tr("Правая", "Right", "Права")
                                    : tr("Левая", "Left", "Ліва")),
                        rowX, rowY, rowW, 22, b -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.options.getMainArm().setValue(
                                    clientArm() == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT);
                            client.options.write();
                            clearAndInit();
                        });
                addAction("Fullbright: " + (fullbright ? "ON" : "OFF"),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            fullbright = !fullbright;
                            clearAndInit();
                        });
                addAction("Kill Aura: " + (killAura ? "ON" : "OFF"),
                        rowX, rowY + 2 * rowGap, rowW, 22, b -> {
                            killAura = !killAura;
                            clearAndInit();
                        });
                addAction(tr("Цели: ", "Targets: ", "Цілі: ") + targetName(),
                        rowX, rowY + 3 * rowGap, rowW, 22, b -> {
                            targetMode = AuraTarget.values()[
                                (targetMode.ordinal() + 1) % AuraTarget.values().length];
                            saveSettings();
                            clearAndInit();
                        });
                addAction("AutoSprint: " + (autoSprint ? "ON" : "OFF"),
                        rowX, rowY + 4 * rowGap, rowW, 22, b -> {
                            autoSprint = !autoSprint;
                            clearAndInit();
                        });
                addAction(tr("Круги целей: ", "Target rings: ", "Кола цілей: ")
                                + (showRings ? "ON" : "OFF"),
                        rowX, rowY + 5 * rowGap, rowW, 22, b -> {
                            showRings = !showRings;
                            clearAndInit();
                        });
            } else if (tab == Tab.SETTINGS) {
                addAction(tr("Язык: ", "Language: ", "Мова: ") + languageName(),
                        rowX, rowY, rowW, 22, b -> {
                            language = Language.values()[(language.ordinal() + 1)
                                    % Language.values().length];
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Стиль: ", "Theme: ", "Стиль: ") + themeName(),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            theme = Theme.values()[(theme.ordinal() + 1)
                                    % Theme.values().length];
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Непрозрачность: ", "Opacity: ", "Непрозорість: ")
                                + transparencyName(),
                        rowX, rowY + 2 * rowGap, rowW, 22, b -> {
                            opacityIndex = (opacityIndex + 1) % 3;
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Горячие клавиши (R — Aura)", "Keybinds (R — Aura)",
                                "Гарячі клавіші (R — Aura)"),
                        rowX, rowY + 3 * rowGap, rowW, 22, b -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(new ControlsOptionsScreen(this, client.options));
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
                ctx.drawText(textRenderer, tr("Версия: 1.2.0 Test",
                        "Version: 1.2.0 Test", "Версія: 1.2.0 Test"),
                        tx, ty + 24, 0xFFFFFFFF, true);
                ctx.drawText(textRenderer, tr("Новости: Kill Aura, руки и настройки",
                        "News: Kill Aura, hands, settings", "Новини: Kill Aura, руки, налаштування"),
                        tx, ty + 46, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Помощь: Right Shift — открыть меню",
                        "Help: Right Shift — open menu", "Допомога: Right Shift — відкрити меню"),
                        tx, ty + 68, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Aura: только по желанию (OFF по умолчанию)",
                        "Aura: opt-in (OFF by default)", "Aura: за бажанням (OFF спочатку)"),
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
