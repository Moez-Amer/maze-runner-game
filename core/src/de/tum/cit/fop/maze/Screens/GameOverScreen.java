package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Game over screen displayed when the player dies (lives reach 0).
 * Shows defeat message and options to retry, return to menu, or quit.
 */
public class GameOverScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final OrthographicCamera camera;
    private final String mapPath;

    /**
     * Constructs the game over screen with UI elements.
     *
     * @param game Reference to the main game instance
     */
    public GameOverScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.mapPath = mapPath;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("GAME OVER", game.getSkin(), "title");
        Label defeatLabel = new Label("You Died...", game.getSkin());

        TextButton retryButton = new TextButton("Try Again", game.getSkin());
        TextButton menuButton = new TextButton("Main Menu", game.getSkin());
        TextButton quitButton = new TextButton("Quit Game", game.getSkin());

        retryButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToGame(mapPath);
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

        table.add(titleLabel).padBottom(50).row();
        table.add(defeatLabel).padBottom(100).row();
        table.add(retryButton).width(300).padBottom(20).row();
        table.add(menuButton).width(300).padBottom(20).row();
        table.add(quitButton).width(300);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        Gdx.gl.glClearColor(0.5f, 0.1f, 0.1f, 1);
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