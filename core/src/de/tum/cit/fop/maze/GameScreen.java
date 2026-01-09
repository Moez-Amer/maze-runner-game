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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Properties;
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
    private static final int TILE_SIZE = 16;

    //enemies
    private Array<Enemy> enemies;

    // Player
    private Player player;

    // Inside GameScreen.java constructor


    // Debug visualization
    private ShapeRenderer shapeRenderer;
    private boolean showCollisionBoxes = false;

    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;
        this.enemies=new Array<>();

        // Load the Tiled map for rendering
        tiledMap = new TmxMapLoader().load("MoriaMap/newmap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        
        // Hide the Player layer from Tiled map (we render our own player)
        if (tiledMap.getLayers().get("Player") != null) {
            tiledMap.getLayers().get("Player").setVisible(false);
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

        // Create player at entry point (around 3,3 based on map)
        player = new Player(3 * TILE_SIZE, 3 * TILE_SIZE, TILE_SIZE, mapData);

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
        if (tiledMap.getLayers().get("Enemy")!=null){
            tiledMap.getLayers().get("Enemy").setVisible(true);
        }
        player.setEnemies(this.enemies);

        for (Enemy enemy : enemies) {
            enemy.setEnemies(this.enemies);
            enemy.setPlayer(player);
        }


    }

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
        for (Enemy enemy : enemies){
            enemy.update(delta);
        }

        // Center camera on player with elevated 3/4 view offset
        camera.position.set(player.getX() + TILE_SIZE / 2f, player.getY() + TILE_SIZE / 2f + 40f, 0);
        camera.update();

        // Render the Tiled map
        mapRenderer.setView(camera);
        mapRenderer.render();

        // Draw player on top
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();
        player.render(game.getSpriteBatch());
        if (tiledMap.getLayers().get("Enemy") != null) {
            tiledMap.getLayers().get("Enemy").setVisible(false);
        }
        for (Enemy enemy : enemies){
            enemy.render(game.getSpriteBatch());
        }

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

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for(Enemy enemy : enemies){
            float[] feetBox = enemy.getFeetCollisionBox();
            float feetX = feetBox[0];
            float feetY = feetBox[1];
            float feetW = feetBox[2];
            float feetH = feetBox[3];
            shapeRenderer.setColor(1,0,0,0.4f);
            shapeRenderer.rect(feetX, feetY, feetW, feetH);
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
