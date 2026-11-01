package com.okean.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;
public class OkeanClient implements ClientModInitializer {
 public static final int CYAN=0xFF42CCFF;
 public static final Map<String,Boolean> MODULES=new LinkedHashMap<>();
 private static KeyBinding menuKey;
 public static boolean on(String name){return MODULES.getOrDefault(name,false);}
 public static void toggle(String name){MODULES.computeIfPresent(name,(k,v)->!v);}
 @Override public void onInitializeClient(){
  for(String name:new String[]{"AutoSprint","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Clean HUD"}) MODULES.put(name,false);
  MODULES.put("FPS HUD",true);
  menuKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Okean Client Menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"Okean Client"));
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   while(menuKey.wasPressed()){
    if(client.currentScreen instanceof ModuleScreen)client.setScreen(null);
    else client.setScreen(new ModuleScreen(client.currentScreen));
   }
   if(client.currentScreen instanceof TitleScreen)client.setScreen(new HomeScreen());
   if(client.player!=null&&on("AutoSprint")&&client.options.forwardKey.isPressed()&&!client.player.isSneaking())client.player.setSprinting(true);
  });
  HudRenderCallback.EVENT.register((draw,tick)->renderHud(draw));
 }
 private static void renderHud(DrawContext ctx){
  MinecraftClient mc=MinecraftClient.getInstance();
  if(mc.player==null||mc.world==null||mc.options.hudHidden)return;
  int y=8;
  if(on("FPS HUD"))y=line(ctx,"Okean | FPS "+mc.getCurrentFps(),y);
  if(on("XYZ HUD"))y=line(ctx,String.format("XYZ: %.0f / %.0f / %.0f",mc.player.getX(),mc.player.getY(),mc.player.getZ()),y);
  if(on("Direction"))y=line(ctx,"Facing: "+mc.player.getHorizontalFacing().asString(),y);
  if(on("Clock"))y=line(ctx,"Time: "+java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),y);
  if(on("Biome"))y=line(ctx,"Biome: "+mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k->k.getValue().getPath()).orElse("unknown"),y);
  if(on("Health HUD"))line(ctx,String.format("HP: %.1f",mc.player.getHealth()),y);
 }
 private static int line(DrawContext ctx,String str,int y){
  MinecraftClient mc=MinecraftClient.getInstance();
  if(!on("Clean HUD"))ctx.fill(6,y-3,12+mc.textRenderer.getWidth(str),y+12,0xA30A1722);
  ctx.drawText(mc.textRenderer,Text.literal(str),9,y,CYAN,true);
  return y+17;
 }
}