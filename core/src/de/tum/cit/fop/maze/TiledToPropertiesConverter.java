package de.tum.cit.fop.maze;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Converts Tiled TMX map files to Java Properties format for the Maze Runner game.
 * Handles multi-layer TMX files with separate layers for walls, entry, exit, traps, enemies, keys.
 * 
 * Usage: java TiledToPropertiesConverter input.tmx output.properties
 */
public class TiledToPropertiesConverter {
    
    // Game object types for the properties file
    public static final int TYPE_WALL = 0;
    public static final int TYPE_PATH = 1;
    public static final int TYPE_EXIT = 2;
    public static final int TYPE_DEATHTRAP = 3;
    public static final int TYPE_ENEMY = 4;
    public static final int TYPE_KEY = 5;
    public static final int TYPE_ENTRY = 6;
    public static final int TYPE_KNIFE_TRAP = 7;
    public static final int TYPE_BOSS = 8;
    public static void convert(String tmxFilePath, String outputPropertiesPath) throws Exception {
        // Parse the TMX file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(tmxFilePath));
        
        Element mapElement = doc.getDocumentElement();
        int width = Integer.parseInt(mapElement.getAttribute("width"));
        int height = Integer.parseInt(mapElement.getAttribute("height"));
        
        System.out.println("Map dimensions: " + width + "x" + height);
        
        // Initialize the game map - default everything to PATH
        int[][] gameMap = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gameMap[x][y] = TYPE_PATH;
            }
        }
        
        // Get all layers
        NodeList layers = doc.getElementsByTagName("layer");
        System.out.println("Found " + layers.getLength() + " layers");
        
        for (int i = 0; i < layers.getLength(); i++) {
            Element layer = (Element) layers.item(i);
            String layerName = layer.getAttribute("name").toLowerCase();
            System.out.println("Processing layer: " + layer.getAttribute("name"));
            
            int[] tiles = parseLayerData(layer, width, height);
            
            // Process based on layer name
            // Note: Check for decoration first to avoid treating "DecorationOnWalls" as walls
            if (layerName.contains("decoration")) {
                // Skip decoration layers - they don't affect collision
                System.out.println("Skipping decoration layer (no collision)");
            } else if (layerName.contains("wall")) {
                processWallLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("entry") || layerName.contains("spawn") || layerName.contains("start")) {
                processEntryLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("player")) {
                processPlayerLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("exit") || layerName.contains("goal") || layerName.contains("end")) {
                processExitLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("death") || layerName.contains("trap") || layerName.contains("pit")) {
                processDeathTrapLayer(tiles, gameMap, width, height);
            }else if (layerName.contains("knife")) {
                processKnifesLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("enemy") || layerName.contains("monster")) {
                processEnemyLayer(tiles, gameMap, width, height);
            } else if (layerName.contains("key") || layerName.contains("item") || layerName.contains("collect")) {
                processKeyLayer(tiles, gameMap, width, height);
            }
        }

        // Create properties file
        Properties props = new Properties();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                props.setProperty(x + "," + y, String.valueOf(gameMap[x][y]));
            }
        }
        
        // Write to properties file
        try (FileOutputStream fos = new FileOutputStream(outputPropertiesPath)) {
            props.store(fos, "Maze generated from " + tmxFilePath + " | Width: " + width + " | Height: " + height);
        }
        
        System.out.println("Successfully converted to: " + outputPropertiesPath);
        printMapStats(gameMap, width, height);
    }
    
    private static int[] parseLayerData(Element layer, int width, int height) throws Exception {
        Element dataElement = (Element) layer.getElementsByTagName("data").item(0);
        String encoding = dataElement.getAttribute("encoding");
        
        if (!"csv".equals(encoding)) {
            throw new RuntimeException("Only CSV encoding is supported. Layer: " + layer.getAttribute("name"));
        }
        
        String csvData = dataElement.getTextContent().trim();
        String[] tileStrings = csvData.split(",");
        int[] tiles = new int[tileStrings.length];
        
        // Tiled uses flip flags in high bits for rotated/flipped tiles
        // We need to mask them out to get the actual tile ID
        final long FLIPPED_HORIZONTALLY_FLAG = 0x80000000L;
        final long FLIPPED_VERTICALLY_FLAG   = 0x40000000L;
        final long FLIPPED_DIAGONALLY_FLAG   = 0x20000000L;
        final long TILE_ID_MASK = 0x1FFFFFFFL;
        
        for (int i = 0; i < tileStrings.length; i++) {
            long rawTileId = Long.parseLong(tileStrings[i].trim());
            tiles[i] = (int)(rawTileId & TILE_ID_MASK); // Strip flip flags, keep tile ID
        }
        
        return tiles;
    }
    
    private static void processWallLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width); // Flip Y-axis
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_WALL;
            }
        }
    }
    
    private static void processEntryLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_ENTRY; // Entry is just a path with spawn point
                System.out.println("Entry point found at: " + x + "," + y);
            }
        }
    }
    
    private static void processPlayerLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_ENTRY; // Player spawns on a path
                System.out.println("=== PLAYER START POSITION FOUND: " + x + "," + y + " ===");
            }
        }
    }
    
    private static void processExitLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_EXIT;
                System.out.println("Exit found at: " + x + "," + y);
            }
        }
    }
    
    private static void processDeathTrapLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_DEATHTRAP;
            }
        }
    }
    private static void processKnifesLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_KNIFE_TRAP;
            }
        }
    }
    private static void processEnemyLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_ENEMY;
                System.out.println("Enemy found at: " + x + "," + y);
            }
        }
    }
    
    private static void processKeyLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_KEY;
                System.out.println("Key found at: " + x + "," + y);
            }
        }
    }
    
    private static void printMapStats(int[][] gameMap, int width, int height) {
        int walls = 0, paths = 0, exits = 0, traps = 0, enemies = 0, keys = 0;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                switch (gameMap[x][y]) {
                    case TYPE_WALL: walls++; break;
                    case TYPE_PATH: paths++; break;
                    case TYPE_EXIT: exits++; break;
                    case TYPE_DEATHTRAP: traps++; break;
                    case TYPE_ENEMY: enemies++; break;
                    case TYPE_KEY: keys++; break;
                }
            }
        }
        
        System.out.println("\n=== Map Statistics ===");
        System.out.println("Walls: " + walls);
        System.out.println("Paths: " + paths);
        System.out.println("Exits: " + exits);
        System.out.println("Traps: " + traps);
        System.out.println("Enemies: " + enemies);
        System.out.println("Keys: " + keys);
        System.out.println("Total tiles: " + (width * height));
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TiledToPropertiesConverter <input.tmx> <output.properties>");
            System.out.println("Example: java TiledToPropertiesConverter assets/MoriaMap/newmap.tmx assets/maps/level-1.properties");
            System.out.println("\nSupported layer names:");
            System.out.println("  - Wall layers: 'wall', 'walls'");
            System.out.println("  - Entry layers: 'entry', 'spawn', 'start', 'entrypoint'");
            System.out.println("  - Exit layers: 'exit', 'goal', 'end'");
            System.out.println("  - Trap layers: 'death', 'trap', 'pit', 'deathpit'");
            System.out.println("  - Enemy layers: 'enemy', 'monster'");
            System.out.println("  - Key layers: 'key', 'item', 'collect'");
            System.out.println("\nGame Types: 0=Wall, 1=Path, 2=Exit, 3=Trap, 4=Enemy, 5=Key");
            return;
        }
        try {
            convert(args[0], args[1]);
        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}