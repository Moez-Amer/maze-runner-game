package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents a collectible item in the maze game.
 * Different types of collectibles provide different benefits to the player.
 */
public class Collectibles extends GameObject {

    /**
     * Enum defining the types of collectibles available in the game.
     */
    public enum CollectibleType {
        HEALTH,
        SPEED_BOOSTER,
        POWER_BOOSTER,
        SHIELD,
        KEY
    }

    // Static textures shared across all collectible instances
    private static TextureRegion heartTexture;
    private static TextureRegion keyTexture;
    private static TextureRegion speedTexture;
    private static TextureRegion potionTexture;
    private static TextureRegion shieldTexture;
    
    // Instance variables
    private final int points;
    private final CollectibleType type;
    private boolean collected;
    private TextureRegion texture;
    
    // Animation variables
    private float floatOffset;
    private float floatTimer;
    private static final float FLOAT_SPEED = 3f;
    private static final float FLOAT_AMOUNT = 2f;

    /**
     * Loads shared textures for all collectibles.
     * Call this once during game initialization.
     * 
     * @param uiAtlas The UI texture atlas containing the heart texture
     */
    public static void loadTextures(TextureAtlas uiAtlas) {
        // Load heart from UI atlas
        heartTexture = uiAtlas.findRegion("heart");
        
        // Load key from standalone PNG file and convert to TextureRegion
        Texture keyTextureFile = new Texture(Gdx.files.internal("Key.png"));
        keyTexture = new TextureRegion(keyTextureFile);
        
        // Load collectibles from sprite sheet
        // Image: 176x152 pixels, tiles: 16x16
        Texture collectiblesSheet = new Texture(Gdx.files.internal("collectibles.png"));
        speedTexture = new TextureRegion(collectiblesSheet, 144, 48, 16, 16);  // Row 4, Col 10
        potionTexture = new TextureRegion(collectiblesSheet, 128, 48, 16, 16); // Row 4, Col 9
        shieldTexture = new TextureRegion(collectiblesSheet, 128, 16, 16, 16); // Row 2, Col 9
    }

    /**
     * Constructs a new Collectible at the given position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Width of the collectible
     * @param height Height of the collectible
     * @param points Point value when collected
     * @param type Type of collectible
     */
    public Collectibles(float x, float y, float width, float height, int points, CollectibleType type) {
        super(x, y, width, height);
        this.points = points;
        this.type = type;
        this.collected = false;
        this.floatTimer = 0f;
        
        // Assign appropriate texture based on type
        switch (type) {
            case HEALTH:
                this.texture = heartTexture;
                break;
            case KEY:
                this.texture = keyTexture;
                break;
            case SPEED_BOOSTER:
                this.texture = speedTexture;
                break;
            case POWER_BOOSTER:
                this.texture = potionTexture;
                break;
            case SHIELD:
                this.texture = shieldTexture;
                break;
            default:
                throw new IllegalArgumentException("Unknown collectible type: " + type);
        }
    }

    /**
     * Updates the collectible floating animation.
     * 
     * @param delta Time since last frame in seconds
     */
    public void update(float delta) {
        if (!collected) {
            floatTimer += delta * FLOAT_SPEED;
            floatOffset = (float) Math.sin(floatTimer) * FLOAT_AMOUNT;
        }
    }

    /**
     * Renders the collectible if it hasn't been collected yet.
     * 
     * @param batch SpriteBatch to draw with
     */
    @Override
    public void render(SpriteBatch batch) {
        if (!collected && texture != null) {
            batch.draw(texture, x, y + floatOffset, width, height);
        }
    }

    /**
     * Marks this collectible as collected.
     */
    public void collect() {
        this.collected = true;
    }

    /**
     * Checks if this collectible has been collected.
     * 
     * @return true if collected, false otherwise
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Gets the type of this collectible.
     * 
     * @return The collectible type
     */
    public CollectibleType getType() {
        return type;
    }

    /**
     * Gets the point value of this collectible.
     * 
     * @return Point value
     */
    public int getPoints() {
        return points;
    }

    /**
     * Disposes of static textures when they are no longer needed.
     * Call this when shutting down the game or switching screens.
     */
    public static void dispose() {
        // Dispose of the key texture since we loaded it separately
        if (keyTexture != null && keyTexture.getTexture() != null) {
            keyTexture.getTexture().dispose();
        }
        // Note: collectiblesSheet texture should also be stored and disposed
        // This is a limitation of the current implementation
    }
}