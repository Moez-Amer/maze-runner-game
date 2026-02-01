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
 * Congratulatory screen shown when the player successfully reaches
 * the exit tile and completes a story-mode level.
 * <p>
 * The screen displays the player's final score and, when present, a
 * list of achievements that were unlocked during the run.  A
 * "Next Level" button appears automatically when a subsequent level
 * exists in the progression sequence; on the final level it is omitted.
 * Additional buttons let the player visit the marketplace or return to
 * the main menu.  Pressing ESC at any time navigates back to the menu.
 * </p>
 * <p>
 * The victory sound effect is started in {@link #show} after the
 * previous music track is stopped via {@link AudioManager}.
 * </p>
 */
public class VictoryScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final OrthographicCamera camera;
    private final Texture background;
    private final float bgZoom = 1.0f;
    private final String currentMapPath;


    /**
     * Constructs the VictoryScreen with full level-progression support.
     * <p>
     * The UI layout is assembled top-to-bottom: title, score, congratulations
     * message, an optional achievement list (rendered only when the
     * {@link de.tum.cit.fop.maze.GameState} contains unlocked
     * achievements), an optional "Next Level" button (rendered only when
     * {@link #getNextLevelPath} returns a non-null value), a marketplace
     * button, and a main-menu button.
     * </p>
     *
     * @param game           The main {@link MazeRunnerGame} instance, used
     *                       for navigation and the shared UI skin.
     * @param score          The total points the player accumulated before
     *                       reaching the exit.
     * @param currentMapPath The internal asset path of the map that was
     *                       just completed (e.g.
     *                       {@code "maps/level-1.properties"}), or
     *                       {@code null} if unknown.
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
                achLabel.setColor(Color.GOLD);
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
     * Derives the next level's map path from the current level's path
     * using a hard-coded five-level progression sequence.
     * <p>
     * The mapping is linear: level <em>N</em> leads to level
     * <em>N+1</em> for N &isin; {1, 2, 3, 4}.  Level 5 is the final
     * level and returns {@code null} so that no "Next Level" button is
     * shown.  A {@code null} input also returns {@code null}.
     * </p>
     *
     * @param currentPath The internal asset path of the level that was
     *                    just completed, or {@code null}.
     * @return The asset path of the next level, or {@code null} if there
     *         is no subsequent level or the input was {@code null}.
     */
    private String getNextLevelPath(String currentPath) {
        if (currentPath == null) {
            return null;
        }

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

    /**
     * Stops any currently playing music and starts the victory sound
     * effect.  Also (re-)registers the {@link Stage} as the active
     * input processor so that buttons receive events even if the screen
     * was shown programmatically after another processor was active.
     */
    @Override
    public void show() {
        AudioManager.stopMusic();
        AudioManager.playVictorySound();
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders one frame of the victory screen.
     * <p>
     * The background is drawn centred at the configured zoom and the
     * Scene2D stage is rendered on top.  An ESC key press at any point
     * navigates back to the main menu.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
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

    /**
     * Updates the stage viewport when the window is resized so that
     * the centred table layout remains correct.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    /**
     * Called when this screen is replaced by another.  Delegates
     * immediately to {@link #dispose} to free resources as soon as
     * the screen is no longer visible.
     */
    @Override
    public void hide() {
        dispose();
    }

    /**
     * Releases the Scene2D {@link Stage} and the background
     * {@link Texture}.
     */
    @Override
    public void dispose() {
        stage.dispose();
        background.dispose();
    }
}