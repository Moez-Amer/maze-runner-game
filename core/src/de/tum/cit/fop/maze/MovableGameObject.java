package de.tum.cit.fop.maze;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import static de.tum.cit.fop.maze.TiledToPropertiesConverter.TYPE_WALL;

/**
 * Abstract base class for all game objects that can move through the maze.
 * <p>
 * Provides tile-based collision detection, axis-aligned movement, and
 * animation playback. Subclasses such as {@link Player} and {@link Enemy}
 * inherit the shared movement pipeline and override behaviour as needed.
 * Collision is resolved independently on each axis so that an object
 * sliding along a wall is not stopped entirely by a corner.
 * </p>
 * <p>
 * A configurable collision box (which may be smaller than the visible
 * sprite) is offset from the object's origin. This allows the logical
 * "feet" of a character to drive collision while the full sprite is
 * rendered above it.
 * </p>
 */
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

    /**
     * Constructs a new MovableGameObject at the given position.
     * The collision box is initialised to the full sprite dimensions
     * with zero offset; call {@link #setCollisionBox} afterwards to
     * define a smaller logical footprint if needed.
     *
     * @param x      The initial X coordinate in world pixels.
     * @param y      The initial Y coordinate in world pixels.
     * @param width  The sprite width in pixels.
     * @param height The sprite height in pixels.
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
     * Updates per-frame state shared by all movable objects.
     * Advances {@link #stateTime} so that the current animation
     * frame advances on the next {@link #render} call.
     *
     * @param delta Time elapsed since the last frame in seconds.
     */
    public void update(float delta) {
        stateTime += delta;
    }

    /**
     * Moves the object one frame along a cardinal direction.
     * <p>
     * Movement is resolved independently on each axis: the X component
     * is applied first, and then the Y component.  This "axis splitting"
     * means the object will slide along a wall rather than stopping
     * completely when it hits a corner.
     * </p>
     *
     * @param delta     Time elapsed since the last frame in seconds.
     * @param direction The cardinal direction to move toward.
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
     * Determines whether the object can legally occupy a candidate position.
     * <p>
     * Four checks are performed in order:
     * <ol>
     *   <li><b>Tile sampling</b> – a small inward margin is applied to the
     *       collision box and the four corner tiles are looked up in
     *       {@link #mapData}.</li>
     *   <li><b>Boundary check</b> – the sampled tile indices must lie within
     *       the map array bounds.</li>
     *   <li><b>Wall check</b> – none of the four corner tiles may be a
     *       {@code TYPE_WALL} tile.</li>
     *   <li><b>Enemy-to-enemy collision</b> – if an {@link #enemies} list is
     *       present, the future collision box is tested against every other
     *       enemy's feet box to prevent stacking.</li>
     * </ol>
     * If {@link #mapData} has not been set the method returns {@code true}
     * unconditionally, allowing free movement during initialisation.
     * </p>
     *
     * @param newX The candidate X position of the object's origin.
     * @param newY The candidate Y position of the object's origin.
     * @return {@code true} if the position is walkable and unoccupied.
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

        boolean isTileWalkable = mapData[tileX1][tileY1] != TYPE_WALL &&
                mapData[tileX2][tileY1] != TYPE_WALL &&
                mapData[tileX1][tileY2] != TYPE_WALL &&
                mapData[tileX2][tileY2] != TYPE_WALL;

        if (!isTileWalkable) return false;

        if (enemies != null) {
            Rectangle futureFeet = new Rectangle(newX + collisionOffsetX, newY + collisionOffsetY, collisionWidth, collisionHeight);

            for (int i = 0; i < enemies.size; i++) {
                Enemy otherEnemy = enemies.get(i);

                if (otherEnemy == this) continue;

                Rectangle enemyFeet = feetBoxToRectangle(otherEnemy.getFeetCollisionBox());

                if (futureFeet.overlaps(enemyFeet)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the world-space collision rectangle at the feet of this object.
     * The box is defined by the current position plus the configured
     * collision offset and dimensions.  It is used by {@link #canMoveTo}
     * for enemy-to-enemy overlap checks and by {@link Enemy#getDamageHitBox}
     * to anchor the damage hitbox.
     *
     * @return A four-element array {@code {x, y, width, height}} in world pixels.
     */
    public float[] getFeetCollisionBox() {
        return new float[]{
                x + collisionOffsetX,
                y + collisionOffsetY,
                collisionWidth,
                collisionHeight
        };
    }

    /**
     * Converts a feet-collision-box array into a libGDX {@link Rectangle}.
     * This is a convenience wrapper used when overlap tests require the
     * richer {@code Rectangle} API (e.g. {@link Rectangle#overlaps}).
     *
     * @param list A four-element array {@code {x, y, width, height}}.
     * @return A {@link Rectangle} constructed from the array values.
     */
    public Rectangle feetBoxToRectangle(float[] list) {
        return new Rectangle(list[0], list[1], list[2], list[3]);
    }

    /**
     * Renders the current animation frame at the object's world position.
     * If no animation has been set the method is a no-op, so subclasses
     * that load their sprites asynchronously will simply be invisible
     * until the first animation is assigned.
     *
     * @param batch The {@link SpriteBatch} that is currently open for drawing.
     */
    public void render(SpriteBatch batch) {
        if (currentAnimation != null) {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x, y, width, height);
        }
    }

    /**
     * Binds the tile map used for wall-collision checks.
     * Must be called before the first {@link #move} or {@link #canMoveTo}
     * invocation if wall detection is required.
     *
     * @param mapData  A 2D array where each cell holds a tile-type constant
     *                 (e.g. {@code TYPE_WALL}, {@code TYPE_PATH}).
     * @param tileSize The width (and height) of one tile in world pixels.
     */
    public void setMapData(int[][] mapData, int tileSize) {
        this.mapData = mapData;
        this.tileSize = tileSize;
    }

    /**
     * Configures the logical collision box used for movement and overlap checks.
     * <p>
     * The collision box is typically smaller than the visible sprite and is
     * positioned at the character's "feet".  The offset is relative to the
     * object's origin {@code (x, y)}.
     * </p>
     *
     * @param width   The collision box width in pixels.
     * @param height  The collision box height in pixels.
     * @param offsetX Horizontal offset from the object's origin in pixels.
     * @param offsetY Vertical offset from the object's origin in pixels.
     */
    public void setCollisionBox(float width, float height, float offsetX, float offsetY) {
        this.collisionWidth = width;
        this.collisionHeight = height;
        this.collisionOffsetX = offsetX;
        this.collisionOffsetY = offsetY;
    }

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