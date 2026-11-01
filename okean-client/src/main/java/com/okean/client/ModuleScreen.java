package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
public class ModuleScreen extends Screen {
 private final Screen previous;
 private final String[] hud={"Watermark","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Speed HUD","Ping HUD","Item HUD","Armor HUD","Session HUD","CPS HUD","Crosshair Info","Hotbar HUD","Module List","Durability HUD","Target HUD","Clean HUD"};
 private final String[] utility={"GUI Move","AutoSprint","Jump Assist","Fullbright","KillAura","AutoTotem"};
 public ModuleScreen(Screen previous){super(Text.literal("Okean HUD"));this.previous=previous;}
 @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
 private int left(){return Math.max(8,width/2-335);}
 private int top(){return 40;}
 private void panel(DrawContext c,int x,int y,int w,int h,int color){c.fill(x+3,y,x+w-3,y+h,color);c.fill(x,y+3,x+w,y+h-3,color);}
 @Override public void render(DrawContext c,int mx,int my,float d){
  c.fill(0,0,width,height,0xB008111E);
  int x=left(),y=top();
  panel(c,x,y,670,Math.min(height-50,516),0xE8101B2C);
  panel(c,x+12,y+10,646,31,0xEF1A2D43);
  c.drawText(textRenderer,"OKEАN CLIENT  /  HUD",x+23,y+21,0xFFFFFFFF,true);
  c.drawText(textRenderer,"Theme: "+OkeanClient.themeName(),x+475,y+21,OkeanClient.accent(),true);
  c.drawText(textRenderer,"Left click: toggle    Right click: settings / theme",x+23,y+51,0xFFA9BACC,false);
  drawList(c,hud,x+18,y+79,310);
  drawList(c,utility,x+342,y+79,310);
  panel(c,x+342,y+235,310,52,0xFF1D3147);
  c.drawText(textRenderer,"THEMES: right click here",x+354,y+247,0xFFFFFFFF,true);
  c.drawText(textRenderer,OkeanClient.themeName(),x+354,y+266,OkeanClient.accent(),false);
  super.render(c,mx,my,d);
 }
 private void drawList(DrawContext c,String[] names,int x,int y,int width){
  for(int i=0;i<names.length;i++){
   int yy=y+i*21; if(yy>height-32)break;
   panel(c,x,yy,width,18,0xCF17283A);
   String name=names[i];
   if(name.equals("KillAura")) name+=" ["+String.format("%.2f",OkeanClient.killAuraRange)+"]";
   if(name.equals("AutoTotem"))name+=" ["+String.format("%.0f",OkeanClient.autoTotemHealth)+" HP]";
   c.drawText(textRenderer,name,x+8,yy+5,0xFFFFFFFF,false);
   panel(c,x+width-35,yy+3,25,12,OkeanClient.on(names[i])?OkeanClient.accent():0xFF485D6E);
  }
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  int x=left(),y=top();
  if(clickList(mx,my,button,hud,x+18,y+79,310))return true;
  if(clickList(mx,my,button,utility,x+342,y+79,310))return true;
  if(button==1&&mx>=x+342&&mx<x+652&&my>=y+235&&my<y+287){OkeanClient.hudTheme=(OkeanClient.hudTheme+1)%OkeanClient.HUD_COLORS.length;return true;}
  return super.mouseClicked(mx,my,button);
 }
 private boolean clickList(double mx,double my,int button,String[] names,int x,int y,int w){
  if(mx<x||mx>x+w||my<y)return false;
  int idx=(int)((my-y)/21);
  if(idx<0||idx>=names.length||my>=y+idx*21+18)return false;
  String name=names[idx];
  if(button==0){OkeanClient.toggle(name);return true;}
  if(button==1){
   if(name.equals("KillAura")){OkeanClient.killAuraRange+=0.25;if(OkeanClient.killAuraRange>3.25)OkeanClient.killAuraRange=2.5;}
   else if(name.equals("AutoTotem")){OkeanClient.autoTotemHealth+=2;if(OkeanClient.autoTotemHealth>20)OkeanClient.autoTotemHealth=8;}
   else OkeanClient.hudTheme=(OkeanClient.hudTheme+1)%OkeanClient.HUD_COLORS.length;
   return true;
  }
  return false;
 }
 @Override public void close(){client.setScreen(previous);}
 @Override public boolean shouldPause(){return false;}
}