package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.Achievement;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Screen that displays the list of all achievements in the game.
 * <p>
 * This screen shows both locked and unlocked achievements with the following features:
 * <ul>
 *   <li>Unlocked achievements are displayed with gold text and full-brightness icons</li>
 *   <li>Locked achievements show progress bars and dimmed icons</li>
 *   <li>Scrollable list with custom-styled scrollbar for easy navigation</li>
 *   <li>Mouse wheel scrolling support without requiring initial click</li>
 * </ul>
 *
 * @author TUM Chair of Information Technology
 * @version 1.0
 * @since 2024
 */
public class AchievementScreen implements Screen {
    /** The stage that contains all UI actors for this screen. */
    private final Stage stage;

    /** Reference to the main game instance. */
    private final MazeRunnerGame game;

    /** Texture for the semi-transparent background of achievement rows. */
    private Texture rowBgTexture;

    /** Drawable wrapper for the row background texture. */
    private Drawable rowBackground;

    /** Texture for the scrollbar background track. */
    private Texture scrollBarTexture;

    /** Texture for the scrollbar draggable knob. */
    private Texture scrollKnobTexture;

    /**
     * Constructs a new AchievementScreen.
     * <p>
     * Initializes the stage with a screen viewport and creates custom background
     * and scrollbar textures. The UI is then built to display all achievements.
     *
     * @param game the main game instance used to access game state and UI skin
     */
    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        // Generate a 1x1 semi-transparent black pixel for the background
        createBackground();

