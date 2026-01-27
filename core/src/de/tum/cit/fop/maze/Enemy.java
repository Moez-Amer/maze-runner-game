package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;

/**
 * Represents an enemy character in the maze game.
 * Enemies can patrol designated areas, chase the player when detected,
 * and attack when in close proximity. They use A* pathfinding for navigation
 * and implement collision avoidance with other enemies.

 *
 */
public class Enemy extends MovableGameObject {
    private Animation<TextureRegion> floatingAnim, attackAnim, floatingLeftAnim, attackLeftAnim;
    private PathFinder pathFinder;
    private ArrayList<Node> path;
    private float pathTimer;
    private float stuckTimer = 0f;
    private float lastX, lastY;
    private enum State {PATROL,ATTACK,CHASE};
    private State state ;
    private ArrayList<Node> patrolPoints;
    int TILE_SIZE=16;
    private float health = 3.0f;
    private boolean isDead = false;
    private float damageFlashTimer = 0f;
    private static final float DAMAGE_FLASH_DURATION = 0.3f;
    private float attackCooldown = 0f;
    private static final float ATTACK_INTERVAL = 1.0f;
    private int patrolDirectionX = 1;
    private int patrolDirectionY = 0;
    private float patrolSpeed = 45f;
    private static final int[][] DIAGONAL_DIRECTIONS={{1, 1},{-1, 1},{-1, -1},{1, -1}};
    private int currentDirectionIndex = 0;
    /**
     * Constructs a new Enemy at the specified position.
     * Initializes the enemy with animations, collision detection, pathfinding,
     * and generates random patrol points around the spawn location.
     * @param x The initial X coordinate in pixels
     * @param y The initial Y coordinate in pixels
     * @param tileSize The size of each tile in the game world
     * @param mapData 2D array representing the walkable/non-walkable tiles
     * @param path The file path to the enemy sprite sheets
     * @param frameWidth Width of each animation frame in pixels
     * @param frameHeight Height of each animation frame in pixels
     */
    public Enemy(float x,float y,int tileSize,int[][] mapData,String path,int frameWidth,int frameHeight) {
        super(x, y, frameWidth, frameHeight);
        this.speed =85f;
        this.facing =Direction.LEFT;
        this.lastX =x;
        this.lastY =y;
        this.state =State.PATROL;

        setMapData(mapData, tileSize);
        if (mapData != null) {
            this.pathFinder = new PathFinder(mapData);
        } else {
            System.out.println("the enemy was created with null mapData");
        }
        setCollisionBox(12, 12, 44, 22);
        loadAnimation(path, frameWidth, frameHeight);
    }
    /**
     * Updates the enemy's state each frame.
     * Calls the parent update method and executes movement logic.
     * @param delta Time elapsed since last frame in seconds
     */
    @Override
    public void update(float delta) {
        super.update(delta);
        // Update damage flash timer
        if (damageFlashTimer > 0) {
            damageFlashTimer -= delta;
        }
        movementLogic(delta);
    }
    /**
     * Applies separation force to prevent enemies from overlapping.
     * Uses a simple repulsion force when enemies get too close to each other,
     * creating more natural-looking group movement.
     */
    private void applySeparation() {
        if (enemies == null) return;
        for (int i = 0; i < enemies.size; i++) {
            Enemy other = enemies.get(i);
            if (other == this || other == null) continue;
            float dx = other.x - this.x;
            float dy = other.y - this.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 15 && d > 0) {
                float pushX = -dx * 0.06f;
                float pushY = -dy * 0.06f;
                if (canMoveTo(this.x + pushX, this.y + pushY)) {
                    this.x += pushX;
                    this.y += pushY;
                }
            }
        }
    }
    /**
     * Loads and initializes all animation frames for the enemy.
     * Creates animations for floating/idle and attacking states,
     * both facing left and right directions.
     *
     * @param path Base path to the sprite sheet files
     * @param frameWidth Width of each frame in pixels
     * @param frameHeight Height of each frame in pixels
     */
    public void loadAnimation(String path, int frameWidth, int frameHeight) {
            Texture idle = new Texture(Gdx.files.internal(path + "idleN.png"));
            Texture attack = new Texture(Gdx.files.internal(path + "attacking.png"));
            TextureRegion[][] idleFrames = TextureRegion.split(idle, frameWidth, frameHeight);
            TextureRegion[][] attackFrames = TextureRegion.split(attack, frameWidth, frameHeight);
            floatingAnim = new Animation<>(0.2f, idleFrames[0]);
            floatingLeftAnim = new Animation<>(0.2f, flipFrames(idleFrames[0]));
            TextureRegion[] attackR = mergeRows(attackFrames[0], attackFrames[1]);
            attackAnim = new Animation<>(0.2f, attackR);
            attackLeftAnim = new Animation<>(0.2f, flipFrames(attackR));
            Animation[] all = {floatingAnim, floatingLeftAnim, attackAnim, attackLeftAnim};
            for (Animation a : all) if (a != null) a.setPlayMode(Animation.PlayMode.LOOP);
            currentAnimation = floatingAnim;
    }
    /**
     * Flips an array of texture regions horizontally.
     * Used to create left-facing animations from right-facing sprites.
     *
     * @param original Array of texture regions to flip
     * @return New array with horizontally flipped texture regions
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
     * Merges two arrays of texture regions into one.
     * Used to combine multiple rows of animation frames from a sprite sheet.
     * @param r1 First array of texture regions
     * @param r2 Second array of texture regions
     * @return Combined array containing all frames from both inputs
     */
    private TextureRegion[] mergeRows(TextureRegion[] r1, TextureRegion[] r2) {
        TextureRegion[] combined = new TextureRegion[r1.length + r2.length];
        System.arraycopy(r1, 0, combined, 0, r1.length);
        System.arraycopy(r2, 0, combined, r1.length, r2.length);
        return combined;
    }
    /**
     * Main logic for enemy movement and behavior.
     * Determines the enemy's state based on distance to player and
     * delegates to appropriate behavior method (patrol, chase, or attack).
     *
     * @param delta Time elapsed since last frame in seconds
     */
    private void movementLogic(float delta){
        if (player == null||pathFinder==null){
            return;
        }
        float[] enemyFeet = getFeetCollisionBox();
        float[] playerFeet = player.getFeetCollisionBox();

        float dxToP = playerFeet[0] - enemyFeet[0];
        float dyToP = playerFeet[1] - enemyFeet[1];
        float distanceToPlayer = (float) Math.sqrt(dxToP * dxToP + dyToP * dyToP);

        // Enemies ignore ghost players - always patrol
        if (player.isGhostMode()) {
            this.state = State.PATROL;
        }
        else if(distanceToPlayer<26f)
        {
            this.state=State.ATTACK;
        }
        else if(distanceToPlayer<150f)
        {
            this.state=State.CHASE;
        }
        else
        {
            this.state=State.PATROL;
        }

        switch (state){
            case ATTACK -> attack(enemyFeet[0],playerFeet[0]);
            case CHASE -> chase(enemyFeet[0],playerFeet[0],delta);
            case PATROL -> patrol(enemyFeet[0],playerFeet[0],delta);
        }
    }
    /**
     * Handles attack behavior when enemy is very close to player.
     * Sets the appropriate attack animation based on relative position to player.
     *
     * @param EnemyX Enemy's X position
     * @param PlayerX Player's X position
     */
    public void attack(float EnemyX, float PlayerX){
        if (EnemyX>PlayerX){
            currentAnimation = attackLeftAnim;

        }
        else{
            currentAnimation = attackAnim;
        }

        if (player != null && !player.isGhostMode()) {
            attackCooldown -= com.badlogic.gdx.Gdx.graphics.getDeltaTime();
            if (attackCooldown <= 0) {
                boolean wasParried = player.takeDamage();
                if (wasParried) {
                    // Player successfully parried! Deal counter damage to enemy
                    float counterDamage = 2.0f * player.getDamageMultiplier();
                    takeDamage(counterDamage);
                    System.out.println("Counter attack! Dealt " + counterDamage + " damage to enemy!");
                }
                attackCooldown = ATTACK_INTERVAL;
            }
        }

    }
    /**
     * Handles chase behavior when enemy detects the player.
     * Uses A* pathfinding to navigate toward the player, recalculating
     * the path periodically or when stuck. Implements smooth movement
     * with wall sliding and collision handling.
     *
     * @param EnemyX Enemy's X position
     * @param PlayerX Player's X position
     * @param delta Time elapsed since last frame in seconds
     */
    public void chase(float EnemyX, float PlayerX, float delta){
        this.speed=85f;
        if(EnemyX>PlayerX){
            currentAnimation = floatingLeftAnim;
        }
        else{
            currentAnimation = floatingAnim;
        }
        float[] enemyFeet = getFeetCollisionBox();
        float[] playerFeet = player.getFeetCollisionBox();
        float eucladeanDistanceMoved = (float) Math.sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY));
        if (eucladeanDistanceMoved < 0.5f) {
            stuckTimer += delta;
        } else {
            stuckTimer = 0f;
        }
        lastX = x;
        lastY = y;

        // path refresh, recalculate if stuck or timer expires
        this.pathTimer += delta;
        if (pathTimer > 0.8f || path == null || path.isEmpty() || stuckTimer > 0.3f) {
            pathTimer = 0;
            stuckTimer = 0;

            // Round to nearest tile center to prevent zigzag
            float playerCenterX = (float)(Math.round(playerFeet[0] / TILE_SIZE) * TILE_SIZE) + TILE_SIZE / 2f;
            float playerCenterY = (float)(Math.round(playerFeet[1] / TILE_SIZE) * TILE_SIZE) + TILE_SIZE / 2f;
            float enemyCenterX = enemyFeet[0] + 8;
            float targetX = playerCenterX ;


        calculatePathTo(playerCenterX,playerCenterY);
        }
        startFollowingThePath(delta);

        applySeparation();
    }
    /**
     * Handles patrol behavior with wall-bounce logic.
     * Enemy moves in diagonal directions until hitting a wall, then bounces.
     *
     * @param EnemyX Enemy's X position
     * @param PlayerX Player's X position
     * @param delta Time elapsed since last frame in seconds
     */
    public void patrol(float EnemyX, float PlayerX, float delta) {
        this.speed = patrolSpeed;
        float[] enemyFeet = getFeetCollisionBox();
        if(patrolDirectionX<0){
            currentAnimation=floatingLeftAnim;
        }
        else
        {
            currentAnimation=floatingAnim;
        }

        float moveX=patrolDirectionX*speed*delta;
        float moveY=patrolDirectionY*speed*delta;

        if (canMoveTo(x+moveX,y+moveY)) {
            x+=moveX;
            y+=moveY;
        }
        else{
            changePatrolDirection();

            moveX=patrolDirectionX*speed*delta;
            moveY=patrolDirectionY*speed*delta;

            if(canMoveTo(x+moveX,y+moveY)) {
                x+=moveX;
                y+=moveY;
            }
        }
        applySeparation();
    }

    /**
     * Changes patrol direction when hitting a wall.
     * Cycles through diagonal directions: (1,1) -> (-1,1) -> (-1,-1) -> (1,-1) -> repeat
     */
    private void changePatrolDirection() {
        currentDirectionIndex = (currentDirectionIndex + 1) % DIAGONAL_DIRECTIONS.length;
        patrolDirectionX = DIAGONAL_DIRECTIONS[currentDirectionIndex][0];
        patrolDirectionY = DIAGONAL_DIRECTIONS[currentDirectionIndex][1];

    }
    /**
     * Gets the damage hitbox for the enemy.
     * This is 3 tiles tall, positioned directly above the feet collision box.
     *
     * @return Rectangle representing the area where enemy can take damage
     */
    public Rectangle getDamageHitBox() {
        float[] feetBox = getFeetCollisionBox();
        float feetX = feetBox[0];
        float feetY = feetBox[1];
        float feetW = feetBox[2];
        float feetH = feetBox[3];

        // Create a hitbox that is 3 tiles tall, starting from the feet box
        float damageWidth = feetW;
        float damageHeight = TILE_SIZE * 3;
        float damageX = feetX;
        float damageY = feetY;

        return new Rectangle(damageX, damageY, damageWidth, damageHeight);
    }

    /**
     * Applies damage to the enemy.
     *
     * @param damage Amount of damage to apply
     */
    public void takeDamage(float damage) {
        health -= damage;
        damageFlashTimer = DAMAGE_FLASH_DURATION;

        if (health <= 0) {
            isDead = true;
        }

        System.out.println("Enemy took " + damage + " damage. Health: " + health);
    }

    /**
     * Checks if the enemy is dead.
     *
     * @return true if enemy health is 0 or below
     */
    public boolean isDead() {
        return isDead;
    }
    @Override
    public void render(SpriteBatch batch) {
        if (currentAnimation != null) {
            TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime);

            // Flash red when damaged
            if (damageFlashTimer > 0) {
                if ((int)(damageFlashTimer * 10) % 2 == 0) {
                    batch.setColor(1f, 0f, 0f, 1f); // Red flash
                }
            }

            batch.draw(currentFrame, x, y, width, height);
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE); // Reset color
        }
    }
    public void  calculatePathTo(float targetX, float targetY){
        float [] enemyFeet= getFeetCollisionBox();
        this.path = pathFinder.findPath(enemyFeet[0], enemyFeet[1], targetX, targetY);
        if (path != null && !path.isEmpty()) {
            path.remove(0);
            this.path = pathFinder.smoothPath(path);
        }
    }
    private void startFollowingThePath(float delta){
        if (this.path != null && !this.path.isEmpty()) {
            float[]enemyFeet = getFeetCollisionBox();
            Node nextNode = this.path.get(0);
            float targetX = nextNode.x * 16;
            float targetY = nextNode.y * 16;
            float currentX = enemyFeet[0];
            float currentY = enemyFeet[1];

            float dx = targetX - currentX;
            float dy = targetY - currentY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > 3f) {
                float normalizedDx = dx /distance ;
                float normalizedDy = dy /distance;

                float moveX = normalizedDx * speed * delta;
                float moveY = normalizedDy * speed * delta;

                boolean moved = false;

                if (canMoveTo(x + moveX, y + moveY)) {
                    x += moveX;
                    y += moveY;
                    moved = true;
                } else {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        if (canMoveTo(x + moveX, y)) {
                            x += moveX;
                            moved = true;
                        } else if (canMoveTo(x, y + moveY)) {
                            y += moveY;
                            moved = true;
                        }
                    } else {
                        if (canMoveTo(x, y + moveY)) {
                            y += moveY;
                            moved = true;
                        } else if (canMoveTo(x + moveX, y)) {
                            x += moveX;
                            moved = true;
                        }
                    }
                }

                // If completely stuck, skip this waypoint
                if (!moved) {
                    this.path.remove(0);
                }

                if (Math.abs(dx) > Math.abs(dy)) {
                    facing = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
                } else {
                    facing = (dy > 0) ? Direction.UP : Direction.DOWN;
                }
            } else {
                this.path.remove(0);
            }
        }
    }



}