package de.tum.cit.fop.maze;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Represents an enemy in the maze game.
 * Extends MovableGameObject to inherit movement and collision detection capabilities.
 */
public class Enemy extends MovableGameObject {

    /**
     * Constructs a new Enemy at the given position.
     * 
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param tileSize Size of each tile in pixels
     * @param mapData Reference to the map data for collision detection
     */
    public Enemy(float x, float y, int tileSize, int[][] mapData) {
        super(x, y, tileSize, tileSize);
        setMapData(mapData, tileSize);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
    }
}
