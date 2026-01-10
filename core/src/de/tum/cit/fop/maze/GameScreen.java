package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import static de.tum.cit.fop.maze.TiledToPropertiesConverter.*;

/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 *
 * THIS VERSION: Supports DUAL loading modes:
 * - Hybrid mode: Tiled map (TMX) + properties overrides
 * - Properties-only mode: Pure properties file loading with manual rendering
 *
 * The loading method is determined by the "loader" property in the properties file:
 * - loader=tiled -> Uses hybrid Tiled map loading
 * - loader=custom -> Uses properties-only loading
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;

    // Tiled map rendering (used only in hybrid mode)
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    // Map data for game logic (collisions, etc.)
    private int[][] mapData;
    private int mapWidth;
    private int mapHeight;
    public static final int TILE_SIZE = 16;

    //enemies
    private Array<Enemy> enemies;

    // Player
    private Player player;

    // Debug visualization
    private ShapeRenderer shapeRenderer;
    private boolean showCollisionBoxes = false;

    // Traps
    private ArrayList<DeathPitTrap> deathPitTraps;
    private ArrayList<KnifesTrap> knifesTraps;
    private float delay;
    private Entry entry;

    // Collectibles and HUD
    private ArrayList<Collectibles> collectibles;
    private HUD hud;
    private TextureAtlas uiAtlas;
    private OrthographicCamera hudCamera;

    // Properties-only mode: Textures for manual tile rendering
    private HashMap<Integer, TextureRegion> tileTextures;
    private TextureRegion groundTexture;
    private TextureRegion exitTexture;
    private TextureRegion entranceTexture;
    private Texture mainlevbuildTexture;

    // Exit and entrance bounds (computed once for properties-only mode)
    private float exitX, exitY, exitWidth, exitHeight;
    private float entranceX, entranceY, entranceWidth, entranceHeight;
    private boolean exitBoundsFound = false;
    private boolean entranceBoundsFound = false;

    // Loading mode flag
    private boolean useTiledMap = false;

    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;
        this.enemies=new Array<>();

        // Create and configure the camera for the game view
        camera = new OrthographicCamera();
        camera.setToOrtho(false, TILE_SIZE * 30, TILE_SIZE * 18);

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // Create shape renderer for debug visualization
        shapeRenderer = new ShapeRenderer();

        // Load map using appropriate method based on properties flag
        loadMap("maps/level-1.properties");

        findEntry();
        // Create player at entry point
        player = new Player(entry.getX(), entry.getY(), TILE_SIZE, mapData);
        findDeathPits_KnifesTraps();
        //Create enemy
        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                if (mapData[i][j] == 4){
                    float centeredX= (i*TILE_SIZE)-100/2f+ TILE_SIZE/2;
                    float centeredY= (j*TILE_SIZE)-20;
                    Enemy enemy = new Enemy(centeredX,centeredY,TILE_SIZE,mapData, "Enemy_Assets/Undead executioner puppet/png/",100,100);
                    this.enemies.add(enemy);
                    for(int xOffSet = 0;xOffSet<2;xOffSet++){
                        for(int yOffSet=0; yOffSet<4; yOffSet++){
                            int checkX=i+xOffSet;
                            int checkY=j+yOffSet;
                            if (checkX<mapWidth&&checkY<mapHeight){
                                mapData[checkX][checkY]=1;
                            }
                        }
                    }

                }
            }
        }
        player.setEnemies(this.enemies);

        for (Enemy enemy : enemies) {
            enemy.setEnemies(this.enemies);
            enemy.setPlayer(player);
        }
        // Load UI atlas for collectibles and HUD
        uiAtlas = new TextureAtlas(Gdx.files.internal("craft/craftacular-ui.atlas"));
        Collectibles.loadTextures(uiAtlas);

        // Create HUD
        BitmapFont regularFont = game.getSkin().getFont("font");
        BitmapFont boldFont = game.getSkin().getFont("bold");
        hud = new HUD(uiAtlas, regularFont, boldFont);
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Spawn collectibles from map data
        spawnCollectibles();

        // Load Audio
        AudioManager.load();
    }

    /**
     * Master loading method that determines which approach to use.
     * Reads the "loader" property from the properties file:
     * - "tiled" = Hybrid mode (Tiled map + properties)
     * - "custom" = Properties-only mode
     *
     * @param propertiesPath Path to the properties file
     */
    private void loadMap(String propertiesPath) {
        try {
            Properties props = new Properties();
            props.load(Gdx.files.internal(propertiesPath).read());

            // Check for loader type flag (default to "custom" if not specified)
            String loaderType = props.getProperty("loader", "custom").trim().toLowerCase();

            System.out.println("Loading map with loader type: " + loaderType);

            if ("tiled".equals(loaderType)) {
                // Hybrid mode: Load Tiled map + apply properties overrides
                loadTiledHybridMap(props);
                useTiledMap = true;
                System.out.println("Map loaded using HYBRID (Tiled + Properties) method");
            } else {
                // Properties-only mode: Load everything from properties
                loadPropertiesOnlyMap(props, propertiesPath);
                useTiledMap = false;
                System.out.println("Map loaded using PROPERTIES-ONLY method");
            }

        } catch (Exception e) {
            System.err.println("Failed to load map: " + e.getMessage());
            e.printStackTrace();
            // Fallback to properties-only mode
            loadPropertiesOnlyMapFallback(propertiesPath);
        }
    }

    /**
     * HYBRID MODE: Load Tiled map and apply properties overrides.
     * This is the original loading method for Tiled-created maps.
     *
     * @param props Properties object with map configuration
     */
    private void loadTiledHybridMap(Properties props) {
        // Get TMX file path from properties (or use default)
        String tmxPath = props.getProperty("tmx_file", "MoriaMap/newmap2.tmx");

        // Load the Tiled map for rendering
        tiledMap = new TmxMapLoader().load(tmxPath);
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        // Hide the Player and Enemy layers from Tiled map (we render our own)
        if (tiledMap.getLayers().get("Player") != null) {
            tiledMap.getLayers().get("Player").setVisible(false);
        }
        if (tiledMap.getLayers().get("Enemy") != null) {
            tiledMap.getLayers().get("Enemy").setVisible(false);
        }
        if (tiledMap.getLayers().get("Knifes") != null) {
            tiledMap.getLayers().get("Knifes").setVisible(false);
        }

        // Get map dimensions from the Tiled map
        mapWidth = tiledMap.getProperties().get("width", Integer.class);
        mapHeight = tiledMap.getProperties().get("height", Integer.class);
        int tileWidth = tiledMap.getProperties().get("tilewidth", Integer.class);
        int tileHeight = tiledMap.getProperties().get("tileheight", Integer.class);

        System.out.println("Tiled map loaded: " + mapWidth + "x" + mapHeight + " tiles, tile size: " + tileWidth + "x" + tileHeight);

        // Load game logic map from the same properties file
        loadMapLogicData(props);
    }

    /**
     * PROPERTIES-ONLY MODE: Load everything from properties file.
     * Manually render tiles without using Tiled map loader.
     *
     * @param props Properties object with map configuration
     * @param propertiesPath Path to the properties file (for debug output)
     */
    private void loadPropertiesOnlyMap(Properties props, String propertiesPath) {
        // Load map logic data from properties
        loadMapLogicData(props);

        // Load tile textures for manual rendering
        loadTileTextures();

        System.out.println("Properties-only map loaded from: " + propertiesPath);
    }

    /**
     * Fallback method if properties file fails to load.
     * Uses default properties-only loading.
     *
     * @param propertiesPath Path to the properties file
     */
    private void loadPropertiesOnlyMapFallback(String propertiesPath) {
        useTiledMap = false;
        loadMapLogicFromFile(propertiesPath);
        loadTileTextures();
        System.out.println("Fallback: Loaded map in properties-only mode");
    }

    /**
     * Load map logic data from Properties object.
     * This populates the mapData[][] array for collision detection.
     *
     * @param props Properties object containing tile data
     */
    private void loadMapLogicData(Properties props) {
        try {
            // Find map dimensions by scanning all coordinate keys
            int maxX = 0, maxY = 0;
            for (String key : props.stringPropertyNames()) {
                if (key.contains(",") && !key.startsWith("#")) {
                    String[] parts = key.split(",");
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }

            mapWidth = maxX + 1;
            mapHeight = maxY + 1;
            mapData = new int[mapWidth][mapHeight];

            // Initialize all tiles as PATH (type 1) by default
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    mapData[x][y] = TYPE_PATH;
                }
            }

            // Load tile data from properties
            for (String key : props.stringPropertyNames()) {
                if (key.contains(",") && !key.startsWith("#")) {
                    String[] parts = key.split(",");
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int type = Integer.parseInt(props.getProperty(key).trim());
                    mapData[x][y] = type;
                }
            }

            System.out.println("Map logic loaded: " + mapWidth + "x" + mapHeight);

        } catch (Exception e) {
            System.err.println("Failed to load map logic data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load map logic from file path (legacy method for fallback).
     *
     * @param filePath Path to the properties file
     */
    private void loadMapLogicFromFile(String filePath) {
        try {
            Properties props = new Properties();
            props.load(Gdx.files.internal(filePath).read());
            loadMapLogicData(props);
        } catch (Exception e) {
            System.err.println("Failed to load map logic from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load textures for manual tile rendering (properties-only mode).
     * Extracts tile regions from the mainlevbuild.png sprite sheet.
     */
    private void loadTileTextures() {
        tileTextures = new HashMap<>();

        // Load the main sprite sheet
        mainlevbuildTexture = new Texture(Gdx.files.internal("mainlevbuild.png"));

        // Ground texture at pixel (736, 208) - 16x16
        groundTexture = new TextureRegion(mainlevbuildTexture, 736, 208, TILE_SIZE, TILE_SIZE);

        // Wall texture at pixel (336, 16) - 16x16
        tileTextures.put(TYPE_WALL, new TextureRegion(mainlevbuildTexture, 336, 16, TILE_SIZE, TILE_SIZE));

        // Exit texture - full rectangle from (640,0) to (704,80) = 64x80 pixels
        exitTexture = new TextureRegion(mainlevbuildTexture, 640, 0, 64, 80);

        // Entrance texture - full rectangle from (720,48) to (752,80) = 32x32 pixels
        entranceTexture = new TextureRegion(mainlevbuildTexture, 720, 48, 32, 32);

        // Death trap texture (black/void area)
        tileTextures.put(TYPE_DEATHTRAP, new TextureRegion(mainlevbuildTexture, 0, 0, TILE_SIZE, TILE_SIZE));

        // Compute exit and entrance bounds from map data
        computeExitEntranceBounds();

        System.out.println("Tile textures loaded from mainlevbuild.png");
    }

    /**
     * Finds the bounding box of all EXIT and ENTRY tiles to render the full textures.
     * Used in properties-only mode to render multi-tile structures as single images.
     */
    private void computeExitEntranceBounds() {
        int exitMinX = Integer.MAX_VALUE, exitMinY = Integer.MAX_VALUE;
        int exitMaxX = Integer.MIN_VALUE, exitMaxY = Integer.MIN_VALUE;
        int entryMinX = Integer.MAX_VALUE, entryMinY = Integer.MAX_VALUE;
        int entryMaxX = Integer.MIN_VALUE, entryMaxY = Integer.MIN_VALUE;

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_EXIT) {
                    exitMinX = Math.min(exitMinX, x);
                    exitMinY = Math.min(exitMinY, y);
                    exitMaxX = Math.max(exitMaxX, x);
                    exitMaxY = Math.max(exitMaxY, y);
                    exitBoundsFound = true;
                }
                if (mapData[x][y] == TYPE_ENTRY) {
                    entryMinX = Math.min(entryMinX, x);
                    entryMinY = Math.min(entryMinY, y);
                    entryMaxX = Math.max(entryMaxX, x);
                    entryMaxY = Math.max(entryMaxY, y);
                    entranceBoundsFound = true;
                }
            }
        }

        if (exitBoundsFound) {
            exitX = exitMinX * TILE_SIZE;
            exitY = exitMinY * TILE_SIZE;
            exitWidth = (exitMaxX - exitMinX + 1) * TILE_SIZE;
            exitHeight = (exitMaxY - exitMinY + 1) * TILE_SIZE;
            System.out.println("Exit bounds: (" + exitX + "," + exitY + ") size: " + exitWidth + "x" + exitHeight);
        }

        if (entranceBoundsFound) {
            entranceX = entryMinX * TILE_SIZE;
            entranceY = entryMinY * TILE_SIZE;
            entranceWidth = (entryMaxX - entryMinX + 1) * TILE_SIZE;
            entranceHeight = (entryMaxY - entryMinY + 1) * TILE_SIZE;
            System.out.println("Entrance bounds: (" + entranceX + "," + entranceY + ") size: " + entranceWidth + "x" + entranceHeight);
        }
    }

    /**
     * Traps initialization scanner.
     * Scans the mapData grid and creates trap instances.
     */
    private void findDeathPits_KnifesTraps() {
        deathPitTraps = new ArrayList<>();
        knifesTraps = new ArrayList<>();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_DEATHTRAP) {
                    deathPitTraps.add(new DeathPitTrap(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, player, entry));
                } else if (mapData[x][y] == TYPE_KNIFE_TRAP) {
                    delay = (x + y) * 0.05f;
                    knifesTraps.add(new KnifesTrap(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, player, delay));
                }
            }
        }
        System.out.println("Found " + deathPitTraps.size() + " death pits and " + knifesTraps.size() + " knife traps");
    }

    /**
     * Spawn point locator.
     * Scans grid for TYPE_ENTRY and returns Entry object.
     */
    private Entry findEntry() {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_ENTRY) {
                    this.entry = new Entry(x * TILE_SIZE, y * TILE_SIZE);
                    System.out.println("Entry point found at: (" + x + ", " + y + ")");
                    return this.entry;
                }
            }
        }
        System.err.println("WARNING: No entry point found in map!");
        return null;
    }

    /**
     * Spawns collectibles randomly on the map.
     */
    private void spawnCollectibles() {
        collectibles = new ArrayList<>();

        // Collect all safe walkable positions
        ArrayList<int[]> walkablePositions = new ArrayList<>();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                int tileType = mapData[x][y];

                if (tileType != TYPE_PATH) {
                    continue;
                }

                // Check all 8 neighboring tiles for safety
                boolean safeLocation = true;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;

                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx < 0 || nx >= mapWidth || ny < 0 || ny >= mapHeight) {
                            safeLocation = false;
                            break;
                        }

                        int neighborType = mapData[nx][ny];
                        if (neighborType != TYPE_PATH && neighborType != TYPE_ENTRY && neighborType != TYPE_EXIT) {
                            safeLocation = false;
                            break;
                        }
                    }
                    if (!safeLocation) break;
                }

                if (safeLocation) {
                    walkablePositions.add(new int[]{x, y});
                }
            }
        }

        System.out.println("Found " + walkablePositions.size() + " safe spawn positions");

        if (walkablePositions.isEmpty()) {
            System.err.println("No safe walkable positions found for collectibles!");
            return;
        }

        int collectibleSize = TILE_SIZE + 8;
        float minDistanceBetweenSameType = 10.0f;
        java.util.Random random = new java.util.Random();

        // Spawn 3 of each collectible type
        spawnCollectibleType(Collectibles.CollectibleType.KEY, 3, 50,
                           collectibleSize, collectibleSize,
                           walkablePositions, minDistanceBetweenSameType, random);

        spawnCollectibleType(Collectibles.CollectibleType.HEALTH, 3, 10,
                           collectibleSize, collectibleSize,
                           walkablePositions, minDistanceBetweenSameType, random);

        spawnCollectibleType(Collectibles.CollectibleType.SPEED_BOOSTER, 3, 20,
                           collectibleSize, collectibleSize,
                           walkablePositions, minDistanceBetweenSameType, random);

        spawnCollectibleType(Collectibles.CollectibleType.POWER_BOOSTER, 3, 20,
                           collectibleSize, collectibleSize,
                           walkablePositions, minDistanceBetweenSameType, random);

        spawnCollectibleType(Collectibles.CollectibleType.SHIELD, 3, 30,
                           collectibleSize, collectibleSize,
                           walkablePositions, minDistanceBetweenSameType, random);

        System.out.println("Spawned " + collectibles.size() + " collectibles");
    }

    private void spawnCollectibleType(Collectibles.CollectibleType type, int count, int points,
                                     float width, float height,
                                     ArrayList<int[]> walkablePositions,
                                     float minDistance, java.util.Random random) {
        ArrayList<int[]> spawnedPositions = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 100;

        while (spawnedPositions.size() < count && attempts < maxAttempts) {
            attempts++;

            int[] pos = walkablePositions.get(random.nextInt(walkablePositions.size()));
            int x = pos[0];
            int y = pos[1];

            boolean tooClose = false;
            for (int[] spawnedPos : spawnedPositions) {
                float dx = x - spawnedPos[0];
                float dy = y - spawnedPos[1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                collectibles.add(new Collectibles(
                    x * TILE_SIZE, y * TILE_SIZE,
                    width, height,
                    points,
                    type
                ));
                spawnedPositions.add(new int[]{x, y});
            }
        }
    }

    private void updateCollectibles(float delta) {
        float[] playerBox = player.getFeetCollisionBox();
        float px = playerBox[0], py = playerBox[1], pw = playerBox[2], ph = playerBox[3];

        Iterator<Collectibles> iterator = collectibles.iterator();
        while (iterator.hasNext()) {
            Collectibles collectible = iterator.next();

            if (collectible.isCollected()) {
                iterator.remove();
                continue;
            }

            collectible.update(delta);

            float cx = collectible.getX(), cy = collectible.getY();
            float cw = collectible.getWidth(), ch = collectible.getHeight();

            if (px < cx + cw && px + pw > cx && py < cy + ch && py + ph > cy) {
                handleCollectiblePickup(collectible);
                iterator.remove();
            }
        }
    }

    private void handleCollectiblePickup(Collectibles collectible) {
        collectible.collect();

        if (collectible.getType() == Collectibles.CollectibleType.KEY) {
            // Play a more distinct sound for quest-critical items like keys
            AudioManager.playPickupKeySound();} else {
            // Play the standard pickup sound for general items
            AudioManager.playPickupSound();
        }

        switch (collectible.getType()) {
            case HEALTH:
                player.addLife();
                break;
            case SPEED_BOOSTER:
                player.applySpeedBoost(5.0f);
                break;
            case POWER_BOOSTER:
                player.applyPowerBoost(5.0f);
                break;
            case SHIELD:
                player.applyShield(8.0f);
                break;
            case KEY:
                player.collectKey();
                break;
        }
    }

    @Override
    public void render(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Toggle collision box visualization with K key
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            showCollisionBoxes = !showCollisionBoxes;
            System.out.println("Collision boxes: " + (showCollisionBoxes ? "ON" : "OFF"));
        }

        ScreenUtils.clear(0, 0, 0, 1);

        // Update player (handles input, movement, animation)
        player.update(delta);
        checkPlayerAttackHits();

        for (Enemy enemy : enemies){
            enemy.update(delta);
        }
        // Update traps
        for (DeathPitTrap pitTrap : deathPitTraps) {
            pitTrap.update();
        }
        for (KnifesTrap knifeTrap : knifesTraps) {
            knifeTrap.update(delta);
        }


        // Update collectibles
        updateCollectibles(delta);

        // Center camera on player
        camera.position.set(player.getX() + TILE_SIZE / 2f, player.getY() + TILE_SIZE / 2f + 40f, 0);
        camera.update();

        // Render map based on loading mode
        if (useTiledMap) {
            // Hybrid mode: Use Tiled map renderer
            mapRenderer.setView(camera);
            mapRenderer.render();
        } else {
            // Properties-only mode: Manual tile rendering
            renderMapTiles();
        }

        // Draw game objects
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // Draw traps
        for (KnifesTrap trap : knifesTraps) {
            trap.render(game.getSpriteBatch());
        }


        // Draw collectibles
        for (Collectibles collectible : collectibles) {
            collectible.render(game.getSpriteBatch());
        }
        for (Enemy enemy : enemies){
            enemy.render(game.getSpriteBatch());
        }

        // Draw player
        player.render(game.getSpriteBatch());
        game.getSpriteBatch().end();

        // Render HUD
        hudCamera.update();
        game.getSpriteBatch().setProjectionMatrix(hudCamera.combined);
        game.getSpriteBatch().begin();
        hud.render(game.getSpriteBatch(), player);
        game.getSpriteBatch().end();

        // Debug visualization
        if (showCollisionBoxes) {
            renderCollisionDebug();
        }
    }

    /**
     * Renders map tiles manually from properties data.
     * Only used in properties-only mode.
     */
    private void renderMapTiles() {
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // Calculate visible tile range
        int startX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2) / TILE_SIZE) - 1);
        int startY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2) / TILE_SIZE) - 1);
        int endX = Math.min(mapWidth - 1, (int) ((camera.position.x + camera.viewportWidth / 2) / TILE_SIZE) + 1);
        int endY = Math.min(mapHeight - 1, (int) ((camera.position.y + camera.viewportHeight / 2) / TILE_SIZE) + 1);

        // Render visible tiles
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                float worldX = x * TILE_SIZE;
                float worldY = y * TILE_SIZE;

                int type = mapData[x][y];

                // Draw ground for walkable tiles
                if (type == TYPE_PATH || type == TYPE_ENTRY || type == TYPE_ENEMY ||
                    type == TYPE_KEY || type == TYPE_KNIFE_TRAP || type == TYPE_EXIT) {
                    if (groundTexture != null) {
                        game.getSpriteBatch().draw(groundTexture, worldX, worldY, TILE_SIZE, TILE_SIZE);
                    }
                }

                // Draw wall tiles
                if (type == TYPE_WALL) {
                    TextureRegion wallTexture = tileTextures.get(TYPE_WALL);
                    if (wallTexture != null) {
                        game.getSpriteBatch().draw(wallTexture, worldX, worldY, TILE_SIZE, TILE_SIZE);
                    }
                }

                // Draw death trap tiles
                if (type == TYPE_DEATHTRAP) {
                    TextureRegion trapTexture = tileTextures.get(TYPE_DEATHTRAP);
                    if (trapTexture != null) {
                        game.getSpriteBatch().draw(trapTexture, worldX, worldY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        // Render full exit texture
        if (exitBoundsFound && exitTexture != null) {
            game.getSpriteBatch().draw(exitTexture, exitX, exitY, exitWidth, exitHeight);
        }

        // Render full entrance texture
        if (entranceBoundsFound && entranceTexture != null) {
            game.getSpriteBatch().draw(entranceTexture, entranceX, entranceY, entranceWidth, entranceHeight);
        }

        game.getSpriteBatch().end();
    }

    /**
     * Checks if player's sword attack hits any enemies.
     * Uses the enemy's damage hitbox (3 tiles tall) for detection.
     */
    private void checkPlayerAttackHits() {
        if (!player.isAttacking()) {
            return;
        }

        if (player.hasAttackHit()) {
            return;
        }

        Rectangle swordHitBox = player.getSwordHitBox();

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);

            Rectangle enemyDamageBox = enemy.getDamageHitBox();

            if (swordHitBox.overlaps(enemyDamageBox)) {
                float damage = 1.0f * player.getDamageMultiplier();

                enemy.takeDamage(damage);

                player.setAttackHasHit(true);

                if (enemy.isDead()) {
                    enemies.removeIndex(i);
                    System.out.println("Enemy defeated! Remaining enemies: " + enemies.size);
                }

                break;
            }
        }
    }

    private void renderCollisionDebug() {
        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Calculate visible tile range based on camera
        int startX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2) / TILE_SIZE) - 1);
        int startY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2) / TILE_SIZE) - 1);
        int endX = Math.min(mapWidth - 1, (int) ((camera.position.x + camera.viewportWidth / 2) / TILE_SIZE) + 1);
        int endY = Math.min(mapHeight - 1, (int) ((camera.position.y + camera.viewportHeight / 2) / TILE_SIZE) + 1);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                int type = mapData[x][y];
                if (type == 0) {
                    shapeRenderer.setColor(1, 0, 0, 0.4f);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                int type = mapData[x][y];
                if (type == 0) {
                    shapeRenderer.setColor(1, 0.3f, 0.3f, 0.8f);
                } else {
                    shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 0.4f);
                }
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        shapeRenderer.end();

        // Draw enemy collision and damage boxes (FILLED)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : enemies) {
            // Draw feet collision box (red) - used for movement/pathfinding
            float[] feetBox = enemy.getFeetCollisionBox();
            float feetX = feetBox[0];
            float feetY = feetBox[1];
            float feetW = feetBox[2];
            float feetH = feetBox[3];
            shapeRenderer.setColor(1, 0, 0, 0.4f);
            shapeRenderer.rect(feetX, feetY, feetW, feetH);

            // Draw damage hitbox (orange) - 3 tiles tall, used for combat
            Rectangle damageBox = enemy.getDamageHitBox();
            shapeRenderer.setColor(1, 0.5f, 0, 0.3f); // Orange, semi-transparent
            shapeRenderer.rect(damageBox.x, damageBox.y, damageBox.width, damageBox.height);
        }
        shapeRenderer.end();

        // Draw enemy collision and damage box outlines (LINES)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Enemy enemy : enemies) {
            // Draw feet collision box outline (bright red)
            float[] feetBox = enemy.getFeetCollisionBox();
            shapeRenderer.setColor(1, 0, 0, 1f);
            shapeRenderer.rect(feetBox[0], feetBox[1], feetBox[2], feetBox[3]);

            // Draw damage hitbox outline (bright orange)
            Rectangle damageBox = enemy.getDamageHitBox();
            shapeRenderer.setColor(1, 0.6f, 0, 1f); // Bright orange
            shapeRenderer.rect(damageBox.x, damageBox.y, damageBox.width, damageBox.height);
        }
        shapeRenderer.end();

        // Get the collision box from the player
        float[] feetBox = player.getFeetCollisionBox();
        float feetX = feetBox[0];
        float feetY = feetBox[1];
        float feetW = feetBox[2];
        float feetH = feetBox[3];

        float spriteX = player.getX();
        float spriteY = player.getY();
        float spriteW = 96;
        float spriteH = 96;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 1, 0.4f);
        shapeRenderer.rect(feetX, feetY, feetW, feetH);

        if (player.isAttacking()) {
            shapeRenderer.setColor(1, 0, 0, 0.5f);
            Rectangle sword = player.getSwordHitBox();
            shapeRenderer.rect(sword.x, sword.y, sword.width, sword.height);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(spriteX, spriteY, spriteW, spriteH);
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(feetX, feetY, feetW, feetH);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        if (useTiledMap && tiledMap != null) {
            // Hybrid mode: Get dimensions from Tiled map
            int tileWidth = tiledMap.getProperties().get("tilewidth", Integer.class);
            int tileHeight = tiledMap.getProperties().get("tileheight", Integer.class);
            camera.setToOrtho(false, tileWidth * 30, tileHeight * 18);
        } else {
            // Properties-only mode: Use TILE_SIZE constant
            camera.setToOrtho(false, TILE_SIZE * 30, TILE_SIZE * 18);
        }
        hudCamera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        // Dispose Tiled map resources (hybrid mode only)
        if (tiledMap != null) tiledMap.dispose();
        if (mapRenderer != null) mapRenderer.dispose();

        // Dispose common resources
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiAtlas != null) uiAtlas.dispose();
        if (hud != null) hud.dispose();

        // Dispose properties-only mode resources
        if (mainlevbuildTexture != null) mainlevbuildTexture.dispose();

        AudioManager.dispose();
    }
    
}
