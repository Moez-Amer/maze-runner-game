package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Abstract superclass for all hazard and trap types in the maze.
 * This class centralizes shared functionality, such as player references
 * and center-point collision detection, to fulfill inheritance requirements.
 */
public abstract class Trap extends GameObject {
    /** The player instance used for collision detection and damage application. */
    protected Player player;

    /**
     * Constructs a new Trap.
     *
     * @param x      The X-coordinate in the game world.
     * @param y      The Y-coordinate in the game world.
     * @param width  The width of the trap's trigger area.
     * @param height The height of the trap's trigger area.
     * @param player Reference to the player entity.
     */
    public Trap(float x, float y, float width, float height, Player player) {
        super(x, y, width, height);
        this.player = player;
    }

    /**
     * Checks if the center point of the player's feet collision box is inside the trap's bounds.
     * This shared logic ensures consistent detection across all trap types.
     *
     * @return true if the player's center overlaps with the trap, false otherwise.
     */
    protected boolean isPlayerOverlapping() {
        float[] feetData = player.getFeetCollisionBox();
        float feetCenterX = feetData[0] + (feetData[2] / 2);
        float feetCenterY = feetData[1] + (feetData[3] / 2);
        return this.bounds.contains(feetCenterX, feetCenterY);
    }

    /**
     * Updates the trap's logic. Implementation depends on specific trap behavior.
     *
     * @param delta Time elapsed since the last frame.
     */
    public abstract void update(float delta);
}