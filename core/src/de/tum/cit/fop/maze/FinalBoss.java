package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Represents the final boss enemy in the game.
 * <p>
 * This class handles specific behaviors for the Final Boss, including:
 * <ul>
 * <li>A 10-frame idle animation loaded from a grid.</li>
 * <li>A passive "Aura of Despair" that deals damage to the player upon contact.</li>
 * <li>Distance-based movement logic where the boss stops approaching within a certain range.</li>
 * </ul>
 */
public class FinalBoss extends Enemy {

    private float health = 20.0f;
    private boolean isDead = false;
    private Animation<TextureRegion> deathAnim;
    private float deathTime = 0f;
    private final float auraRadius = 70f;
    private final float stopDistance = 40f;
    private final ShapeRenderer shapeRenderer;
    private final Circle auraBounds = new Circle();
    private final Rectangle feetBounds = new Rectangle();
    /**
     * Constructs a new FinalBoss instance.
     *
     * @param x          The starting X-coordinate in the world.
     * @param y          The starting Y-coordinate in the world.
     * @param tileSize   The size of the world tiles.
     * @param mapData    The collision map data.
     */
    public FinalBoss(float x, float y, int tileSize, int[][] mapData) {
        super(x, y, tileSize, mapData, "Wizard/", 80, 80);
        this.speed = 100f;
        this.stateTime = 0f;
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * Loads the boss animations from the asset files.
     * <p>
     * Specifically handles the 'idle' animation by safely extracting 10 frames from
     * a sprite sheet grid and the 'death' animation from a standard row.
     *
     * @param path        The directory path to the assets.
     * @param frameWidth  The width of a single frame.
     * @param frameHeight The height of a single frame.
     */
    @Override
    public void loadAnimation(String path, int frameWidth, int frameHeight) {
        Texture idleSheet = new Texture(Gdx.files.internal(path + "wizard idle.png"));
        TextureRegion[][] tmp = TextureRegion.split(idleSheet, frameWidth, frameHeight);

        TextureRegion[] allIdleFrames = new TextureRegion[10];
        int index = 0;

        for (int r = 0; r < tmp.length && index < 10; r++) {
            for (int c = 0; c < tmp[r].length && index < 10; c++) {
                allIdleFrames[index++] = tmp[r][c];
            }
        }

        if (index < 10) {
            TextureRegion[] actualFrames = new TextureRegion[index];
            System.arraycopy(allIdleFrames, 0, actualFrames, 0, index);
            allIdleFrames = actualFrames;
        }

        this.floatingAnim = new Animation<>(0.15f, allIdleFrames);
        this.floatingLeftAnim = new Animation<>(0.15f, flipFrames(allIdleFrames));

        Texture deathSheet = new Texture(Gdx.files.internal(path + "wizard death.png"));
        TextureRegion[][] dTmp = TextureRegion.split(deathSheet, frameWidth, frameHeight);
        this.deathAnim = new Animation<>(0.1f, dTmp[0]);

        this.floatingAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.floatingLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.deathAnim.setPlayMode(Animation.PlayMode.NORMAL);
        this.currentAnimation = floatingAnim;
    }

    /**
     * Updates the boss's logic for the current frame.
     * <p>
     * Handles death timers, movement towards the player (stopping at a set distance),
     * applying aura damage, and updating the animation state.
     *
     * @param delta The time elapsed since the last frame.
     */
    @Override
    public void update(float delta) {
        if (isDead) {
            deathTime += delta;
            return;
        }

        if (player != null) {
            float dist = Vector2.dst(this.x, this.y, player.getX(), player.getY());
            if (dist > stopDistance) {
                super.update(delta);
            }

            applyAuraDamage(player);
        } else {
            super.update(delta);
        }

        if (facing == Direction.LEFT) currentAnimation = floatingLeftAnim;
        else currentAnimation = floatingAnim;
    }

    /**
     * Checks for collision between the boss's aura and the player's feet.
     * <p>
     * If the player's feet collision box overlaps with the aura circle,
     * damage is applied to the player.
     *
     * @param player The player instance to check against.
     */
    public void applyAuraDamage(Player player) {
        if (isDead || player == null || player.isGhostMode()) return;

        auraBounds.set(this.x + 35, this.y + 35, auraRadius);

        float[] feet = player.getFeetCollisionBox();
        feetBounds.set(feet[0], feet[1], feet[2], feet[3]);

        if (Intersector.overlaps(auraBounds, feetBounds)) {
            player.takeDamage();
        }
    }

    /**
     * Renders the boss and the visual effect of the aura.
     *
     * @param batch The SpriteBatch used for drawing textures.
     */
    @Override
    public void render(SpriteBatch batch) {
        drawAuraEffect(batch);
        TextureRegion frame = isDead ? deathAnim.getKeyFrame(deathTime) : currentAnimation.getKeyFrame(stateTime);
        batch.setColor(0.8f, 0.4f, 1.0f, 1.0f);
        batch.draw(frame, x, y, 70, 70);
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws the semi-transparent purple aura effect using a ShapeRenderer.
     * <p>
     * This method temporarily ends the SpriteBatch to perform OpenGL shape rendering
     * with blending enabled.
     *
     * @param batch The SpriteBatch context (used to retrieve the projection matrix).
     */
    private void drawAuraEffect(SpriteBatch batch) {
        if (isDead) return;
        batch.end();
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.6f, 0.2f, 0.9f, 0.2f);
        shapeRenderer.circle(x + 35, y + 35, auraRadius);
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
        batch.begin();
    }

    /**
     * Applies damage to the boss and handles death logic.
     *
     * @param damage The amount of damage to be subtracted from health.
     */
    @Override
    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0 && !isDead) {
            isDead = true;
            deathTime = 0;
        }
    }

    /**
     * Checks if the boss is dead and the death animation has finished playing.
     *
     * @return True if the boss is dead and the animation is complete, false otherwise.
     */
    @Override
    public boolean isDead() { return isDead && deathAnim.isAnimationFinished(deathTime); }

    /**
     * Creates a new array of TextureRegions that are horizontally flipped.
     *
     * @param original The original array of frames.
     * @return A new array containing the flipped frames.
     */
    private TextureRegion[] flipFrames(TextureRegion[] original) {
        TextureRegion[] flipped = new TextureRegion[original.length];
        for (int i = 0; i < original.length; i++) {
            flipped[i] = new TextureRegion(original[i]);
            flipped[i].flip(true, false);
        }
        return flipped;
    }

    /**
     * Releases resources used by the ShapeRenderer.
     */
    public void dispose() { shapeRenderer.dispose(); }
}