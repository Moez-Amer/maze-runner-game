package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The Heads-Up Display (HUD) for the Maze Runner game.
 * <p>
 * This class is responsible for rendering the player's status overlay, including
 * health (lives), collected keys, scrolls, score, and achievement notifications.
 * </p>
 */
public class HUD {

    private static final float HEART_SIZE = 40f;
    private static final float HEART_SPACING = 45f;
    private static final float KEY_SIZE = 40f;
    private static final float PADDING = 20f;
    private static final float POPUP_DURATION = 5.0f;

    private final TextureAtlas atlas;
    private final TextureRegion heartTexture;
    private final TextureRegion heartBgTexture;
    private final TextureRegion keyTexture;
    private final TextureRegion scrollTexture;
    private final TextureRegion scoreIconTexture;

    private final Texture heartTextureFile;
    private final Texture keyTextureFile;
    private final Texture scrollTextureFile;
    private final Texture scoreIconFile;

    private final BitmapFont font;
    private final BitmapFont boldFont;
    private final GlyphLayout layout;

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
        this.layout = new GlyphLayout();

        this.heartTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon12.png"));
        this.heartTexture = new TextureRegion(heartTextureFile);
        this.heartBgTexture = null;

        this.keyTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon41.png"));
        this.keyTexture = new TextureRegion(keyTextureFile);

        this.scrollTextureFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon43.png"));
        this.scrollTexture = new TextureRegion(scrollTextureFile);

