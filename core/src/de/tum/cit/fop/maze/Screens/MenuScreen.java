package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * The primary entry point screen presented to the player when the game starts.
 * <p>
 * All top-level navigation is offered here through a vertical stack of
 * animated buttons: starting a new game, selecting a level, entering
 * Survival Mode, visiting the marketplace, viewing the leaderboard,
 * checking achievements, watching the trailer, adjusting settings, and
 * exiting the application.
 * </p>

 */
public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture background;
    private final float bgZoom = 1.5f;

    /**
     * Constructs the MenuScreen and assembles the full button layout.
     * <p>
     * An {@link OrthographicCamera} and {@link ScreenViewport} are created
     * and passed to the {@link Stage}.  A centred {@link Table} is then
     * populated with a title label followed by one button per navigation
     * destination.  Each button is registered with a staggered delay so
     * that the entrance animations cascade down the list.
     * </p>
     *
     * @param game The main {@link MazeRunnerGame} instance, used to access
     *             the shared skin, {@link com.badlogic.gdx.graphics.g2d.SpriteBatch},
     *             and all navigation helper methods.
     */
    public MenuScreen(MazeRunnerGame game) {
        this.game = game;
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("BG.png"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.add(new Label("WIZARD HUNT", game.getSkin(), "title")).padBottom(80).row();

        addButton(table,"Start New Game", ()-> {
            game.goToGame("maps/level-1.properties");
        },0.1f);

        addButton(table,"Select Level", ()-> {
            game.goToSelectMap();
        },0.2f);

        addButton(table,"Survival Mode", ()-> {
            game.goToSurvival();
        },0.3f);

        addButton(table, "Marketplace (Shop)", () -> {
            game.goToMarketplace();
        }, 0.4f);

        addButton(table, "Leaderboard", () -> {
            game.goToLeaderboard();
        }, 0.5f);

        addButton(table, "Achievements", () -> {
            game.goToAchievements();
        }, 0.6f);

        addButton(table, "Watch Trailer", () -> {
            game.watchTrailer();
        }, 0.7f);

        addButton(table, "Settings", () -> {
            game.goToSettings(this);
        }, 0.8f);

        addButton(table,"Exit",()->{
            Gdx.app.exit();
        },0.9f);

    }

    /**
     * Renders one frame of the main menu.
     * <p>
     * The background is drawn first at the configured zoom, centred on
     * the window.  The Scene2D stage is then acted and drawn on top so
     * that button animations and hover effects are processed every frame.
     * The stage delta is capped at {@code 1/30} seconds to prevent
     * large jumps after focus loss from distorting entrance animations.
     * </p>
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
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    /**
     * Updates the stage viewport when the window is resized so that the
     * centred table layout remains correct at any resolution.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Releases the {@link Stage} and the background {@link Texture}.
     */
    @Override
    public void dispose() {
        stage.dispose();
        background.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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
     * Creates a menu button with hover-scale and staggered entrance
     * animations and adds it to the supplied table.
     * <p>
     * The button starts fully transparent and offset 20 pixels below its
     * final position.  After {@code delay} seconds a parallel action fades
     * it in and slides it upward using a quadratic-ease-out interpolation.
     * While the mouse (or touch) is over the button it scales to 110 %
     * around its centre; leaving the button returns it to 100 %.
     * </p>
     *
     * @param table  The {@link Table} to which the button row is appended.
     * @param text   The label displayed on the button.
     * @param action The {@link Runnable} executed when the button is clicked.
     * @param delay  Seconds to wait before the entrance animation begins,
     *               used to stagger multiple buttons.
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