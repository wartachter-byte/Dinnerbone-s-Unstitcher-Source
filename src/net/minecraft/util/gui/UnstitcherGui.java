package net.minecraft.util.gui;

import net.minecraft.util.Loggable;
import net.minecraft.util.Unstitcher;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;
import java.awt.event.*;


public class UnstitcherGui extends JPanel implements Loggable {
  public enum OS {
    LINUX, SOLARIS, WINDOWS, MACOS, UNKNOWN;
  }

  private final JTextArea log = new JTextArea();
  private final JScrollPane scroll = new JScrollPane(this.log, 22, 31);
  private static final byte[] BUFFER = new byte[10485760];

  public UnstitcherGui() {
    setPreferredSize(new Dimension(670, 480));
    setLayout(new BorderLayout());

    this.log.setEditable(false);
    this.log.setWrapStyleWord(true);
    this.log.setLineWrap(true);

    add(this.scroll);
  }

  public void load() {
    JFileChooser chooser = new JFileChooser();
    File minecraftDir = getMinecraftDirectory();

    log("Initialized. Please select a texturepack (zip only) for conversion. The output will be saved to the same directory in a separate zip.");

    chooser.setFileFilter(new FileNameExtensionFilter("Zip texture packs", new String[] { "zip" }));

    if (minecraftDir != null && minecraftDir.exists()) {
      File dir = new File(minecraftDir, "texturepacks");

      if (dir.isDirectory()) {
        chooser.setCurrentDirectory(dir);
      }
    } 

    if (chooser.showDialog(this, "Convert Texturepack") == 0 && chooser.getSelectedFile().isFile()) {
      File input = chooser.getSelectedFile();
      File output = new File(input.getParentFile(), "converted-" + input.getName());
      log("Selected texturepack '" + input.getAbsolutePath() + "'");
      log("Output will be saved to '" + output.getAbsolutePath() + "'");

      try {
        unstitch(input, output);
        log("All done!");
        log("Your items.png and terrain.png have been replaced with any images not cut from the image.");
        log("The unstitched images can be found in textures/blocks/*.png and textures/items/*.png respectively.");
      } catch (Throwable t) {
        log("Error unstitching file!");
        log(getExceptionMessage(t));
        log("Stopping...");
      } 
    } else {
      System.exit(0);
    } 
  }

  public String getExceptionMessage(Throwable exception) {
    StringWriter writer = null;
    PrintWriter printWriter = null;
    String result = exception.toString();

    try {
      writer = new StringWriter();
      printWriter = new PrintWriter(writer);
      exception.printStackTrace(printWriter);
      result = writer.toString();
    } finally {
      try {
        if (writer != null) writer.close(); 
        if (printWriter != null) printWriter.close(); 
      } catch (IOException iOException) {}
    } 
 
    return result;
  }

  public void unstitch(File inputFile, File outputFile) throws IOException {
    ZipFile input = new ZipFile(inputFile);
    ZipOutputStream result = new ZipOutputStream(new FileOutputStream(outputFile));

    Enumeration<? extends ZipEntry> entries = input.entries();
    InputStream terrain = null;
    InputStream items = null;

    log("Creating a copy of the texturepack...");
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();

      if (!entry.isDirectory()) {
        if (entry.getName().equals("terrain.png")) {
          terrain = input.getInputStream(entry); continue;
        }  if (entry.getName().equals("gui/items.png")) {
          items = input.getInputStream(entry); continue;
        } 
        log("Copying " + entry.getName());

        result.putNextEntry(new ZipEntry(entry.getName()));
        copy(input.getInputStream(entry), result);
        result.closeEntry();
      } 
    } 


    if (terrain != null) {
      log("Unstitching terrain.png...");
      unstitch(terrain, result, "terrain", "blocks", "terrain.png");
    } else {
      log("Skipping terrain; nothing to do");
    } 

    if (items != null) {
      log("Unstitching items.png...");
      unstitch(items, result, "item", "items", "gui/items.png");
    } else {
      log("Skipping items; nothing to do");
    } 

    input.close();
    result.close();
  }

  public void unstitch(InputStream input, ZipOutputStream output, String type, String folder, String original) throws IOException {
    Unstitcher unstitcher = new Unstitcher();

    unstitcher.load(input);
    unstitcher.loadPositions(Unstitcher.class.getResourceAsStream("/" + folder + ".txt"), this);

    while (unstitcher.hasNext()) {
      String name = unstitcher.getNextName();
      if (name == null) {
        unstitcher.skip();

        continue;
      } 
      log("Cutting out " + type + " '" + name + "' ...");
      output.putNextEntry(new ZipEntry("textures/" + folder + "/" + name + ".png"));
      unstitcher.unstitch(output);
      output.closeEntry();

      if (type.equals("terrain") && (name.equals("carrots_0") || name.equals("carrots_1") || name.equals("carrots_2"))) {
        String newName = "potatoes_" + name.substring("carrots_".length());
        log("Copying " + name + " to " + newName);
        output.putNextEntry(new ZipEntry("textures/" + folder + "/" + newName + ".png"));
        unstitcher.copyLast(output);
        output.closeEntry();
      } 
    } 

    output.putNextEntry(new ZipEntry(original));
    unstitcher.saveUntouched(output);
    output.closeEntry();
  }

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
  }


  public static OS getPlatform() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) return OS.WINDOWS; 
    if (osName.contains("mac")) return OS.MACOS; 
    if (osName.contains("solaris")) return OS.SOLARIS; 
    if (osName.contains("sunos")) return OS.SOLARIS; 
    if (osName.contains("linux")) return OS.LINUX; 
    if (osName.contains("unix")) return OS.LINUX; 
    return OS.UNKNOWN;
  }

  public void log(String text) {
    this.log.append(String.valueOf(text) + "\n");
    this.log.setCaretPosition(this.log.getDocument().getLength());
  }

  public static void main(String[] args) {
    UnstitcherGui gui = new UnstitcherGui();

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable throwable) {}


    JFrame frame = new JFrame("Minecraft Texture Unstitcher");
    frame.add(gui);
    frame.pack();
    frame.setLocationRelativeTo((Component)null);
    frame.setVisible(true);
    frame.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent we) {
        System.exit(0);
      }
    });

    gui.load();
  } 
  
  public static void copy(InputStream input, OutputStream output) throws IOException {
     int bytesRead;
    while ((bytesRead = input.read(BUFFER)) != -1)
    output.write(BUFFER, 0, bytesRead); 
  }
}
