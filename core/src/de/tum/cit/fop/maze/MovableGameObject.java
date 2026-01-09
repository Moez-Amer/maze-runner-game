package de.tum.cit.fop.maze;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Rectangle;import com.badlogic.gdx.math.Rectangle;import com.badlogic.gdx.math.Rectangle;
public abstract class MovableGameObject extends GameObject {
    protected Array<Enemy> enemies;
    protected Player player;
    protected float speed;
    protected Animation<TextureRegion> currentAnimation;
    protected float stateTime;
    protected int[][] mapData;
    protected int tileSize;

    protected float collisionWidth;
    protected float collisionHeight;
    protected float collisionOffsetX;
    protected float collisionOffsetY;
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    protected Direction facing;
    public MovableGameObject(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.speed = 0;
        this.stateTime = 0f;
        this.facing = Direction.RIGHT;
        this.collisionWidth = width;
        this.collisionHeight = height;
        this.collisionOffsetX = 0;
        this.collisionOffsetY = 0;
    }

    public void update(float delta) {
        stateTime += delta;
    }
    protected void move(float delta, Direction direction) {
        float dx = 0, dy = 0;

        switch (direction) {
            case UP:    dy = 1;  break;
            case DOWN:  dy = -1; break;
            case LEFT:  dx = -1; break;
            case RIGHT: dx = 1;  break;
        }

        float newX = x + dx * speed * delta;
        float newY = y + dy * speed * delta;

        if (canMoveTo(newX, y)) {
            x = newX;
        }
        if (canMoveTo(x, newY)) {
            y = newY;
        }

        bounds.setPosition(x, y);
    }
    protected boolean canMoveTo(float newX, float newY) {
        if (mapData == null || tileSize == 0) return true;

        // 1. Calculate the 'Scout' area for tiles
        float collisionX = newX + collisionOffsetX;
        float collisionY = newY + collisionOffsetY;
        float margin = 2f;
        float checkX = collisionX + margin;
        float checkY = collisionY + margin;
        float checkW = collisionWidth - margin * 2;
        float checkH = collisionHeight - margin * 2;

        int tileX1 = (int) (checkX / tileSize);
        int tileY1 = (int) (checkY / tileSize);
        int tileX2 = (int) ((checkX + checkW - 1) / tileSize);
        int tileY2 = (int) ((checkY + checkH - 1) / tileSize);

        // 2. Map Boundary Check
        if (tileX1 < 0 || tileY1 < 0 || tileX2 >= mapData.length || tileY2 >= mapData[0].length) {
            return false;
        }

        // 3. Wall Tile Check
        boolean isTileWalkable = mapData[tileX1][tileY1] != 0 &&
                mapData[tileX2][tileY1] != 0 &&
                mapData[tileX1][tileY2] != 0 &&
                mapData[tileX2][tileY2] != 0;

        if (!isTileWalkable) return false;

        // 4. Enemy-to-Enemy Collision Check (Modified to prevent nesting crash)
        if (enemies != null) {
            Rectangle futureFeet = new Rectangle(newX + collisionOffsetX, newY + collisionOffsetY, collisionWidth, collisionHeight);

            // We use a standard for-loop with an index (i) instead of an Iterator
            for (int i = 0; i < enemies.size; i++) {
                Enemy otherEnemy = enemies.get(i);

                // Don't check collision against yourself
                if (otherEnemy == this) continue;

                Rectangle enemyFeet = feetBoxToRectangle(otherEnemy.getFeetCollisionBox());

                if (futureFeet.overlaps(enemyFeet)) {
                    return false; // Found a collision, cannot move here
                }
            }
        }
        return true;
    }
    public float[] getFeetCollisionBox() {
        return new float[]{
                x + collisionOffsetX,
                y + collisionOffsetY,
                collisionWidth,
                collisionHeight
        };
    }
    public Rectangle feetBoxToRectangle(float[] list) {
        return new Rectangle(list[0], list[1], list[2], list[3]);
    }
    public void render(SpriteBatch batch) {
        if (currentAnimation != null) {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x, y, width, height);
        }
    }
    public void setMapData(int[][] mapData, int tileSize) {
        this.mapData = mapData;
        this.tileSize = tileSize;
    }

    public void setCollisionBox(float width, float height, float offsetX, float offsetY) {
        this.collisionWidth = width;
        this.collisionHeight = height;
        this.collisionOffsetX = offsetX;
        this.collisionOffsetY = offsetY;
    }

    // Getters and setters
    public float getSpeed(){
        return speed;
    }

    public void setSpeed(float speed){
        this.speed = speed;
    }

    public Direction getFacing(){
        return facing;
    }

    public void setFacing(Direction facing){
        this.facing = facing;
    }

    public Animation<TextureRegion> getCurrentAnimation(){
        return currentAnimation;
    }

    public void setCurrentAnimation(Animation<TextureRegion> animation){
        this.currentAnimation = animation;
    }

    public void setEnemies(Array<Enemy> enemies) {
        this.enemies = enemies;
    }
    public void setPlayer(Player plyer){
        this.player=plyer;
    }
}