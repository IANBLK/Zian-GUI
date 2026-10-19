package com.ianblk.ziangui.client;

/** Includes the last visible border pixel without overlapping a neighboring card. */
public final class ButtonHitArea {
 private ButtonHitArea(){}
 public static boolean contains(double mouseX,double mouseY,int x,int y,int width,int height){
  return width>0&&height>0&&mouseX>=x-.5&&mouseY>=y-.5&&mouseX<=x+width-.5&&mouseY<=y+height-.5;
 }
}
