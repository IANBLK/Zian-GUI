package com.ianblk.ziangui.client;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import com.ianblk.ziangui.network.GuiPayloads;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Opt-in native screen checks. Runs only in the dedicated visual-test client. */
final class GuiVisualSmoke {
 private static int stage,ticks;private static boolean begun;
 static void register(){if(Boolean.getBoolean("ziangui.visualSmoke"))net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GuiVisualSmoke::tick);}
 private static void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
 private static void open(Minecraft mc){mc.options.guiScale().set(stage+2);mc.resizeDisplay();check(mc.getWindow().getGuiScale()==stage+2,"Requested GUI scale was clamped by viewport");var buttons=new ArrayList<GuiPayloads.ButtonView>();for(int n=0;n<24;n++)buttons.add(new GuiPayloads.ButtonView("button_"+n,"Opción "+(n+1),"minecraft:compass",true,"Requisito de ejemplo"));mc.setScreen(new ZianGuiScreen(new GuiPayloads.OpenMenu("principal",1,"Rassvet · prueba visual",buttons,false)));ticks=0;}
 private static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();try{if(!begun){if(mc.screen==null||mc.getOverlay()!=null)return;begun=true;open(mc);return;}if(++ticks!=30)return;check(mc.screen instanceof ZianGuiScreen,"Expected native GUI");var screen=(ZianGuiScreen)mc.screen;var next=screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals(">" )).map(c->(Button)c).findFirst().orElseThrow();var layout=ResponsiveLayout.fit(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),440,330);check(screen.mouseClicked(layout.x()+next.getX()*layout.scale(),layout.y()+next.getY()*layout.scale(),0),"Actual pager border click not accepted");check(screen.children().stream().noneMatch(c->c instanceof Button b&&b.getMessage().getString().equals("Opción 1")),"Border click did not change page");var previous=screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals("<")).map(c->(Button)c).findFirst().orElseThrow();check(screen.mouseClicked(layout.x()+(previous.getX()+previous.getWidth()-.5)*layout.scale(),layout.y()+(previous.getY()+previous.getHeight()-.5)*layout.scale(),0),"Opposite pager border click not accepted");check(screen.children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("Opción 1")),"Previous page not restored");Screenshot.grab(mc.gameDirectory,"gui-scale-"+(stage+2)+".png",mc.getMainRenderTarget(),c->{});if(++stage<3)open(mc);else{Files.writeString(mc.gameDirectory.toPath().resolve("visual-result.txt"),"PASS: native menu, real pager edge clicks and inverse scaled input at GUI scales 2, 3 and 4; three screenshots. No server command execution tested in this client.\n");mc.stop();}}catch(Throwable error){try{Files.writeString(mc.gameDirectory.toPath().resolve("visual-result.txt"),"FAIL: "+error);}catch(Exception ignored){}mc.stop();}}
}
