package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Static hazard: a bottomless pit that respawns the player.
 *
 * Design goals:
 * - Instant positional penalty (respawn at Entry)
 * - Invisible logic object (visuals are handled by Tiled map layer)
 * - High stakes obstacle requiring careful movement
 *
 * Logic:
 * - Checks collision with player's feet center
 * - Triggers damage + teleport on contact
 */
public class DeathPitTrap extends GameObject {

    private Player player;
    private Entry entry;

    /**
     * Constructs a new DeathPitTrap.
     *
     * @param x      World X position
     * @param y      World Y position
     * @param width  Trigger width
     * @param height Trigger height
     * @param player Reference for damage and position reset
     * @param entry  Reference for respawn coordinates
     */
    public DeathPitTrap(float x, float y, float width, float height, Player player, Entry entry) {
        super(x, y, width, height);
        this.player = player;
        this.entry= entry;
    }

    /**
     * Checks for player intersection.
     *
     * Logic:
     * - Calculates center point of player's feet collision box
     * - Triggers trap if that point is strictly inside the pit bounds
     */
    public void update() {
        float[] feetData = player.getFeetCollisionBox();
        float feetX = feetData[0];
        float feetY = feetData[1];
        float feetWidth = feetData[2];
        float feetHeight = feetData[3];

        //  Calculate the exact center of the feet
        float feetCenterX = feetX + (feetWidth / 2);
        float feetCenterY = feetY + (feetHeight / 2);

        //  check if the CENTER of the player's feet is inside this specific trap tile.
        if (this.bounds.contains(feetCenterX, feetCenterY)) {
            triggerDeathTrap();
        }
    }

    /**
     * Executes hazard consequences.
     *
     * - Logs debug message
     * - Applies damage to player
     * - Teleports player back to the Entry point (respawn)
     */
    private void triggerDeathTrap() {
        System.out.println(" Fell in a hole. ");
        //  Lose a life
        player.takeDamage();

        player.fallIntoHole(entry.getX(),entry.getY());

    }
    /**
            * No-op render.
            *
            * - Visuals are drawn by the MapRenderer (background layer)
     * - This object exists purely for collision logic
     */
    @Override
    public void render(SpriteBatch batch) {
    }
}
