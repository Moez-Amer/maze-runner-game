package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
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

/**
 * Defeat screen displayed when the player's lives reach zero.
 * <p>
 * The screen presents the player's final score together with four
 * navigation options: retry the current map, visit the marketplace to
 * spend earned skill points, return to the main menu, or quit the
 * application.  Pressing ESC at any time acts as a shortcut back to
 * the menu.
 * </p>
 * <p>
 * A full-screen background image ({@code DefeatFinal.png}) is drawn
 * first, and the Scene2D {@link Stage} is layered on top so that the
 * UI elements remain centred and readable regardless of window size.
 * Game-over music is started in {@link #show} and the previous track
 * is stopped via {@link AudioManager}.
 * </p>
 * <p>
 * When the map path contains the token {@code "survival"} the retry
 * button routes the player back to Survival Mode via
 * {@link MazeRunnerGame#goToSurvival()}; otherwise it restarts the
 * standard story-mode level.
 * </p>
 */
public class GameOverScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final OrthographicCamera camera;
    private final String mapPath;
    private final Texture background;
    private final float bgZoom = 1.0f;

    /**
     * Constructs the Game Over screen and assembles the full UI layout.
     * <p>
     * All {@link Label} and {@link TextButton} actors are added to a
     * centred {@link Table} in a single column.  Each button is wired
     * to a {@link ChangeListener} that performs the appropriate
     * navigation or exits the application.
     * </p>
     *
     * @param game    Reference to the main {@link MazeRunnerGame} instance,
     *                used for navigation helpers and shared UI resources
     *                such as the skin.
     * @param mapPath The internal path of the map that was being played.
     *                Used by the retry button to reload the correct level;
     *                also inspected for the {@code "survival"} token to
     *                choose between story-mode and survival-mode restart.
     * @param score   The total points the player accumulated before dying.
     *                Displayed on the screen as a final-score label.
     */
    public GameOverScreen(MazeRunnerGame game, String mapPath, int score) {
        this.game = game;
        this.mapPath = mapPath;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        background = new Texture(Gdx.files.internal("DefeatFinal.png"));

        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("GAME OVER", game.getSkin(), "title");
        Label scoreLabel = new Label("Points Earned: " + score, game.getSkin());
        Label defeatLabel = new Label("You Died...", game.getSkin());

        TextButton retryButton = new TextButton("Try Again", game.getSkin());
        TextButton marketButton = new TextButton("Visit Marketplace", game.getSkin());
        TextButton menuButton = new TextButton("Main Menu", game.getSkin());
        TextButton quitButton = new TextButton("Quit Game", game.getSkin());

        retryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                if(mapPath.contains("survival")){
                    game.goToSurvival();
                }else {
                    game.goToGame(mapPath);
                }
            }
        });

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

        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        // UI Layout
        table.add(titleLabel).padBottom(20).row();
        table.add(scoreLabel).padBottom(20).row();
        table.add(defeatLabel).padBottom(60).row();
        table.add(retryButton).width(300).padBottom(15).row();
        table.add(marketButton).width(300).padBottom(15).row();
        table.add(menuButton).width(300).padBottom(15).row();
        table.add(quitButton).width(300);
    }

    /**
     * Activates this screen by stopping any currently playing music,
     * starting the game-over audio track, and registering the
     * {@link Stage} as the active input processor so that button
     * clicks are received.
     */
    @Override
    public void show() {
        AudioManager.stopMusic();
        AudioManager.playGameOverMusic();
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders one frame of the Game Over screen.
     * <p>
     * The background image is drawn first at the current zoom level,
     * centred on the window.  The Scene2D stage is then updated and
     * drawn on top.  An ESC key press at any point navigates back to
     * the main menu as a global shortcut.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        Gdx.gl.glClearColor(0.5f, 0.1f, 0.1f, 1);
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

    /** No-op; no per-frame state needs to be suspended. */
    @Override
    public void pause() {}

    /** No-op; no per-frame state needs to be resumed. */
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
     * Releases the Scene2D {@link Stage} and all actors it owns.
     * The background {@link Texture} is managed by the asset manager
     * and is therefore not disposed here.
     */
    @Override
    public void dispose() {
        stage.dispose();
    }
}