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
 * Screen that displays the list of all achievements.
 * Shows progress bars for locked achievements and gold text for unlocked ones.
 */
public class AchievementScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;

    // We create a custom background texture to avoid "Missing Drawable" crashes
    private Texture rowBgTexture;
    private Drawable rowBackground;

    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        // Generate a 1x1 semi-transparent black pixel for the background
        createBackground();

        rebuildUI();
    }

    /**
     * Creates a semi-transparent black background programmatically.
     */
    private void createBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.5f); // 50% opacity black
        pixmap.fill();
        rowBgTexture = new Texture(pixmap);
        pixmap.dispose();
        rowBackground = new TextureRegionDrawable(new TextureRegion(rowBgTexture));
    }

    /**
     * Builds the scrollable list of achievements.
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

            // --- Row Container ---
            Table row = new Table();
            // Use our custom generated background
            row.setBackground(rowBackground);

            // --- 1. Name & Status ---
            Label nameLabel = new Label(ach.name, game.getSkin(), "bold");
            if (isUnlocked) {
                nameLabel.setColor(Color.GOLD);
                nameLabel.setText(ach.name + " [UNLOCKED]");
            } else {
                nameLabel.setColor(Color.GRAY);
            }
            row.add(nameLabel).left().expandX().pad(10).row();

            // --- 2. Description ---
            Label descLabel = new Label(ach.description, game.getSkin());
            descLabel.setFontScale(0.8f);
            descLabel.setColor(Color.LIGHT_GRAY);
            row.add(descLabel).left().pad(0, 10, 10, 10).row();

            // --- 3. Progress Bar (If Locked) ---
            if (!isUnlocked) {
                float progress = Math.min(currentVal, ach.targetValue);
                ProgressBar bar = new ProgressBar(0, ach.targetValue, 1, false, game.getSkin());
                bar.setValue(progress);
                row.add(bar).width(400).padBottom(5).row();

                Label progressLabel = new Label((int)progress + " / " + (int)ach.targetValue, game.getSkin());
                progressLabel.setFontScale(0.7f);
                row.add(progressLabel).padBottom(10).row();
            }

            listTable.add(row).width(600).padBottom(20).row();
        }

        ScrollPane scroll = new ScrollPane(listTable, game.getSkin());
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(650).height(400).padBottom(30).row();

        TextButton backButton = new TextButton("Back to Menu", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.goToMenu();
            }
        });
        root.add(backButton).width(300).height(60);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override public void dispose() {
        stage.dispose();
        // Clean up our custom texture
        if (rowBgTexture != null) rowBgTexture.dispose();
    }
}