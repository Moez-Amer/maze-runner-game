package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hall-of-Fame screen that ranks all local player profiles by their
 * best Survival Mode performance.
 * <p>
 * On construction the screen queries {@link SaveManager} for every
 * saved profile, extracts each player's highest survival score, the
 * wave they reached, and the longest time they survived, and sorts
 * the results in descending score order.  Only the top ten entries
 * are displayed.  Profiles that have never completed a survival run
 * (score &le; 0) are excluded from the ranking.
 * </p>
 * <p>
 * The table fades in over one second using a Scene2D
 * {@link Actions#fadeIn(float)} action.  A single "Back" button at
 * the bottom returns the player to the main menu.
 * </p>
 */
public class LeaderboardScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture background;
    private final float bgZoom = 1.0f;

    /**
     * Constructs the LeaderboardScreen and populates the Hall of Fame
     * table immediately so that it is ready to render on the first frame.
     *
     * @param game The main {@link MazeRunnerGame} instance, used to access
     *             the shared skin for label and button styling and to
     *             provide the navigation helper {@link MazeRunnerGame#goToMenu}.
     */
    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        background = new Texture(Gdx.files.internal("Leadership2.png"));
        // Reuse game's SpriteBatch for rendering efficiency
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        rebuildUI();
    }

    /**
     * Clears the stage and reconstructs the entire leaderboard UI.
     * <p>
     * The method performs four steps in order:
     * <ol>
     *   <li>All existing actors are removed from the {@link Stage}.</li>
     *   <li>Every local profile is loaded via {@link SaveManager} and
     *       those with a positive survival score are collected into a
     *       list of {@link PlayerScore} records.</li>
     *   <li>The list is sorted in descending score order.</li>
     *   <li>A {@link Table} is populated with column headers and up to
     *       ten data rows, then a fade-in action is applied.</li>
     * </ol>
     * Calling this method while the screen is visible will instantly
     * refresh the leaderboard with the latest saved data.
     * </p>
     */
    private void rebuildUI() {
        stage.clear();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);
        table.getColor().a = 0;
        table.addAction(Actions.fadeIn(1.0f));

        // Title Row
        table.add(new Label("HALL OF FAME", game.getSkin(), "title")).colspan(4).padBottom(20).row();

        // Data Loading and Sorting
        List<PlayerScore> scores = new ArrayList<>();
        // Iterate through all found profile names in local storage
        for (String name : SaveManager.getAllProfileNames()) {
            GameState gs = SaveManager.loadProfile(name);
            // Only include players who have established a survival record
            if (gs.getSurvivalBestScore() > 0) {
                scores.add(new PlayerScore(
                        name,
                        gs.getSurvivalBestScore(),
                        gs.getSurvivalBestWave(),
                        gs.getSurvivalLongestTime()
                ));
            }
        }

        // Sort descending: Highest score at the top
        Collections.sort(scores, (a, b) -> b.score - a.score);

        // Display Score Table Headers
        Table scoreTable = new Table();
        scoreTable.add(new Label("PLAYER", game.getSkin(), "bold")).padRight(40).left();
        scoreTable.add(new Label("SCORE", game.getSkin(), "bold")).padRight(40).right();
        scoreTable.add(new Label("WAVE", game.getSkin(), "bold")).padRight(40).right();
        scoreTable.add(new Label("TIME", game.getSkin(), "bold")).right().row();

        // Populate Table with Top 10
        int rank = 1;
        for (PlayerScore ps : scores) {
            scoreTable.add(new Label(rank + ". " + ps.name, game.getSkin())).padRight(40).left();
            scoreTable.add(new Label("" + ps.score, game.getSkin())).padRight(40).right();
            scoreTable.add(new Label("" + ps.wave, game.getSkin())).padRight(40).right();
            scoreTable.add(new Label(ps.time + "s", game.getSkin())).right().row();

            if (++rank > 10) break;
        }

        // Add the scrollable/list table to the main layout
        table.add(scoreTable).colspan(4).padBottom(40).row();

        // Back Button to return to Menu
        TextButton back = new TextButton("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.goToMenu();
            }
        });

        table.add(back).colspan(4).width(200);
    }

    /**
     * Immutable value object that bundles a single player's best
     * Survival Mode statistics for sorting and display.
     */
    private static class PlayerScore {
        /** The player's profile name as stored on disk. */
        String name;
        /** The highest score the player has achieved in any survival run. */
        int score;
        /** The highest wave the player has reached in any survival run. */
        int wave;
        /** The longest time (in seconds) the player has survived in any run. */
        int time;

        /**
         * Constructs a PlayerScore record.
         *
         * @param n Player profile name.
         * @param s Highest survival score reached.
         * @param w Highest wave reached.
         * @param t Longest time survived in seconds.
         */
        PlayerScore(String n, int s, int w, int t) {
            this.name = n;
            this.score = s;
            this.wave = w;
            this.time = t;
        }
    }

    /**
     * Renders one frame of the leaderboard screen.
     * The background image is drawn first at the configured zoom, centred
     * on the window, and then the Scene2D stage is updated and drawn on top.
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.getSpriteBatch().begin();
        float width = Gdx.graphics.getWidth() * bgZoom;
        float height = Gdx.graphics.getHeight() * bgZoom;
        float x = (Gdx.graphics.getWidth() - width) / 2;
        float y = (Gdx.graphics.getHeight() - height) / 2;
        game.getSpriteBatch().draw(background, x, y, width, height);
        game.getSpriteBatch().end();
        stage.act(delta);
        stage.draw();
    }

    /**
     * Registers the {@link Stage} as the active input processor so that
     * the "Back" button and any future interactive elements receive
     * touch and mouse events.
     */
    @Override
    public void show() {
        // Required to enable mouse/touch interaction
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Updates the stage viewport when the window is resized so that
     * the centred table layout scales correctly.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Clears the global input processor when this screen is hidden so
     * that input events are not routed to a disposed stage.
     */
    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    /**
     * Releases the Scene2D {@link Stage} and all actors and actions
     * it owns.
     */
    @Override
    public void dispose() {
        stage.dispose();
    }

    /** No-op; no per-frame state needs to be suspended. */
    @Override public void pause() {}

    /** No-op; no per-frame state needs to be resumed. */
    @Override public void resume() {}
}