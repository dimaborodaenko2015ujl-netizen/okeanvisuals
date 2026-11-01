package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
public class ConfigScreen extends Screen {
 private final Screen previous;
 private String status="Настройки сохраняются автоматически";
 public ConfigScreen(Screen previous){super(Text.literal("Okean Configs"));this.previous=previous;}
 @Override protected void init(){
  addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить сейчас"),b->{OkeanClient.saveConfig();status="Конфиг сохранён";})
   .dimensions(width/2-100,height/2,200,24).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->client.setScreen(previous))
   .dimensions(width/2-100,height/2+35,200,24).build());
 }
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  ctx.fillGradient(0,0,width,height,0xFF081522,0xFF17344D);
  ctx.drawCenteredTextWithShadow(textRenderer,"OKEАN CLIENT / КОНФИГИ",width/2,height/2-60,OkeanClient.accent());
  ctx.drawCenteredTextWithShadow(textRenderer,status,width/2,height/2-38,0xFFFFFFFF);
  ctx.drawCenteredTextWithShadow(textRenderer,"config/okean-client.properties",width/2,height/2-22,0xFFAFBCCB);
  super.render(ctx,mx,my,delta);
 }
 @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
 @Override public void close(){client.setScreen(previous);}
}