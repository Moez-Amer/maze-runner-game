package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
    private final Texture background;
    private final float bgZoom = 1.0f;
    private final String currentMapPath;

    /**
     * Constructs the victory screen with UI elements.
     *
     * @param game Reference to the main game instance
     * @param score The final score achieved by the player
     */
    public VictoryScreen(MazeRunnerGame game, int score) {
        this(game, score, null);
    }

    /**
     * Enhanced constructor that tracks the current map path.
     *
     * @param game Reference to the main game instance
     * @param score The final score achieved by the player
     * @param currentMapPath The path of the map that was just completed (e.g., "maps/level-1.properties")
     */
    public VictoryScreen(MazeRunnerGame game, int score, String currentMapPath) {
        this.game = game;
        this.currentMapPath = currentMapPath;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        background = new Texture(Gdx.files.internal("VictoryFinal.png"));
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("VICTORY!", game.getSkin(), "title");
        Label congratsLabel = new Label("You have escaped the maze!", game.getSkin());
        Label scoreLabel = new Label("Final Score: " + score, game.getSkin());

        table.add(titleLabel).padBottom(10).row();
        table.add(scoreLabel).padBottom(20).row();
        table.add(congratsLabel).padBottom(20).row();

        List<String> unlocked = game.getGameState().unlockedAchievements;
        if (!unlocked.isEmpty()) {
            table.add(new Label("ACHIEVEMENTS UNLOCKED", game.getSkin(), "bold")).padBottom(10).row();

            Table achTable = new Table();
            for (String achievementId : unlocked) {
                String displayName = achievementId.replace("_", " ").toLowerCase();
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

                Label achLabel = new Label("★ " + displayName, game.getSkin());
                achLabel.setColor(Color.GOLD); // Distinctive color for rewards
                achTable.add(achLabel).padBottom(5).row();
            }
            table.add(achTable).padBottom(30).row();
        }

        String nextLevelPath = getNextLevelPath(currentMapPath);
        if (nextLevelPath != null) {
            TextButton nextLevelButton = new TextButton("Next Level →", game.getSkin());
            nextLevelButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.goToGame(nextLevelPath);
                }
            });
            nextLevelButton.setColor(Color.GREEN);
            table.add(nextLevelButton).width(300).padBottom(13).row();
        }

        TextButton marketButton = new TextButton("Visit Marketplace", game.getSkin());
        marketButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMarketplace();
            }
        });
        table.add(marketButton).width(300).padBottom(13).row();


        TextButton menuButton = new TextButton("Main Menu", game.getSkin());
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(menuButton).width(300).row();
    }

    /**
     * Determines the next level based on the current map path.
     *
     * @param currentPath The path of the current/completed level
     * @return The path to the next level, or null if there is no next level
     */
    private String getNextLevelPath(String currentPath) {
        if (currentPath == null) {
            return null;
        }

        // Level progression mapping
        if (currentPath.equals("maps/level-1.properties")) {
            return "maps/level-2.properties";
        } else if (currentPath.equals("maps/level-2.properties")) {
            return "maps/level-3.properties";
        } else if (currentPath.equals("maps/level-3.properties")) {
            return "maps/level-4.properties";
        } else if (currentPath.equals("maps/level-4.properties")) {
            return "maps/level-5.properties";
        } else if (currentPath.equals("maps/level-5.properties")) {
            return null;
        }
        return null;
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

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.getSpriteBatch().begin();
        float width = Gdx.graphics.getWidth() * bgZoom;
        float height = Gdx.graphics.getHeight() * bgZoom;
        float x = (Gdx.graphics.getWidth() - width) / 2;
        float y = (Gdx.graphics.getHeight() - height) / 2;
        game.getSpriteBatch().draw(background, x, y, width, height);
        game.getSpriteBatch().end();

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
        background.dispose();
    }
}