package com.okean.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
public class ModuleScreen extends Screen {
 private final Screen previous;
 private final String[][] groups={{"COMBAT","Health HUD"},{"MOVEMENT","AutoSprint","Direction"},{"VISUALS","FPS HUD","XYZ HUD","Biome","Day Mode","Night Mode","Fullbright"},{"PLAYER","Clock","Speed HUD","Ping HUD"},{"MISC","Clean HUD"}};
 public ModuleScreen(Screen previous){super(Text.literal("Okean Modules"));this.previous=previous;}
 @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
 private int cardX(int i){return 12+i*Math.max(105,(width-24)/5);}
 private int cardWidth(){return Math.max(99,(width-24)/5-6);}
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  ctx.fill(0,0,width,height,0xD909111C);
  ctx.drawCenteredTextWithShadow(textRenderer,"O K E A N / MODULES",width/2,15,OkeanClient.CYAN);
  ctx.drawCenteredTextWithShadow(textRenderer,"Right Shift = close",width/2,30,0xFF96A7B6);
  for(int i=0;i<groups.length;i++){
   int x=cardX(i),w=cardWidth(),y=58;
   ctx.fill(x,y,x+w,Math.min(height-17,58+34+groups[i].length*28),0xEF102030);
   ctx.fill(x,y,x+w,y+2,OkeanClient.CYAN);
   ctx.drawText(textRenderer,groups[i][0],x+8,y+11,0xFFFFFFFF,true);
   for(int j=1;j<groups[i].length;j++){
    int yy=y+30+(j-1)*28;
    boolean active=OkeanClient.on(groups[i][j]);
    ctx.drawText(textRenderer,groups[i][j],x+8,yy+4,active?0xFFFFFFFF:0xFFA1B1C2,false);
    ctx.fill(x+w-19,yy+3,x+w-5,yy+14,active?OkeanClient.CYAN:0xFF445766);
   }
  }
  super.render(ctx,mx,my,delta);
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  if(button==0)for(int i=0;i<groups.length;i++){
   int x=cardX(i),w=cardWidth();
   for(int j=1;j<groups[i].length;j++){
    int y=88+(j-1)*28;
    if(mx>=x&&mx<x+w&&my>=y&&my<y+23){OkeanClient.toggle(groups[i][j]);return true;}
   }
  }
  return super.mouseClicked(mx,my,button);
 }
 @Override public void close(){client.setScreen(previous);}
 @Override public boolean shouldPause(){return false;}
}