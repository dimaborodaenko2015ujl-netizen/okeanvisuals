package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
public class HomeScreen extends Screen {
 public HomeScreen(){super(Text.literal("Okean Client"));}
 @Override protected void init(){
  int x=32,w=190,y=height/2-42;
  addDrawableChild(ButtonWidget.builder(Text.literal("Одиночная игра"),b->client.setScreen(new SelectWorldScreen(this))).dimensions(x,y,w,22).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Сетевая игра"),b->client.setScreen(new MultiplayerScreen(this))).dimensions(x,y+29,w,22).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Модули / Right Shift"),b->client.setScreen(new ModuleScreen(this))).dimensions(x,y+58,w,22).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Настройки"),b->client.setScreen(new OptionsScreen(this,client.options))).dimensions(x,y+87,w,22).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Выход"),b->client.scheduleStop()).dimensions(x,y+116,w,22).build());
 }
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  ctx.fillGradient(0,0,width,height,0xFF071621,0xFF102C43);
  ctx.fill(12,10,248,height-10,0xCC07111E);
  ctx.fill(12,10,15,height-10,OkeanClient.CYAN);
  ctx.drawText(textRenderer,Text.literal("~ OKEAN CLIENT"),32,32,OkeanClient.CYAN,true);
  ctx.drawText(textRenderer,Text.literal("Рады видеть тебя!"),32,59,0xFFFFFFFF,true);
  ctx.drawText(textRenderer,Text.literal("Fabric 1.21.4 | v0.1"),32,78,0xFF9AB8C8,false);
  ctx.drawCenteredTextWithShadow(textRenderer,"O K E A N",width*2/3,height/2-18,0xFFFFFFFF);
  ctx.drawCenteredTextWithShadow(textRenderer,"Your world. Your style.",width*2/3,height/2+4,OkeanClient.CYAN);
  super.render(ctx,mx,my,delta);
 }
 @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
}