package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
public class ModuleScreen extends Screen {
 private final Screen previous;
 private int tab=0,drag=0,dx=0,dy=0,scroll=0;
 private final String[] hud={"Watermark","FPS HUD","XYZ HUD","Direction","Clock","Biome","Health HUD","Speed HUD","Ping HUD","Item HUD","Armor HUD","Session HUD","CPS HUD","Crosshair Info","Hotbar HUD","Module List","Durability HUD","Target HUD","Clean HUD"};
 private final String[] visual={"Fullbright","Center Dot","Health Bar","Coordinates Box"};
 private final String[] utility={"GUI Move","AutoSprint","Jump Assist","KillAura","AutoTotem"};
 public ModuleScreen(Screen prev){super(Text.literal("Okean Client"));previous=prev;}
 private int left(){return width/2-214;}
 private int top(){return height/2-151;}
 private void panel(DrawContext c,int x,int y,int w,int h,int col){
  c.fill(x+4,y,x+w-4,y+h,col);c.fill(x,y+4,x+w,y+h-4,col);
 }
 private String[] current(){return tab==0?hud:tab==1?visual:utility;}
 @Override public void renderBackground(DrawContext c,int x,int y,float d){}
 @Override public void render(DrawContext c,int mx,int my,float d){
  c.fill(0,0,width,height,0x7107111B);
  int x=left(),y=top();
  panel(c,x,y,428,302,0xF0162234);
  panel(c,x+9,y+9,410,40,0xFF203349);
  c.drawText(textRenderer,"O K E A N",x+20,y+25,0xFFFFFFFF,true);
  c.drawText(textRenderer,"Theme: "+OkeanClient.themeName(),x+285,y+25,OkeanClient.accent(),true);
  String[] tabs={"HUD","VISUALS","UTILITY"};
  for(int i=0;i<3;i++){
   int tx=x+16+i*136;
   panel(c,tx,y+57,126,24,i==tab?0xFF245773:0xFF253143);
   c.drawCenteredTextWithShadow(textRenderer,tabs[i],tx+63,y+65,i==tab?OkeanClient.accent():0xFFAABACB);
  }
  String[] names=current();
  int max=Math.min(names.length,8);
  for(int j=0;j<max;j++){
   int idx=j+scroll;if(idx>=names.length)break;
   int yy=y+92+j*23;
   panel(c,x+16,yy,396,20,0xFF1C2C3E);
   String name=names[idx];
   if(name.equals("KillAura"))name+=" ["+String.format("%.2f",OkeanClient.killAuraRange)+"]";
   if(name.equals("AutoTotem"))name+=" ["+String.format("%.0f",OkeanClient.autoTotemHealth)+" HP]";
   c.drawText(textRenderer,name,x+27,yy+6,0xFFFFFFFF,false);
   panel(c,x+372,yy+4,29,12,OkeanClient.on(names[idx])?OkeanClient.accent():0xFF536378);
  }
  c.drawText(textRenderer,"LMB: toggle  |  RMB: setting  |  scroll: list",x+19,y+282,0xFF9EAEBD,false);
  // Movable overlay previews can be dragged by their title bars on the HUD tab.
  if(tab==0){
   panel(c,Math.max(4,OkeanClient.watermarkX),Math.max(4,OkeanClient.watermarkY),104,15,0xD42B314F);
   c.drawText(textRenderer,"Okean | drag",OkeanClient.watermarkX+8,OkeanClient.watermarkY+3,OkeanClient.accent(),false);
   panel(c,OkeanClient.targetX,OkeanClient.targetY,110,19,0xDB24384B);
   c.drawText(textRenderer,"Target HUD | drag",OkeanClient.targetX+6,OkeanClient.targetY+5,OkeanClient.accent(),false);
  }
  super.render(c,mx,my,d);
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  int x=left(),y=top();
  if(tab==0&&button==0){
   if(mx>=OkeanClient.watermarkX&&mx<OkeanClient.watermarkX+104&&my>=OkeanClient.watermarkY&&my<OkeanClient.watermarkY+15){
    drag=1;dx=(int)mx-OkeanClient.watermarkX;dy=(int)my-OkeanClient.watermarkY;return true;
   }
   if(mx>=OkeanClient.targetX&&mx<OkeanClient.targetX+110&&my>=OkeanClient.targetY&&my<OkeanClient.targetY+19){
    drag=2;dx=(int)mx-OkeanClient.targetX;dy=(int)my-OkeanClient.targetY;return true;
   }
  }
  if(mx>=x+16&&mx<x+420&&my>=y+57&&my<y+81&&button==0){
   int t=(int)(mx-x-16)/136;if(t>=0&&t<3){tab=t;scroll=0;return true;}
  }
  if(mx>=x+16&&mx<x+412&&my>=y+92&&my<y+276){
   int idx=(int)(my-y-92)/23+scroll;
   String[] names=current();
   if(idx>=0&&idx<names.length){
    String name=names[idx];
    if(button==0)OkeanClient.toggle(name);
    else if(button==1){
     if(name.equals("AutoTotem")){OkeanClient.autoTotemHealth+=2;if(OkeanClient.autoTotemHealth>20)OkeanClient.autoTotemHealth=8;}
     else if(name.equals("KillAura")){OkeanClient.killAuraRange+=.25;if(OkeanClient.killAuraRange>3.25)OkeanClient.killAuraRange=2.5;}
     else OkeanClient.hudTheme=(OkeanClient.hudTheme+1)%OkeanClient.HUD_COLORS.length;
    }
    return true;
   }
  }
  if(button==1&&mx>=x+270&&mx<x+417&&my>=y+9&&my<y+49){
   OkeanClient.hudTheme=(OkeanClient.hudTheme+1)%OkeanClient.HUD_COLORS.length;return true;
  }
  return super.mouseClicked(mx,my,button);
 }
 @Override public boolean mouseDragged(double mx,double my,int b,double deltaX,double deltaY){
  if(drag==1){OkeanClient.watermarkX=Math.max(0,Math.min(width-110,(int)mx-dx));OkeanClient.watermarkY=Math.max(0,Math.min(height-22,(int)my-dy));return true;}
  if(drag==2){OkeanClient.targetX=Math.max(0,Math.min(width-180,(int)mx-dx));OkeanClient.targetY=Math.max(0,Math.min(height-52,(int)my-dy));return true;}
  return super.mouseDragged(mx,my,b,deltaX,deltaY);
 }
 @Override public boolean mouseReleased(double mx,double my,int button){drag=0;return super.mouseReleased(mx,my,button);}
 @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
  scroll=Math.max(0,Math.min(Math.max(0,current().length-8),scroll-(int)Math.signum(vertical)));return true;
 }
 @Override public void close(){client.setScreen(previous);}
 @Override public boolean shouldPause(){return false;}
}