package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private float detectionRange=150f;
    private float LosePlayerRange=200f;
    private enum State {PATROL,ATTACK,CHASE};
    private State state ;
    private ArrayList<Node> patrolPoints;
    int TILE_SIZE=16;
    private int currentPatrolIndex = 0;  // ← ADD THIS at top of Enemy class
    /**
     * Constructs a new Enemy at the specified position.
     * Initializes the enemy with animations, collision detection, pathfinding,
     * and generates random patrol points around the spawn location.
     *
     * @param x The initial X coordinate in pixels
     * @param y The initial Y coordinate in pixels
     * @param tileSize The size of each tile in the game world
     * @param mapData 2D array representing the walkable/non-walkable tiles
     * @param path The file path to the enemy sprite sheets
     * @param frameWidth Width of each animation frame in pixels
     * @param frameHeight Height of each animation frame in pixels
     */
    public Enemy(float x, float y, int tileSize, int[][] mapData, String path, int frameWidth, int frameHeight) {
        super(x, y, frameWidth, frameHeight);
        this.speed = 85f;
        this.facing = Direction.LEFT;
        this.lastX = x;
        this.lastY = y;
        this.state = State.PATROL;

        setMapData(mapData, tileSize);
        if (mapData != null) {
            this.pathFinder = new PathFinder(mapData);
        } else {
            System.out.println("the enemy was created with null mapData");
        }

        setCollisionBox(12, 12, 44, 22);
        patrolPoints = new ArrayList<>();
        int attempt=0;
        int maxAttempts=50;
        int TILEx=(int)(x/TILE_SIZE);
        int TILEy=(int) (y/TILE_SIZE);
        while (patrolPoints.size()!=3&&attempt<maxAttempts){
                attempt++;
                int range = 5;
                int randomOffsetX = (int) (Math.random() * (2*range+1))-range;
                int randomOffsetY = (int) (Math.random() * (2*range+1))-range;

                int patrolX = TILEx+randomOffsetX;
                int patrolY = TILEy+randomOffsetY;

            if (patrolX >= 0 && patrolX < mapData.length &&
                    patrolY >= 0 && patrolY < mapData[0].length){
                if  (mapData[patrolX][patrolY]!=0){
                    Node patrolNode = new Node(patrolX,patrolY);
                    patrolPoints.add(patrolNode);
                }}
        }
        if (patrolPoints.isEmpty()) {
            patrolPoints.add(new Node(TILEx, TILEy));
        }
        loadAnimation(path, frameWidth, frameHeight);
    }
    /**
     * Updates the enemy's state each frame.
     * Calls the parent update method and executes movement logic.
     *
     * @param delta Time elapsed since last frame in seconds
     */
    @Override
    public void update(float delta) {
        super.update(delta);
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
                // Only push if it won't cause collision
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
     * <p>
     * Used to combine multiple rows of animation frames from a sprite sheet.
     * </p>
     *
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

        if(distanceToPlayer<26f)
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
            case PATROL -> patrol(enemyFeet[0],playerFeet[0]);
        }

//        if (enemyFeet[0]>playerFeet[0]){
//            currentAnimation = attackLeftAnim;
//            return;
//        }
//        else{
//            currentAnimation = attackAnim;
//        }
//        if(enemyFeet[0]>playerFeet[0]){
//            currentAnimation = floatingLeftAnim;
//        }
//        else{
//            currentAnimation = floatingAnim;
//        }

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

            float playerCenterX = playerFeet[0] + 8;
            float enemyCenterX = enemyFeet[0] + 8;
            float targetX=0f;
            if (enemyCenterX < playerCenterX){
                targetX = playerCenterX ;
            }
            else {
                targetX = playerCenterX ;
            }
            this.path = pathFinder.findPath(enemyFeet[0], enemyFeet[1], targetX, playerFeet[1] + 8);
            if (path != null && !path.isEmpty()) {
                path.remove(0);
                this.path = pathFinder.smoothPath(path);
            }
        }

        if (this.path != null && !this.path.isEmpty()) {
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
        applySeparation();
    }
    /**
     * Handles patrol behavior when player is not detected.
     * Enemy cycles through predetermined patrol points.
     * Currently only handles animation; movement logic to be implemented.
     *
     * @param EnemyX Enemy's X position
     * @param PlayerX Player's X position
     */
    public void patrol(float EnemyX, float PlayerX){
        if(EnemyX>PlayerX){
            currentAnimation = floatingLeftAnim;
        }
        else{
            currentAnimation = floatingAnim;
        }



    }

}