package com.ianblk.ziangui.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ButtonHitAreaTest {
 @Test void allBordersAndCornersAreClickableAfterViewportTransform(){
  for(int[] viewport:new int[][]{{1920,1080},{640,360},{320,180},{427,240}}){
   var layout=ResponsiveLayout.fit(viewport[0],viewport[1],440,330);
   for(double x:new double[]{12,12.01,78,143.49})for(double y:new double[]{48,48.01,63,77.49})
    assertTrue(ButtonHitArea.contains(layout.mouseX(layout.x()+x*layout.scale()),layout.mouseY(layout.y()+y*layout.scale()),12,48,132,30));
   assertFalse(ButtonHitArea.contains(150,60,12,48,132,30));
  }
 }
}
