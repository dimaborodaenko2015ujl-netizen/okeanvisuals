package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModuleScreen extends Screen {
 private final Screen previous;
 private int page=0,scroll=0;
 private final String[][] groups={
  {"COMBAT","KillAura","Target HUD"},
  {"MOVEMENT","AutoSprint","Jump Assist","GUI Move"},
  {"VISUALS","Fullbright","Item Radar","Mob Radar","Player Radar","Center Dot","Health Bar","Coordinates Box"},
  {"PLAYER","AutoTotem","Armor HUD","Health HUD"},
  {"MISC","Watermark","Clock","Module List"}
 };
 private final String[] hud={"Watermark","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Speed HUD","Ping HUD","Item HUD","Armor HUD","Session HUD","CPS HUD","Crosshair Info","Hotbar HUD","Module List","Durability HUD","Target HUD","Clean HUD"};
 public ModuleScreen(Screen prior){super(Text.literal("Okean Client"));previous=prior;}
 private void rounded(DrawContext c,int x,int y,int w,int h,int color){
  c.fill(x+5,y,x+w-5,y+h,color);c.fill(x,y+5,x+w,y+h-5,color);
  c.fill(x+2,y+2,x+w-2,y+h-2,color);
 }
 @Override public void renderBackground(DrawContext c,int mx,int my,float d){}
 @Override public void render(DrawContext c,int mx,int my,float d){
  c.fill(0,0,width,height,0x85060D18);
  int maxWidth=Math.min(width-24,1020),x0=(width-maxWidth)/2;
  int headerY=Math.max(12,height/2-177);
  rounded(c,x0,headerY,maxWidth,47,0xEA0B1827);
  c.drawText(textRenderer,"O K E A N  C L I E N T",x0+16,headerY+11,0xFFFFFFFF,true);
  c.drawText(textRenderer,"Fabric 1.21.4  |  "+OkeanClient.themeName(),x0+16,headerY+27,OkeanClient.accent(),false);
  int hudButtonX=x0+maxWidth-96;
  rounded(c,hudButtonX,headerY+10,80,25,page==1?0xFF277DA7:0xFF263B50);
  c.drawCenteredTextWithShadow(textRenderer,page==1?"MODULES":"HUD",hudButtonX+40,headerY+18,0xFFFFFFFF);
  int y=headerY+57;
  if(page==0){
   int gap=9,cardW=(maxWidth-4*gap)/5;
   for(int i=0;i<5;i++){
    int x=x0+i*(cardW+gap);
    rounded(c,x,y,cardW,250,0xE41B2535);
    c.fill(x+10,y+37,x+cardW-10,y+38,0xFF314355);
    c.drawText(textRenderer,groups[i][0],x+12,y+13,OkeanClient.accent(),true);
    for(int j=1;j<groups[i].length;j++){
     int yy=y+45+(j-1)*29;
     boolean enabled=OkeanClient.on(groups[i][j]);
     rounded(c,x+7,yy,cardW-14,25,enabled?0xB52A4257:0x8F223142);
     String s=groups[i][j];
     if(s.length()>14)s=s.substring(0,14);
     c.drawText(textRenderer,s,x+15,yy+8,0xFFE5EDF5,false);
     rounded(c,x+cardW-35,yy+7,20,11,enabled?OkeanClient.accent():0xFF546276);
    }
   }
  }else{
   int columns=2,colW=(maxWidth-10)/2;
   for(int col=0;col<columns;col++){
    int xx=x0+col*(colW+10);
    rounded(c,xx,y,colW,250,0xE41B2535);
    for(int row=0;row<8;row++){
     int index=scroll+col*8+row;
     if(index>=hud.length)break;
     int yy=y+9+row*29;
     rounded(c,xx+8,yy,colW-16,25,0xB5233548);
     c.drawText(textRenderer,hud[index],xx+18,yy+8,0xFFFFFFFF,false);
     rounded(c,xx+colW-38,yy+7,21,11,OkeanClient.on(hud[index])?OkeanClient.accent():0xFF526378);
    }
   }
  }
  rounded(c,x0,y+260,maxWidth,37,0xD8122336);
  c.drawText(textRenderer,"HUD SIZE: "+OkeanClient.hudScale+"%   [-] [+]",x0+maxWidth-183,y+273,OkeanClient.accent(),true);
  c.drawText(textRenderer,"LMB: toggle   RMB: theme / setting   Wheel: scroll HUD",x0+14,y+273,0xFFB9C8D6,false);
  super.render(c,mx,my,d);
 }
 @Override public boolean mouseClicked(double mx,double my,int b){
  int w=Math.min(width-24,1020),x0=(width-w)/2,y0=Math.max(12,height/2-177),y=y0+57;
  if(b==0&&mx>=x0+w-96&&mx<=x0+w-16&&my>=y0+10&&my<=y0+35){page=1-page;scroll=0;return true;}
  if(page==0){
   int gap=9,cardW=(w-4*gap)/5;
   for(int i=0;i<5;i++){
    int x=x0+i*(cardW+gap);
    for(int j=1;j<groups[i].length;j++){
     int yy=y+45+(j-1)*29;
     if(mx>=x+7&&mx<=x+cardW-7&&my>=yy&&my<yy+25){change(groups[i][j],b);return true;}
    }
   }
  }else{
   int colW=(w-10)/2;
   for(int col=0;col<2;col++)for(int row=0;row<8;row++){
    int index=scroll+col*8+row;
    if(index>=hud.length)continue;
    int xx=x0+col*(colW+10),yy=y+9+row*29;
    if(mx>=xx+8&&mx<xx+colW-8&&my>=yy&&my<yy+25){change(hud[index],b);return true;}
   }
  }
  return super.mouseClicked(mx,my,b);
 }
 private void change(String name,int button){
  if(button==0){OkeanClient.toggle(name);return;}
  if(button==1){
   if(name.equals("KillAura")){OkeanClient.killAuraRange+=0.25;if(OkeanClient.killAuraRange>3.25)OkeanClient.killAuraRange=2.5;}
   else if(name.equals("AutoTotem")){OkeanClient.autoTotemHealth+=2;if(OkeanClient.autoTotemHealth>20)OkeanClient.autoTotemHealth=8;}
   else OkeanClient.hudTheme=(OkeanClient.hudTheme+1)%OkeanClient.HUD_COLORS.length;
   OkeanClient.saveConfig();
  }
 }
 @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
  if(page==1){scroll=Math.max(0,Math.min(Math.max(0,hud.length-16),scroll-(int)Math.signum(vertical)));return true;}
  return super.mouseScrolled(mx,my,horizontal,vertical);
 }
 @Override public void close(){client.setScreen(previous);}
 @Override public boolean shouldPause(){return false;}
}