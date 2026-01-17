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
 * Displays hearts for lives, key status, active boosts, and achievement notifications.
 */
public class HUD {

    private TextureAtlas atlas;
    private TextureRegion heartTexture;
    private TextureRegion heartBgTexture;
    private TextureRegion keyTexture;
    private Texture keyTextureFile;
    private BitmapFont font;
    private BitmapFont boldFont;

    private static final float HEART_SIZE = 32f;
    private static final float HEART_SPACING = 36f;
    private static final float PADDING = 20f;
    private static final float KEY_SIZE = 36f;

    // --- Achievement Popup Variables ---
    private String popupText = "";
    private float popupTimer = 0;
    private static final float POPUP_DURATION = 3.0f;
    private boolean showPopup = false;

    /**
     * Constructs the HUD with textures from the UI atlas.
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

        // Load key from standalone PNG file
        keyTextureFile = new Texture(Gdx.files.internal("Key.png"));
        keyTexture = new TextureRegion(keyTextureFile);
    }

    /**
     * Triggers a visual popup notification for an unlocked achievement.
     * @param achievementName The name of the achievement to display.
     */
    public void showAchievementPopup(String achievementName) {
        this.popupText = "UNLOCKED: " + achievementName;
        this.popupTimer = POPUP_DURATION;
        this.showPopup = true;
    }

    /**
     * Renders the complete HUD.
     * @param batch SpriteBatch to draw with
     * @param player Player to display status for
     */
    public void render(SpriteBatch batch, Player player) {
        MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
        GameState state = game.getGameState();

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        renderLives(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE);
        renderKeyStatus(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE - 50);
        renderBoostStatus(batch, player, screenWidth - 180, screenHeight - PADDING - 30);

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, "SCORE: " + player.getScore(), screenWidth / 2f - 40, screenHeight - PADDING);

        // Render Skill Points
        float pointsY = screenHeight - PADDING - HEART_SIZE - 100;
        boldFont.getData().setScale(0.7f);

        boldFont.setColor(Color.RED);
        boldFont.draw(batch, "WARRIOR PTS: " + state.warriorPoints, PADDING, pointsY);

        boldFont.setColor(Color.CYAN);
        boldFont.draw(batch, "SWIFTNESS PTS: " + state.swiftnessPoints, PADDING, pointsY - 25);

        boldFont.setColor(Color.GREEN);
        boldFont.draw(batch, "VITALITY PTS: " + state.vitalityPoints, PADDING, pointsY - 50);

        boldFont.getData().setScale(1.0f);
        boldFont.setColor(Color.WHITE);

        // --- ACHIEVEMENT POPUP RENDER LOGIC ---
        if (showPopup) {
            renderPopup(batch, screenWidth, screenHeight);
        }
    }

    /**
     * Handles the animation and rendering of the achievement popup.
     */
    private void renderPopup(SpriteBatch batch, float screenWidth, float screenHeight) {
        popupTimer -= Gdx.graphics.getDeltaTime();
        if (popupTimer <= 0) {
            showPopup = false;
        }

        // Slide In/Out Animation logic
        float yOffset;
        if (popupTimer > 2.5f) { // Slide In (0.5s)
            yOffset = (3.0f - popupTimer) * 120; // Moves down
        } else if (popupTimer < 0.5f) { // Slide Out (0.5s)
            yOffset = popupTimer * 120; // Moves up
        } else { // Stay visible
            yOffset = 60;
        }

        float popupY = screenHeight - yOffset;
        float popupX = screenWidth / 2f - 150;

        // Draw Text with Gold Color
        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, popupText, popupX, popupY);
        boldFont.setColor(Color.WHITE);
    }

    private void renderLives(SpriteBatch batch, Player player, float x, float y) {
        int lives = player.getLives();
        int maxLives = player.getMaxLives();

        for (int i = 0; i < maxLives; i++) {
            float heartX = x + (i * HEART_SPACING);
            if (heartBgTexture != null) batch.draw(heartBgTexture, heartX, y, HEART_SIZE, HEART_SIZE);
            if (i < lives && heartTexture != null) batch.draw(heartTexture, heartX, y, HEART_SIZE, HEART_SIZE);
        }
    }

    private void renderKeyStatus(SpriteBatch batch, Player player, float x, float y) {
        if (keyTexture != null) {
            int keyCount = player.getKeyCount();
            int requiredKeys = 3;
            if (keyCount > 0) {
                batch.setColor(1f, 0.85f, 0f, 1f);
                batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
                batch.setColor(Color.WHITE);
                boldFont.setColor(1f, 0.85f, 0f, 1f);
                boldFont.draw(batch, "KEYS: " + keyCount + "/" + requiredKeys, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
                boldFont.setColor(Color.WHITE);
            } else {
                batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
                batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
                batch.setColor(Color.WHITE);
                font.setColor(0.5f, 0.5f, 0.5f, 0.8f);
                font.draw(batch, "KEYS: 0/" + requiredKeys, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
                font.setColor(Color.WHITE);
            }
        }
    }

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

    public void dispose() {
        if (keyTextureFile != null) keyTextureFile.dispose();
    }
}