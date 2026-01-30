package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Represents a static bottomless pit hazard.
 * If the player falls in, they take damage and are teleported back to the entry point.
 */
public class DeathPitTrap extends Trap {
    /** The entry point used for respawning the player. */
    private final Entry entry;

    /**
     * Constructs a new DeathPitTrap.
     *
     * @param x      World X position.
     * @param y      World Y position.
     * @param width  Trigger width.
     * @param height Trigger height.
     * @param player Reference for damage and position reset.
     * @param entry  Reference for respawn coordinates.
     */
    public DeathPitTrap(float x, float y, float width, float height, Player player, Entry entry) {
        super(x, y, width, height, player);
        this.entry = entry;
    }

    /**
     * Checks for player intersection and triggers the death sequence if detected.
     *
     * @param delta Time since last frame.
     */
    @Override
    public void update(float delta) {
        if (isPlayerOverlapping()) {
            triggerDeathTrap();
        }
    }

    /**
     * Applies damage to the player and initiates the falling/respawn sequence.
     */
    private void triggerDeathTrap() {
        System.out.println(" Fell in a hole. ");
        player.takeDamage();
        player.fallIntoHole(entry.getX(), entry.getY());
    }

    /**
     * Visuals for this trap are typically drawn by the MapRenderer layer.
     */
    @Override
    public void render(SpriteBatch batch) {
    }
}