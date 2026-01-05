package de.tum.cit.fop.maze;

import static de.tum.cit.fop.maze.GameScreen.TILE_SIZE;

/**
 * Level start point data container.
 *
 * Design goals:
 * - Immutable coordinate storage for player respawn
 * - Distinct from GameObject (no render/update logic needed)
 * - Serves as a reference point for DeathPitTrap resets
 *
 * Logic:
 * - Applies offset to raw map coordinates for correct sprite alignment
 */
public class Entry {

    private final float x;
    private final float y;

    /**
     * Creates entry point with alignment offset.
     *
     * Logic:
     * - Subtracts TILE_SIZE from inputs to center/align the player sprite
     * relative to the logical grid tile
     *
     * @param x Raw tile X coordinate from map data
     * @param y Raw tile Y coordinate from map data
     */
    public Entry(float x, float y) {
        // to center the player on the Enter
        this.x = x - TILE_SIZE;
        this.y = y - TILE_SIZE;
    }


    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}