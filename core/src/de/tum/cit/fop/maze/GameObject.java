package de.tum.cit.fop.maze;

import com.badlogic.gdx.math.Rectangle;

/**
 * Abstract base class for all game objects in the maze game.
 * Provides position, size, and collision bounds.
 * All specific game objects (player, wall, enemy, etc.) should extend this class.
 */
public abstract class GameObject {
    protected float x, y;           
    protected float width, height; 
    protected Rectangle bounds;

    /**
     * Constructs a new GameObject at the given position and size.
     * @param x X coordinate (bottom-left)
     * @param y Y coordinate (bottom-left)
     * @param width Width of the object
     * @param height Height of the object
     */
    public GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Renders the object using the given SpriteBatch.
     * Each subclass must implement its own rendering logic.
     * @param batch The SpriteBatch to draw with
     */
    public abstract void render(com.badlogic.gdx.graphics.g2d.SpriteBatch batch);

    /**
     * Gets the collision bounds of the object.
     * @return Rectangle representing the object's bounds
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Sets the position of the object and updates its bounds.
     * @param x New X coordinate
     * @param y New Y coordinate
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.bounds.setPosition(x, y);
    }

    /**
     * Gets the X coordinate of the object.
     * @return X coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the Y coordinate of the object.
     * @return Y coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the width of the object.
     * @return Width
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the object.
     * @return Height
     */
    public float getHeight() {
        return height;
    }
}