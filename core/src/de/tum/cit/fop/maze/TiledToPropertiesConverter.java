package de.tum.cit.fop.maze;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

/**
 * Converts Tiled TMX map files into Java {@link Properties} files consumed
 * by the Maze Runner game at runtime.
 * <p>
 * Each tile in the TMX source is mapped to a single integer type constant
 * (see the {@code TYPE_*} fields) and written as a {@code "x,y"} key in
 * the output properties file.  The converter supports multi-layer TMX
 * files; layers are identified by keywords in their name (case-insensitive)
 * and processed in the order they appear in the XML.  Decoration layers
 * are explicitly skipped because they carry no collision data.
 * </p>
 * <p>
 * Tiled stores flipped/rotated tiles by setting high-order flag bits on
 * the tile GID.  These bits are masked off during parsing so that the
 * logical tile ID is preserved regardless of visual transforms applied
 * in the editor.
 * </p>
 */
public class TiledToPropertiesConverter {
    public static final int TYPE_WALL = 0;
    public static final int TYPE_ENTRY = 1;
    public static final int TYPE_EXIT = 2;
    public static final int TYPE_KNIFE_TRAP = 3;
    public static final int TYPE_ENEMY = 4;
    public static final int TYPE_KEY = 5;
    public static final int TYPE_DEATHTRAP = 6;
    public static final int TYPE_PATH = 7;
    public static final int TYPE_BOSS = 8;

