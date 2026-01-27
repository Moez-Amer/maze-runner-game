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
        KEY,
        SCROLL
    }

    // Static textures shared across all collectible instances
    private static TextureRegion heartTexture;
    private static TextureRegion keyTexture;
    private static TextureRegion speedTexture;
    private static TextureRegion potionTexture;
    private static TextureRegion shieldTexture;
    private static TextureRegion scrollTexture;
    
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
     * @param uiAtlas The UI texture atlas (not used anymore, kept for compatibility)
     */
    public static void loadTextures(TextureAtlas uiAtlas) {
        // Load heart from undead loot icons - Icon 12
        Texture heartTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon12.png"));
        heartTexture = new TextureRegion(heartTextureFile);

        // Load key from undead loot icons - Icon 41
        Texture keyTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon41.png"));
        keyTexture = new TextureRegion(keyTextureFile);

        // Load potions from magic potions pack
        // Speed booster (blue) - Icon 10
        Texture speedTextureFile = new Texture(Gdx.files.internal("48 Free Magic Potions Pixel Art Icons/PNG/Transperent/Icon10.png"));
        speedTexture = new TextureRegion(speedTextureFile);

        // Power booster (purple) - Icon 12
        Texture potionTextureFile = new Texture(Gdx.files.internal("48 Free Magic Potions Pixel Art Icons/PNG/Transperent/Icon12.png"));
        potionTexture = new TextureRegion(potionTextureFile);

        // Shield (green potion) - Icon 19
        Texture shieldTextureFile = new Texture(Gdx.files.internal("48 Free Magic Potions Pixel Art Icons/PNG/Transperent/Icon19.png"));
        shieldTexture = new TextureRegion(shieldTextureFile);

        // Scroll - Icon 43
        Texture scrollTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon43.png"));
        scrollTexture = new TextureRegion(scrollTextureFile);
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
            case SCROLL:
                this.texture = scrollTexture;
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
        // Dispose of all the texture files
        if (heartTexture != null && heartTexture.getTexture() != null) {
            heartTexture.getTexture().dispose();
        }
        if (keyTexture != null && keyTexture.getTexture() != null) {
            keyTexture.getTexture().dispose();
        }
        if (speedTexture != null && speedTexture.getTexture() != null) {
            speedTexture.getTexture().dispose();
        }
        if (potionTexture != null && potionTexture.getTexture() != null) {
            potionTexture.getTexture().dispose();
        }
        if (shieldTexture != null && shieldTexture.getTexture() != null) {
            shieldTexture.getTexture().dispose();
        }
        if (scrollTexture != null && scrollTexture.getTexture() != null) {
            scrollTexture.getTexture().dispose();
        }
    }
}