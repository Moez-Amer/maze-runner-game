package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents the ultimate challenge in Level 5.
 * The FinalBoss extends the base Enemy class but utilizes unique triple-asset
 * animation loading (Idle, Attack, and Death) and enhanced combat stats.
 * * Logic:
 * - Uses a consistent 80x80 grid for all frames.
 * - Features higher health and a distinctive visual scale.
 * - Overrides asset loading to handle separate PNG files instead of a single sheet.
 */
public class FinalBoss extends Enemy {

    private float health = 20.0f;
    private boolean isDead = false;
    private Animation<TextureRegion> deathAnim;
    private float deathTime = 0f;

    /**
     * Constructs the FinalBoss with specific wizard assets.
     * * @param x          Starting X position in pixels.
     * @param y          Starting Y position in pixels.
     * @param tileSize   World tile size (typically 16).
     * @param mapData    The collision grid.
     */
    public FinalBoss(float x, float y, int tileSize, int[][] mapData) {
        // Initializes with the Wizard assets folder and 80x80 frame dimensions
        super(x, y, tileSize, mapData, "Wizard/", 80, 80);
        this.speed = 100f; // Faster than standard enemies
        this.stateTime = 0f;
    }

    /**
     * Specialized asset loader for the Wizard boss.
     * Unlike the base Enemy which loads from one file, this loads from:
     * 1. wizard idle.png (10 frames)
     * 2. wizard fly attack.png (6 frames)
     * 3. wizard death.png (12 frames)
     * * @param path        The directory containing the wizard PNGs.
     * @param frameWidth  80 pixels based on sprite sheet analysis.
     * @param frameHeight 80 pixels based on sprite sheet analysis.
     */
    @Override
    public void loadAnimation(String path, int frameWidth, int frameHeight) {
        // 1. Load Idle Animation (10 frames)
        Texture idleSheet = new Texture(Gdx.files.internal(path + "wizard idle.png"));
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, frameWidth, frameHeight);
        this.floatingAnim = new Animation<>(0.15f, idleFrames[0]);
        this.floatingLeftAnim = new Animation<>(0.15f, flipFrames(idleFrames[0]));

        // 2. Load Attack Animation (6 frames)
        Texture attackSheet = new Texture(Gdx.files.internal(path + "wizard fly attack.png"));
        TextureRegion[][] attackFrames = TextureRegion.split(attackSheet, frameWidth, frameHeight);
        this.attackAnim = new Animation<>(0.1f, attackFrames[0]);
        this.attackLeftAnim = new Animation<>(0.1f, flipFrames(attackFrames[0]));

        // 3. Load Death Animation (12 frames)
        Texture deathSheet = new Texture(Gdx.files.internal(path + "wizard death.png"));
        TextureRegion[][] deathFrames = TextureRegion.split(deathSheet, frameWidth, frameHeight);
        this.deathAnim = new Animation<>(0.1f, deathFrames[0]);

        // Set PlayModes
        this.floatingAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.floatingLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.attackAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.attackLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        this.deathAnim.setPlayMode(Animation.PlayMode.NORMAL);

        this.currentAnimation = floatingAnim;
    }

    /**
     * Updates the boss logic. If health reaches zero, transitions to the
     * death animation sequence.
     * * @param delta Time elapsed since last frame.
     */
    @Override
    public void update(float delta) {
        if (isDead) {
            deathTime += delta;
            return;
        }
        super.update(delta);
    }

    /**
     * Renders the boss with a larger scale and a purple "Boss" tint.
     * * @param batch The SpriteBatch used for drawing.
     */
    @Override
    public void render(SpriteBatch batch) {
        TextureRegion frame;
        if (isDead) {
            frame = deathAnim.getKeyFrame(deathTime);
        } else {
            frame = currentAnimation.getKeyFrame(stateTime);
        }

        // Tint the boss to distinguish it from standard enemies
        batch.setColor(0.8f, 0.4f, 1.0f, 1.0f);

        // Draw the boss 50% larger than its source frame for presence
        batch.draw(frame, x - 20, y, 100, 100);

        batch.setColor(Color.WHITE);
    }

    /**
     * Reduces boss health. Overrides to set isDead flag when health is depleted.
     * * @param damage Amount of damage taken.
     */
    @Override
    public void takeDamage(float damage) {
        health -= damage;
        if (health <= 0 && !isDead) {
            isDead = true;
            deathTime = 0;
        }
    }

    @Override
    public boolean isDead() {
        return isDead && deathAnim.isAnimationFinished(deathTime);
    }

    /**
     * Helper to flip frames for left-facing movement.
     */
    private TextureRegion[] flipFrames(TextureRegion[] original) {
        TextureRegion[] flipped = new TextureRegion[original.length];
        for (int i = 0; i < original.length; i++) {
            flipped[i] = new TextureRegion(original[i]);
            flipped[i].flip(true, false);
        }
        return flipped;
    }
}