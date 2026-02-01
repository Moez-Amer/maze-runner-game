package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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

    protected final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final Viewport viewport;
    private final String mapPath;

    // Tiled map rendering (used only in hybrid mode)
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    // Map data for game logic (collisions, etc.)
    protected int[][] mapData;
    protected int mapWidth;
    protected int mapHeight;
    public static final int TILE_SIZE = 16;

    //enemies
    protected Array<Enemy> enemies;

    // Player
    protected Player player;

    // Debug visualization
    private ShapeRenderer shapeRenderer;
    protected boolean showCollisionBoxes = false;

    // Traps
    private ArrayList<Trap> traps;

    private float delay;
    protected Entry entry;

    // Collectibles and HUD
    private ArrayList<Collectibles> collectibles;
    protected HUD hud;
    private TextureAtlas uiAtlas;
    private OrthographicCamera hudCamera;

    // Voodoo doll for revive mechanic
    protected VoodooDoll voodooDoll;

    // Kill streak system
    protected int killStreak;
    protected String killStreakText;
    protected float killStreakDisplayTimer;
    protected int previousPlayerLives;
    private static final float KILL_STREAK_DISPLAY_DURATION = 2.0f;
    protected Texture killStreakIconTexture;
    protected TextureRegion killStreakIcon;

    // Properties-only mode: Textures for manual tile rendering
    private HashMap<Integer, TextureRegion> tileTextures;
    private TextureRegion groundTexture;
    private TextureRegion exitTexture;

    // Tile exploration tracking
    private HashSet<String> visitedTiles;
    private TextureRegion entranceTexture;
    private Texture mainlevbuildTexture;

    private Texture arrowTexture;
    private com.badlogic.gdx.graphics.g2d.Sprite arrowSprite;
    private com.badlogic.gdx.math.Vector2 exitPosition;

    // Exit and entrance bounds (computed once for properties-only mode)
    private float exitX, exitY, exitWidth, exitHeight;
    private float entranceX, entranceY, entranceWidth, entranceHeight;
    private boolean exitBoundsFound = false;
    private boolean entranceBoundsFound = false;
    // Loading mode flag
    private boolean useTiledMap = false;
    private KeyBindings keys;
    // Track time between kills
    protected float timeSinceLastKill;
    private static final float KILL_STREAK_WINDOW = 3.0f; // 3.0 seconds to get the next kill

    // Developer console support
    private boolean consolePaused = false;
    private boolean godModeEnabled = false;
    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.mapPath = mapPath;
        this.enemies = new Array<>();
        this.keys = KeyBindings.getKeyBindings();
        this.visitedTiles = new HashSet<>();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(TILE_SIZE * 30, TILE_SIZE * 18, camera);
        camera.setToOrtho(false);
        camera.zoom = 1.3f;
        font = game.getSkin().getFont("font");

        shapeRenderer = new ShapeRenderer();

        loadMap(mapPath);
        findEntry();

        player = new Player(entry.getX(), entry.getY(), TILE_SIZE, mapData, game.getGameState());
        findDeathPits_KnifesTraps();

        for (int j = 0; j < mapHeight; j++) {
            for (int i = 0; i < mapWidth; i++) {
                if (mapData[i][j] == TYPE_ENEMY){
                    float centeredX= (i*TILE_SIZE);
                    float centeredY= (j*TILE_SIZE);
                    Enemy enemy = new Enemy(centeredX,centeredY,TILE_SIZE,mapData, "Enemy_Assets/Undead executioner puppet/png/",100,100);
                    this.enemies.add(enemy);
                    // ... existing path marking logic ...
                    for(int xOffSet = 0;xOffSet<2;xOffSet++){
                        for(int yOffSet=0; yOffSet<4; yOffSet++){
                            int checkX=i+xOffSet;
                            int checkY=j+yOffSet;
                            if (checkX<mapWidth&&checkY<mapHeight){
                                mapData[checkX][checkY]=TYPE_PATH;
                            }
                        }
                    }
                }
                else if (mapData[i][j] == TYPE_BOSS) { // TYPE_BOSS
                    float centeredX = (i * TILE_SIZE);
                    float centeredY = (j * TILE_SIZE);

                    FinalBoss boss = new FinalBoss(centeredX, centeredY, TILE_SIZE, mapData);
                    this.enemies.add(boss);

                    // Mark the ground as walkable sohe boss isn't stuck
                    mapData[i][j] = TYPE_PATH;
                    System.out.println("FINAL BOSS SPAWNED AT: " + i + "," + j);
                }
            }
        }
        player.setEnemies(this.enemies);
        for (Enemy enemy : enemies) {
            enemy.setEnemies(this.enemies);
            enemy.setPlayer(player);
        }

        uiAtlas = new TextureAtlas(Gdx.files.internal("craft/craftacular-ui.atlas"));
        Collectibles.loadTextures(uiAtlas);
        VoodooDoll.loadTexture();

        // Load kill streak icon
        killStreakIconTexture = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon1.png"));
        killStreakIcon = new TextureRegion(killStreakIconTexture);

        BitmapFont regularFont = game.getSkin().getFont("font");
        BitmapFont boldFont = game.getSkin().getFont("bold");
        hud = new HUD(uiAtlas, regularFont, boldFont);

        // --- NEW: LISTENER FOR POPUPS ---
        GameState.getAchievementManager().setListener(new AchievementManager.AchievementListener() {
            @Override
            public void onAchievementUnlocked(Achievement achievement) {
                hud.showAchievementPopup(achievement);
                AudioManager.playPickupSound();
            }
        });
        // --------------------------------

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        spawnCollectibles();
        AudioManager.load();

        arrowTexture = new Texture(Gdx.files.internal("Arrow.png"));
        arrowSprite = new com.badlogic.gdx.graphics.g2d.Sprite(arrowTexture);
        arrowSprite.setOriginCenter();
        arrowSprite.setScale(0.8f);

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_EXIT) {
                    this.exitPosition = new com.badlogic.gdx.math.Vector2(x * TILE_SIZE + TILE_SIZE / 2f, y * TILE_SIZE + TILE_SIZE / 2f);
                    break;
                }
            }
        }
        if (exitPosition == null) {
            exitPosition = new com.badlogic.gdx.math.Vector2(0, 0);
        }

        // Initialize kill streak
        this.killStreak = 0;
        this.killStreakText = "";
        this.killStreakDisplayTimer = 0f;
        this.previousPlayerLives = player.getLives();

        this.timeSinceLastKill = 0f;
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
        if (tiledMap.getLayers().get("Key") != null) {
            tiledMap.getLayers().get("Key").setVisible(false);
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
        groundTexture = new TextureRegion(mainlevbuildTexture, 736, 320, TILE_SIZE, TILE_SIZE);

        // Wall texture at pixel (336, 16) - 16x16
        tileTextures.put(TYPE_WALL, new TextureRegion(mainlevbuildTexture, 151, 224, TILE_SIZE, TILE_SIZE));

        // Exit texture - full rectangle from (640,0) to (704,80) = 64x80 pixels
        exitTexture = new TextureRegion(mainlevbuildTexture, 880, 32, 64, 80);

        // Entrance texture - full rectangle from (720,48) to (752,80) = 32x32 pixels
        entranceTexture = new TextureRegion(mainlevbuildTexture, 736, 64, 32, 32);

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
        traps = new ArrayList<>();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_DEATHTRAP) {
                    // Both subclasses are now treated as 'Trap' objects
                    traps.add(new DeathPitTrap(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, player, entry));
                } else if (mapData[x][y] == TYPE_KNIFE_TRAP) {
                    delay = (x + y) * 0.05f;
                    traps.add(new KnifesTrap(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, player, delay));
                }
            }
        }
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
     * Scans the mapData grid for TYPE_KEY tiles (5) and spawns Key collectibles.
     * Matches the approach used for Walls, Entrance, and Traps.
     */
    private void findKeysInMap() {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                // Check if the tile is a KEY (Type 5)
                // You can use TiledToPropertiesConverter.TYPE_KEY or just 5
                if (mapData[x][y] == TYPE_KEY) {

                    // Spawn the key exactly at this tile's position
                    collectibles.add(new Collectibles(
                            x * TILE_SIZE,
                            y * TILE_SIZE,
                            TILE_SIZE, TILE_SIZE,
                            50,
                            Collectibles.CollectibleType.KEY
                    ));

                    System.out.println("Loaded Key from Map at: " + x + "," + y);
                }
            }
        }
    }

    /**
     * Spawns collectibles randomly on the map.
     */
    protected void spawnCollectibles() {
        collectibles = new ArrayList<>();
        findKeysInMap();

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

        int collectibleSize = TILE_SIZE; // 16 pixels
        float minDistanceBetweenColl = 160.0f;
        java.util.Random random = new java.util.Random();

        // Spawn collectibles

        spawnCollectibleType(Collectibles.CollectibleType.SCROLL, 3, 50,
                collectibleSize, collectibleSize,
                walkablePositions, minDistanceBetweenColl, random);

        spawnCollectibleType(Collectibles.CollectibleType.HEALTH, 2, 10,
                collectibleSize, collectibleSize,
                walkablePositions, minDistanceBetweenColl, random);

        spawnCollectibleType(Collectibles.CollectibleType.SPEED_BOOSTER, 2, 20,
                collectibleSize, collectibleSize,
                walkablePositions, minDistanceBetweenColl, random);

        spawnCollectibleType(Collectibles.CollectibleType.POWER_BOOSTER, 2, 20,
                collectibleSize, collectibleSize,
                walkablePositions, minDistanceBetweenColl, random);

        spawnCollectibleType(Collectibles.CollectibleType.SHIELD, 2, 30,
                collectibleSize, collectibleSize,
                walkablePositions, minDistanceBetweenColl, random);

        System.out.println("Spawned " + collectibles.size() + " collectibles");
    }

    protected void spawnCollectibleType(Collectibles.CollectibleType type, int count, int points,
                                      float width, float height,
                                      ArrayList<int[]> walkablePositions,
                                      float minDistance, java.util.Random random) {
        int attempts = 0;
        int maxAttempts = 300; // Increased attempts slightly since constraints are harder
        int spawnedCount = 0;

        while (spawnedCount < count && attempts < maxAttempts) {
            attempts++;

            // Pick a random safe tile
            int[] pos = walkablePositions.get(random.nextInt(walkablePositions.size()));
            float potentialX = pos[0] * TILE_SIZE;
            float potentialY = pos[1] * TILE_SIZE;

            boolean tooClose = false;

            // CHECK GLOBAL LIST: ensuring we don't spawn near ANY existing collectible
            // This prevents potions from spawning on top of Keys, Scrolls, or other Potions
            for (Collectibles existing : collectibles) {
                float dx = potentialX - existing.getX();
                float dy = potentialY - existing.getY();
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                collectibles.add(new Collectibles(
                        potentialX, potentialY,
                        width, height,
                        points,
                        type
                ));
                spawnedCount++;
            }
        }
    }

    private void updateCollectibles(float delta) {
        // Check if player is in ghost mode; if so, skip pickup logic entirely
        if (player.isGhostMode()) {
            // We still call update on collectibles so they continue to float/animate
            for (Collectibles collectible : collectibles) {
                collectible.update(delta);
            }
            return;
        }

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

    protected void handleCollectiblePickup(Collectibles collectible) {
        collectible.collect();
        player.addScore(collectible.getPoints()); // Add points to run

        if (collectible.getType() == Collectibles.CollectibleType.KEY || collectible.getType() == Collectibles.CollectibleType.SCROLL) {
            // Play a more distinct sound for quest-critical items (keys and scrolls)
            AudioManager.playPickupSound();}
        else if (collectible.getType() == Collectibles.CollectibleType.SPEED_BOOSTER ||
                    collectible.getType() == Collectibles.CollectibleType.POWER_BOOSTER ||
                    collectible.getType() == Collectibles.CollectibleType.SHIELD) {
                AudioManager.playPotionPickupSound();
        } else {
            // Play the standard pickup sound for general items
            AudioManager.playPickupSound();
        }

        switch (collectible.getType()) {
            case HEALTH:
                game.getGameState().recordHeartPickup();
                player.addLife();
                break;
            case SPEED_BOOSTER:
                player.applySpeedBoost(5.0f);
                game.getGameState().recordPotionUsed();
                break;
            case POWER_BOOSTER:
                player.applyPowerBoost(5.0f);
                game.getGameState().recordPotionUsed();
                break;
            case SHIELD:
                player.applyShield(8.0f);
                game.getGameState().recordPotionUsed();
                break;
            case KEY:
                player.collectKey();
                game.getGameState().recordKeyCollected();
                break;
            case SCROLL:
                player.collectScroll();
                game.getGameState().recordScrollCollected();
                break;
        }
    }

    /**
     * Renders the kill streak announcement text and icon on screen.
     */
    protected void renderKillStreakAnnouncement(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        BitmapFont boldFont = game.getSkin().getFont("bold");

        // Scale effect - text and icon grow as they appear
        float progress = 1.0f - (killStreakDisplayTimer / KILL_STREAK_DISPLAY_DURATION);
        float scale = Math.min(1.0f + progress * 0.5f, 1.5f);

        boldFont.getData().setScale(scale);

        // Color based on streak level
        Color color;
        if (killStreak >= 15) {
            color = new Color(1f, 0.84f, 0f, 1f); // Gold
        } else if (killStreak >= 10) {
            color = new Color(1f, 0.5f, 0f, 1f); // Orange
        } else if (killStreak >= 5) {
            color = new Color(1f, 0f, 0f, 1f); // Red
        } else {
            color = Color.WHITE;
        }

        boldFont.setColor(color);

        // Position - center of screen, slightly above middle
        GlyphLayout layout = new GlyphLayout(boldFont, killStreakText);
        float textX = (screenWidth - layout.width) / 2f;
        float textY = screenHeight / 2f + 100;

        // Draw icon above the text
        if (killStreakIcon != null) {
            float iconSize = 48f * scale; // Icon scales with text
            float iconX = (screenWidth - iconSize) / 2f;
            float iconY = textY + 20; // Position above text

            batch.setColor(color);
            batch.draw(killStreakIcon, iconX, iconY, iconSize, iconSize);
            batch.setColor(Color.WHITE);
        }

        boldFont.draw(batch, killStreakText, textX, textY);

        // Reset font
        boldFont.getData().setScale(1.0f);
        boldFont.setColor(Color.WHITE);
    }

    /**
     * Displays kill streak announcement based on current streak count.
     */
    protected void announceKillStreak(int streak) {
        String announcement = "";
        switch (streak) {
            case 2:
                announcement = "DOUBLE KILL!";
                break;
            case 3:
                announcement = "TRIPLE KILL!";
                break;
            case 4:
                announcement = "QUAD KILL!";
                break;
            case 5:
                announcement = "KILLING SPREE!";
                break;
            case 7:
                announcement = "RAMPAGE!";
                break;
            case 10:
                announcement = "UNSTOPPABLE!";
                break;
            case 15:
                announcement = "GODLIKE!";
                break;
            default:
                if (streak > 15) {
                    announcement = "LEGENDARY!";
                }
                break;
        }

        if (!announcement.isEmpty()) {
            killStreakText = announcement;
            killStreakDisplayTimer = KILL_STREAK_DISPLAY_DURATION;
            System.out.println("Kill Streak: " + announcement + " (" + streak + " kills)");
        }
    }

    /**
     * Resets the kill streak (called when player takes damage).
     */
    protected void resetKillStreak() {
        if (killStreak > 0) {
            System.out.println("Kill streak ended at " + killStreak);
            killStreak = 0;
        }
    }

    /**
     * Tracks tile exploration for achievements.
     * Records when player visits a new tile.
     */
    protected void trackTileExploration() {
        // Get player's current tile position
        int tileX = (int) (player.getX() / TILE_SIZE);
        int tileY = (int) (player.getY() / TILE_SIZE);

        // Create unique key for this tile
        String tileKey = tileX + "," + tileY;

        // If this is a new tile, record it
        if (!visitedTiles.contains(tileKey)) {
            visitedTiles.add(tileKey);
            game.getGameState().recordTileExplored();
        }
    }

    /**
     * Checks if the ghost player has collected their voodoo doll to revive.
     */
    protected void checkVoodooDollCollection() {
        if (voodooDoll == null || !player.isGhostMode()) {
            return;
        }

        float[] playerBox = player.getFeetCollisionBox();
        float px = playerBox[0], py = playerBox[1], pw = playerBox[2], ph = playerBox[3];
        float vx = voodooDoll.getX(), vy = voodooDoll.getY();
        float vw = voodooDoll.getWidth(), vh = voodooDoll.getHeight();

        // Check collision between player and voodoo doll
        if (px < vx + vw && px + pw > vx && py < vy + vh && py + ph > vy) {
            voodooDoll.collect();
            player.revive();
            AudioManager.playPickupSound(); // Play special sound for revive
            System.out.println("Player revived with 1 life!");
        }
    }

    @Override
    public void render(float delta) {
        // Check for escape key press to go back to the pause
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToPause(this);
            return;
        }

        if (!consolePaused) {
            float zoomSpeed = 0.5f * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.EQUALS) || Gdx.input.isKeyPressed(Input.Keys.PLUS)) {
                camera.zoom = Math.max(0.1f, camera.zoom - zoomSpeed); // Zoom In
            }
            if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
                camera.zoom = Math.min(3.0f, camera.zoom + zoomSpeed); // Zoom Out
            }
        }
        // Toggle collision box visualization with K key
        if (keys.isKeyJustPressed("Debug")) {
            showCollisionBoxes = !showCollisionBoxes;
            System.out.println("Collision boxes: " + (showCollisionBoxes ? "ON" : "OFF"));
        }

        ScreenUtils.clear(0, 0, 0, 1);

        if (!consolePaused) {
            player.update(delta);
            // Track tile exploration
            trackTileExploration();

            // Check if player took damage (reset kill streak)
            if (player.getLives() < previousPlayerLives) {
                resetKillStreak();
            }
            previousPlayerLives = player.getLives();

            checkPlayerAttackHits();

            for (Enemy enemy : enemies){
                enemy.update(delta);
            }
            // Update traps
            for (Trap trap : traps) {
                trap.update(delta);
            }


            // Update collectibles
            updateCollectibles(delta);

            // Update voodoo doll if it exists
            if (voodooDoll != null && !voodooDoll.isCollected()) {
                voodooDoll.update(delta);
                checkVoodooDollCollection();
            }

            if (killStreak > 0) {
                timeSinceLastKill += delta;
                if (timeSinceLastKill > KILL_STREAK_WINDOW) {
                    resetKillStreak();
                }
            }

            // Update kill streak display timer
            if (killStreakDisplayTimer > 0) {
                killStreakDisplayTimer -= delta;
            }

            checkWinCondition();
            checkLoseCondition();
            if (player.isMoving() && player.isRunning()) {
                game.getGameState().recordSprinting(player.getSpeed() * delta);
            }
        }

        // Center camera on player
        centerCameraOnPlayer();
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

        for (Trap trap : traps) {
            trap.render(game.getSpriteBatch());
        }


        // Draw collectibles
        for (Collectibles collectible : collectibles) {
            collectible.render(game.getSpriteBatch());
        }

        // Draw voodoo doll if active
        if (voodooDoll != null && !voodooDoll.isCollected()) {
            voodooDoll.render(game.getSpriteBatch());
        }

        for (Enemy enemy : enemies){
            enemy.render(game.getSpriteBatch());
        }

        // Logic for updating and rendering the navigation arrow
        if (exitPosition != null && (exitPosition.x != 0 || exitPosition.y != 0)){
            float[] feet = player.getFeetCollisionBox();
            float pCenterX = feet[0] + feet[2] / 2f;
            float pCenterY = feet[1] + feet[3] / 2f;

            // Calculate the direction vector from player to exit
            float dx = exitPosition.x - pCenterX;
            float dy = exitPosition.y - pCenterY;

            // Calculate the rotation angle based on the direction vector
            float angle = com.badlogic.gdx.math.MathUtils.atan2(dy, dx) * com.badlogic.gdx.math.MathUtils.radDeg;

            // Position the arrow slightly above the player's head
            float arrowX = pCenterX - (arrowSprite.getWidth() / 2f);
            float arrowY = player.getY() + 48f;

            arrowSprite.setPosition(arrowX, arrowY);
            arrowSprite.setRotation(angle);

            // Turn arrow yellow when the objective is met (key + all scrolls collected)
            if (player.canExitMaze()) {
                arrowSprite.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
            } else {
                arrowSprite.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            }

            // Render the arrow sprite in the world coordinate system
            arrowSprite.draw(game.getSpriteBatch());
        }

        // Draw player
        player.render(game.getSpriteBatch());
        game.getSpriteBatch().end();

        // Render HUD
        hudCamera.update();
        game.getSpriteBatch().setProjectionMatrix(hudCamera.combined);
        game.getSpriteBatch().begin();

        if (this instanceof SurvivalGameScreen) {
            SurvivalGameScreen survivalScreen = (SurvivalGameScreen) this;
            hud.renderSurvival(game.getSpriteBatch(), player, survivalScreen.getWaveManager(), survivalScreen.getGameScreenTime());
        } else {
            hud.render(game.getSpriteBatch(), player);
        }

        // Render kill streak announcement
        if (killStreakDisplayTimer > 0 && !killStreakText.isEmpty()) {
            renderKillStreakAnnouncement(game.getSpriteBatch());
        }
        game.getSpriteBatch().end();

        // Debug visualization
        if (showCollisionBoxes) {
            renderCollisionDebug();
        }
        game.updateConsole(delta);
        game.renderConsole();
    }

    /**
     * Renders map tiles manually from properties data.
     * Only used in properties-only mode.
     */
    protected void renderMapTiles() {
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        // Render ALL tiles
        int startX = 0;
        int startY = 0;
        int endX = mapWidth - 1;
        int endY = mapHeight - 1;
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
    protected void checkPlayerAttackHits() {
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
                // Updated: Damage is now calculated only by base multiplier and power boosts
                float damage = 1.0f * (player.hasPowerBoost() ? 2.0f : 1.0f);

                enemy.takeDamage(damage);
                player.setAttackHasHit(true);

                
                if (enemy.isDead()) {
                    enemies.removeIndex(i);
                    game.getGameState().recordKill();
                    player.addScore(100);
                    SaveManager.save(game.getGameState());

                    killStreak++;
                    timeSinceLastKill = 0f;
                    announceKillStreak(killStreak);
                }
                break;
            }
        }
    }

    /**
     * Checks if the player has won the game.
     * Win condition: Player must have 1 key AND 3 scrolls AND reach the exit.
     */
    protected void checkWinCondition() {
        if (!player.canExitMaze()) {
            return;
        }

        float[] playerBox = player.getFeetCollisionBox();
        float px = playerBox[0], py = playerBox[1], pw = playerBox[2], ph = playerBox[3];


        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_EXIT) {
                    float tileX = x * TILE_SIZE;
                    float tileY = y * TILE_SIZE;

                    if (px < tileX + TILE_SIZE && px + pw > tileX &&
                            py < tileY + TILE_SIZE && py + ph > tileY) {

                        System.out.println("Victory condition met!");

                        int currentHighScore = game.getGameState().levelHighScores.getOrDefault(mapPath, 0);
                        if (player.getScore() > currentHighScore) {
                            game.getGameState().levelHighScores.put(mapPath, player.getScore());
                            SaveManager.save(game.getGameState());
                        }

                        // Track maze completion
                        game.getGameState().recordMazeCompleted();

                        // Check if it was a perfect maze (no damage taken)
                        if (player.getLives() == player.getMaxLives()) {
                            game.getGameState().recordPerfectMaze();
                        }

                        game.goToVictory(player.getScore(),mapPath);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Checks if the player has lost the game.
     * Lose condition: Player's lives reach 0.
     * If they haven't used their revive yet, enters ghost mode.
     */
    protected void checkLoseCondition() {
        if (player.isDead() && !player.isGhostMode()) {
            // Check if player can use revive mechanic (once per level)
            if (!player.hasUsedRevive()) {
                // Spawn voodoo doll at death location
                float[] playerBox = player.getFeetCollisionBox();
                float deathX = playerBox[0];
                float deathY = playerBox[1];
                voodooDoll = new VoodooDoll(deathX, deathY, TILE_SIZE * 1.5f, TILE_SIZE * 1.5f);

                // Enter ghost mode and respawn at entry
                player.enterGhostMode(entry.getX(), entry.getY());
                System.out.println("Ghost mode activated! Find your voodoo doll to revive!");
            } else {
                // Already used revive or time ran out - game over
                game.goToGameOver(mapPath, player.getScore());
            }
        }
    }

    protected void renderCollisionDebug() {

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
                // Shade exit tiles in green
                if (type == TYPE_EXIT) {
                    shapeRenderer.setColor(0, 1, 0, 0.4f);
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
        // This tells the viewport to recalculate based on new window dimensions
        viewport.update(width, height, false);

        // Update the HUD camera separately so UI stays correctly sized
        hudCamera.setToOrtho(false, width, height);

        // Readjust camera position immediately so the player stays centered
        centerCameraOnPlayer();

        // Resize console
        if (game.getConsole() != null) {
            game.getConsole().resize(width, height);
        }
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

        if (arrowTexture != null) arrowTexture.dispose();
        if (killStreakIconTexture != null) killStreakIconTexture.dispose();

        VoodooDoll.dispose();
    }

    public String getMapPath() {
        return mapPath;
    }

    /**
     * Centers the camera on the player's position with the required offset.
     */
    protected void centerCameraOnPlayer() {
        if (player != null) {
            // Keeps the player centered + 40 pixel vertical offset
            camera.position.set(player.getX() + TILE_SIZE / 2f, player.getY() + TILE_SIZE / 2f + 40f, 0);
            camera.update();
        }
    }

    /**
     * Gets the player instance.
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }
    /**
     * Sets whether the console has paused the game.
     * @param paused True to pause game logic, false to resume
     */
    public void setConsolePaused(boolean paused) {
        this.consolePaused = paused;
    }
    /**
     * Toggles god mode and returns the new state.
     * @return True if god mode is now enabled, false otherwise
     */
    public boolean toggleGodMode() {
        godModeEnabled = !godModeEnabled;
        if (player != null) {
            player.setGodMode(godModeEnabled);
        }
        return godModeEnabled;
    }
    /**
     * Kills all enemies on the map.
     * @return Number of enemies killed
     */
    public int killAllEnemies() {
        int count = enemies.size;
        enemies.clear();
        return count;
    }
    /**
     * Spawns an enemy at the specified position.
     * @param x X coordinate in pixels
     * @param y Y coordinate in pixels
     */
    public void spawnEnemyAtPosition(float x, float y) {
        Enemy enemy = new Enemy(x, y, TILE_SIZE, mapData, "Enemy_Assets/Undead executioner puppet/png/", 100, 100);
        enemy.setEnemies(this.enemies);
        enemy.setPlayer(player);
        this.enemies.add(enemy);
        System.out.println("Enemy spawned at (" + x + ", " + y + ")");
    }

    /**
     * Gets the map width in tiles.
     * @return The map width
     */
    public int getMapWidth() {
        return mapWidth;
    }

    /**
     * Gets the map height in tiles.
     * @return The map height
     */
    public int getMapHeight() {
        return mapHeight;
    }

    /**
     * Gets the map data array.
     * @return 2D array of tile types
     */
    public int[][] getMapData() {
        return mapData;
    }

    /**
     * Gets the enemies array.
     * @return Array of all active enemies
     */
    public Array<Enemy> getEnemies() {
        return enemies;
    }

    /**
     * Adds an enemy to the game.
     * @param enemy The enemy to add
     */
    public void addEnemy(Enemy enemy) {
        this.enemies.add(enemy);
    }

    /**
     * Gets the HUD instance.
     * @return The HUD
     */
    public HUD getHUD() {
        return hud;
    }

    /**
     * Gets the game state.
     * @return The game state
     */
    public GameState getGameState() {
        return game.getGameState();
    }

    /**
     * Gets the sprite batch from the main game.
     * @return The sprite batch
     */
    public SpriteBatch getSpriteBatch() {
        return game.getSpriteBatch();
    }

    /**
     * Gets the main game instance.
     * @return The game
     */
    public MazeRunnerGame getGame() {
        return game;
    }
}