package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Level-selection screen that lets the player choose which story-mode
 * map to play.
 * <p>
 * Each of the five levels is shown as a compact row containing the
 * level button on the left and the player's high score on the right.
 * Levels that have been completed at least once are highlighted in
 * green with a check-mark (✓) appended to the label; their recorded
 * high score is shown in gold.  Levels that have never been finished
 * appear in light grey with {@code "---"} as the score placeholder.
 * </p>
 */
public class SelectMapScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final Texture background;
    private final float bgZoom = 1.0f;

    /**
     * Constructs the SelectMapScreen and builds the level list.
     * <p>
     * The method iterates over the five known level paths, queries the
     * current {@link GameState} for completion status and high score,
     * and delegates to {@link #addCompactLevelRow} for each entry.  A
     * "Return to Menu" button is appended at the bottom via
     * {@link #addSmallMenuButton}.
     * </p>
     *
     * @param game The main {@link MazeRunnerGame} instance, used to
     *             access the game state, shared skin, and navigation
     *             helpers.
     */
    public SelectMapScreen(MazeRunnerGame game) {
        this.game = game;
        var camera = new OrthographicCamera();
        camera.zoom = 1.0f;
        background = new Texture(Gdx.files.internal("SelectLevelBG.jpg"));
        this.stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.add(new Label("SELECT LEVEL", game.getSkin(), "title")).padBottom(30).colspan(2).row();

        GameState state = game.getGameState();
        String[] mapPaths = {
                "maps/level-1.properties", "maps/level-2.properties",
                "maps/level-3.properties", "maps/level-4.properties",
                "maps/level-5.properties"
        };

        for (int i = 0; i < mapPaths.length; i++) {
            final String path = mapPaths[i];
            boolean completed = state.levelHighScores.containsKey(path);
            Integer highScore = state.levelHighScores.get(path);

            String scoreText = completed ? "" + highScore : "---";
            String btnText = "LVL " + (i + 1) + (completed ? " \u2713" : "");

            addCompactLevelRow(table, btnText, scoreText, () -> game.goToGame(path), 0.05f * i, completed);
        }

        addSmallMenuButton(table, "Return to Menu", game::goToMenu, 0.4f);
    }

    /**
     * Adds a single level row to the table containing a score label on
     * the left and a level button on the right.
     * <p>
     * When {@code done} is {@code true} the button is coloured green and
     * the score label is coloured gold to visually distinguish completed
     * levels from those that are still locked or unfinished.  Both
     * actors start fully transparent and fade in after {@code delay}
     * seconds so that the list cascades in progressively.
     * </p>
     *
     * @param table    The {@link Table} to which the row is appended.
     * @param btnTxt   The text displayed on the level button (e.g.
     *                 {@code "LVL 1 ✓"}).
     * @param scoreTxt The high-score string to display, or {@code "---"}
     *                 if the level has not been completed.
     * @param action   The {@link Runnable} executed when the button is
     *                 clicked; typically {@code () -> game.goToGame(path)}.
     * @param delay    Seconds to wait before the fade-in animation begins.
     * @param done     {@code true} if the level has been completed at
     *                 least once; controls colour highlighting.
     */
    private void addCompactLevelRow(Table table, String btnTxt, String scoreTxt, Runnable action, float delay, boolean done) {
        Label scoreLabel = new Label(scoreTxt, game.getSkin());
        TextButton button = new TextButton(btnTxt, game.getSkin());

        if (done) {
            button.setColor(Color.GREEN);
            scoreLabel.setColor(Color.GOLD);
        } else {
            button.setColor(Color.LIGHT_GRAY);
        }

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });

        button.getColor().a = 0;
        scoreLabel.getColor().a = 0;
        button.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.4f)));
        scoreLabel.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.4f)));

        table.add(scoreLabel).width(80).padRight(15).right();
        table.add(button).width(180).height(35).padBottom(6).left().row();
    }

    /**
     * Adds a wider navigation button (e.g. "Return to Menu") that spans
     * both columns of the level table.
     * <p>
     * The button starts fully transparent and fades in after {@code delay}
     * seconds.  No hover-scale effect is applied to keep the visual
     * weight lighter than the main menu buttons.
     * </p>
     *
     * @param table  The {@link Table} to which the button row is appended.
     * @param text   The label displayed on the button.
     * @param action The {@link Runnable} executed when the button is clicked.
     * @param delay  Seconds to wait before the fade-in animation begins.
     */
    private void addSmallMenuButton(Table table, String text, Runnable action, float delay) {
        TextButton button = new TextButton(text, game.getSkin());
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });

        button.getColor().a = 0;
        button.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.5f)));

        table.add(button).width(350).height(65).padTop(20).colspan(2).row();
    }

    /**
     * Registers the {@link Stage} as the active input processor so that
     * level buttons receive click events.
     */
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    /**
     * Renders one frame of the level-selection screen.
     * The background is drawn centred at the configured zoom, and the
     * Scene2D stage is rendered on top.
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override public void render(float delta) {
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
     * Updates the stage viewport when the window is resized.
     *
     * @param w The new window width in pixels.
     * @param h The new window height in pixels.
     */
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override public void hide() {}

    @Override public void pause() {}

    @Override public void resume() {}

    /**
     * Releases the Scene2D {@link Stage}.
     */
    @Override public void dispose() { stage.dispose(); }
}