        this.scoreIconFile = new Texture(Gdx.files.internal("free-undead-loot-pixel-art-icons/PNG/Transperent/Icon99.png"));
        this.scoreIconTexture = new TextureRegion(scoreIconFile);
    }

    /**
     * Renders the complete HUD overlay.
     * <p>
     * This method orchestrates the drawing of all HUD components: lives, keys,
     * scrolls, score, and any active achievement popups.
     * </p>
     *
     * @param batch  The SpriteBatch used to draw the 2D elements.
     * @param player The current player entity to retrieve stats from.
     */
    public void render(SpriteBatch batch, Player player) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float topBarY = screenHeight - PADDING - HEART_SIZE;

        renderLives(batch, player, PADDING, topBarY);

        float nextX = PADDING + (player.getMaxLives() * HEART_SPACING) + 40;
        renderKeyStatus(batch, player, nextX, topBarY);

        nextX += KEY_SIZE + 40;
        renderScrollStatus(batch, player, nextX, topBarY);

        String scoreValue = String.valueOf(player.getScore());
        layout.setText(boldFont, scoreValue);

        float iconSize = 40f;
        float spacing = 10f;
        float totalScoreWidth = iconSize + spacing + layout.width;
        float scoreStartX = screenWidth - totalScoreWidth - PADDING;

        if (scoreIconTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(scoreIconTexture, scoreStartX, topBarY, iconSize, iconSize);
        }

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, scoreValue, scoreStartX + iconSize + spacing, screenHeight - PADDING - 10);
        boldFont.setColor(Color.WHITE);

        if (player.isGhostMode()) {
            renderGhostModeTimer(batch, player, screenWidth, screenHeight);
        }

        if (showPopup) {
            renderPopup(batch, screenWidth, screenHeight);
        }
    }

    /**
     * Renders the survival mode HUD overlay.
     * <p>
     * Displays wave-specific information including:
     * - Wave number and enemies remaining
     * - Time survived
     * - Lives and score
     * - Difficulty multipliers (speed, health, score)
     * </p>
     *
     * @param batch       The SpriteBatch used to draw the 2D elements
     * @param player      The current player entity
     * @param waveManager The wave manager containing wave information
     * @param timeAlive   Total time survived in seconds
     */
    public void renderSurvival(SpriteBatch batch, Player player, WaveManager waveManager, float timeAlive) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float topBarY = screenHeight - PADDING - HEART_SIZE;

        renderLives(batch, player, PADDING, topBarY);

        float nextX = PADDING + (player.getMaxLives() * HEART_SPACING) + 40;
        String scoreValue = String.valueOf(player.getScore());
        layout.setText(boldFont, scoreValue);

        float iconSize = 40f;
        float spacing = 10f;
        float totalScoreWidth = iconSize + spacing + layout.width;
        float scoreStartX = screenWidth - totalScoreWidth - PADDING;

        if (scoreIconTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(scoreIconTexture, scoreStartX, topBarY, iconSize, iconSize);
        }

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, scoreValue, scoreStartX + iconSize + spacing, screenHeight - PADDING - 10);
        boldFont.setColor(Color.WHITE);

        if (player.isGhostMode()) {
            renderGhostModeTimer(batch, player, screenWidth, screenHeight);
        }

        if (showPopup) {
            renderPopup(batch, screenWidth, screenHeight);
        }


        font.setColor(Color.YELLOW);

        String waveText = "WAVE " + waveManager.getCurrentWave();
        GlyphLayout waveLayout = new GlyphLayout(font, waveText);
        font.draw(batch, waveText,
                screenWidth - waveLayout.width - 20,
                screenHeight - 80);

        String enemyText = "Enemies: " + waveManager.getEnemiesRemaining() + "/" + waveManager.getTotalEnemiesInWave();
        GlyphLayout enemyLayout = new GlyphLayout(font, enemyText);
        font.draw(batch, enemyText,
                screenWidth - enemyLayout.width - 20,
                screenHeight - 110);

        int minutes = (int) timeAlive / 60;
        int seconds = (int) timeAlive % 60;
        String timeText = String.format("Time: %d:%02d", minutes, seconds);
        GlyphLayout timeLayout = new GlyphLayout(font, timeText);
        font.draw(batch, timeText,
                screenWidth - timeLayout.width - 20,
                screenHeight - 140);

        font.setColor(Color.WHITE);
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
                if (heartTexture != null) {
                    batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
                    batch.draw(heartTexture, heartX, y, HEART_SIZE, HEART_SIZE);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws the key collection status.
     *
     * @param batch  The SpriteBatch to draw with.
     * @param player The player containing key inventory data.
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     */
    private void renderKeyStatus(SpriteBatch batch, Player player, float x, float y) {
        if (keyTexture == null) return;
        boolean hasKey = player.getKeyCount() > 0;

        if (hasKey) {
            batch.setColor(0.8f, 0.8f, 0.9f, 1f);
        } else {
            batch.setColor(0.2f, 0.2f, 0.2f, 0.4f);
        }

        batch.draw(keyTexture, x, y, KEY_SIZE, KEY_SIZE);
        batch.setColor(Color.WHITE);
    }

    /**
     * Draws the scroll collection status.
     *
     * @param batch  The SpriteBatch to draw with.
     * @param player The player containing scroll inventory data.
     * @param x      The X coordinate.
     * @param y      The Y coordinate.
     */
    private void renderScrollStatus(SpriteBatch batch, Player player, float x, float y) {
        if (scrollTexture == null) return;
        int scrollCount = player.getScrollCount();
        int maxScrolls = 3;

        for (int i = 0; i < maxScrolls; i++) {
            float scrollX = x + (i * (KEY_SIZE + 15));
            if (i < scrollCount) {
                batch.setColor(0.9f, 0.8f, 0.6f, 1f);
            } else {
                batch.setColor(0.2f, 0.2f, 0.2f, 0.4f);
            }
            batch.draw(scrollTexture, scrollX, y, KEY_SIZE, KEY_SIZE);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Renders the ghost mode countdown timer and instructions.
     *
     * @param batch        The SpriteBatch to draw with.
     * @param player       The player to get timer info from.
     * @param screenWidth  Width of the screen for centering.
     * @param screenHeight Height of the screen for positioning.
     */
    private void renderGhostModeTimer(SpriteBatch batch, Player player, float screenWidth, float screenHeight) {
        int secondsRemaining = (int) Math.ceil(player.getGhostModeTimer());

        Color timerColor;
        if (secondsRemaining <= 5) {
            float flash = (player.getGhostModeTimer() * 4) % 1.0f;
            timerColor = flash < 0.5f ? Color.RED : Color.ORANGE;
        } else if (secondsRemaining <= 10) {
            timerColor = Color.ORANGE;
        } else {
            timerColor = Color.CYAN;
        }

        boldFont.getData().setScale(1.5f);
        boldFont.setColor(timerColor);

        String timerText = secondsRemaining + "s";
        layout.setText(boldFont, timerText);

        float textX = (screenWidth - layout.width) / 2f;
        float textY = screenHeight - 100;

        boldFont.draw(batch, timerText, textX, textY);

        boldFont.getData().setScale(0.8f);
        boldFont.setColor(Color.WHITE);

        String instruction = "COLLECT VOODOO DOLL TO REVIVE!";
        layout.setText(boldFont, instruction);

        float instructionX = (screenWidth - layout.width) / 2f;
        float instructionY = textY - 60;

        boldFont.draw(batch, instruction, instructionX, instructionY);

        boldFont.getData().setScale(1.0f);
        boldFont.setColor(Color.WHITE);
    }

    /**
     * Triggers a visual popup notification for an unlocked achievement.
     *
     * @param achievement The achievement to display.
     */
    public void showAchievementPopup(Achievement achievement) {
        this.popupText = achievement.name;
        this.popupTimer = POPUP_DURATION;
        this.showPopup = true;

        if (achievement.iconPath != null && !achievement.iconPath.isEmpty()) {
            if (popupIconTexture != null && !achievement.iconPath.equals(currentIconPath)) {
                popupIconTexture.dispose();
                popupIconTexture = null;
            }
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

        float maxDrop = 140f;
        float yOffset;

        if (popupTimer > POPUP_DURATION - 0.5f) {
            float progress = (POPUP_DURATION - popupTimer) / 0.5f;
            yOffset = progress * maxDrop;
        } else if (popupTimer < 0.5f) {
            float progress = popupTimer / 0.5f;
            yOffset = progress * maxDrop;
        } else {
            yOffset = maxDrop;
        }

        float popupY = screenHeight - (maxDrop - yOffset);
        float drawBaseY = screenHeight - 20 - (maxDrop - yOffset);

        float iconSize = 64f;

        float centerX = screenWidth / 2f;

        if (popupIconTexture != null) {
            float iconX = centerX - (iconSize / 2f);
            float iconY = drawBaseY - iconSize;

            batch.setColor(1f, 1f, 0.8f, 1f);
            batch.draw(popupIconTexture, iconX, iconY, iconSize, iconSize);
            batch.setColor(Color.WHITE);
        }

        layout.setText(boldFont, popupText);
        float textX = centerX - (layout.width / 2f);
        float textY = drawBaseY - iconSize - 15;

        boldFont.setColor(Color.GOLD);
        boldFont.draw(batch, popupText, textX, textY);
        boldFont.setColor(Color.WHITE);
    }
    /**
     * Releases resources managed by this HUD.
     * Should be called when the game screen is destroyed.
     */
    public void dispose() {
        if (heartTextureFile != null) heartTextureFile.dispose();
        if (keyTextureFile != null) keyTextureFile.dispose();
        if (scrollTextureFile != null) scrollTextureFile.dispose();
        if (popupIconTexture != null) popupIconTexture.dispose();
        if (scoreIconFile != null) scoreIconFile.dispose();
    }
}