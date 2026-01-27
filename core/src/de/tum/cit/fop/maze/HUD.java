package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The Heads-Up Display (HUD) for the Maze Runner game.
 * <p>
 * This class is responsible for rendering the player's status overlay, including
 * health (lives), collected keys, skill points, score, and
 * achievement notifications.
 * </p>
 */
public class HUD {

    private static final float HEART_SIZE = 24f;
    private static final float HEART_SPACING = 28f;
    private static final float PADDING = 20f;
    private static final float KEY_SIZE = 28f;
    private static final float POPUP_DURATION = 3.0f;

    private final TextureAtlas atlas;
    private final TextureRegion heartTexture;
    private final TextureRegion heartBgTexture;
    private final TextureRegion keyTexture;
    private final TextureRegion scrollTexture;
    private final Texture heartTextureFile;
    private final Texture keyTextureFile;
    private final Texture scrollTextureFile;
    private final BitmapFont font;
    private final BitmapFont boldFont;

    private String popupText = "";
    private float popupTimer = 0;
    private boolean showPopup = false;
    private Texture popupIconTexture = null;
    private String currentIconPath = null;

    /**
     * Constructs the HUD and initializes necessary textures and fonts.
     *
     * @param uiAtlas  The UI texture atlas containing general sprites (backgrounds).
     * @param font     The regular font used for standard text.
     * @param boldFont The bold font used for emphasis and headers.
     */
    public HUD(TextureAtlas uiAtlas, BitmapFont font, BitmapFont boldFont) {
        this.atlas = uiAtlas;
        this.font = font;
        this.boldFont = boldFont;

        // Load heart from undead loot icons - Icon 12
        this.heartTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon12.png"));
        this.heartTexture = new TextureRegion(heartTextureFile);
        this.heartBgTexture = null; // Don't use heart background anymore

        // Load key from undead loot icons - Icon 41 (silver key)
        this.keyTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon41.png"));
        this.keyTexture = new TextureRegion(keyTextureFile);

        // Load scroll from undead loot icons - Icon 43
        this.scrollTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon43.png"));
        this.scrollTexture = new TextureRegion(scrollTextureFile);
    }

    /**
     * Renders the complete HUD overlay.
     * <p>
     * This method orchestrates the drawing of all HUD components: lives, keys,
     * score, skill points and any active achievement popups.
     * </p>
     *
     * @param batch  The SpriteBatch used to draw the 2D elements.
     * @param player The current player entity to retrieve stats from.
     */
    public void render(SpriteBatch batch, Player player) {
        MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
        GameState state = game.getGameState();

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        renderLives(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE);
        renderKeyStatus(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE - 40);
        renderScrollStatus(batch, player, PADDING, screenHeight - PADDING - HEART_SIZE - 75);

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, "SCORE: " + player.getScore(), screenWidth / 2f - 40, screenHeight - PADDING);
        boldFont.setColor(Color.WHITE);

        float pointsY = screenHeight - PADDING - HEART_SIZE - 120;
        renderSkillPoints(batch, state, PADDING, pointsY);

        // Render ghost mode timer if active
        if (player.isGhostMode()) {
            renderGhostModeTimer(batch, player, screenWidth, screenHeight);
        }

