package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static de.tum.cit.fop.maze.GameScreen.TILE_SIZE;

/**
 * Represents the player character in the maze game.
 * Handles player-specific logic: lives, keys, input, damage effects.
 */
public class Player extends MovableGameObject {
    
    private static final float WALK_SPEED = 80f;
    private static final float RUN_SPEED = 150f;
    private float shieldSoundTimer = 0;
    private  float swordSize ;
    private int attackCombo ;
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
    
    private boolean isMoving;
    private boolean isDamaged;
    private boolean isAttacking;
    private float damageTimer;
    private float fallingTimer;
    private static final float DAMAGE_FLASH_DURATION = 1.0f;
    private static final float INVULNERABILITY_TIME = 1.5f;
    private static final float FALLING_DURATION = .5f;
    private float invulnerabilityTimer;
    
    private Animation<TextureRegion> idleDownAnim, idleUpAnim, idleLeftAnim, idleRightAnim;
    private Animation<TextureRegion> runDownAnim, runUpAnim, runLeftAnim, runRightAnim;
    private Animation<TextureRegion> attackDownAnim1, attackLeftAnim1, attackRightAnim1, attackUpAnim1;
    private Animation<TextureRegion> attackDownAnim2, attackLeftAnim2, attackRightAnim2, attackUpAnim2;

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
        this.isAttacking = false;
        this.invulnerabilityTimer = 0f;
        this.speedBoostTimer = 0f;
        this.powerBoostTimer = 0f;
        this.shieldTimer = 0f;
        
        this.attackCombo=1;
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

        Texture attackDown1 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 1/attack1_down.png"));
        Texture attackLeft1 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 1/attack1_left.png"));
        Texture attackRight1 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 1/attack1_right.png"));
        Texture attackUp1 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 1/attack1_up.png"));

        Texture attackDown2 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 2/attack2_down.png"));
        Texture attackLeft2 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 2/attack2_left.png"));
        Texture attackRight2 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 2/attack2_right.png"));
        Texture attackUp2 = new Texture(Gdx.files.internal("Character1_Assets/FREE_Adventurer 2D Pixel Art/Sprites/ATTACK 2/attack2_up.png"));



        TextureRegion[][] idleDownFrames = TextureRegion.split(idleDown, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleUpFrames = TextureRegion.split(idleUp, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleLeftFrames = TextureRegion.split(idleLeft, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] idleRightFrames = TextureRegion.split(idleRight, FRAME_WIDTH, FRAME_HEIGHT);
        
        TextureRegion[][] runDownFrames = TextureRegion.split(runDown, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runUpFrames = TextureRegion.split(runUp, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runLeftFrames = TextureRegion.split(runLeft, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] runRightFrames = TextureRegion.split(runRight, FRAME_WIDTH, FRAME_HEIGHT);


        TextureRegion[][] attackDownFrames1 = TextureRegion.split(attackDown1, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackLeftFrames1 = TextureRegion.split(attackLeft1, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackRightFrames1 = TextureRegion.split(attackRight1, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackUpFrames1 = TextureRegion.split(attackUp1, FRAME_WIDTH, FRAME_HEIGHT);

        TextureRegion[][] attackDownFrames2 = TextureRegion.split(attackDown2, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackLeftFrames2 = TextureRegion.split(attackLeft2, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackRightFrames2 = TextureRegion.split(attackRight2, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[][] attackUpFrames2 = TextureRegion.split(attackUp2, FRAME_WIDTH, FRAME_HEIGHT);


        idleDownAnim = new Animation<>(0.2f, idleDownFrames[0]);
        idleUpAnim = new Animation<>(0.2f, idleUpFrames[0]);
        idleLeftAnim = new Animation<>(0.2f, idleLeftFrames[0]);
        idleRightAnim = new Animation<>(0.2f, idleRightFrames[0]);
        
        runDownAnim = new Animation<>(0.1f, runDownFrames[0]);
        runUpAnim = new Animation<>(0.1f, runUpFrames[0]);
        runLeftAnim = new Animation<>(0.1f, runLeftFrames[0]);
        runRightAnim = new Animation<>(0.1f, runRightFrames[0]);

        attackDownAnim1 =new Animation<>(0.1f, attackDownFrames1[0]);
        attackLeftAnim1 =new Animation<>(0.1f, attackLeftFrames1[0]);
        attackRightAnim1 = new Animation<>(0.1f, attackRightFrames1[0]);
        attackUpAnim1    =new Animation<>(0.1f, attackUpFrames1[0]);

        attackDownAnim2 =new Animation<>(0.1f, attackDownFrames2[0]);
        attackLeftAnim2 =new Animation<>(0.1f, attackLeftFrames2[0]);
        attackRightAnim2 = new Animation<>(0.1f, attackRightFrames2[0]);
        attackUpAnim2    =new Animation<>(0.1f, attackUpFrames2[0]);

        idleDownAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleUpAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleRightAnim.setPlayMode(Animation.PlayMode.LOOP);

        runDownAnim.setPlayMode(Animation.PlayMode.LOOP);
        runUpAnim.setPlayMode(Animation.PlayMode.LOOP);
        runLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        runRightAnim.setPlayMode(Animation.PlayMode.LOOP);

        attackDownAnim1.setPlayMode(Animation.PlayMode.NORMAL);
        attackUpAnim1.setPlayMode(Animation.PlayMode.NORMAL);
        attackRightAnim1.setPlayMode(Animation.PlayMode.NORMAL);
        attackLeftAnim1.setPlayMode(Animation.PlayMode.NORMAL);

        attackDownAnim2.setPlayMode(Animation.PlayMode.NORMAL);
        attackUpAnim2.setPlayMode(Animation.PlayMode.NORMAL);
        attackRightAnim2.setPlayMode(Animation.PlayMode.NORMAL);
        attackLeftAnim2.setPlayMode(Animation.PlayMode.NORMAL);

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

        //Fix frozen walking
        stateTime += delta;

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

        if (shieldSoundTimer > 0) {
            shieldSoundTimer -= delta;
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

        if(isAttacking){
            if(currentAnimation.isAnimationFinished(stateTime)){
                isAttacking = false;
            }
        }
        isMoving = false;
        // We only process keys if we aren't busy attacking
        if (!isAttacking) {
            isMoving = handleInput(delta);
        }
        updateAnimation(isMoving);
    }
    
    /**
     * Handles keyboard input for player movement.
     * 
     * @param delta Time elapsed since last frame
     */
    private boolean handleInput(float delta) {

        //  Attack Check
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            isAttacking = true;
            // Play the sound when player is attacking
            AudioManager.playAttackSound();
            stateTime = 0f;
            speed = 0;
            if (attackCombo == 1) {
                attackCombo = 2; // Next time, do Attack 2
            } else {
                attackCombo = 1; // Next time, go back to Attack 1
            }
            return false; // Not moving when the player is attacking
        }


        isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float baseSpeed = isRunning ? RUN_SPEED : WALK_SPEED;
        speed = speedBoostTimer > 0 ? baseSpeed * SPEED_BOOST_MULTIPLIER : baseSpeed;

        // check movement keys
        boolean isUp    = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean isDown  = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean isLeft  = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean isRight = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (!isUp && !isDown && !isLeft && !isRight) {
            return false;
        }
        //speed = isRunning ? RUN_SPEED : WALK_SPEED;
        
        Direction moveDirection = null;
        
        if (isUp) {
            moveDirection = Direction.UP;
            facing = Direction.UP;
        } else if (isDown) {
            moveDirection = Direction.DOWN;
            facing = Direction.DOWN;

        } else if (isLeft) {
            moveDirection = Direction.LEFT;
            facing = Direction.LEFT;

        } else if (isRight) {
            moveDirection = Direction.RIGHT;
            facing = Direction.RIGHT;

        }
        
        if ( moveDirection != null) {
            move(delta, moveDirection);
        }
        
       return true;
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
        if(isAttacking){
            if(attackCombo==1){
            switch(facing) {
                case UP: currentAnimation = attackUpAnim1; break;
                case DOWN: currentAnimation= attackDownAnim1; break;
                case LEFT: currentAnimation= attackLeftAnim1;break;
                case RIGHT:currentAnimation= attackRightAnim1; break;
            }
            }else {
                switch(facing) {
                    case UP: currentAnimation = attackUpAnim2; break;
                    case DOWN: currentAnimation= attackDownAnim2; break;
                    case LEFT: currentAnimation= attackLeftAnim2;break;
                    case RIGHT:currentAnimation= attackRightAnim2; break;
                }
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
                currentFrame = currentAnimation.getKeyFrame(stateTime);
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
     * Attack hitbox calculator.
     *
     * Logic:
     * - Uses the player's feet collision box as a stable anchor point (ignoring sprite padding)
     * - Calculates dimensions (width/height) based on facing direction
     *  3x2 box when the attack is up and down and 2x2 box attack when it is left and right
     * @return Rectangle representing the active sword swing area for collision checks
     */
    public Rectangle getSwordHitBox() {

        float[] feetBox = getFeetCollisionBox();
        float feetX = feetBox[0];
        float feetY = feetBox[1];
        float feetW = feetBox[2];
        float feetH = feetBox[3];
        float feetCenterX = feetX + (feetW / 2);
        float feetCenterY = feetY + (feetH / 2);

        float startX = 0;
        float startY = 0;
        float width = 0;
        float height = 0;

        switch(facing) {
            case UP:
                width = 3 * tileSize;
                height = 2 * tileSize;
                startX = feetCenterX - (width / 2);
                startY = feetY + feetH;
                break;

            case DOWN:
                width = 3 * tileSize;
                height = 2 * tileSize;
                startX = feetCenterX - (width / 2);
                startY = feetY - height;
                break;
            case LEFT:
                width = 2 * tileSize;
                height = 2 * tileSize;
                startX = feetX - width;
                startY = feetCenterY - (height / 4);
                break;
            case RIGHT:
                width = 2 * tileSize;
                height = 2 * tileSize;
                startX = feetX + feetW;
                startY = feetCenterY - (height / 4);
                break;
        }
        return new Rectangle(startX, startY, width, height);
    }
    
    /**
     * Makes the player take damage and lose a life.
     */
    public void takeDamage() {
        if (invulnerabilityTimer <= 0 && shieldTimer <= 0) {
            lives--;
            isDamaged = true;
            // Play the standard hit sound effect
            AudioManager.playHitSound();
            damageTimer = 0f;
            invulnerabilityTimer = INVULNERABILITY_TIME;
        } else if (shieldTimer > 0) {
            System.out.println("Shield blocked damage!");
            // Play the hit sound with shield protected effect
            if (shieldSoundTimer <= 0) {
                AudioManager.playHitWithShieldSound();
                shieldSoundTimer = 0.5f;
            }
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
    public boolean isMoving() {return isMoving;}
    public boolean isAttacking() {return isAttacking;}
}