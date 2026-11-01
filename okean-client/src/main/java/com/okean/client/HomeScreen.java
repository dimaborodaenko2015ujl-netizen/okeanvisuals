package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
public class HomeScreen extends Screen {
 private final String[] labels={"Одиночная игра","Сетевая игра","Модули / Right Shift","Настройки","Выход"};
 public HomeScreen(){super(Text.literal("Okean Client"));}
 @Override protected void init(){}
 private int startY(){return height/2-40;}
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  ctx.fillGradient(0,0,width,height,0xFF061823,0xFF123450);
  ctx.fill(12,10,255,height-10,0xE5091728);
  ctx.fill(12,10,16,height-10,OkeanClient.CYAN);
  ctx.drawText(textRenderer,Text.literal("~ OKEAN CLIENT"),34,35,OkeanClient.CYAN,true);
  ctx.drawText(textRenderer,Text.literal("Рады видеть тебя!"),34,62,0xFFFFFFFF,true);
  ctx.drawText(textRenderer,Text.literal("Fabric 1.21.4 | v0.2"),34,83,0xFFB9D3E1,false);
  ctx.drawCenteredTextWithShadow(textRenderer,"O K E A N",width*2/3,height/2-18,0xFFFFFFFF);
  ctx.drawCenteredTextWithShadow(textRenderer,"Your world. Your style.",width*2/3,height/2+4,OkeanClient.CYAN);
  for(int i=0;i<labels.length;i++){
   int x=34,y=startY()+i*31,w=190,h=24;
   boolean hover=mx>=x&&mx<x+w&&my>=y&&my<y+h;
   ctx.fill(x,y,x+w,y+h,hover?0xFF46CCF8:0xFF177FB8);
   ctx.drawBorder(x,y,w,h,hover?0xFFC7F5FF:0xFF48C8F3);
   ctx.drawCenteredTextWithShadow(textRenderer,labels[i],x+w/2,y+8,0xFFFFFFFF);
  }
  super.render(ctx,mx,my,delta);
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  if(button==0&&mx>=34&&mx<224){
   for(int i=0;i<labels.length;i++){
    int y=startY()+i*31;
    if(my>=y&&my<y+24){
     switch(i){
      case 0 -> client.setScreen(new SelectWorldScreen(this));
      case 1 -> client.setScreen(new MultiplayerScreen(this));
      case 2 -> client.setScreen(new ModuleScreen(this));
      case 3 -> client.setScreen(new OptionsScreen(this,client.options));
      case 4 -> client.scheduleStop();
     }
     return true;
    }
   }
  }
  return super.mouseClicked(mx,my,button);
 }
 @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
}