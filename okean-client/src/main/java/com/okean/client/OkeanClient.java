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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
public class OkeanClient implements ClientModInitializer {
 public static final int CYAN=0xFF42CCFF;
 public static final Map<String,Boolean> MODULES=new LinkedHashMap<>();
 private static KeyBinding menuKey;
 private static boolean nightVisionApplied=false;
 private static long lastAttackTick=-100;
 private static long ticks=0;
 public static double killAuraRange=3.25;
 public static float autoTotemHealth=14f;
 public static String[] HUD_THEME_NAMES={"Cyan","Purple","Mint","Sunset","Pink"};
 public static int hudTheme=0;
 public static int accent(){return HUD_COLORS[hudTheme%HUD_COLORS.length];}
 public static String themeName(){return HUD_THEME_NAMES[hudTheme%HUD_THEME_NAMES.length];}
 public static int hudColor=0;
 public static int watermarkX=8,watermarkY=7;
 public static int targetX=12,targetY=120;
 public static boolean compactHud=false;
 public static int hudScale=100;
 private static Path configPath(){return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("okean-client.properties");}
 public static void saveConfig(){
  Properties p=new Properties();
  for(var e:MODULES.entrySet())p.setProperty("module."+e.getKey(),Boolean.toString(e.getValue()));
  p.setProperty("theme",Integer.toString(hudTheme));
  p.setProperty("hudScale",Integer.toString(hudScale));
  p.setProperty("watermarkX",Integer.toString(watermarkX));p.setProperty("watermarkY",Integer.toString(watermarkY));
  p.setProperty("targetX",Integer.toString(targetX));p.setProperty("targetY",Integer.toString(targetY));
  p.setProperty("totemHealth",Float.toString(autoTotemHealth));
  try(var out=Files.newOutputStream(configPath())){p.store(out,"Okean Client config");}
  catch(Exception ex){System.err.println("[Okean] Save failed: "+ex.getMessage());}
 }
 private static void loadConfig(){
  Path path=configPath();if(!Files.isRegularFile(path))return;
  Properties p=new Properties();
  try(var in=Files.newInputStream(path)){
   p.load(in);
   for(String name:MODULES.keySet())if(p.containsKey("module."+name))MODULES.put(name,Boolean.parseBoolean(p.getProperty("module."+name)));
   hudTheme=Math.floorMod(Integer.parseInt(p.getProperty("theme","0")),HUD_COLORS.length);
   hudScale=Math.max(50,Math.min(200,Integer.parseInt(p.getProperty("hudScale","100"))));
   watermarkX=Integer.parseInt(p.getProperty("watermarkX","8"));watermarkY=Integer.parseInt(p.getProperty("watermarkY","7"));
   targetX=Integer.parseInt(p.getProperty("targetX","12"));targetY=Integer.parseInt(p.getProperty("targetY","120"));
   autoTotemHealth=Float.parseFloat(p.getProperty("totemHealth","14"));
  }catch(Exception ex){System.err.println("[Okean] Load failed: "+ex.getMessage());}
 }
 public static final int[] HUD_COLORS={0xFF42CCFF,0xFFBA85FF,0xFF71E7A1,0xFFFFC069,0xFFFF728B};
 private static LivingEntity closestTarget(MinecraftClient client){
  if(client.player==null||client.world==null)return null;
  LivingEntity best=null;
  double distance=killAuraRange*killAuraRange;
  for(Entity entity:client.world.getEntities()){
   if(entity==client.player||!(entity instanceof LivingEntity living)||!living.isAlive()||living.isSpectator())continue;
   double d=client.player.squaredDistanceTo(living);
   if(d<distance){distance=d;best=living;}
  }
  return best;
 }
 public static boolean on(String name){return MODULES.getOrDefault(name,false);}
 public static void toggle(String name){
  MODULES.computeIfPresent(name,(k,v)->!v);
  saveConfig();
 }
 @Override public void onInitializeClient(){
  for(String name:new String[]{"AutoSprint","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Clean HUD","Speed HUD","Ping HUD","Fullbright","Watermark","KillAura","Target HUD","Durability HUD","Jump Assist","Item HUD","Armor HUD","Session HUD","CPS HUD","Crosshair Info","Hotbar HUD","Module List","Watermark Accent","GUI Move","AutoTotem","Center Dot","Health Bar","Coordinates Box"}) MODULES.put(name,false);
  MODULES.put("FPS HUD",true);
  MODULES.put("Watermark",true);
  MODULES.put("Item HUD",true);
  loadConfig();
  menuKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Okean Client Menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"Okean Client"));
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   ticks++;
   while(menuKey.wasPressed()){
    if(client.currentScreen instanceof ModuleScreen)client.setScreen(null);
    else client.setScreen(new ModuleScreen(client.currentScreen));
   }
   if(client.currentScreen instanceof TitleScreen)client.setScreen(new HomeScreen());
   if(client.player!=null&&on("AutoSprint")&&client.options.forwardKey.isPressed()&&!client.player.isSneaking())client.player.setSprinting(true);
   if(on("GUI Move")&&client.currentScreen!=null&&!(client.currentScreen instanceof ChatScreen)&&client.player!=null){
    long handle=client.getWindow().getHandle();
    client.options.forwardKey.setPressed(InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_W));
    client.options.backKey.setPressed(InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_S));
    client.options.leftKey.setPressed(InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_A));
    client.options.rightKey.setPressed(InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_D));
    client.options.jumpKey.setPressed(InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_SPACE));
   }
   if(on("AutoTotem")&&client.player!=null&&client.currentScreen==null&&client.interactionManager!=null
       &&client.player.getHealth()<=autoTotemHealth&&!client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)){
    int slot=-1;
    for(int i=0;i<client.player.getInventory().main.size();i++)
     if(client.player.getInventory().main.get(i).isOf(Items.TOTEM_OF_UNDYING)){slot=i;break;}
    if(slot>=0){
     int screenSlot=slot<9?36+slot:slot;
     int sync=client.player.currentScreenHandler.syncId;
     client.interactionManager.clickSlot(sync,screenSlot,0,SlotActionType.PICKUP,client.player);
     client.interactionManager.clickSlot(sync,45,0,SlotActionType.PICKUP,client.player);
     client.interactionManager.clickSlot(sync,screenSlot,0,SlotActionType.PICKUP,client.player);
    }
   }
   if(client.player!=null&&client.world!=null&&client.currentScreen==null){
    if(on("Jump Assist")&&client.options.forwardKey.isPressed()&&client.player.isOnGround())client.player.jump();
    if(on("KillAura")&&client.interactionManager!=null&&ticks-lastAttackTick>=12
      &&client.player.getAttackCooldownProgress(0.0f)>=0.95f){
     LivingEntity target=closestTarget(client);
     if(target!=null){
      client.interactionManager.attackEntity(client.player,target);
      client.player.swingHand(Hand.MAIN_HAND);
      lastAttackTick=ticks;
     }
    }
   }
   if(client.player!=null){
    if(on("Fullbright")){
     client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,260,0,false,false,false));
     nightVisionApplied=true;
    }else if(nightVisionApplied){
     client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
     nightVisionApplied=false;
    }
   }
  });
  HudRenderCallback.EVENT.register((draw,tick)->renderHud(draw));
 }
 private static void renderHud(DrawContext ctx){
  MinecraftClient mc=MinecraftClient.getInstance();
  if(mc.player==null||mc.world==null||mc.options.hudHidden)return;
  int y=on("Watermark")?Math.max(30,watermarkY+29):8;
  if(on("Watermark")){
   int ping=0;
   if(mc.getNetworkHandler()!=null){
    var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
    if(entry!=null)ping=entry.getLatency();
   }
   String label="Okean Client  /  "+ping+" ms  /  "+mc.getCurrentFps()+" FPS";
   // HUD scale is a size setting, independent of color themes.
   ctx.getMatrices().push();
   ctx.getMatrices().scale(hudScale/100f,hudScale/100f,1f);
   int w=mc.textRenderer.getWidth(label)+42;
   rounded(ctx,watermarkX,watermarkY,watermarkX+w,watermarkY+20,0xE20B1220);
   ctx.fill(watermarkX,watermarkY+3,watermarkX+3,watermarkY+17,accent());
   ctx.drawText(mc.textRenderer,Text.literal("O"),watermarkX+10,watermarkY+6,0xFFB39CFF,true);
   ctx.drawText(mc.textRenderer,Text.literal(label),watermarkX+25,watermarkY+6,0xFFFFFFFF,true);
   ctx.getMatrices().pop();
  }
  if(on("Center Dot")){
   int cx=ctx.getScaledWindowWidth()/2,cy=ctx.getScaledWindowHeight()/2;
   ctx.fill(cx-1,cy-1,cx+2,cy+2,accent());
  }
  if(on("Health Bar")){
   int max=Math.max(1,(int)mc.player.getMaxHealth()),current=Math.max(0,(int)mc.player.getHealth());
   int x=ctx.getScaledWindowWidth()/2-55,bottom=ctx.getScaledWindowHeight()-62;
   rounded(ctx,x,bottom,x+110,bottom+8,0xB9000C19);
   ctx.fill(x+2,bottom+2,x+2+(106*Math.min(current,max)/max),bottom+6,accent());
  }
  if(on("Coordinates Box")){
   int x=ctx.getScaledWindowWidth()-136;
   rounded(ctx,x,8,x+128,36,0xDA101C2B);
   ctx.drawText(mc.textRenderer,"XYZ "+mc.player.getBlockX()+", "+mc.player.getBlockY()+", "+mc.player.getBlockZ(),x+6,18,accent(),true);
  }
  if(on("Session HUD"))y=line(ctx,"Session ticks: "+ticks,y);
  if(on("Crosshair Info"))y=line(ctx,"Look: "+mc.player.getHorizontalFacing().asString(),y);
  if(on("CPS HUD"))y=line(ctx,"Attack cooldown: "+(int)(mc.player.getAttackCooldownProgress(0)*100)+"%",y);
  if(on("Armor HUD")) {
   int armorPieces=0;
   for(ItemStack stack:mc.player.getInventory().armor)if(!stack.isEmpty())armorPieces++;
   y=line(ctx,"Armor pieces: "+armorPieces+"/4",y);
  }
  if(on("Hotbar HUD"))y=line(ctx,"Selected slot: "+(mc.player.getInventory().selectedSlot+1),y);
  if(on("Item HUD")) {
   y=line(ctx,"Totems: "+count(mc,Items.TOTEM_OF_UNDYING)+" | Pearls: "+count(mc,Items.ENDER_PEARL),y);
   y=line(ctx,"Golden apples: "+count(mc,Items.GOLDEN_APPLE),y);
  }
  if(on("Module List")){
   int active=0;
   for(Boolean state:MODULES.values())if(state)active++;
   y=line(ctx,"Modules enabled: "+active,y);
  }
  if(on("FPS HUD"))y=line(ctx,"Okean | FPS "+mc.getCurrentFps(),y);
  if(on("XYZ HUD"))y=line(ctx,String.format("XYZ: %.0f / %.0f / %.0f",mc.player.getX(),mc.player.getY(),mc.player.getZ()),y);
  if(on("Direction"))y=line(ctx,"Facing: "+mc.player.getHorizontalFacing().asString(),y);
  if(on("Clock"))y=line(ctx,"Time: "+java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),y);
  if(on("Biome"))y=line(ctx,"Biome: "+mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k->k.getValue().getPath()).orElse("unknown"),y);
  if(on("Health HUD"))y=line(ctx,String.format("HP: %.1f",mc.player.getHealth()),y);
  if(on("Speed HUD"))y=line(ctx,String.format("Speed: %.2f b/s",mc.player.getVelocity().horizontalLength()*20.0),y);
  if(on("Durability HUD")&&mc.player.getMainHandStack().isDamageable()){
   var stack=mc.player.getMainHandStack();
   y=line(ctx,"Durability: "+(stack.getMaxDamage()-stack.getDamage())+"/"+stack.getMaxDamage(),y);
  }
  if(on("Target HUD")){
   LivingEntity target=closestTarget(mc);
   if(target!=null){
    int x=targetX,t=targetY,ww=174;
    rounded(ctx,x,t,x+ww,t+48,0xE3162234);
    ctx.fill(x+3,t+3,x+39,t+39,0xFF35465A);
    ctx.drawText(mc.textRenderer,target.getName().getString().substring(0,Math.min(18,target.getName().getString().length())),x+45,t+8,0xFFFFFFFF,true);
    ctx.drawText(mc.textRenderer,String.format("%.1f HP",target.getHealth()),x+45,t+23,accent(),false);
    float health=Math.max(0,Math.min(1,target.getHealth()/Math.max(1,target.getMaxHealth())));
    ctx.fill(x+45,t+37,x+166,t+42,0xFF334055);
    ctx.fill(x+45,t+37,x+45+(int)(121*health),t+42,accent());
   }
  }
  if(on("Ping HUD")&&mc.getNetworkHandler()!=null){
   var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
   if(entry!=null)y=line(ctx,"Ping: "+entry.getLatency()+" ms",y);
  }
 }
 private static void rounded(DrawContext c,int x,int y,int r,int b,int color){
  c.fill(x+3,y,r-3,b,color);c.fill(x,y+3,r,b-3,color);
 }
 private static int count(MinecraftClient mc, Item item){
  int n=0;
  for(ItemStack stack:mc.player.getInventory().main)if(stack.isOf(item))n+=stack.getCount();
  if(mc.player.getOffHandStack().isOf(item))n+=mc.player.getOffHandStack().getCount();
  return n;
 }
 private static int line(DrawContext ctx,String str,int y){
  MinecraftClient mc=MinecraftClient.getInstance();
  if(!on("Clean HUD"))ctx.fill(6,y-3,12+mc.textRenderer.getWidth(str),y+12,0xA30A1722);
  ctx.drawText(mc.textRenderer,Text.literal(str),9,y,accent(),true);
  return y+17;
 }
}