        rebuildUI();
    }

    /**
     * Creates custom textures for UI backgrounds and scrollbar components.
     * <p>
     * This method programmatically generates:
     * <ul>
     *   <li>A semi-transparent black background for achievement rows (50% opacity)</li>
     *   <li>A 20px wide dark gray scrollbar background track (80% opacity)</li>
     *   <li>An 18px wide lighter gray scrollbar knob (90% opacity)</li>
     * </ul>
     * These custom textures ensure proper rendering and avoid missing drawable crashes.
     */
    private void createBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.5f); // 50% opacity black
        pixmap.fill();
        rowBgTexture = new Texture(pixmap);
        pixmap.dispose();
        rowBackground = new TextureRegionDrawable(new TextureRegion(rowBgTexture));

        // Create wider scrollbar background (20px wide)
        Pixmap scrollBarPixmap = new Pixmap(20, 1, Pixmap.Format.RGBA8888);
        scrollBarPixmap.setColor(0.3f, 0.3f, 0.3f, 0.8f);
        scrollBarPixmap.fill();
        scrollBarTexture = new Texture(scrollBarPixmap);
        scrollBarPixmap.dispose();

        // Create wider scroll knob (18px wide)
        Pixmap scrollKnobPixmap = new Pixmap(18, 1, Pixmap.Format.RGBA8888);
        scrollKnobPixmap.setColor(0.6f, 0.6f, 0.6f, 0.9f);
        scrollKnobPixmap.fill();
        scrollKnobTexture = new Texture(scrollKnobPixmap);
        scrollKnobPixmap.dispose();
    }

    /**
     * Builds the complete UI for the achievement screen.
     * <p>
     * This method creates a scrollable list of all achievements, displaying:
     * <ul>
     *   <li>Achievement icon (64x64 pixels, dimmed if locked)</li>
     *   <li>Achievement name (gold if unlocked, gray if locked)</li>
     *   <li>Achievement description</li>
     *   <li>Progress bar and counter for locked achievements</li>
     * </ul>
     * The scroll pane is configured with:
     * <ul>
     *   <li>Custom wider scrollbar (20px) for better visibility</li>
     *   <li>Automatic scroll focus for immediate mouse wheel support</li>
     *   <li>Smooth scrolling and flick scroll enabled</li>
     * </ul>
     * A "Back to Menu" button is added at the bottom.
     */
    private void rebuildUI() {
        stage.clear();
        Gdx.input.setInputProcessor(stage);
        GameState state = game.getGameState();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label("ACHIEVEMENTS", game.getSkin(), "title")).padBottom(40).row();

        Table listTable = new Table();
        listTable.top();

        // Retrieve all achievement definitions
        java.util.List<Achievement> allAchievements = GameState.getAchievementManager().getAllAchievements();

        for (Achievement ach : allAchievements) {
            boolean isUnlocked = state.unlockedAchievements.contains(ach.id);

            // Determine current progress for this specific achievement
            float currentVal = 0;
            if ("enemiesKilled".equals(ach.statName)) currentVal = state.enemiesKilledCounter;
            else if ("distanceSprinted".equals(ach.statName)) currentVal = state.distanceSprintedCounter;
            else if ("heartsCollected".equals(ach.statName)) currentVal = state.heartsCollectedCounter;
            else if ("tilesExplored".equals(ach.statName)) currentVal = state.tilesExploredCounter;
            else if ("keysCollected".equals(ach.statName)) currentVal = state.keysCollectedCounter;
            else if ("coinsCollected".equals(ach.statName)) currentVal = state.coinsCollectedCounter;
            else if ("potionsUsed".equals(ach.statName)) currentVal = state.potionsUsedCounter;
            else if ("perfectMazes".equals(ach.statName)) currentVal = state.perfectMazesCounter;
            else if ("mazesCompleted".equals(ach.statName)) currentVal = state.mazesCompletedCounter;

            // --- Row Container ---
            Table row = new Table();
            // Use our custom generated background
            row.setBackground(rowBackground);

            // --- Icon + Name Row ---
            Table headerRow = new Table();
            headerRow.left();

            // --- 0. Achievement Icon ---
            if (ach.iconPath != null && !ach.iconPath.isEmpty()) {
                try {
                    Texture iconTexture = new Texture(Gdx.files.internal(ach.iconPath));
                    Image icon = new Image(iconTexture);
                    icon.setSize(64, 64);

                    if (!isUnlocked) {
                        // Dim locked icons
                        icon.setColor(0.5f, 0.5f, 0.5f, 0.7f);
                    }

                    headerRow.add(icon).size(64, 64).padRight(15);
                } catch (Exception e) {
                    System.err.println("Failed to load achievement icon: " + ach.iconPath);
                }
            }

            // --- 1. Name & Status ---
            Label nameLabel = new Label(ach.name, game.getSkin(), "bold");
            if (isUnlocked) {
                nameLabel.setColor(Color.GOLD);
            } else {
                nameLabel.setColor(Color.GRAY);
            }
            headerRow.add(nameLabel).left().expandX();

            row.add(headerRow).left().expandX().pad(15, 15, 5, 15).row();

            // --- 2. Description ---
            Label descLabel = new Label(ach.description, game.getSkin());
            descLabel.setFontScale(0.85f);
            descLabel.setColor(Color.LIGHT_GRAY);
            descLabel.setWrap(true);
            row.add(descLabel).left().width(700).pad(0, 15, 10, 15).row();

            // --- 3. Progress Bar (If Locked) ---
            if (!isUnlocked) {
                float progress = Math.min(currentVal, ach.targetValue);
                ProgressBar bar = new ProgressBar(0, ach.targetValue, 1, false, game.getSkin());
                bar.setValue(progress);
                row.add(bar).width(250).padTop(5).padBottom(5).row();

                Label progressLabel = new Label((int)progress + " / " + (int)ach.targetValue, game.getSkin());
                progressLabel.setFontScale(0.75f);
                progressLabel.setColor(Color.LIGHT_GRAY);
                row.add(progressLabel).padBottom(15).row();
            } else {
                // Add spacing for unlocked achievements to maintain consistent row height
                row.add().height(10).row();
            }

            listTable.add(row).width(750).padBottom(15).row();
        }

        ScrollPane scroll = new ScrollPane(listTable, game.getSkin());

        // Apply custom wider scrollbar style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle(scroll.getStyle());
        scrollStyle.vScrollKnob = new TextureRegionDrawable(new TextureRegion(scrollKnobTexture));
        scrollStyle.vScroll = new TextureRegionDrawable(new TextureRegion(scrollBarTexture));
        scroll.setStyle(scrollStyle);

        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setScrollbarsVisible(true);
        scroll.setVariableSizeKnobs(false);
        scroll.setSmoothScrolling(true);
        scroll.setFlickScroll(true);
        scroll.setScrollBarPositions(false, true);
        scroll.setForceScroll(false, true);
        scroll.setOverscroll(false, false);

        // Make scroll pane capture scroll events immediately
        scroll.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scroll);
            }
        });

        root.add(scroll).width(800).height(500).padBottom(30).row();

        // Set focus to scroll pane so mouse wheel works immediately
        stage.setScrollFocus(scroll);

        TextButton backButton = new TextButton("Back to Menu", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.goToMenu();
            }
        });
        root.add(backButton).width(300).height(60);
    }

    /**
     * Renders the achievement screen.
     * <p>
     * Clears the screen with a dark gray background and updates/draws all stage actors.
     *
     * @param delta the time in seconds since the last render call
     */
    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    /**
     * Called when this screen becomes the current screen.
     * <p>
     * Sets the input processor to the stage to handle user input events.
     */
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    /**
     * Called when the screen is resized.
     * <p>
     * Updates the stage's viewport to match the new screen dimensions.
     *
     * @param width the new screen width in pixels
     * @param height the new screen height in pixels
     */
    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    /**
     * Called when this screen is no longer the current screen.
     * <p>
     * Currently does nothing as no cleanup is needed when hiding.
     */
    @Override public void hide() {}
    /**
     * Called when the game is paused (typically on Android).
     * <p>
     * Currently does nothing as no pause-specific logic is needed.
     */
    @Override public void pause() {}
    /**
     * Called when the game is resumed from a paused state (typically on Android).
     * <p>
     * Currently does nothing as no resume-specific logic is needed.
     */
    @Override public void resume() {}

    /**
     * Disposes of all resources used by this screen.
     * <p>
     * Cleans up:
     * <ul>
     *   <li>The stage and all its actors</li>
     *   <li>Row background texture</li>
     *   <li>Scrollbar background texture</li>
     *   <li>Scrollbar knob texture</li>
     * </ul>
     * This method should be called when the screen is no longer needed to prevent memory leaks.
     */
    @Override public void dispose() {
        stage.dispose();
        // Clean up our custom textures
        if (rowBgTexture != null) rowBgTexture.dispose();
        if (scrollBarTexture != null) scrollBarTexture.dispose();
        if (scrollKnobTexture != null) scrollKnobTexture.dispose();
    }
}