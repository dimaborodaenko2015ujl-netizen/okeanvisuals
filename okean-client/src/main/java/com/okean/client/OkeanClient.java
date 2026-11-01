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
 public static boolean compactHud=false;
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
 }
 @Override public void onInitializeClient(){
  for(String name:new String[]{"AutoSprint","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Clean HUD","Speed HUD","Ping HUD","Fullbright","Watermark","KillAura","Target HUD","Durability HUD","Jump Assist","Item HUD","Armor HUD","Session HUD","CPS HUD","Crosshair Info","Hotbar HUD","Module List","Watermark Accent","GUI Move","AutoTotem"}) MODULES.put(name,false);
  MODULES.put("FPS HUD",true);
  MODULES.put("Watermark",true);
  MODULES.put("Item HUD",true);
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
    var keys=new net.minecraft.client.option.KeyBinding[]{client.options.forwardKey,client.options.backKey,client.options.leftKey,client.options.rightKey,client.options.jumpKey};
    for(var key:keys)if(key.getBoundKey().getCategory()==InputUtil.Type.KEYSYM)
     key.setPressed(InputUtil.isKeyPressed(handle,key.getBoundKey().getCode()));
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
  int y=on("Watermark")?30:8;
  if(on("Watermark")){
   int ping=0;
   if(mc.getNetworkHandler()!=null){
    var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
    if(entry!=null)ping=entry.getLatency();
   }
   String label="Okean Client  /  "+ping+" ms  /  "+mc.getCurrentFps()+" FPS";
   int w=mc.textRenderer.getWidth(label)+42;
   ctx.fill(7,5,w,25,0xE20B1220);
   ctx.fill(7,5,10,25,on("Watermark Accent")?accent():0xFF6B50E9);
   ctx.drawText(mc.textRenderer,Text.literal("O"),16,11,0xFFB39CFF,true);
   ctx.drawText(mc.textRenderer,Text.literal(label),31,11,0xFFFFFFFF,true);
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
   if(target!=null)y=line(ctx,"Target: "+target.getName().getString()+" | HP "+String.format("%.1f",target.getHealth()),y);
  }
  if(on("Ping HUD")&&mc.getNetworkHandler()!=null){
   var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
   if(entry!=null)y=line(ctx,"Ping: "+entry.getLatency()+" ms",y);
  }
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
  ctx.drawText(mc.textRenderer,Text.literal(str),9,y,HUD_COLORS[hudColor],true);
  return y+17;
 }
}