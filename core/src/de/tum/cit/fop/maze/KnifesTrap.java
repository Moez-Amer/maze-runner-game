package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import static de.tum.cit.fop.maze.GameScreen.TILE_SIZE;

/**
 * Dynamic floor trap hazard: retractable spikes with synchronized timing.
 *
 * Design goals:
 * - Clear distinction between SAFE (hidden) and ACTIVE (danger) states
 * - "Wave" or "Ripple" activation patterns supported via startup delays
 * - Visual depth (spikes render taller than the tile footprint)
 * - Ping-pong animation for smooth extend/retract motion
 *
 * Logic:
 * - State machine toggles based on fixed duration timers
 * - Damage is triggered only on frame-perfect overlap with player center
 * - Assets are manually sliced from a shared sprite sheet
 */
public class KnifesTrap extends GameObject {

    private static final float DURATION_ACTIVE = 1.0f;
    private static final float DURATION_COOLDOWN = 1.0f;
    private static final float FRAME_DURATION = 0.1f;
    private float stateTime;
    private boolean isActive;
    private Player player;
    private float delay;
    private TextureRegion textureDown;
    private Animation<TextureRegion> animationUp;

    /**
     * Constructs a new KnifesTrap at the specified position.
     *
     * Initializes the trap's timer and state. The stateTime is set to a negative value
     * based on the provided delay. This effectively pauses the trap's cycle at the start,
     * allowing it to wait before its first activation.
     *
     * @param x      The X-coordinate of the trap's bottom-left corner.
     * @param y      The Y-coordinate of the trap's bottom-left corner.
     * @param width  The width of the trap's collision area.
     * @param height The height of the trap's collision area.
     * @param player The player instance used for collision detection.
     * @param delay  The time in seconds to wait before the trap starts its main cycle.
     */
    public KnifesTrap(float x, float y, float width, float height,Player player,float delay) {
        super(x, y, width, height);
        this.player = player;
        this.delay = delay;
        stateTime = -delay;
        isActive = false;

        loadAssets();

    }

    /**
     * Updates trap state machine.
     *
     * Logic:
     * - Increments timer
     * - Toggles active/safe state based on DURATION constants
     * - Checks center-point collision with player feet
     *
     * @param delta Time since last frame
     */
    public void update(float delta) {
        stateTime += delta;

        if (isActive) {
            // If dangerous, wait until time is up -> Go Safe
            if (stateTime >= DURATION_ACTIVE) {
                isActive = false;
                stateTime = 0;
            }
        } else {
            // If safe, wait until cooldown is up -> Go Dangerous
            if (stateTime >= DURATION_COOLDOWN) {
                isActive = true;
                stateTime = 0;
            }
        }
        float[] feetData = player.getFeetCollisionBox();
        float feetX = feetData[0];
        float feetY = feetData[1];
        float feetWidth = feetData[2];
        float feetHeight = feetData[3];

        //  Calculate the exact center of the feet
        float feetCenterX = feetX + (feetWidth / 2);
        float feetCenterY = feetY + (feetHeight / 2);

        //  check if the CENTER of the player's feet is inside this specific trap tile.
        if (this.bounds.contains(feetCenterX, feetCenterY)&& isActive) {
            triggerKnifeTrap();
        }
    }

    /**
     * Renders current trap state.
     *
     * - Active: Plays ping-pong animation (Extend -> Retract)
     * - Safe: Renders static hidden frame
     *
     * @param batch SpriteBatch for drawing
     */
    @Override
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame;

        if (isActive) {
            currentFrame = animationUp.getKeyFrame(stateTime, true);
        } else {
            currentFrame = textureDown;
        }
        batch.draw(currentFrame, x, y, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Internal asset loading.
     *
     * - Loads individual knife animation frames from KnifeAnimation folder
     * - Knife_0.png = fully retracted (safe state)
     * - Knife_4.png = fully extended (most dangerous)
     */
    private void loadAssets() {
        Array<TextureRegion> frames = new Array<>();

        // Load each frame from separate PNG files
        frames.add(new TextureRegion(new Texture(Gdx.files.internal("KnifeAnimation/Knife_0.png"))));
        frames.add(new TextureRegion(new Texture(Gdx.files.internal("KnifeAnimation/Knife_1.png"))));
        frames.add(new TextureRegion(new Texture(Gdx.files.internal("KnifeAnimation/Knife_2.png"))));
        frames.add(new TextureRegion(new Texture(Gdx.files.internal("KnifeAnimation/Knife_3.png"))));
        frames.add(new TextureRegion(new Texture(Gdx.files.internal("KnifeAnimation/Knife_4.png"))));

        // Create Animation with ping-pong for smooth extend/retract
        this.animationUp = new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.LOOP_PINGPONG);

        // First frame (Knife_0) is the safe/retracted state
        this.textureDown = frames.get(0);
    }


    /**
     * Handles collision consequence.
     * - Logs debug message to console
     * - Applies damage to player instance
     */
    private void triggerKnifeTrap() {
        System.out.println(" Step into knifes. ");
        //  Lose a life
        player.takeDamage();
    }
}
