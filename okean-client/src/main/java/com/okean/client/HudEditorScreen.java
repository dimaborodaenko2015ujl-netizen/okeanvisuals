package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
public class HudEditorScreen extends Screen {
 private final Screen parent;
 private int dragging=0,offsetX,offsetY;
 public HudEditorScreen(Screen parent){super(Text.literal("Okean HUD Editor"));this.parent=parent;}
 private void card(DrawContext c,int x,int y,int w,int h,int color){
  c.fill(x+4,y,x+w-4,y+h,color);c.fill(x,y+4,x+w,y+h-4,color);
 }
 @Override public void renderBackground(DrawContext c,int mx,int my,float delta){}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  c.fill(0,0,width,height,0xA60B1423);
  card(c,width/2-157,8,314,35,0xEE142A40);
  c.drawCenteredTextWithShadow(textRenderer,"HUD EDITOR - drag panels with mouse",width/2,18,0xFFFFFFFF);
  c.drawCenteredTextWithShadow(textRenderer,"Esc = save and close",width/2,30,OkeanClient.accent());
  int wx=OkeanClient.watermarkX,wy=OkeanClient.watermarkY;
  card(c,wx,wy,186,22,0xEE162942);
  c.drawText(textRenderer,"Okean Client | drag",wx+8,wy+7,OkeanClient.accent(),true);
  int tx=OkeanClient.textX,ty=OkeanClient.textY;
  card(c,tx-4,ty-4,172,76,0xD5132738);
  c.drawText(textRenderer,"HUD TEXT - drag",tx+4,ty+5,0xFFFFFFFF,true);
  c.drawText(textRenderer,"FPS / XYZ / Items",tx+4,ty+24,OkeanClient.accent(),false);
  c.drawText(textRenderer,"All enabled text overlays",tx+4,ty+42,0xFFB8CCDA,false);
  int px=OkeanClient.targetX,py=OkeanClient.targetY;
  card(c,px,py,172,48,0xE31A2F43);
  c.drawText(textRenderer,"TARGET HUD - drag",px+8,py+10,OkeanClient.accent(),true);
  c.fill(px+8,py+32,px+156,py+37,0xFF45566C);
  c.fill(px+8,py+32,px+101,py+37,OkeanClient.accent());
  super.render(c,mx,my,delta);
 }
 private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
 @Override public boolean mouseClicked(double mx,double my,int b){
  if(b!=0)return super.mouseClicked(mx,my,b);
  if(inside(mx,my,OkeanClient.watermarkX,OkeanClient.watermarkY,186,22)){
   dragging=1;offsetX=(int)mx-OkeanClient.watermarkX;offsetY=(int)my-OkeanClient.watermarkY;return true;
  }
  if(inside(mx,my,OkeanClient.targetX,OkeanClient.targetY,172,48)){
   dragging=3;offsetX=(int)mx-OkeanClient.targetX;offsetY=(int)my-OkeanClient.targetY;return true;
  }
  if(inside(mx,my,OkeanClient.textX-4,OkeanClient.textY-4,172,76)){
   dragging=2;offsetX=(int)mx-OkeanClient.textX;offsetY=(int)my-OkeanClient.textY;return true;
  }
  return super.mouseClicked(mx,my,b);
 }
 @Override public boolean mouseDragged(double mx,double my,int b,double dx,double dy){
  if(dragging==0)return super.mouseDragged(mx,my,b,dx,dy);
  int x=Math.max(0,Math.min(width-190,(int)mx-offsetX));
  int y=Math.max(0,Math.min(height-85,(int)my-offsetY));
  if(dragging==1){OkeanClient.watermarkX=x;OkeanClient.watermarkY=y;}
  if(dragging==2){OkeanClient.textX=x;OkeanClient.textY=y;}
  if(dragging==3){OkeanClient.targetX=x;OkeanClient.targetY=y;}
  return true;
 }
 @Override public boolean mouseReleased(double mx,double my,int b){
  if(dragging!=0){dragging=0;OkeanClient.saveConfig();return true;}
  return super.mouseReleased(mx,my,b);
 }
 @Override public void close(){OkeanClient.saveConfig();client.setScreen(parent);}
 @Override public boolean shouldPause(){return false;}
}