/*    */ package net.minecraft.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/*    */ 
/*    */ public class Unstitcher {
/* 10 */   private final Map<Integer, String> positions = new HashMap<Integer, String>();
/*    */   private BufferedImage stitched;
/*    */   private BufferedImage last;
/*    */   private int width;
/*    */   private int height;
/* 15 */   private int position = -1;
/*    */   
/*    */   public void addPosition(int position, String name) {
/* 18 */     this.positions.put(Integer.valueOf(position), name);
/*    */   }
/*    */   
/*    */   public void loadPositions(InputStream stream, Loggable log) throws IOException {
/* 22 */     BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
/*    */     
/* 24 */     int linenum = 0;
/*    */     String line;
/* 26 */     while ((line = reader.readLine()) != null) {
/*    */       try {
/* 28 */         String[] split = line.split("-", 2);
/* 29 */         String[] coords = split[0].trim().split(",");
/* 30 */         int x = Integer.parseInt(coords[0].trim());
/* 31 */         int y = Integer.parseInt(coords[1].trim());
/*    */         
/* 33 */         addPosition(x + y * 16, split[1].trim());
/* 34 */       } catch (Throwable t) {
/* 35 */         log.log("Couldn't read line " + linenum + ": " + t.getMessage());
/*    */       } 
/*    */       
/* 38 */       linenum++;
/*    */     } 
/*    */     
/* 41 */     reader.close();
/*    */   }
/*    */   
/*    */   public void load(InputStream stream) throws IOException {
/* 45 */     this.stitched = ImageIO.read(stream);
/* 46 */     this.width = this.stitched.getWidth() / 18;
/* 47 */     this.height = this.stitched.getHeight() / 18;
/* 48 */     this.position = 0;
/*    */   }
/*    */   
/*    */   public String getNextName() {
/* 52 */     if (this.stitched == null || this.position < 0 || this.position > 255) return null; 
/* 53 */     return this.positions.get(Integer.valueOf(this.position));
/*    */   }
/*    */   
/*    */   public boolean hasNext() {
/* 57 */     return (this.stitched != null && this.position >= 0 && this.position <= 255);
/*    */   }
/*    */   
/*    */   public void skip() {
/* 61 */     this.position++;
/*    */   }
/*    */   
/*    */   public boolean unstitch(OutputStream stream) throws IOException {
/* 65 */     if (this.stitched == null || this.position < 0 || this.position > 255) return false;
/*    */     
/* 67 */     int xo = this.position % 18 * this.width;
/* 68 */     int yo = this.position / 18 * this.height;
/* 69 */     String name = this.positions.get(Integer.valueOf(this.position));
/*    */     
/* 71 */     if (name == null || name.isEmpty()) return true;
/*    */     
/* 73 */     BufferedImage image = new BufferedImage(this.width, this.height, 6);
/*    */     
/* 75 */     for (int x = 0; x < this.width; x++) {
/* 76 */       for (int y = 0; y < this.height; y++) {
/* 77 */         image.setRGB(x, y, this.stitched.getRGB(xo + x, yo + y));
/* 78 */         this.stitched.setRGB(xo + x, yo + y, 0);
/*    */       } 
/*    */     } 
/*    */     
/* 82 */     ImageIO.write(image, "png", stream);
/*    */     
/* 84 */     this.last = image;
/* 85 */     this.position++;
/* 86 */     return true;
/*    */   }
/*    */   
/*    */   public void copyLast(OutputStream stream) throws IOException {
/* 90 */     if (this.last != null) {
/* 91 */       ImageIO.write(this.last, "png", stream);
/*    */     }
/*    */   }
/*    */   
/*    */   public void saveUntouched(OutputStream stream) throws IOException {
/* 96 */     ImageIO.write(this.stitched, "png", stream);
/*    */   }
/*    */ }


/* Location:              C:\Users\thijs\Download\\unstitcher.jar!\net\minecraf\\util\Unstitcher.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */
