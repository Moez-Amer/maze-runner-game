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
    
    private float speedBoostTimer;
    private float powerBoostTimer;
    private float shieldTimer;
    private static final float BOOST_DURATION = 5.0f;
    private static final float SHIELD_DURATION = 8.0f;
    private static final float SPEED_BOOST_MULTIPLIER = 1.5f;
    private static final float POWER_BOOST_MULTIPLIER = 2.0f;
    
    private boolean isDamaged;
    private float damageTimer;
    private float fallingTimer;
    private static final float DAMAGE_FLASH_DURATION = 1.0f;
    private static final float INVULNERABILITY_TIME = 1.5f;
    private static final float FALLING_DURATION = .5f;
    private float invulnerabilityTimer;
    
    private Animation<TextureRegion> idleDownAnim, idleUpAnim, idleLeftAnim, idleRightAnim;
    private Animation<TextureRegion> runDownAnim, runUpAnim, runLeftAnim, runRightAnim;

    private boolean isFalling;
    private float respawnX, respawnY;
    
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
        this.isFalling = false;
        this.invulnerabilityTimer = 0f;
        this.speedBoostTimer = 0f;
        this.powerBoostTimer = 0f;
        this.speedBoostTimer = 0f;
        this.powerBoostTimer = 0f;
        this.shieldTimer = 0f;
        
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
        
        facing = Direction.RIGHT;
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
        
        if (speedBoostTimer > 0) {
            speedBoostTimer -= delta;
        }
        
        if (powerBoostTimer > 0) {
            powerBoostTimer -= delta;
        }
        
        if (shieldTimer > 0) {
            shieldTimer -= delta;
        }

        if(isFalling){
            fallingTimer += delta;
            if(fallingTimer >= FALLING_DURATION) {
                this.x = respawnX;
                this.y = respawnY;
                isFalling = false;
                fallingTimer = 0f;
            }
            return;
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
        float baseSpeed = isRunning ? RUN_SPEED : WALK_SPEED;
        speed = speedBoostTimer > 0 ? baseSpeed * SPEED_BOOST_MULTIPLIER : baseSpeed;
        
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
        if (isFalling) {
            switch (facing) {
                case UP: currentAnimation = idleUpAnim; break;
                case DOWN: currentAnimation = idleDownAnim; break;
                case LEFT: currentAnimation = idleLeftAnim; break;
                case RIGHT: currentAnimation = idleRightAnim; break;
            }
            return;
        }

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
        if (currentAnimation != null) {
            TextureRegion currentFrame;
            if (isFalling) {
                currentFrame = currentAnimation.getKeyFrame(0, false);
            } else {
                currentFrame = currentAnimation.getKeyFrame(stateTime, true);
            }
            //  Color (RGB)
            // Start with White (1, 1, 1)
            float r = 1f;
            float g = 1f;
            float b = 1f;

            // If damaged, remove Green and Blue to make it RED (1, 0, 0)
            if (isDamaged && ((int)(damageTimer * 10) % 2 == 0)) {
                g = 0f;
                b = 0f;
            }

            // Transparency (Alpha) and Size
            float alpha = 1.0f;
            float drawWidth = 96f;
            float drawHeight = 80f;
            float drawX = this.x;
            float drawY = this.y;

            if (isFalling) {
                float progress = Math.min(fallingTimer / FALLING_DURATION, 1.0f);

                // Calculate Fade (Alpha 1.0 -> 0.0)
                alpha = 1.0f - progress;

                // Calculate Shrink (Scale 1.0 -> 0.0)
                float currentScale = 1.0f - progress;
                float scaledWidth = drawWidth * currentScale;
                float scaledHeight = drawHeight * currentScale;

                // Center the sprite
                drawX += (drawWidth - scaledWidth) / 2f;
                drawY += (drawHeight - scaledHeight) / 2f;

                drawWidth = scaledWidth;
                drawHeight = scaledHeight;

            }

            batch.setColor(r, g, b, alpha);
            
            // Draw aura effects for active boosts (behind the player)
            if (powerBoostTimer > 0 && !isDamaged) {
                // Bright red aura for power boost - multi-layer pulsing glow
                float pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 6);
                // Outer glow layer
                batch.setColor(1f, 0f, 0f, pulse * 0.4f);
                batch.draw(currentFrame, drawX - 8, drawY - 8, drawWidth + 16, drawHeight + 16);
                // Middle glow layer
                batch.setColor(1f, 0.1f, 0.1f, pulse * 0.6f);
                batch.draw(currentFrame, drawX - 5, drawY - 5, drawWidth + 10, drawHeight + 10);
                // Inner bright layer
                batch.setColor(1f, 0.3f, 0.2f, pulse * 0.8f);
                batch.draw(currentFrame, drawX - 2, drawY - 2, drawWidth + 4, drawHeight + 4);
                batch.setColor(r, g, b, alpha);
            }
            
            if (speedBoostTimer > 0) {
                // Bright blue aura for speed boost - multi-layer pulsing glow
                float pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 8);
                // Outer glow layer
                batch.setColor(0f, 0.5f, 1f, pulse * 0.4f);
                batch.draw(currentFrame, drawX - 8, drawY - 8, drawWidth + 16, drawHeight + 16);
                // Middle glow layer
                batch.setColor(0.2f, 0.6f, 1f, pulse * 0.6f);
                batch.draw(currentFrame, drawX - 5, drawY - 5, drawWidth + 10, drawHeight + 10);
                // Inner bright layer
                batch.setColor(0.4f, 0.8f, 1f, pulse * 0.8f);
                batch.draw(currentFrame, drawX - 2, drawY - 2, drawWidth + 4, drawHeight + 4);
                batch.setColor(r, g, b, alpha);
            }

            batch.draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
        }

        // Always reset to standard White for the rest of the game
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
        if (invulnerabilityTimer <= 0 && shieldTimer <= 0) {
            lives--;
            isDamaged = true;
            damageTimer = 0f;
            invulnerabilityTimer = INVULNERABILITY_TIME;
        } else if (shieldTimer > 0) {
            System.out.println("Shield blocked damage!");
        }
    }
    /**
     * Triggers the falling sequence.
     * * Logic:
     * - Sets isFalling flag to disable input
     * - Stores respawn coordinates for use after animation ends
     * * @param respawnX Target X coordinate after fall
     * @param respawnY Target Y coordinate after fall
     */
    public void fallIntoHole(float respawnX, float respawnY) {
        if(isFalling == true)return; // to not fall twice
        isFalling= true;
        fallingTimer=0f;

        this.respawnX = respawnX;
        this.respawnY = respawnY;
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
    
    /**
     * Applies a speed boost for the specified duration.
     * 
     * @param duration Duration of boost in seconds
     */
    public void applySpeedBoost(float duration) {
        this.speedBoostTimer = duration;
    }
    
    /**
     * Applies a power boost for the specified duration.
     * 
     * @param duration Duration of boost in seconds
     */
    public void applyPowerBoost(float duration) {
        this.powerBoostTimer = duration;
    }
    
    /**
     * Applies shield protection for the specified duration.
     * 
     * @param duration Duration of shield in seconds
     */
    public void applyShield(float duration) {
        this.shieldTimer = duration;
        System.out.println("Shield activated for " + duration + " seconds!");
    }
    
    /**
     * Checks if speed boost is currently active.
     * 
     * @return true if speed boost is active
     */
    public boolean hasSpeedBoost() {
        return speedBoostTimer > 0;
    }
    
    /**
     * Checks if power boost is currently active.
     * 
     * @return true if power boost is active
     */
    public boolean hasPowerBoost() {
        return powerBoostTimer > 0;
    }
    
    /**
     * Checks if shield protection is currently active.
     * 
     * @return true if shield is active
     */
    public boolean hasShield() {
        return shieldTimer > 0;
    }
    
    /**
     * Gets the damage multiplier based on active boosts.
     * 
     * @return Damage multiplier
     */
    public float getDamageMultiplier() {
        return powerBoostTimer > 0 ? POWER_BOOST_MULTIPLIER : 1.0f;
    }
    
    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }
    public boolean hasKey() { return hasKey; }
    public boolean isRunning() { return isRunning; }
}