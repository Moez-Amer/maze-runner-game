package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Represents the exit point of the maze.
 * <p>
 * The exit is the goal the player must reach after collecting the
 * required key and scrolls. This class provides the object-oriented
 * representation of exit tiles while actual exit detection is handled
 * in GameScreen for efficiency.
 * </p>
 */
public class Exit extends GameObject {

    private boolean isUnlocked;

    /**
     * Constructs a new Exit at the specified position.
     *
     * @param x      The X-coordinate in world pixels.
     * @param y      The Y-coordinate in world pixels.
     * @param width  The width of the exit area in pixels.
     * @param height The height of the exit area in pixels.
     */
    public Exit(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.isUnlocked = false;
    }

    /**
     * Unlocks the exit, allowing the player to leave the maze.
     * Called when the player has collected all required items.
     */
    public void unlock() {
        this.isUnlocked = true;
    }

    /**
     * Checks if the exit is currently unlocked.
     *
     * @return true if the player can use this exit, false otherwise.
     */
    public boolean isUnlocked() {
        return isUnlocked;
    }

    /**
     * Renders the exit.
     * <p>
     * Note: Exit rendering is typically handled by the map renderer.
     * This method exists for interface compliance.
     * </p>
     *
     * @param batch The SpriteBatch used for drawing.
     */
    @Override
    public void render(SpriteBatch batch) {
    }
}
