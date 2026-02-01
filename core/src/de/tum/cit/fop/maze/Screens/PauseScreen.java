package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.AudioManager;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Overlay screen shown when the player pauses the game mid-play.
 * <p>
 * Rather than replacing the game visuals entirely, the {@link PauseScreen}
 * keeps a reference to the active {@link GameScreen} and re-renders it
 * every frame with a {@code delta} of {@code 0} so that the frozen
 * game world remains visible behind a semi-transparent dark overlay.
 * The overlay is drawn using a {@link ShapeRenderer} with alpha blending
 * enabled, and the pause menu's Scene2D {@link Stage} is layered on top.
 * </p>
 * <p>
 * Five actions are available: resuming play, restarting the current map
 * (with automatic survival-mode detection), opening settings, returning
 * to the main menu, or quitting the application.
 */
public class PauseScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final GameScreen currentGameScreen;
    private final ShapeRenderer shapeRenderer;
    private static final Color OVERLAY_COLOR= new Color(0,0,0,0.6f);

    /**
     * Constructs the PauseScreen and assembles the pause menu layout.
     * <p>
     * The {@link GameScreen} reference is retained so that it can be
     * re-rendered behind the overlay each frame and so that the Restart
     * button knows which map to reload.  A {@link ShapeRenderer} is
     * created for the overlay rectangle.
     * </p>
     *
     * @param game                The main {@link MazeRunnerGame} instance,
     *                            used for navigation and shared UI resources.
     * @param currentGameScreen   The {@link GameScreen} that was running
     *                            before the pause was triggered.  Must not
     *                            be {@code null}.
     */
    public PauseScreen( MazeRunnerGame game,GameScreen currentGameScreen) {
        this.game = game;
        this.currentGameScreen= currentGameScreen;
        this.shapeRenderer= new ShapeRenderer();

        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        stage= new Stage(new ScreenViewport(camera),game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label label = new Label("GAME PAUSED", game.getSkin(), "title");
        table.add(label).padTop(40).padBottom(80).row();

        addButton(table,"Resume",()->{
            AudioManager.playGameMusic();
            game.setScreen(currentGameScreen);
        },0.1f);

        addButton(table,"Restart",()->{
            String currentMap = currentGameScreen.getMapPath();
            currentGameScreen.dispose();
            if(currentMap.contains("survival")){
                game.goToSurvival();
            }else {
                game.goToGame(currentMap);
            }
        },0.2f);

        addButton(table, "Settings", () -> {
            game.goToSettings(this);
        }, 0.3f);

        addButton(table, "Return to Menu",()-> {
            currentGameScreen.dispose();
            game.goToMenu();
        }, 0.4f);

        addButton(table, "Quit Game", () -> {
            Gdx.app.exit();
        }, 0.5f);

    }

    /**
     * Registers the {@link Stage} as the active input processor so
     * that pause-menu buttons receive click events.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders one frame of the pause overlay.
     * <p>
     * The sequence each frame is:
     * <ol>
     *   <li>The underlying {@link GameScreen} is rendered with
     *       {@code delta = 0} so that the world is visible but does
     *       not advance.</li>
     *   <li>A semi-transparent black rectangle is drawn over the
     *       entire window via {@link #drawDarkOverlay}.</li>
     *   <li>The Scene2D stage (containing the pause buttons) is
     *       updated and drawn on top.</li>
     * </ol>
     * An ESC key press resumes the game immediately, mirroring the
     * behaviour of the Resume button.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {

        currentGameScreen.render(0);

        drawDarkOverlay();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            AudioManager.playGameMusic();
            game.setScreen(currentGameScreen);
        }
    }

    /**
     * Draws a full-window semi-transparent black rectangle using the
     * {@link ShapeRenderer}.
     * <p>
     * GL blending is enabled before the draw call and disabled
     * afterwards to avoid polluting the blend state for subsequent
     * rendering passes (e.g. the Scene2D stage).
     * </p>
     */
    private void drawDarkOverlay() {

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(OVERLAY_COLOR);
        shapeRenderer.rect(
                0,0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

    }

    /**
     * Updates both the pause-menu stage viewport and the underlying
     * {@link GameScreen} viewport when the window is resized.  The
     * {@link ShapeRenderer}'s projection matrix is also rebuilt so
     * that the overlay rectangle covers the new window dimensions.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        currentGameScreen.resize(width, height);
        shapeRenderer.setProjectionMatrix(
                shapeRenderer.getProjectionMatrix()
                        .setToOrtho2D(0, 0, width, height)
        );
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    /**
     * Releases the Scene2D {@link Stage} and the {@link ShapeRenderer}.
     * The underlying {@link GameScreen} is <em>not</em> disposed here;
     * that responsibility belongs to whichever button action decides
     * the game screen is no longer needed.
     */
    @Override
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
    }

    /**
     * Creates a pause-menu button with hover-scale and staggered
     * entrance animations and adds it to the supplied table.
     * <p>
     * The animation sequence is identical to the one used by
     * { MenuScreen#addButton}: the button starts invisible and
     * 20 pixels below its target, waits {@code delay} seconds, then
     * fades in while sliding up with a quadratic ease-out.  A
     * {@link ClickListener} scales the button to 110 % on hover and
     * back to 100 % on exit.
     * </p>
     *
     * @param table  The {@link Table} to which the button row is appended.
     * @param text   The label displayed on the button.
     * @param action The {@link Runnable} executed when the button is clicked.
     * @param delay  Seconds to wait before the entrance animation begins.
     */
    private void addButton(Table table, String text, Runnable action, float delay) {
        TextButton button = new TextButton(text, game.getSkin());

        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                button.addAction(Actions.scaleTo(1.1f, 1.1f, 0.1f));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                button.addAction(Actions.scaleTo(1.0f, 1.0f, 0.1f));
            }
        });

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });

        button.setTransform(true);
        button.setOrigin(Align.center);
        button.getColor().a = 0f;
        button.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.parallel(
                        Actions.fadeIn(0.5f),
                        Actions.moveBy(0, 20, 0.5f, Interpolation.pow2Out)
                )
        ));

        button.moveBy(0, -20);

        table.add(button).width(500).height(80).padBottom(18).row();
    }
}