    /**
     * Parses a TMX file and writes the converted map to a properties file.
     * <p>
     * The game map array is first filled entirely with {@link #TYPE_PATH}.
     * Each layer is then iterated and its tiles are written over the
     * default values according to the layer's keyword classification.
     * A later layer therefore takes precedence over an earlier one on the
     * same tile.
     * </p>
     *
     * @param tmxFilePath            Path to the source Tiled TMX file.
     * @param outputPropertiesPath   Path where the output properties file
     *                               will be created or overwritten.
     * @throws Exception             If the TMX file cannot be parsed or the
     *                               output file cannot be written.
     */
    public static void convert(String tmxFilePath, String outputPropertiesPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(tmxFilePath));
        Element mapElement = doc.getDocumentElement();
        int width = Integer.parseInt(mapElement.getAttribute("width"));
        int height = Integer.parseInt(mapElement.getAttribute("height"));
        System.out.println("Map dimensions: " + width + "x" + height);
        int[][] gameMap = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gameMap[x][y] = TYPE_PATH;
            }
        }

        NodeList layers = doc.getElementsByTagName("layer");
        System.out.println("Found " + layers.getLength() + " layers");

        for (int i = 0; i < layers.getLength(); i++) {
            Element layer = (Element) layers.item(i);
            String layerName = layer.getAttribute("name").toLowerCase();
            System.out.println("Processing layer: " + layer.getAttribute("name"));

            int[] tiles = parseLayerData(layer, width, height);

            if (layerName.contains("decoration")) {
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
            } else if (layerName.contains("boss")) {
                processBossLayer(tiles, gameMap, width, height);
            }else if (layerName.contains("key") || layerName.contains("item") || layerName.contains("collect")) {
                processKeyLayer(tiles, gameMap, width, height);
            }
        }
        Properties props = new Properties();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                props.setProperty(x + "," + y, String.valueOf(gameMap[x][y]));
            }
        }
        try (FileOutputStream fos = new FileOutputStream(outputPropertiesPath)) {
            props.store(fos, "Maze generated from " + tmxFilePath + " | Width: " + width + " | Height: " + height);
        }

        System.out.println("Successfully converted to: " + outputPropertiesPath);
        printMapStats(gameMap, width, height);
    }

    /**
     * Extracts the flat tile-ID array from a single TMX layer element.
     * <p>
     * Only CSV-encoded {@code &lt;data&gt;} blocks are supported.  Each raw
     * tile value may carry Tiled flip flags in its three most-significant
     * bits; these are stripped via {@code TILE_ID_MASK} so that the
     * returned IDs are pure tile indices regardless of any rotation or
     * mirror applied in the Tiled editor.
     * </p>
     *
     * @param layer  The {@code &lt;layer&gt;} DOM element to parse.
     * @param width  The map width in tiles (used only for validation context).
     * @param height The map height in tiles (used only for validation context).
     * @return An array of tile IDs in row-major order (top-to-bottom,
     *         left-to-right as stored by Tiled).
     * @throws RuntimeException If the layer does not use CSV encoding.
     * @throws Exception        If the XML structure is malformed.
     */
    private static int[] parseLayerData(Element layer, int width, int height) throws Exception {
        Element dataElement = (Element) layer.getElementsByTagName("data").item(0);
        String encoding = dataElement.getAttribute("encoding");

        if (!"csv".equals(encoding)) {
            throw new RuntimeException("Only CSV encoding is supported. Layer: " + layer.getAttribute("name"));
        }

        String csvData = dataElement.getTextContent().trim();
        String[] tileStrings = csvData.split(",");
        int[] tiles = new int[tileStrings.length];

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

    /**
     * Writes {@link #TYPE_WALL} into every cell of {@code gameMap} that
     * corresponds to a non-zero tile in the wall layer.
     * The Y axis is flipped so that Tiled's top-origin row order is
     * converted to the game's bottom-origin coordinate system.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processWallLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);

            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_WALL;
            }
        }
    }

    /**
     * Writes {@link #TYPE_ENTRY} into every cell that corresponds to a
     * non-zero tile in an entry or spawn layer.  The first such tile
     * found becomes the player's spawn point at runtime.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processEntryLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);

            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_ENTRY;
                System.out.println("Entry point found at: " + x + "," + y);
            }
        }
    }

    /**
     * Writes {@link #TYPE_ENTRY} into cells occupied by the explicit
     * player-spawn layer.  Functionally identical to
     * {@link #processEntryLayer} but prints a distinct log message so
     * that the player's start position is easy to locate in the console
     * output during level design.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processPlayerLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);

            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_ENTRY;
                System.out.println("=== PLAYER START POSITION FOUND: " + x + "," + y + " ===");
            }
        }
    }

    /**
     * Writes {@link #TYPE_EXIT} into every cell that corresponds to a
     * non-zero tile in an exit or goal layer.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
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

    /**
     * Writes {@link #TYPE_DEATHTRAP} into every cell that corresponds to
     * a non-zero tile in a death-trap, trap, or pit layer.  Death traps
     * kill the player instantly on contact at runtime.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processDeathTrapLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);

            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_DEATHTRAP;
            }
        }
    }

    /**
     * Writes {@link #TYPE_KNIFE_TRAP} into every cell that corresponds to
     * a non-zero tile in a knife-trap layer.  Knife traps deal periodic
     * damage to the player while they remain in range.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processKnifesLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);
            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_KNIFE_TRAP;
            }
        }
    }

    /**
     * Writes {@link #TYPE_ENEMY} into every cell that corresponds to a
     * non-zero tile in an enemy or monster layer.  Each marked tile
     * becomes an {@link Enemy} spawn point when the level is loaded.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
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

    /**
     * Writes {@link #TYPE_BOSS} into every cell that corresponds to a
     * non-zero tile in a boss layer.  Boss tiles mark the spawn position
     * of the level's boss enemy.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
    private static void processBossLayer(int[] tiles, int[][] gameMap, int width, int height) {
        for (int i = 0; i < tiles.length; i++) {
            int x = i % width;
            int y = height - 1 - (i / width);

            if (tiles[i] != 0) {
                gameMap[x][y] = TYPE_BOSS;
                System.out.println("Enemy found at: " + x + "," + y);
            }
        }
    }

    /**
     * Writes {@link #TYPE_KEY} into every cell that corresponds to a
     * non-zero tile in a key, item, or collectible layer.  Keys must be
     * collected by the player to progress through locked sections.
     *
     * @param tiles   Flat tile-ID array produced by {@link #parseLayerData}.
     * @param gameMap The output map array being populated.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
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

    /**
     * Prints a summary of every tile type present in the converted map.
     * Intended for console output during the build / conversion step so
     * that level designers can quickly verify the map was processed
     * correctly.
     *
     * @param gameMap The fully populated map array.
     * @param width   Map width in tiles.
     * @param height  Map height in tiles.
     */
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

    /**
     * Entry point for command-line conversion.
     * Expects exactly two arguments: the path to the input TMX file and
     * the path for the output properties file.  Prints usage information
     * and supported layer-name keywords when called with the wrong number
     * of arguments.
     *
     * @param args Command-line arguments – {@code args[0]} is the TMX
     *             input path, {@code args[1]} is the properties output path.
     */
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