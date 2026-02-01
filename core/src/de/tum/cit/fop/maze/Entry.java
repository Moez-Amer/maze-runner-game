package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static de.tum.cit.fop.maze.Screens.GameScreen.TILE_SIZE;

/**
 * Represents the entry/spawn point of the maze.
 * <p>
 * The entry point is where the player spawns at the start of the level
 * and where they respawn after falling into a death pit. This class
 * extends GameObject to satisfy inheritance requirements while serving
 * primarily as a coordinate container.
 * </p>
 */
public class Entry extends GameObject {

    /**
     * Constructs a new Entry at the specified position.
     * <p>
     * An offset is applied to center the player sprite relative to
     * the logical grid tile.
     * </p>
     *
     * @param x The X-coordinate from map data in world pixels.
     * @param y The Y-coordinate from map data in world pixels.
     */
    public Entry(float x, float y) {
        super(x - TILE_SIZE, y - TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Renders the entry point.
     * <p>
     * Note: The entry point is typically rendered as part of the map.
     * This method exists for interface compliance.
     * </p>
     *
     * @param batch The SpriteBatch used for drawing.
     */
    @Override
    public void render(SpriteBatch batch) {
        // Entry rendering is handled by the map renderer
    }

    /**
     * Returns the spawn X-coordinate for the player.
     *
     * @return The X position in world pixels.
     */
    @Override
    public float getX() {
        return x;
    }

    /**
     * Returns the spawn Y-coordinate for the player.
     *
     * @return The Y position in world pixels.
     */
    @Override
    public float getY() {
        return y;
    }
}