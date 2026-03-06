/*     */ package net.minecraft.util.gui;

import net.minecraft.util.Loggable;
import net.minecraft.util.Unstitcher;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;
import java.awt.event.*;

/*     */ 
/*     */ public class UnstitcherGui extends JPanel implements Loggable {
/*     */   public enum OS {
/*  19 */     LINUX, SOLARIS, WINDOWS, MACOS, UNKNOWN;
/*     */   }
/*     */   
/*  22 */   private final JTextArea log = new JTextArea();
/*  23 */   private final JScrollPane scroll = new JScrollPane(this.log, 22, 31);
/*  24 */   private static final byte[] BUFFER = new byte[10485760];
/*     */   
/*     */   public UnstitcherGui() {
/*  27 */     setPreferredSize(new Dimension(670, 480));
/*  28 */     setLayout(new BorderLayout());
/*     */     
/*  30 */     this.log.setEditable(false);
/*  31 */     this.log.setWrapStyleWord(true);
/*  32 */     this.log.setLineWrap(true);
/*     */     
/*  34 */     add(this.scroll);
/*     */   }
/*     */   
/*     */   public void load() {
/*  38 */     JFileChooser chooser = new JFileChooser();
/*  39 */     File minecraftDir = getMinecraftDirectory();
/*     */     
/*  41 */     log("Initialized. Please select a texturepack (zip only) for conversion. The output will be saved to the same directory in a separate zip.");
              log("LMAO SO EASY TO MOD THIS!");
/*     */     
/*  43 */     chooser.setFileFilter(new FileNameExtensionFilter("Zip texture packs", new String[] { "zip" }));
/*     */     
/*  45 */     if (minecraftDir != null && minecraftDir.exists()) {
/*  46 */       File dir = new File(minecraftDir, "texturepacks");
/*     */       
/*  48 */       if (dir.isDirectory()) {
/*  49 */         chooser.setCurrentDirectory(dir);
/*     */       }
/*     */     } 
/*     */     
/*  53 */     if (chooser.showDialog(this, "Convert Texturepack") == 0 && chooser.getSelectedFile().isFile()) {
/*  54 */       File input = chooser.getSelectedFile();
/*  55 */       File output = new File(input.getParentFile(), "converted-" + input.getName());
/*  56 */       log("Selected texturepack '" + input.getAbsolutePath() + "'");
/*  57 */       log("Output will be saved to '" + output.getAbsolutePath() + "'");
/*     */       
/*     */       try {
/*  60 */         unstitch(input, output);
/*  61 */         log("All done!");
/*  62 */         log("Your items.png and terrain.png have been replaced with any images not cut from the image.");
/*  63 */         log("The unstitched images can be found in textures/blocks/*.png and textures/items/*.png respectively.");
/*  64 */       } catch (Throwable t) {
/*  65 */         log("Error unstitching file!");
/*  66 */         log(getExceptionMessage(t));
/*  67 */         log("Stopping...");
/*     */       } 
/*     */     } else {
/*  70 */       System.exit(0);
/*     */     } 
/*     */   }
/*     */   
/*     */   public String getExceptionMessage(Throwable exception) {
/*  75 */     StringWriter writer = null;
/*  76 */     PrintWriter printWriter = null;
/*  77 */     String result = exception.toString();
/*     */     
/*     */     try {
/*  80 */       writer = new StringWriter();
/*  81 */       printWriter = new PrintWriter(writer);
/*  82 */       exception.printStackTrace(printWriter);
/*  83 */       result = writer.toString();
/*     */     } finally {
/*     */       try {
/*  86 */         if (writer != null) writer.close(); 
/*  87 */         if (printWriter != null) printWriter.close(); 
/*  88 */       } catch (IOException iOException) {}
/*     */     } 
/*     */     
/*  91 */     return result;
/*     */   }
/*     */   
/*     */   public void unstitch(File inputFile, File outputFile) throws IOException {
/*  95 */     ZipFile input = new ZipFile(inputFile);
/*  96 */     ZipOutputStream result = new ZipOutputStream(new FileOutputStream(outputFile));
/*     */     
/*  98 */     Enumeration<? extends ZipEntry> entries = input.entries();
/*  99 */     InputStream terrain = null;
/* 100 */     InputStream items = null;
/*     */     
/* 102 */     log("Creating a copy of the texturepack...");
/* 103 */     while (entries.hasMoreElements()) {
/* 104 */       ZipEntry entry = entries.nextElement();
/*     */       
/* 106 */       if (!entry.isDirectory()) {
/* 107 */         if (entry.getName().equals("terrain.png")) {
/* 108 */           terrain = input.getInputStream(entry); continue;
/* 109 */         }  if (entry.getName().equals("gui/items.png")) {
/* 110 */           items = input.getInputStream(entry); continue;
/*     */         } 
/* 112 */         log("Copying " + entry.getName());
/*     */         
/* 114 */         result.putNextEntry(new ZipEntry(entry.getName()));
/* 115 */         copy(input.getInputStream(entry), result);
/* 116 */         result.closeEntry();
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 121 */     if (terrain != null) {
/* 122 */       log("Unstitching terrain.png...");
/* 123 */       unstitch(terrain, result, "terrain", "blocks", "terrain.png");
/*     */     } else {
/* 125 */       log("Skipping terrain; nothing to do");
/*     */     } 
/*     */     
/* 128 */     if (items != null) {
/* 129 */       log("Unstitching items.png...");
/* 130 */       unstitch(items, result, "item", "items", "gui/items.png");
/*     */     } else {
/* 132 */       log("Skipping items; nothing to do");
/*     */     } 
/*     */     
/* 135 */     input.close();
/* 136 */     result.close();
/*     */   }
/*     */   
/*     */   public void unstitch(InputStream input, ZipOutputStream output, String type, String folder, String original) throws IOException {
/* 140 */     Unstitcher unstitcher = new Unstitcher();
/*     */     
/* 142 */     unstitcher.load(input);
/* 143 */     unstitcher.loadPositions(Unstitcher.class.getResourceAsStream("/" + folder + ".txt"), this);
/*     */     
/* 145 */     while (unstitcher.hasNext()) {
/* 146 */       String name = unstitcher.getNextName();
/* 147 */       if (name == null) {
/* 148 */         unstitcher.skip();
/*     */         
/*     */         continue;
/*     */       } 
/* 152 */       log("Cutting out " + type + " '" + name + "' ...");
/* 153 */       output.putNextEntry(new ZipEntry("textures/" + folder + "/" + name + ".png"));
/* 154 */       unstitcher.unstitch(output);
/* 155 */       output.closeEntry();
/*     */       
/* 157 */       if (type.equals("terrain") && (name.equals("carrots_0") || name.equals("carrots_1") || name.equals("carrots_2"))) {
/* 158 */         String newName = "potatoes_" + name.substring("carrots_".length());
/* 159 */         log("Copying " + name + " to " + newName);
/* 160 */         output.putNextEntry(new ZipEntry("textures/" + folder + "/" + newName + ".png"));
/* 161 */         unstitcher.copyLast(output);
/* 162 */         output.closeEntry();
/*     */       } 
/*     */     } 
/*     */     
/* 166 */     output.putNextEntry(new ZipEntry(original));
/* 167 */     unstitcher.saveUntouched(output);
/* 168 */     output.closeEntry();
/*     */   }
/*     */   
    private File getMinecraftDirectory() {
        String home = System.getProperty("user.home", ".");
        File dir;
        String applicationData;

        switch (getPlatform()) {
            case SOLARIS:
            case LINUX:
                dir = new File(home, ".minecraft/");
                return dir;

            case WINDOWS:
                applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    dir = new File(applicationData, ".minecraft/");
                } else {
                    dir = new File(home, ".minecraft/");
                }
                return dir;

            case MACOS:
                dir = new File(home, "Library/Application Support/minecraft");
                return dir;

            default:
                dir = new File(home, "minecraft/");
                return dir;
        }
    } // <-- Zorg dat deze accolade er staat om de methode af te sluiten!

/*     */   
/*     */   public static OS getPlatform() {
/* 196 */     String osName = System.getProperty("os.name").toLowerCase();
/* 197 */     if (osName.contains("win")) return OS.WINDOWS; 
/* 198 */     if (osName.contains("mac")) return OS.MACOS; 
/* 199 */     if (osName.contains("solaris")) return OS.SOLARIS; 
/* 200 */     if (osName.contains("sunos")) return OS.SOLARIS; 
/* 201 */     if (osName.contains("linux")) return OS.LINUX; 
/* 202 */     if (osName.contains("unix")) return OS.LINUX; 
/* 203 */     return OS.UNKNOWN;
/*     */   }
/*     */   
/*     */   public void log(String text) {
/* 207 */     this.log.append(String.valueOf(text) + "\n");
/* 208 */     this.log.setCaretPosition(this.log.getDocument().getLength());
/*     */   }
/*     */   
/*     */   public static void main(String[] args) {
/* 212 */     UnstitcherGui gui = new UnstitcherGui();
/*     */     
/*     */     try {
/* 215 */       UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
/* 216 */     } catch (Throwable throwable) {}
/*     */ 
/*     */     
/* 219 */     JFrame frame = new JFrame("Minecraft Texture Unstitcher");
/* 220 */     frame.add(gui);
/* 221 */     frame.pack();
/* 222 */     frame.setLocationRelativeTo((Component)null);
/* 223 */     frame.setVisible(true);
/* 224 */     frame.addWindowListener(new WindowAdapter()
/*     */         {
/*     */           public void windowClosing(WindowEvent we) {
/* 227 */             System.exit(0);
/*     */           }
/*     */         });
/*     */     
/* 231 */     gui.load();
/*     */   }
/*     */ 
/*     */   
/*     */   public static void copy(InputStream input, OutputStream output) throws IOException {
/*     */     int bytesRead;
/* 237 */     while ((bytesRead = input.read(BUFFER)) != -1)
/* 238 */       output.write(BUFFER, 0, bytesRead); 
/*     */   }
/*     */ }


/* Location:              C:\Users\thijs\Download\\unstitcher.jar!\net\minecraf\\util\gui\UnstitcherGui.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */
