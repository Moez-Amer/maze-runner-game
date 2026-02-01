package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Represents the player character in the maze game.
 * <p>
 * This class handles all player-specific logic, including:
 * <ul>
 * <li>Movement and collision detection</li>
 * <li>Combat mechanics (attacking, taking damage)</li>
 * <li>Inventory management (keys, scrolls)</li>
 * <li>Skill integration (speed, power, health upgrades)</li>
 * <li>Ghost mode (revival mechanic)</li>
 * </ul>
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
    private int keyCount;
    private int scrollCount;

    private boolean isGhostMode;
    private float ghostModeTimer;
    private boolean hasUsedRevive;
    private static final float GHOST_MODE_DURATION = 20.0f;

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
    private boolean attackHasHit = false;

    private KeyBindings keys;

    private int sessionScore = 0;
    private float currentWalkSpeed;
    private float currentRunSpeed;
    private float damageMultiplier;

    // God mode for developer console
    private boolean godModeEnabled = false;

    /**
     * Constructs a new Player at the given position.
     * Initializes player stats based on the current skill levels found in the GameState.
     * * @param x Starting X coordinate in the world.
     * @param y Starting Y coordinate in the world.
     * @param tileSize Size of each tile in pixels (used for collision scaling).
     * @param mapData Reference to the map data grid for collision detection.
     * @param state The current GameState used to retrieve unlocked skill levels.
     */
    public Player(float x, float y, int tileSize, int[][] mapData, GameState state) {
        super(x, y, tileSize, tileSize);

        int vitLevel = state.getSkillLevel("Vitality");
        int swiftLevel = state.getSkillLevel("Swiftness");
        int warLevel = state.getSkillLevel("Warrior");

        this.maxLives = 3 + vitLevel;

        float speedMult = 1.0f + (0.25f * swiftLevel);
        this.currentWalkSpeed = 80f * speedMult;
        this.currentRunSpeed = 150f * speedMult;

        this.damageMultiplier = 1.0f + (0.5f * warLevel);

        this.lives = this.maxLives;
        this.speed = currentWalkSpeed;
        this.keyCount = 0;
        this.scrollCount = 0;
        this.isRunning = false;
        this.isDamaged = false;
        this.isFalling = false;
        this.isAttacking = false;
        this.invulnerabilityTimer = 0f;
        this.speedBoostTimer = 0f;
        this.powerBoostTimer = 0f;
        this.shieldTimer = 0f;
        this.attackCombo = 1;
        this.keys = KeyBindings.getKeyBindings();
        this.isGhostMode = false;
        this.ghostModeTimer = 0f;
        this.hasUsedRevive = false;
        setMapData(mapData, tileSize);
        setCollisionBox(16, 16, 40, 20);
        loadAnimations();
    }

    /**
     * Loads all player animations from internal asset files.
     * Splits sprite sheets into frames and creates Animation objects for
     * idle, running, and attacking states in all four directions.
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
     * Updates the player's state for the current frame.
     * Handles timers (damage, boosts, ghost mode), falling mechanics,
     * and delegates input handling and animation updates.
     * * @param delta Time elapsed since last frame in seconds.
     */
    @Override
    public void update(float delta) {
        super.update(delta);

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

        if (isGhostMode) {
            ghostModeTimer -= delta;
            if (ghostModeTimer <= 0) {
                lives = 0;
                isGhostMode = false;
            }
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
        if (!isAttacking) {
            isMoving = handleInput(delta);
        }
        updateAnimation(isMoving);
    }

    /**
     * Handles keyboard input to control player movement and actions.
     * Checks for attack, sprint, and directional keys.
     * * @param delta Time elapsed since last frame in seconds.
     * @return true if the player is currently moving, false otherwise.
     */
    private boolean handleInput(float delta) {

        if((keys.isKeyJustPressed("Attack")) ){
            isAttacking = true;
            attackHasHit = false;
            AudioManager.playAttackSound();
            stateTime = 0f;
            speed = 0;

            if (attackCombo == 1) {
                attackCombo = 2;
            } else {
                attackCombo = 1;
            }
            return false;
        }

        isRunning = (keys.isKeyPressed("Sprint"));
        float baseSpeed = isRunning ? currentRunSpeed : currentWalkSpeed;
        speed = speedBoostTimer > 0 ? baseSpeed * SPEED_BOOST_MULTIPLIER : baseSpeed;

        boolean isUp    = (keys.isKeyPressed("Move Up"));
        boolean isDown  = (keys.isKeyPressed("Move Down"));
        boolean isLeft  = (keys.isKeyPressed("Move Left"));
        boolean isRight =(keys.isKeyPressed("Move Right"));

        if (!isUp && !isDown && !isLeft && !isRight) {
            return false;
        }

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
     * Selects and updates the current animation based on the player's state.
     * Handles logic for falling, attacking, running, and idling in all directions.
     * * @param moving Whether the player is currently moving.
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
     * Renders the player sprite to the screen.
     * applies visual effects such as:
     * <ul>
     * <li>Red flash when damaged</li>
     * <li>Transparency and blue tint for Ghost Mode</li>
     * <li>Shrinking and fading when falling into a pit</li>
     * <li>Glowing auras for active boosts (Speed, Power, Shield)</li>
     * </ul>
     * * @param batch The SpriteBatch used for drawing.
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
            float r = 1f;
            float g = 1f;
            float b = 1f;

            if (isDamaged && ((int)(damageTimer * 10) % 2 == 0)) {
                g = 0f;
                b = 0f;
            }

            float alpha = 1.0f;
            float drawWidth = 96f;
            float drawHeight = 80f;
            float drawX = this.x;
            float drawY = this.y;

            if (isGhostMode) {
                alpha = 0.5f;
                r = 0.8f;
                g = 0.9f;
                b = 1.0f;
            }

            if (isFalling) {
                float progress = Math.min(fallingTimer / FALLING_DURATION, 1.0f);

                alpha = 1.0f - progress;

                float currentScale = 1.0f - progress;
                float scaledWidth = drawWidth * currentScale;
                float scaledHeight = drawHeight * currentScale;

                drawX += (drawWidth - scaledWidth) / 2f;
                drawY += (drawHeight - scaledHeight) / 2f;

                drawWidth = scaledWidth;
                drawHeight = scaledHeight;

            }

            batch.setColor(r, g, b, alpha);

            if (powerBoostTimer > 0 && !isDamaged) {
                float pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 6);
                batch.setColor(0.6f, 0f, 0.8f, pulse * 0.4f);
                batch.draw(currentFrame, drawX - 8, drawY - 8, drawWidth + 16, drawHeight + 16);
                batch.setColor(0.7f, 0.1f, 0.9f, pulse * 0.6f);
                batch.draw(currentFrame, drawX - 5, drawY - 5, drawWidth + 10, drawHeight + 10);
                batch.setColor(0.8f, 0.3f, 1f, pulse * 0.8f);
                batch.draw(currentFrame, drawX - 2, drawY - 2, drawWidth + 4, drawHeight + 4);
                batch.setColor(r, g, b, alpha);
            }

            if (speedBoostTimer > 0) {
                float pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 8);
                batch.setColor(0f, 0.5f, 1f, pulse * 0.4f);
                batch.draw(currentFrame, drawX - 8, drawY - 8, drawWidth + 16, drawHeight + 16);
                batch.setColor(0.2f, 0.6f, 1f, pulse * 0.6f);
                batch.draw(currentFrame, drawX - 5, drawY - 5, drawWidth + 10, drawHeight + 10);
                batch.setColor(0.4f, 0.8f, 1f, pulse * 0.8f);
                batch.draw(currentFrame, drawX - 2, drawY - 2, drawWidth + 4, drawHeight + 4);
                batch.setColor(r, g, b, alpha);
            }

            if (shieldTimer > 0) {
                float pulse = 0.7f + 0.3f * (float) Math.sin(stateTime * 5);
                batch.setColor(0f, 0.8f, 0.2f, pulse * 0.4f);
                batch.draw(currentFrame, drawX - 8, drawY - 8, drawWidth + 16, drawHeight + 16);
                batch.setColor(0.2f, 0.9f, 0.3f, pulse * 0.6f);
                batch.draw(currentFrame, drawX - 5, drawY - 5, drawWidth + 10, drawHeight + 10);
                batch.setColor(0.4f, 1f, 0.5f, pulse * 0.8f);
                batch.draw(currentFrame, drawX - 2, drawY - 2, drawWidth + 4, drawHeight + 4);
                batch.setColor(r, g, b, alpha);
            }

            batch.draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * Calculates the collision box for the player's feet.
     * This smaller box is used for environment collisions to allow for pseudo-3D movement overlap.
     * * @return A float array containing {x, y, width, height} of the feet collision box.
     */
    public float[] getFeetCollisionBox() {
        float feetWidth = 16;
        float feetHeight = 16;
        float feetX = x + (96 - feetWidth) / 2f;
        float feetY = y + 20;
        return new float[]{feetX, feetY, feetWidth, feetHeight};
    }

    /**
     * Calculates the hit box for the player's sword attack.
     * The box dimensions and position change based on the player's facing direction.
     * * @return A Rectangle representing the area where the sword deals damage.
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
     * Attempts to apply damage to the player.
     * Handles logic for invulnerability frames, and shield protection.
     * * @return true if the damage was successfully parried, false otherwise.
     */
    public boolean takeDamage() {
        if (godModeEnabled) {
            return false;
        }

        if (isGhostMode) {
            return false;
        }

        if (invulnerabilityTimer <= 0 && shieldTimer <= 0) {
            lives--;
            isDamaged = true;
            AudioManager.playHitSound();
            damageTimer = 0f;
            invulnerabilityTimer = INVULNERABILITY_TIME;
        } else if (shieldTimer > 0) {
            System.out.println("Shield blocked damage!");
            if (shieldSoundTimer <= 0) {
                AudioManager.playHitWithShieldSound();
                shieldSoundTimer = 0.5f;
            }
        }
        return false;
    }

    /**
     * Triggers the sequence for the player falling into a death pit trap.
     * Disables control and sets the respawn location.
     * * @param respawnX The X coordinate where the player will respawn.
     * @param respawnY The Y coordinate where the player will respawn.
     */
    public void fallIntoHole(float respawnX, float respawnY) {
        if(isFalling == true)return;
        isFalling= true;
        fallingTimer=0f;

        this.respawnX = respawnX;
        this.respawnY = respawnY;
    }

    /**
     * Increments the player's life count, up to the maximum limit.
     */
    public void addLife() {
        if (lives < maxLives) {
            lives++;
        }
    }

    /**
     * Adds points to the player's current session score.
     * * @param p The amount of points to add.
     */
    public void addScore(int p) { this.sessionScore += p; }

    public int getScore() { return sessionScore; }


    /**
     * Increments the count of collected keys.
     */
    public void collectKey() {
        keyCount++;
    }

    /**
     * Increments the count of collected scrolls.
     */
    public void collectScroll() {
        scrollCount++;
    }

    /**
     * Checks if the player has collected at least one key.
     * @return true if key count >= 1.
     */
    public boolean hasAllKeys(){
        return keyCount >= 1;
    }

    /**
     * Checks if the player has collected all required scrolls.
     * @return true if scroll count >= 3.
     */
    public boolean hasAllScrolls(){
        return scrollCount >= 3;
    }

    /**
     * Checks if the victory conditions (keys and scrolls) are met.
     * @return true if the player can exit the maze.
     */
    public boolean canExitMaze(){
        return hasAllKeys() && hasAllScrolls();
    }

    /**
     * Checks if the player is dead (lives <= 0).
     * @return true if dead.
     */
    public boolean isDead() {
        return lives <= 0;
    }

    /**
     * Checks if the player is currently invulnerable to damage.
     * @return true if invulnerable.
     */
    public boolean isInvulnerable() {
        return invulnerabilityTimer > 0;
    }

    /**
     * Activates a movement speed boost for a set duration.
     * @param duration Duration in seconds.
     */
    public void applySpeedBoost(float duration) {
        this.speedBoostTimer = duration;
    }

    /**
     * Activates a damage power boost for a set duration.
     * @param duration Duration in seconds.
     */
    public void applyPowerBoost(float duration) {
        this.powerBoostTimer = duration;
    }

    /**
     * Activates a protective shield for a set duration.
     * @param duration Duration in seconds.
     */
    public void applyShield(float duration) {
        this.shieldTimer = duration;
        System.out.println("Shield activated for " + duration + " seconds!");
    }

    /**
     * Checks if the speed boost is active.
     * @return true if active.
     */
    public boolean hasSpeedBoost() {
        return speedBoostTimer > 0;
    }

    /**
     * Checks if the power boost is active.
     * @return true if active.
     */
    public boolean hasPowerBoost() {
        return powerBoostTimer > 0;
    }

    /**
     * Checks if the shield is active.
     * @return true if active.
     */
    public boolean hasShield() {
        return shieldTimer > 0;
    }

    /**
     * Calculates the player's current damage multiplier.
     * Factors in Warrior skill level, power boosts.
     * * @return The calculated damage multiplier.
     */
    public float getDamageMultiplier() {
        float totalDamage = damageMultiplier;

        if (powerBoostTimer > 0) {
            totalDamage += 1.0f;
        }

        return totalDamage;
    }

    public boolean hasAttackHit() {return attackHasHit;}

    public void setAttackHasHit(boolean hit) {this.attackHasHit = hit;}

    public int getLives() { return lives; }

    public int getMaxLives() { return maxLives; }


    public int getKeyCount() { return keyCount; }


    public int getScrollCount() { return scrollCount; }

    public boolean isRunning() { return isRunning; }

    public boolean isMoving() {return isMoving;}

    public boolean isAttacking() {return isAttacking;}

    public boolean isGhostMode() { return isGhostMode; }

    public float getGhostModeTimer() { return ghostModeTimer; }

    public boolean hasUsedRevive() { return hasUsedRevive; }

    /**
     * Activates Ghost Mode upon death.
     * Respawns the player at the entry point with 0 lives and starts the revival timer.
     * * @param entryX The X coordinate of the level entry point.
     * @param entryY The Y coordinate of the level entry point.
     */
    public void enterGhostMode(float entryX, float entryY) {
        isGhostMode = true;
        ghostModeTimer = GHOST_MODE_DURATION;
        hasUsedRevive = true;
        lives = 0;
        this.x = entryX;
        this.y = entryY;
        isDamaged = false;
        invulnerabilityTimer = 0f;
    }

    /**
     * Revives the player from Ghost Mode.
     * Restores 1 life, grants brief invulnerability, and returns the player to normal gameplay.
     */
    public void revive() {
        isGhostMode = false;
        ghostModeTimer = 0f;
        lives = 1;
        invulnerabilityTimer = INVULNERABILITY_TIME;
    }
    public void setGodMode(boolean enabled) {
        this.godModeEnabled = enabled;
    }
}