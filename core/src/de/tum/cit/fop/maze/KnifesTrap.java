package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import static de.tum.cit.fop.maze.Screens.GameScreen.TILE_SIZE;

/**
 * Represents a dynamic floor trap with retractable spikes.
 * The trap cycles between a safe hidden state and a dangerous active state.
 */
public class KnifesTrap extends Trap {
    private static final float DURATION_ACTIVE = 1.0f;
    private static final float DURATION_COOLDOWN = 1.0f;
    private static final float FRAME_DURATION = 0.1f;

    private float stateTime;
    private boolean isActive;
    private final float delay;
    private TextureRegion textureDown;
    private Animation<TextureRegion> animationUp;

    /**
     * Constructs a new KnifesTrap.
     *
     * @param x      World X-coordinate.
     * @param y      World Y-coordinate.
     * @param width  Collision width.
     * @param height Collision height.
     * @param player Reference to the player.
     * @param delay  Initial delay to offset the trap's activation cycle.
     */
    public KnifesTrap(float x, float y, float width, float height, Player player, float delay) {
        super(x, y, width, height, player);
        this.delay = delay;
        this.stateTime = -delay;
        this.isActive = false;
        loadAssets();
    }

    /**
     * Updates the trap's state machine (Active vs. Cooldown) and checks for collisions
     * while the trap is active.
     *
     * @param delta Time elapsed since last frame.
     */
    @Override
    public void update(float delta) {
        stateTime += delta;

        if (isActive) {
            if (stateTime >= DURATION_ACTIVE) {
                isActive = false;
                stateTime = 0;
            }
        } else {
            if (stateTime >= DURATION_COOLDOWN) {
                isActive = true;
                stateTime = 0;
            }
        }

        if (isActive && isPlayerOverlapping()) {
            triggerKnifeTrap();
        }
    }

    /**
     * Renders either the static hidden texture or the active spike animation.
     *
     * @param batch SpriteBatch used for drawing.
     */
    @Override
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = isActive ?
                animationUp.getKeyFrame(stateTime, true) : textureDown;
        batch.draw(currentFrame, x, y, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Slices and loads animation frames for the spikes.
     */
    private void loadAssets() {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i <= 4; i++) {
            frames.add(new TextureRegion(new Texture(
                    Gdx.files.internal("KnifeAnimation/Knife_" + i + ".png"))));
        }
        this.animationUp = new Animation<>(FRAME_DURATION, frames,
                Animation.PlayMode.LOOP_PINGPONG);
        this.textureDown = frames.get(0);
    }

    /**
     * Applies damage to the player if they step on active spikes.
     */
    private void triggerKnifeTrap() {
        System.out.println(" Step into knifes. ");
        player.takeDamage();
    }
}