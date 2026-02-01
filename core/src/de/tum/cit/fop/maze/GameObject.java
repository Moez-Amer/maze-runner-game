package de.tum.cit.fop.maze;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Abstract base class for every object that exists in the game world.
 * <p>
 * A GameObject owns a position, a size, and an axis-aligned bounding box
 * that is kept in sync with the position at all times.
 * All concrete game entities extend this class and implement render to draw
 * themselves each frame. Coordinates follow libGDX conventions: the origin
 * (0, 0) is at the bottom left corner of the world, with Y increasing upward.
 * </p>
 */
public abstract class GameObject {
    protected float x, y;
    protected float width, height;
    protected Rectangle bounds;

    /**
     * Constructs a new GameObject at the given position and size.
     * The internal bounds rectangle is initialised to match
     * the supplied coordinates and dimensions immediately.
     *
     * @param x      The initial X-coordinate (bottom-left corner), in world pixels.
     * @param y      The initial Y-coordinate (bottom-left corner), in world pixels.
     * @param width  The width of the object, in pixels.
     * @param height The height of the object, in pixels.
     */
    public GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Draws this object to the screen using the supplied SpriteBatch.
     * Concrete subclasses must implement this method to render their
     * current animation frame or static graphic at the correct world position.
     *
     * @param batch The SpriteBatch that is currently open for drawing.
     */
    public abstract void render(SpriteBatch batch);

    /**
     * Returns the axis-aligned bounding rectangle of this object.
     * The returned Rectangle is the internal instance so modifying it
     * directly will affect collision checks. Use setPosition to move
     * the object instead.
     *
     * @return The current bounding rectangle.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Moves this object to a new world position and updates the bounding rectangle.
     *
     * @param x The new X-coordinate (bottom-left corner), in world pixels.
     * @param y The new Y-coordinate (bottom-left corner), in world pixels.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.bounds.setPosition(x, y);
    }

    /**
     * Returns the current X-coordinate of this object's bottom-left corner.
     *
     * @return The X position in world pixels.
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the current Y-coordinate of this object's bottom-left corner.
     *
     * @return The Y position in world pixels.
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the rendered width of this object.
     *
     * @return The width in pixels.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Returns the rendered height of this object.
     *
     * @return The height in pixels.
     */
    public float getHeight() {
        return height;
    }
}