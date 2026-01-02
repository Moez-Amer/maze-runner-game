package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Represents the player character in the maze game.
 * Handles player-specific logic: lives, keys, input, damage effects.
 */
public class Player extends MovableGameObject {
    
    private static final float WALK_SPEED = 80f;
    private static final float RUN_SPEED = 150f;
    private boolean isRunning;
    
    private int lives;
    private int maxLives;
    private boolean hasKey;
    
    private boolean isDamaged;
    private float damageTimer;
    private static final float DAMAGE_FLASH_DURATION = 1.0f;
    private static final float INVULNERABILITY_TIME = 1.5f;
    private float invulnerabilityTimer;
    
    private Animation<TextureRegion> idleDownAnim, idleUpAnim, idleLeftAnim, idleRightAnim;
    private Animation<TextureRegion> runDownAnim, runUpAnim, runLeftAnim, runRightAnim;
    
    /**
     * Constructs a new Player at the given position.
     * 
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param tileSize Size of each tile in pixels
     * @param mapData Reference to the map data for collision detection
     */
    public Player(float x, float y, int tileSize, int[][] mapData) {
        super(x, y, tileSize, tileSize);
        
        this.speed = WALK_SPEED;
        this.lives = 3;
        this.maxLives = 3;
        this.hasKey = false;
        this.isRunning = false;
        this.isDamaged = false;
        this.invulnerabilityTimer = 0f;
        
        setMapData(mapData, tileSize);
        setCollisionBox(16, 16, 40, 20);
        loadAnimations();
    }
    
    /**
     * Loads all player animations from sprite files.
     */
    private void loadAnimations() {
        int FRAME_WIDTH = 96;
        int FRAME_HEIGHT = 80;
        
        Texture idleDown = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/IDLE/idle_down.png"));
        Texture idleUp = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/IDLE/idle_up.png"));
        Texture idleLeft = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/IDLE/idle_left.png"));
        Texture idleRight = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/IDLE/idle_right.png"));
        
        Texture runDown = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/RUN/run_down.png"));
        Texture runUp = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/RUN/run_up.png"));
        Texture runLeft = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/RUN/run_left.png"));
        Texture runRight = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/RUN/run_right.png"));
        
        TextureRegion[][] idleDownFrames = TextureRegion.split(idleDown, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleUpFrames = TextureRegion.split(idleUp, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleLeftFrames = TextureRegion.split(idleLeft, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleRightFrames = TextureRegion.split(idleRight, FRAME_WIDTH, FRAME_HEIGHT);
        
        TextureRegion[][] runDownFrames = TextureRegion.split(runDown, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runUpFrames = TextureRegion.split(runUp, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runLeftFrames = TextureRegion.split(runLeft, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runRightFrames = TextureRegion.split(runRight, FRAME_WIDTH, FRAME_HEIGHT);
        
        idleDownAnim = new Animation<>(0.2f, idleDownFrames[0]);
        idleUpAnim = new Animation<>(0.2f, idleUpFrames[0]);
        idleLeftAnim = new Animation<>(0.2f, idleLeftFrames[0]);
        idleRightAnim = new Animation<>(0.2f, idleRightFrames[0]);
        
        runDownAnim = new Animation<>(0.1f, runDownFrames[0]);
        runUpAnim = new Animation<>(0.1f, runUpFrames[0]);
        runLeftAnim = new Animation<>(0.1f, runLeftFrames[0]);
        runRightAnim = new Animation<>(0.1f, runRightFrames[0]);
        
        idleDownAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleUpAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        runDownAnim.setPlayMode(Animation.PlayMode.LOOP);
        runUpAnim.setPlayMode(Animation.PlayMode.LOOP);
        runLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        runRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        
        facing = Direction.DOWN;
        currentAnimation = idleDownAnim;
    }
    
    /**
     * Updates player state, handles input and movement.
     * 
     * @param delta Time elapsed since last frame
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        
        if (isDamaged) {
            damageTimer += delta;
            if (damageTimer >= DAMAGE_FLASH_DURATION) {
                isDamaged = false;
                damageTimer = 0f;
            }
        }
        
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= delta;
        }
        
        handleInput(delta);
    }
    
    /**
     * Handles keyboard input for player movement.
     * 
     * @param delta Time elapsed since last frame
     */
    private void handleInput(float delta) {
        isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || 
                    Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        speed = isRunning ? RUN_SPEED : WALK_SPEED;
        
        boolean moving = false;
        Direction moveDirection = null;
        
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveDirection = Direction.UP;
            facing = Direction.UP;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveDirection = Direction.DOWN;
            facing = Direction.DOWN;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveDirection = Direction.LEFT;
            facing = Direction.LEFT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveDirection = Direction.RIGHT;
            facing = Direction.RIGHT;
            moving = true;
        }
        
        if (moving && moveDirection != null) {
            move(delta, moveDirection);
        }
        
        updateAnimation(moving);
    }
    
    /**
     * Updates the current animation based on movement state and direction.
     * 
     * @param moving Whether the player is currently moving
     */
    private void updateAnimation(boolean moving) {
        if (moving) {
            switch (facing) {
                case UP: currentAnimation = runUpAnim; break;
                case DOWN: currentAnimation = runDownAnim; break;
                case LEFT: currentAnimation = runLeftAnim; break;
                case RIGHT: currentAnimation = runRightAnim; break;
            }
        } else {
            switch (facing) {
                case UP: currentAnimation = idleUpAnim; break;
                case DOWN: currentAnimation = idleDownAnim; break;
                case LEFT: currentAnimation = idleLeftAnim; break;
                case RIGHT: currentAnimation = idleRightAnim; break;
            }
        }
    }
    
    /**
     * Renders the player with damage effect if applicable.
     * 
     * @param batch SpriteBatch to draw with
     */
    @Override
    public void render(SpriteBatch batch) {
        if (isDamaged && ((int)(damageTimer * 10) % 2 == 0)) {
            batch.setColor(1, 0, 0, 1);
        } else {
            batch.setColor(Color.WHITE);
        }
        
        if (currentAnimation != null) {
            TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(currentFrame, x, y, 96, 96);
        }
        
        batch.setColor(Color.WHITE);
    }
    
    /**
     * Gets the collision box position and size for feet-based collision.
     * The collision box is at the bottom-center of the sprite where the feet are.
     * @return float array: [feetX, feetY, feetWidth, feetHeight]
     */
    public float[] getFeetCollisionBox() {
        float feetWidth = 16;
        float feetHeight = 16;
        float feetX = x + (96 - feetWidth) / 2f;
        float feetY = y + 20;
        return new float[]{feetX, feetY, feetWidth, feetHeight};
    }
    
    /**
     * Makes the player take damage and lose a life.
     */
    public void takeDamage() {
        if (invulnerabilityTimer <= 0) {
            lives--;
            isDamaged = true;
            damageTimer = 0f;
            invulnerabilityTimer = INVULNERABILITY_TIME;
        }
    }
    
    /**
     * Adds a life to the player (up to max).
     */
    public void addLife() {
        if (lives < maxLives) {
            lives++;
        }
    }
    
    /**
     * Collects a key.
     */
    public void collectKey() {
        hasKey = true;
    }
    
    /**
     * Checks if player is dead.
     * 
     * @return true if lives <= 0
     */
    public boolean isDead() {
        return lives <= 0;
    }
    
    /**
     * Checks if player is currently invulnerable.
     * 
     * @return true if invulnerable
     */
    public boolean isInvulnerable() {
        return invulnerabilityTimer > 0;
    }
    
    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }
    public boolean hasKey() { return hasKey; }
    public boolean isRunning() { return isRunning; }
}