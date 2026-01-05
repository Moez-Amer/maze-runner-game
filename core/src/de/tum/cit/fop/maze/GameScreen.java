package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
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
        // Create player at entry point (around 3,3 based on map)
        player = new Player(entry.getX(), entry.getY(), TILE_SIZE, mapData);
        findDeathPits_KnifesTraps();
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
        // Draw the player
        player.render(game.getSpriteBatch());
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
    }

}