        if (showPopup) {
            renderPopup(batch, screenWidth, screenHeight);
        }
    }

    /**
     * Triggers a visual popup notification for an unlocked achievement.
     * <p>
     * The popup appears at the top center of the screen and persists for a short duration.
     * </p>
     *
     * @param achievement The achievement to display with icon.
     */
    public void showAchievementPopup(Achievement achievement) {
        this.popupText = "UNLOCKED: " + achievement.name;
        this.popupTimer = POPUP_DURATION;
        this.showPopup = true;

        // Load achievement icon
        if (achievement.iconPath != null && !achievement.iconPath.isEmpty()) {
            // Dispose old icon if exists
            if (popupIconTexture != null && !achievement.iconPath.equals(currentIconPath)) {
                popupIconTexture.dispose();
                popupIconTexture = null;
            }

            // Load new icon
            if (!achievement.iconPath.equals(currentIconPath)) {
                try {
                    popupIconTexture = new Texture(Gdx.files.internal(achievement.iconPath));
                    currentIconPath = achievement.iconPath;
                } catch (Exception e) {
                    System.err.println("Failed to load achievement icon: " + achievement.iconPath);
                    popupIconTexture = null;
                }
            }
        }
    }

    /**
     * Releases resources managed by this HUD.
     * Should be called when the game screen is destroyed.
     */
    public void dispose() {
        if (heartTextureFile != null) {
            heartTextureFile.dispose();
        }
        if (keyTextureFile != null) {
            keyTextureFile.dispose();
        }
        if (scrollTextureFile != null) {
            scrollTextureFile.dispose();
        }
        if (popupIconTexture != null) {
            popupIconTexture.dispose();
        }
    }

    /**
     * Draws the player's remaining lives using heart icons.
     *
     * @param batch  The SpriteBatch to draw with.
     * @param player The player containing life data.
     * @param x      The X coordinate for the start of the row.
     * @param y      The Y coordinate for the row.
     */
    private void renderLives(SpriteBatch batch, Player player, float x, float y) {
        int lives = player.getLives();
        int maxLives = player.getMaxLives();

        for (int i = 0; i < maxLives; i++) {
            float heartX = x + (i * HEART_SPACING);
            if (i < lives && heartTexture != null) {
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(heartTexture, heartX, y, HEART_SIZE, HEART_SIZE);
            } else {
                // Draw empty/dark heart for missing lives
                if (heartTexture != null) {
                    batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
                    batch.draw(heartTexture, heartX, y, HEART_SIZE, HEART_SIZE);
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f); // Reset color
    }

    /**
     * Draws the scroll collection status.
     * Changes color based on whether the player has collected scrolls or not.
     *
     * @param batch  The SpriteBatch to draw with.
     * @param player The player containing scroll inventory data.
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     */
    private void renderScrollStatus(SpriteBatch batch, Player player, float x, float y) {
        if (scrollTexture == null) return;

        int scrollCount = player.getScrollCount();
        int requiredScrolls = 3;

        if (scrollCount > 0) {
            // Parchment/beige color for collected scrolls
            batch.setColor(0.9f, 0.8f, 0.6f, 1f);
            batch.draw(scrollTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
            batch.setColor(Color.WHITE);

            boldFont.setColor(0.9f, 0.8f, 0.6f, 1f);
            boldFont.draw(batch, "SCROLLS: " + scrollCount + "/" + requiredScrolls, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
            boldFont.setColor(Color.WHITE);
        } else {
            batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
            batch.draw(scrollTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
            batch.setColor(Color.WHITE);

            font.setColor(0.5f, 0.5f, 0.5f, 0.8f);
            font.draw(batch, "SCROLLS: 0/" + requiredScrolls, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
            font.setColor(Color.WHITE);
        }
    }

    /**
     * Draws the key collection status (silver key).
     * Changes color based on whether the player has collected the key or not.
     *
     * @param batch  The SpriteBatch to draw with.
     * @param player The player containing key inventory data.
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     */
    private void renderKeyStatus(SpriteBatch batch, Player player, float x, float y) {
        if (keyTexture == null) return;

        int keyCount = player.getKeyCount();
        int requiredKeys = 1;

        if (keyCount > 0) {
            // Silver/white color for collected key
            batch.setColor(0.8f, 0.8f, 0.9f, 1f);
            batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
            batch.setColor(Color.WHITE);

            boldFont.setColor(0.8f, 0.8f, 0.9f, 1f);
            boldFont.draw(batch, "KEY: " + keyCount + "/" + requiredKeys, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
            boldFont.setColor(Color.WHITE);
        } else {
            batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
            batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE * 0.5f);
            batch.setColor(Color.WHITE);

            font.setColor(0.5f, 0.5f, 0.5f, 0.8f);
            font.draw(batch, "KEY: 0/" + requiredKeys, x + KEY_SIZE + 8, y + KEY_SIZE * 0.35f);
            font.setColor(Color.WHITE);
        }
    }


    /**
     * Draws the RPG skill points (Warrior, Swiftness, Vitality).
     *
     * @param batch The SpriteBatch to draw with.
     * @param state The GameState containing the point values.
     * @param x     The X coordinate.
     * @param y     The starting Y coordinate.
     */
    private void renderSkillPoints(SpriteBatch batch, GameState state, float x, float y) {
        boldFont.getData().setScale(0.7f);

        boldFont.setColor(Color.RED);
        boldFont.draw(batch, "WARRIOR PTS: " + state.warriorPoints, x, y);

        boldFont.setColor(Color.CYAN);
        boldFont.draw(batch, "SWIFTNESS PTS: " + state.swiftnessPoints, x, y - 25);

        boldFont.setColor(Color.GREEN);
        boldFont.draw(batch, "VITALITY PTS: " + state.vitalityPoints, x, y - 50);

        boldFont.getData().setScale(1.0f);
        boldFont.setColor(Color.WHITE);
    }

    /**
     * Renders the ghost mode countdown timer prominently in the center of the screen.
     *
     * @param batch        The SpriteBatch to draw with.
     * @param player       The player to get timer info from.
     * @param screenWidth  Width of the screen for centering.
     * @param screenHeight Height of the screen for positioning.
     */
    private void renderGhostModeTimer(SpriteBatch batch, Player player, float screenWidth, float screenHeight) {
        int secondsRemaining = (int) Math.ceil(player.getGhostModeTimer());

        // Change color based on remaining time
        Color timerColor;
        if (secondsRemaining <= 5) {
            // Red and flashing when time is running out
            float flash = (player.getGhostModeTimer() * 4) % 1.0f;
            timerColor = flash < 0.5f ? Color.RED : Color.ORANGE;
        } else if (secondsRemaining <= 10) {
            timerColor = Color.ORANGE;
        } else {
            timerColor = Color.CYAN;
        }

        boldFont.getData().setScale(1.5f);
        boldFont.setColor(timerColor);

        String timerText = "GHOST MODE: " + secondsRemaining + "s";
        float textX = screenWidth / 2f - 100;
        float textY = screenHeight - 80;

        boldFont.draw(batch, timerText, textX, textY);

        // Draw instruction below timer
        boldFont.getData().setScale(0.8f);
        boldFont.setColor(Color.WHITE);
        String instruction = "Find your voodoo doll to revive!";
        float instructionX = screenWidth / 2f - 120;
        float instructionY = textY - 30;
        boldFont.draw(batch, instruction, instructionX, instructionY);

        // Reset font scale and color
        boldFont.getData().setScale(1.0f);
        boldFont.setColor(Color.WHITE);
    }

    /**
     * Handles the animation logic and rendering for the achievement popup.
     *
     * @param batch        The SpriteBatch to draw with.
     * @param screenWidth  Width of the screen for centering.
     * @param screenHeight Height of the screen for offset calculations.
     */
    private void renderPopup(SpriteBatch batch, float screenWidth, float screenHeight) {
        popupTimer -= Gdx.graphics.getDeltaTime();
        if (popupTimer <= 0) {
            showPopup = false;
            return;
        }

        float yOffset;
        if (popupTimer > 2.5f) {
            yOffset = (3.0f - popupTimer) * 120;
        } else if (popupTimer < 0.5f) {
            yOffset = popupTimer * 120;
        } else {
            yOffset = 60;
        }

        float popupY = screenHeight - yOffset;

        // Draw icon if available
        float iconSize = 48f;
        float totalWidth = (popupIconTexture != null ? iconSize + 10 : 0) + boldFont.getSpaceXadvance() * popupText.length() * 0.5f;
        float popupX = screenWidth / 2f - totalWidth / 2f;

        if (popupIconTexture != null) {
            // Add glowing effect
            batch.setColor(1f, 1f, 0.8f, 1f);
            batch.draw(popupIconTexture, popupX, popupY - iconSize + 10, iconSize, iconSize);
            batch.setColor(Color.WHITE);
            popupX += iconSize + 10;
        }

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, popupText, popupX, popupY);
        boldFont.setColor(Color.WHITE);
    }
}