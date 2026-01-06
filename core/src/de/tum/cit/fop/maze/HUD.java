package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Heads-up display for showing player status.
 * Displays hearts for lives, key status, and active boosts.
 */
public class HUD {
    
    private TextureAtlas atlas;
    private TextureRegion heartTexture;
    private TextureRegion heartBgTexture;
    private TextureRegion keyTexture;
    private Texture keyTextureFile;  // Keep reference for disposal
    private BitmapFont font;
    private BitmapFont boldFont;
    
    private static final float HEART_SIZE = 32f;
    private static final float HEART_SPACING = 36f;
    private static final float PADDING = 20f;
    private static final float KEY_SIZE = 36f;
    
    /**
     * Constructs the HUD with textures from the UI atlas.
     * 
     * @param uiAtlas The UI texture atlas containing heart and key sprites
     * @param font Regular font for text
     * @param boldFont Bold font for emphasis
     */
    public HUD(TextureAtlas uiAtlas, BitmapFont font, BitmapFont boldFont) {
        this.atlas = uiAtlas;
        this.font = font;
        this.boldFont = boldFont;
        
        heartTexture = atlas.findRegion("heart");
        heartBgTexture = atlas.findRegion("heart-bg");
        
        // Load key from standalone PNG file (same as collectibles)
        keyTextureFile = new Texture(Gdx.files.internal("Key.png"));
        keyTexture = new TextureRegion(keyTextureFile);
    }
    
    /**
     * Renders the complete HUD.
     * 
     * @param batch SpriteBatch to draw with
     * @param player Player to display status for
     */
    public void render(SpriteBatch batch, Player player) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        
        renderLives(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE);
        renderKeyStatus(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE - 50);
        renderBoostStatus(batch, player, screenWidth - 180, screenHeight - PADDING - 30);
    }
    
    /**
     * Renders hearts representing player lives.
     * Full hearts = current lives, empty hearts = lost lives.
     * 
     * @param batch SpriteBatch to draw with
     * @param player Player to get lives from
     * @param x Starting X position
     * @param y Y position
     */
    private void renderLives(SpriteBatch batch, Player player, float x, float y) {
        int lives = player.getLives();
        int maxLives = player.getMaxLives();
        
        for (int i = 0; i < maxLives; i++) {
            float heartX = x + (i * HEART_SPACING);
            
            if (heartBgTexture != null) {
                batch.draw(heartBgTexture, heartX, y, HEART_SIZE, HEART_SIZE);
            }
            
            if (i < lives && heartTexture != null) {
                batch.draw(heartTexture, heartX, y, HEART_SIZE, HEART_SIZE);
            }
        }
    }
    
    /**
     * Renders key collection status.
     * 
     * @param batch SpriteBatch to draw with
     * @param player Player to check key status
     * @param x X position
     * @param y Y position
     */
    private void renderKeyStatus(SpriteBatch batch, Player player, float x, float y) {
        if (keyTexture != null) {
            if (player.hasKey()) {
                batch.setColor(1f, 0.85f, 0f, 1f);
                batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
                batch.setColor(Color.WHITE);
                
                boldFont.setColor(1f, 0.85f, 0f, 1f);
                boldFont.draw(batch, "KEY", x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
                boldFont.setColor(Color.WHITE);
            } else {
                batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
                batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
                batch.setColor(Color.WHITE);
                
                font.setColor(0.5f, 0.5f, 0.5f, 0.8f);
                font.draw(batch, "No Key", x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
                font.setColor(Color.WHITE);
            }
        }
    }
    
    /**
     * Renders active boost indicators in the top-right corner.
     * 
     * @param batch SpriteBatch to draw with
     * @param player Player to check boost status
     * @param x X position
     * @param y Y position
     */
    private void renderBoostStatus(SpriteBatch batch, Player player, float x, float y) {
        float lineHeight = 25;
        float currentY = y;
        
        if (player.hasSpeedBoost()) {
            boldFont.setColor(0f, 0.9f, 1f, 1f);
            boldFont.draw(batch, "SPEED BOOST!", x, currentY);
            currentY -= lineHeight;
        }
        
        if (player.hasPowerBoost()) {
            boldFont.setColor(1f, 0.5f, 0f, 1f);
            boldFont.draw(batch, "POWER BOOST!", x, currentY);
            currentY -= lineHeight;
        }
        
        if (player.hasShield()) {
            boldFont.setColor(0.4f, 0.7f, 1f, 1f);
            boldFont.draw(batch, "SHIELD ACTIVE!", x, currentY);
        }
        
        boldFont.setColor(Color.WHITE);
    }
    
    /**
     * Disposes resources.
     */
    public void dispose() {
        // Atlas is managed by game, don't dispose here
        if (keyTextureFile != null) {
            keyTextureFile.dispose();
        }
    }
}
