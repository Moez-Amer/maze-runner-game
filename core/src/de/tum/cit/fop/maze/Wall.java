package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Represents a wall tile in the maze.
 * <p>
 * Walls are impassable obstacles that block player and enemy movement.
 * This class exists primarily to satisfy the object-oriented inheritance
 * requirement; actual wall collision is handled via the mapData grid
 * for performance reasons.
 * </p>
 */
public class Wall extends GameObject {

    /**
     * Constructs a new Wall at the specified tile position.
     *
     * @param x      The X-coordinate in world pixels.
     * @param y      The Y-coordinate in world pixels.
     * @param width  The width of the wall tile in pixels.
     * @param height The height of the wall tile in pixels.
     */
    public Wall(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    /**
     * Renders the wall tile.
     * <p>
     * Note: Wall rendering is typically handled by the TiledMapRenderer
     * or the manual tile renderer in GameScreen for performance.
     * This method exists for interface compliance.
     * </p>
     *
     * @param batch The SpriteBatch used for drawing.
     */
    @Override
    public void render(SpriteBatch batch) {
        // Wall rendering is handled by the map renderer for efficiency
    }
}
