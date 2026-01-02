package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Represents a trap in the maze game.
 * Extends GameObject for position and collision detection.
 */
public class Trap extends GameObject {

    /**
     * Constructs a new Trap at the given position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Width of the trap
     * @param height Height of the trap
     */
    public Trap(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void render(SpriteBatch batch) {
    }
}
