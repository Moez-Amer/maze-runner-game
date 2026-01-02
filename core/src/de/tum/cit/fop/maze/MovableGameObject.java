package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Abstract class for all movable game objects (player, enemies, etc.).
 * Adds movement, animation, and update capabilities to GameObject.
 */
public abstract class MovableGameObject extends GameObject {

    protected float speed;
    protected Animation<TextureRegion> currentAnimation;
    protected float stateTime;
    protected int[][] mapData;
    protected int tileSize;
    
    protected float collisionWidth;
    protected float collisionHeight;
    protected float collisionOffsetX;
    protected float collisionOffsetY;
    
    /**
     * Direction enum for any movable object
     */
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    
    protected Direction facing;

    /**
     * Constructs a MovableGameObject.
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Width
     * @param height Height
     */
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

    /**
     * Updates the object's state. Called every frame.
     * Subclasses should override to add specific behavior.
     * 
     * @param delta Time since last frame
     */
    public void update(float delta) {
        stateTime += delta;
    }

    /**
     * Moves the object in a single direction with collision detection.
     * 
     * @param delta Time since last frame
     * @param direction Direction to move (UP, DOWN, LEFT, RIGHT)
     */
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

    /**
     * Checks if the object can move to the given position (collision detection).
     * Uses feet-based collision - checks the bottom-center area of the sprite.
     * 
     * @param newX New X coordinate (sprite position)
     * @param newY New Y coordinate (sprite position)
     * @return true if movement is allowed, false otherwise
     */
    protected boolean canMoveTo(float newX, float newY) {
        if (mapData == null || tileSize == 0) return true;
        
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
        
        if (tileX1 < 0 || tileY1 < 0 || tileX2 >= mapData.length || tileY2 >= mapData[0].length) {
            return false;
        }
        
        return mapData[tileX1][tileY1] != 0 &&
               mapData[tileX2][tileY1] != 0 &&
               mapData[tileX1][tileY2] != 0 &&
               mapData[tileX2][tileY2] != 0;
    }

    /**
     * Renders the object with its current animation.
     * 
     * @param batch SpriteBatch to draw with
     */
    public void render(SpriteBatch batch) {
        if (currentAnimation != null) {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x, y, width, height);
        }
    }
    
    /**
     * Sets the map data for collision detection.
     * 
     * @param mapData 2D array of tile types
     * @param tileSize Size of each tile in pixels
     */
    public void setMapData(int[][] mapData, int tileSize) {
        this.mapData = mapData;
        this.tileSize = tileSize;
    }
    
    /**
     * Sets the collision box dimensions and offset relative to sprite position.
     * 
     * @param width Width of the collision box
     * @param height Height of the collision box
     * @param offsetX X offset from sprite's bottom-left corner
     * @param offsetY Y offset from sprite's bottom-left corner
     */
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

}