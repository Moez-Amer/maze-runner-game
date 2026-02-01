package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

/**
 * Profile-selection screen shown before the main menu when the game
 * needs to know which player is playing.
 * <p>
 * The screen is divided into two sections.  The upper section lets a
 * new player type a name into a {@link TextField} and click
 * "New Profile" to create a fresh save.  The lower section enumerates
 * every profile that already exists on disk (via
 * {@link SaveManager#getAllProfileNames}) and presents each one as a
 * clickable button so that returning players can resume their progress
 * without retyping their name.
 * </p>
 */
public class ProfileSelectionScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture background;
    /** Uniform scale applied to the background texture relative to the window size. */
    private final float bgZoom = 1.5f;

    /**
     * Constructs the ProfileSelectionScreen and assembles the full UI.
     * <p>
     * The layout is built inside a single centred {@link Table}:
     * <ol>
     *   <li>A title label ("WHO IS PLAYING?").</li>
     *   <li>A name {@link TextField} and a "New Profile"
     *       {@link TextButton} wired to create the profile when the
     *       field is non-empty.</li>
     *   <li>A "Previous Players" heading followed by one button per
     *       existing profile returned by {@link SaveManager}.</li>
     * </ol>
     * </p>
     *
     * @param game The main {@link MazeRunnerGame} instance, used to
     *             access the shared skin and the
     *             {@link MazeRunnerGame#setProfileAndContinue} navigation
     *             helper.
     */
    public ProfileSelectionScreen(MazeRunnerGame game) {
        this.game = game;

        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());

        Gdx.input.setInputProcessor(stage);

        background = new Texture(Gdx.files.internal("BG.png"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.add(new Label("WHO IS PLAYING?", game.getSkin(), "title")).padBottom(30).row();

        // 1. New Player Section
        final TextField nameField = new TextField("", game.getSkin());
        TextButton createButton = new TextButton("New Profile", game.getSkin());
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (!nameField.getText().isEmpty()) game.setProfileAndContinue(nameField.getText());
            }
        });

        table.add(new Label("Enter Name:", game.getSkin())).padBottom(5);
        table.row();
        table.add(nameField).width(300).padBottom(10).row();
        table.add(createButton).width(300).padBottom(40).row();

        // 2. Existing Players Section
        table.add(new Label("Previous Players:", game.getSkin())).padBottom(10).row();
        for (String name : SaveManager.getAllProfileNames()) {
            TextButton playerBtn = new TextButton(name, game.getSkin());
            playerBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    game.setProfileAndContinue(name);
                }
            });
            table.add(playerBtn).width(300).padBottom(5).row();
        }
    }

    /**
     * Renders one frame of the profile-selection screen.
     * The background is drawn centred at the configured zoom and the
     * Scene2D stage is rendered on top so that the text field and
     * buttons receive focus and display correctly.
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
     * Registers the {@link Stage} as the active input processor so that
     * the text field accepts typed characters and buttons receive clicks.
     */
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    /**
     * Updates the stage viewport when the window is resized.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /** No-op; no per-frame state needs to be suspended. */
    @Override public void pause() {}

    /** No-op; no per-frame state needs to be resumed. */
    @Override public void resume() {}

    /** No-op; cleanup is handled by {@link #dispose}. */
    @Override public void hide() {}

    /**
     * Releases the Scene2D {@link Stage} and the background
     * {@link Texture}.
     */
    @Override public void dispose() {
        stage.dispose();
        background.dispose();
    }
}