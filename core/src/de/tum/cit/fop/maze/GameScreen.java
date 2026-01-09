package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import static de.tum.cit.fop.maze.TiledToPropertiesConverter.*;


/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;

    // Tiled map rendering
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;

    // Map data for game logic (collisions, etc.)
    private int[][] mapData;
    private int mapWidth;
    private int mapHeight;
    public static final int TILE_SIZE = 16;

    private Player player;

    // Debug visualization
    private ShapeRenderer shapeRenderer;
    private boolean showCollisionBoxes = false;
    //traps
    private ArrayList<DeathPitTrap> deathPitTraps ;
    private ArrayList<KnifesTrap>  knifesTraps;
    private float delay;
    private Entry entry;
    
    // Collectibles and HUD
    private ArrayList<Collectibles> collectibles;
    private HUD hud;
    private TextureAtlas uiAtlas;
    private OrthographicCamera hudCamera;
    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // Load the Tiled map for rendering
        tiledMap = new TmxMapLoader().load("MoriaMap/newmap1.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        
        // Hide the Player layer from Tiled map (we render our own player)
        if (tiledMap.getLayers().get("Player") != null) {
            tiledMap.getLayers().get("Player").setVisible(false);
            tiledMap.getLayers().get("Knifes").setVisible(false);
        }

        // Get map dimensions from the Tiled map
        mapWidth = tiledMap.getProperties().get("width", Integer.class);
        mapHeight = tiledMap.getProperties().get("height", Integer.class);
        int tileWidth = tiledMap.getProperties().get("tilewidth", Integer.class);
        int tileHeight = tiledMap.getProperties().get("tileheight", Integer.class);

        System.out.println("Tiled map loaded: " + mapWidth + "x" + mapHeight + " tiles, tile size: " + tileWidth + "x" + tileHeight);

        // Create and configure the camera for the game view
        camera = new OrthographicCamera();
        // Set viewport to show about 30x18 tiles at a time for 3/4 top-down view
        camera.setToOrtho(false, tileWidth * 30, tileHeight * 18);

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");

        // Create shape renderer for debug visualization
        shapeRenderer = new ShapeRenderer();

        // Load game logic map from properties file (for collisions)
        loadMapLogic("maps/level-1.properties");

        findEntry();
        // Create player at entry point
        player = new Player(entry.getX(), entry.getY(), TILE_SIZE, mapData);
        findDeathPits_KnifesTraps();
        
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
     * Parsing helper for collision data.
     *
     * Logic:
     * - Reads a .properties file containing grid coordinates and types
     * - Determines map dimensions by scanning for max X/Y keys
     * - Populates the mapData[][] array for logical collision checks
     *
     * @param filePath Path to the internal .properties file
     */
    private void loadMapLogic(String filePath) {
        try {
            Properties props = new Properties();
            props.load(Gdx.files.internal(filePath).read());

            // Find map dimensions by scanning all keys
            int maxX = 0, maxY = 0;
            for (String key : props.stringPropertyNames()) {
                if (key.contains(",")) {
                    String[] parts = key.split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }

            int logicWidth = maxX + 1;
            int logicHeight = maxY + 1;
            mapData = new int[logicWidth][logicHeight];

            // Load tile data
            for (String key : props.stringPropertyNames()) {
                if (key.contains(",")) {
                    String[] parts = key.split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int type = Integer.parseInt(props.getProperty(key));
                    mapData[x][y] = type;
                }
            }
            System.out.println("Map logic loaded: " + logicWidth + "x" + logicHeight);

        } catch (Exception e) {
            System.err.println("Failed to load map logic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traps initialization scanner.
     *
     * Logic:
     * - Iterates through the entire mapData grid
     * - Instantiates DeathPitTrap or KnifesTrap objects where map types match
     * - Calculates unique startup delays for KnifesTraps based on (x+y) to create wave patterns
     */
    private void findDeathPits_KnifesTraps() {
        deathPitTraps = new ArrayList<>();
        knifesTraps = new ArrayList<>();
        for(int x = 0; x < mapWidth; x++){
            for(int y = 0; y < mapHeight; y++){
                if(mapData[x][y] == TYPE_DEATHTRAP){
                    deathPitTraps.add(new DeathPitTrap(x*TILE_SIZE,y*TILE_SIZE,TILE_SIZE,TILE_SIZE,player,entry));
                } else if (mapData[x][y] == TYPE_KNIFE_TRAP) {
                    //  X and Y coordinate to calculate a unique delay for specific tile so each one play in different time
                     delay = (x + y) * 0.1f;
                        knifesTraps.add(new KnifesTrap(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE, player, delay));
                }
            }
        }
    }
    /**
     * Spawn point locator.
     *
     * Logic:
     * - Scans grid for TYPE_ENTRY
     * - Returns the first found Entry object adjusted for tile centering
     * - Used to set initial player position and respawn target
     *
     * @return Entry object with coordinate data, or null if not found
     */
    private Entry findEntry() {
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (mapData[x][y] == TYPE_ENTRY) {
                    return this.entry = new Entry(x * TILE_SIZE, y * TILE_SIZE);
                }
            }
        }
        return null;
    }

    /**
     * Spawns collectibles randomly on the map.
     * Distributes 3 of each type, ensuring same-type items are not too close together.
     */
    private void spawnCollectibles() {
        collectibles = new ArrayList<>();
        
        // Use mapData dimensions (from properties file), not tiledMap dimensions
        int dataWidth = mapData.length;
        int dataHeight = mapData[0].length;
        
        // Collect all safe walkable positions (only PATH tiles, excluding dangerous areas)
        ArrayList<int[]> walkablePositions = new ArrayList<>();
        for (int x = 0; x < dataWidth; x++) {
            for (int y = 0; y < dataHeight; y++) {
                int tileType = mapData[x][y];
                
                // Only spawn on PATH tiles (type 1) - explicitly exclude everything else
                if (tileType != TYPE_PATH) {
                    continue;
                }
                
                // Additional safety: check ALL 8 neighboring tiles (including diagonals)
                // This prevents spawning at edges near pits, walls, or dangerous areas
                boolean safeLocation = true;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue; // Skip the center tile
                        
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        // Check bounds - if at edge of map, not safe
                        if (nx < 0 || nx >= dataWidth || ny < 0 || ny >= dataHeight) {
                            safeLocation = false;
                            break;
                        }
                        
                        int neighborType = mapData[nx][ny];
                        // Neighbor must be PATH, ENTRY, or EXIT (safe ground types)
                        // Explicitly reject: 0 (wall), 3 (death trap), 4 (knife trap), etc.
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
        float minDistanceBetweenSameType = 10.0f; // Minimum 10 tiles apart
        java.util.Random random = new java.util.Random();
        
        // Spawn 3 of each collectible type (5 types = 15 total)
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
        
        System.out.println("Spawned " + collectibles.size() + " collectibles randomly across the map");
    }
    
    /**
     * Spawns a specific type of collectible multiple times, ensuring minimum distance.
     * 
     * @param type The type of collectible to spawn
     * @param count How many to spawn
     * @param points Point value
     * @param width Render width
     * @param height Render height
     * @param walkablePositions List of valid spawn positions
     * @param minDistance Minimum distance between same-type items
     * @param random Random number generator
     */
    private void spawnCollectibleType(Collectibles.CollectibleType type, int count, int points,
                                     float width, float height,
                                     ArrayList<int[]> walkablePositions, 
                                     float minDistance, java.util.Random random) {
        ArrayList<int[]> spawnedPositions = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 100;
        
        while (spawnedPositions.size() < count && attempts < maxAttempts) {
            attempts++;
            
            // Pick random walkable position
            int[] pos = walkablePositions.get(random.nextInt(walkablePositions.size()));
            int x = pos[0];
            int y = pos[1];
            
            // Check distance from previously spawned items of this type
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
                // Spawn the collectible
                collectibles.add(new Collectibles(
                    x * TILE_SIZE, y * TILE_SIZE,
                    width, height,
                    points,
                    type
                ));
                spawnedPositions.add(new int[]{x, y});
                System.out.println("Spawned " + type + " at (" + x + ", " + y + ")");
            }
        }
        
        if (spawnedPositions.size() < count) {
            System.err.println("Warning: Only spawned " + spawnedPositions.size() + " of " + count + " " + type + " collectibles");
        }
    }
    
    /**
     * Updates collectibles and checks for player collisions.
     * 
     * @param delta Time elapsed since last frame
     */
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
            
            // Rectangle overlap collision check
            float cx = collectible.getX(), cy = collectible.getY();
            float cw = collectible.getWidth(), ch = collectible.getHeight();
            
            if (px < cx + cw && px + pw > cx && py < cy + ch && py + ph > cy) {
                handleCollectiblePickup(collectible);
                iterator.remove();
            }
        }
    }
    
    /**
     * Handles the effects of collecting a collectible.
     * 
     * @param collectible The collectible that was picked up
     */
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

        // loop through the pitTraps to update themselves
        for (DeathPitTrap pitTrap : deathPitTraps) {
            pitTrap.update();
        }
        // loop through the knifeTraps to update themselves
        for(KnifesTrap knifeTrap  : knifesTraps ){
            knifeTrap.update(delta);
        }
        
        // Update collectibles and check collisions
        updateCollectibles(delta);
        
        // Center camera on player with elevated 3/4 view offset
        camera.position.set(player.getX() + TILE_SIZE / 2f, player.getY() + TILE_SIZE / 2f + 40f, 0);
        camera.update();

        // Render the Tiled map
        mapRenderer.setView(camera);
        mapRenderer.render();

        // Draw traps then the player on them
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
        // Draw the player
        player.render(game.getSpriteBatch());
        game.getSpriteBatch().end();
        
        // Render HUD (screen coordinates, not world coordinates)
        hudCamera.update();
        game.getSpriteBatch().setProjectionMatrix(hudCamera.combined);
        game.getSpriteBatch().begin();
        hud.render(game.getSpriteBatch(), player);
        game.getSpriteBatch().end();

        // Draw collision debug visualization
        if (showCollisionBoxes) {
            renderCollisionDebug();
        }
    }

    /**
     * Renders debug visualization showing collision boxes.
     * Press K to toggle this view.
     */
    private void renderCollisionDebug() {
        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Calculate visible tile range based on camera
        int startX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2) / TILE_SIZE) - 1);
        int startY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2) / TILE_SIZE) - 1);
        int endX = Math.min(mapData.length - 1, (int) ((camera.position.x + camera.viewportWidth / 2) / TILE_SIZE) + 1);
        int endY = Math.min(mapData[0].length - 1, (int) ((camera.position.y + camera.viewportHeight / 2) / TILE_SIZE) + 1);

        // Draw wall tiles (filled red, semi-transparent)
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

        // Draw tile grid outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                int type = mapData[x][y];
                if (type == 0) {
                    shapeRenderer.setColor(1, 0.3f, 0.3f, 0.8f); // Red for walls
                } else {
                    shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 0.4f); // Green for walkable
                }
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        shapeRenderer.end();

        // Get the collision box from the player
        float[] feetBox = player.getFeetCollisionBox();
        float feetX = feetBox[0];
        float feetY = feetBox[1];
        float feetW = feetBox[2];
        float feetH = feetBox[3];

        // Sprite bounds
        float spriteX = player.getX();
        float spriteY = player.getY();
        float spriteW = 96;
        float spriteH = 96;

        // Draw filled collision box
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 1, 0.4f);
        shapeRenderer.rect(feetX, feetY, feetW, feetH);

        // Sword Attack Box
        if (player.isAttacking()) {
            shapeRenderer.setColor(1, 0, 0, 0.5f);
            // attack dimensions
            Rectangle sword = player.getSwordHitBox();
            shapeRenderer.rect(sword.x, sword.y, sword.width, sword.height);
        }

        shapeRenderer.end();

        // Draw outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        // Yellow = Sprite area
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(spriteX, spriteY, spriteW, spriteH);

        // Cyan = Collision box
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(feetX, feetY, feetW, feetH);

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        int tileWidth = tiledMap.getProperties().get("tilewidth", Integer.class);
        int tileHeight = tiledMap.getProperties().get("tileheight", Integer.class);
        camera.setToOrtho(false, tileWidth * 30, tileHeight * 18);
        hudCamera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (tiledMap != null) tiledMap.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiAtlas != null) uiAtlas.dispose();
        if (hud != null) hud.dispose();
        AudioManager.dispose();
    }

}
