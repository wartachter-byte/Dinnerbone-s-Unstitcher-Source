package net.minecraft.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;


public class Unstitcher {
  private final Map<Integer, String> positions = new HashMap<Integer, String>();
  private BufferedImage stitched;
  private BufferedImage last;
  private int width;
  private int height;
  private int position = -1;
   
  public void addPosition(int position, String name) {
    this.positions.put(Integer.valueOf(position), name);
  }
   
  public void loadPositions(InputStream stream, Loggable log) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
  
    int linenum = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      try {
        String[] split = line.split("-", 2);
        String[] coords = split[0].trim().split(",");
        int x = Integer.parseInt(coords[0].trim());
        int y = Integer.parseInt(coords[1].trim());
    
        addPosition(x + y * 16, split[1].trim());
      } catch (Throwable t) {
        log.log("Couldn't read line " + linenum + ": " + t.getMessage());
      } 
    
      linenum++;
    } 
   
    reader.close();
  }
  
  public void load(InputStream stream) throws IOException {
    this.stitched = ImageIO.read(stream);
    this.width = this.stitched.getWidth() / 18;
    this.height = this.stitched.getHeight() / 18;
    this.position = 0;
  }

  public String getNextName() {
    if (this.stitched == null || this.position < 0 || this.position > 255) return null; 
    return this.positions.get(Integer.valueOf(this.position));
  }

  public boolean hasNext() {
    return (this.stitched != null && this.position >= 0 && this.position <= 255);
  }

  public void skip() {
    this.position++;
  }
 
  public boolean unstitch(OutputStream stream) throws IOException {
    if (this.stitched == null || this.position < 0 || this.position > 255) return false;
     
    int xo = this.position % 18 * this.width;
    int yo = this.position / 18 * this.height;
    String name = this.positions.get(Integer.valueOf(this.position));

    if (name == null || name.isEmpty()) return true;
 
    BufferedImage image = new BufferedImage(this.width, this.height, 6);
 
    for (int x = 0; x < this.width; x++) {
      for (int y = 0; y < this.height; y++) {
        image.setRGB(x, y, this.stitched.getRGB(xo + x, yo + y));
        this.stitched.setRGB(xo + x, yo + y, 0);
      } 
    } 

    ImageIO.write(image, "png", stream);

    this.last = image;
    this.position++;
    return true;
  }

  public void copyLast(OutputStream stream) throws IOException {
    if (this.last != null) {
      ImageIO.write(this.last, "png", stream);
    }
  }

  public void saveUntouched(OutputStream stream) throws IOException {
    ImageIO.write(this.stitched, "png", stream);
  }
}
