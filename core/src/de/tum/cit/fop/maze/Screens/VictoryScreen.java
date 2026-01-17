package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.AudioManager;
import de.tum.cit.fop.maze.MazeRunnerGame;

import java.util.List;

/**
 * Victory screen displayed when the player successfully escapes the maze.
 * Shows congratulatory message, score, and any unlocked achievements.
 */
public class VictoryScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final OrthographicCamera camera;

    /**
     * Constructs the victory screen with UI elements.
     *
     * @param game Reference to the main game instance
     * @param score The final score achieved by the player
     */
    public VictoryScreen(MazeRunnerGame game, int score) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Labels for Victory and Score
        Label titleLabel = new Label("VICTORY!", game.getSkin(), "title");
        Label congratsLabel = new Label("You have escaped the maze!", game.getSkin());
        Label scoreLabel = new Label("Final Score: " + score, game.getSkin());

        // Buttons for Navigation
        TextButton marketButton = new TextButton("Visit Marketplace", game.getSkin());
        TextButton menuButton = new TextButton("Main Menu", game.getSkin());

        marketButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMarketplace();
            }
        });
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        // --- Build UI Table ---
        table.add(titleLabel).padBottom(10).row();
        table.add(scoreLabel).padBottom(20).row();
        table.add(congratsLabel).padBottom(30).row();

        // ACHIEVEMENT SECTION: Dynamic Check
        List<String> unlocked = game.getGameState().unlockedAchievements;
        if (!unlocked.isEmpty()) {
            table.add(new Label("ACHIEVEMENTS UNLOCKED", game.getSkin(), "bold")).padBottom(10).row();

            Table achTable = new Table();
            for (String achievementId : unlocked) {
                // Convert IDs like "WIND_RUNNER" to "Wind runner" for better display
                String displayName = achievementId.replace("_", " ").toLowerCase();
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

                Label achLabel = new Label("★ " + displayName, game.getSkin());
                achLabel.setColor(Color.GOLD); // Distinctive color for rewards
                achTable.add(achLabel).padBottom(5).row();
            }
            table.add(achTable).padBottom(30).row();
        }

        table.add(marketButton).width(300).padBottom(20).row();
        table.add(menuButton).width(300).row();
    }

    @Override
    public void show() {
        AudioManager.stopMusic();
        AudioManager.playVictorySound();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Dark green theme for victory
        Gdx.gl.glClearColor(0.1f, 0.4f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}