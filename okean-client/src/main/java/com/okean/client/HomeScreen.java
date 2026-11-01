package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
public class HomeScreen extends Screen {
 private final String[] nav={"Главная","Модули","HUD","Конфиги","Настройки","Выход"};
 public HomeScreen(){super(Text.literal("Okean Client"));}
 @Override protected void init(){}
 private void box(DrawContext c,int x,int y,int w,int h,int color){
  c.fill(x+5,y,x+w-5,y+h,color);c.fill(x,y+5,x+w,y+h-5,color);
 }
 @Override public void renderBackground(DrawContext c,int x,int y,float d){}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  c.fillGradient(0,0,width,height,0xFF071523,0xFF15334A);
  // Layered, atmospheric original pixel landscape; no external images.
  int horizon=height*2/3;
  c.fill(0,horizon,width,height,0xFF0B2636);
  for(int i=0;i<23;i++){
   int px=(i*131+77)%Math.max(1,width);
   int h=50+(i*43)%100;
   c.fill(px,horizon-h,px+36,horizon,0xFF163746);
   c.fill(px+14,horizon-h-14,px+22,horizon-h+5,0xFF245363);
  }
  int side=Math.max(130,Math.min(194,width/4));
  box(c,9,10,side,height-20,0xEE081523);
  c.drawText(textRenderer,"≈ OKEAN",29,37,OkeanClient.accent(),true);
  c.drawText(textRenderer,"CLIENT",29,52,0xFFFFFFFF,true);
  for(int i=0;i<nav.length;i++){
   int y=94+i*34;
   boolean hot=mx>=19&&mx<=side+1&&my>=y&&my<y+27;
   box(c,19,y,side-18,27,hot?0xFF187BAC:0xCC14283A);
   c.drawText(textRenderer,nav[i],32,y+9,0xFFFFFFFF,false);
  }
  int x=side+30;
  c.drawText(textRenderer,"Рады видеть тебя",x,55,0xFFFFFFFF,true);
  c.drawText(textRenderer,"в Okean Client!",x,74,OkeanClient.accent(),true);
  c.drawText(textRenderer,"Minecraft Fabric 1.21.4",x,96,0xFFBCD7E5,false);
  box(c,x,126,Math.min(width-x-15,310),78,0xC90E2635);
  c.drawText(textRenderer,"YOUR WORLD. YOUR STYLE.",x+14,141,0xFFFFFFFF,true);
  c.drawText(textRenderer,"Right Shift - ClickGUI",x+14,161,OkeanClient.accent(),false);
  c.drawText(textRenderer,"Mod settings are saved",x+14,179,0xFFC2D4DE,false);
  String[] games={"Одиночная игра","Сетевая игра"};
  for(int i=0;i<2;i++){
   int y=220+i*36;
   box(c,x,y,Math.min(width-x-15,310),29,mx>=x&&mx<=x+310&&my>=y&&my<y+29?0xFF29A2CB:0xDF176B9A);
   c.drawText(textRenderer,games[i],x+13,y+11,0xFFFFFFFF,true);
  }
  super.render(c,mx,my,delta);
 }
 @Override public boolean mouseClicked(double mx,double my,int b){
  if(b!=0)return super.mouseClicked(mx,my,b);
  int side=Math.max(130,Math.min(194,width/4)),x=side+30;
  if(mx>=19&&mx<=side+1){
   for(int i=0;i<nav.length;i++){
    int y=94+i*34;
    if(my>=y&&my<y+27){
     switch(i){
      case 0->client.setScreen(new HomeScreen());
      case 1,2,3->client.setScreen(new ModuleScreen(this));
      case 4->client.setScreen(new OptionsScreen(this,client.options));
      case 5->client.scheduleStop();
     }
     return true;
    }
   }
  }
  if(mx>=x&&mx<=x+310){
   if(my>=220&&my<249){client.setScreen(new SelectWorldScreen(this));return true;}
   if(my>=256&&my<285){client.setScreen(new MultiplayerScreen(this));return true;}
  }
  return super.mouseClicked(mx,my,b);
 }
}