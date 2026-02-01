package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents the player's voodoo doll that appears when they die.
 * The player must reach this doll in ghost form to revive.
 */
public class VoodooDoll extends GameObject {
    private static TextureRegion voodooDollTexture;
    private boolean collected;
    private float floatOffset;
    private float floatTimer;
    private float pulseTimer;
    private static final float FLOAT_SPEED = 3f;
    private static final float FLOAT_AMOUNT = 3f;
    private static final float PULSE_SPEED = 4f;

    /**
     * Loads the voodoo doll texture from the assets.
     * This method should be called once during game initialization to prepare static resources.
     */
    public static void loadTexture() {
        Texture voodooDollTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon38.png"));
        voodooDollTexture = new TextureRegion(voodooDollTextureFile);
    }

    /**
     * Constructs a new VoodooDoll at the given position.
     * Initializes the doll as uncollected and resets animation timers.
     *
     * @param x      The X coordinate of the doll's position.
     * @param y      The Y coordinate of the doll's position.
     * @param width  The width of the doll.
     * @param height The height of the doll.
     */
    public VoodooDoll(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.collected = false;
        this.floatTimer = 0f;
        this.pulseTimer = 0f;
    }

    /**
     * Updates the voodoo doll floating and pulsing animation state.
     * Calculations are based on the time elapsed since the last frame.
     *
     * @param delta The time in seconds since the last render frame.
     */
    public void update(float delta) {
        if (!collected) {
            floatTimer += delta * FLOAT_SPEED;
            floatOffset = (float) Math.sin(floatTimer) * FLOAT_AMOUNT;
            pulseTimer += delta * PULSE_SPEED;
        }
    }

    /**
     * Renders the voodoo doll with a pulsing glow effect.
     * The doll is only rendered if it has not yet been collected.
     *
     * @param batch The SpriteBatch used to draw the texture and effects.
     */
    @Override
    public void render(SpriteBatch batch) {
        if (!collected && voodooDollTexture != null) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(pulseTimer);

            batch.setColor(0.5f, 0.8f, 1f, pulse * 0.3f);
            batch.draw(voodooDollTexture, x - 8, y + floatOffset - 8, width + 16, height + 16);

            batch.setColor(0.6f, 0.9f, 1f, pulse * 0.5f);
            batch.draw(voodooDollTexture, x - 4, y + floatOffset - 4, width + 8, height + 8);

            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(voodooDollTexture, x, y + floatOffset, width, height);
        }
    }

    /**
     * Marks this voodoo doll as collected.
     * Once collected, the doll will stop updating and rendering.
     */
    public void collect() {
        this.collected = true;
    }

    /**
     * Checks if this voodoo doll has been collected by the player.
     *
     * @return true if the doll has been collected, false otherwise.
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Disposes of the voodoo doll texture when it is no longer needed.
     * This prevents memory leaks by freeing the texture resource.
     */
    public static void dispose() {
        if (voodooDollTexture != null && voodooDollTexture.getTexture() != null) {
            voodooDollTexture.getTexture().dispose();
        }
    }
}