package com.Nanbin.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Texture generator for CRT APG Door 1 multi-face model
 * Generates 6 face textures with placeholder colors
 */
public class TextureGenerator {

    private static final String OUTPUT_DIR = "src/main/resources/assets/nanbin/textures/block";
    private static final String BASE_NAME = "crt_apg_door_1_top";
    private static final int TEXTURE_SIZE = 32;

    /**
     * Face definitions with UV coordinates and placeholder colors
     */
    private static final Map<String, FaceDefinition> FACES = new HashMap<>();

    static {
        // Front face: UV(0,0), size 16x2
        FACES.put("front", new FaceDefinition(0, 0, 16, 2, new Color(200, 200, 200)));
        // Back face: UV(0,4), size 16x2
        FACES.put("back", new FaceDefinition(0, 4, 16, 2, new Color(180, 180, 180)));
        // Left face: UV(0,8), size 1x2
        FACES.put("left", new FaceDefinition(0, 8, 1, 2, new Color(160, 160, 160)));
        // Right face: UV(0,12), size 1x2
        FACES.put("right", new FaceDefinition(0, 12, 1, 2, new Color(140, 140, 140)));
        // Top face: UV(0,16), size 16x1
        FACES.put("up", new FaceDefinition(0, 16, 16, 1, new Color(220, 220, 220)));
        // Bottom face: UV(0,20), size 16x1
        FACES.put("down", new FaceDefinition(0, 20, 16, 1, new Color(120, 120, 120)));
    }

    /**
     * Face definition with UV position, size and color
     */
    private static class FaceDefinition {
        final int uvX;
        final int uvY;
        final int width;
        final int height;
        final Color color;

        FaceDefinition(int uvX, int uvY, int width, int height, Color color) {
            this.uvX = uvX;
            this.uvY = uvY;
            this.width = width;
            this.height = height;
            this.color = color;
        }
    }

    /**
     * Generate all 6 face textures
     */
    public static void generateAllTextures() {
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (Map.Entry<String, FaceDefinition> entry : FACES.entrySet()) {
            String faceName = entry.getKey();
            FaceDefinition def = entry.getValue();
            generateTexture(faceName, def);
        }

        System.out.println("Generated " + FACES.size() + " textures in " + OUTPUT_DIR);
    }

    /**
     * Generate a single face texture
     */
    private static void generateTexture(String faceName, FaceDefinition def) {
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill transparent background
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Draw face area with placeholder color
        int pixelX = def.uvX;
        int pixelY = def.uvY;
        int pixelWidth = def.width;
        int pixelHeight = def.height;

        g2d.setColor(def.color);
        g2d.fillRect(pixelX, pixelY, pixelWidth, pixelHeight);

        // Draw border for visibility
        g2d.setColor(Color.BLACK);
        g2d.drawRect(pixelX, pixelY, pixelWidth, pixelHeight);

        g2d.dispose();

        // Save to file
        String fileName = BASE_NAME + "_" + faceName + ".png";
        File outputFile = new File(OUTPUT_DIR, fileName);
        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("Created: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create texture: " + fileName);
            e.printStackTrace();
        }
    }

    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        generateAllTextures();
    }
}