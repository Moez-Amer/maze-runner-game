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
import de.tum.cit.fop.maze.AudioManager;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Victory screen displayed when the player successfully escapes the maze.
 * Shows congratulatory message and options to return to menu or quit.
 */
public class VictoryScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final OrthographicCamera camera;

    /**
     * Constructs the victory screen with UI elements.
     *
     * @param game Reference to the main game instance
     */
    public VictoryScreen(MazeRunnerGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("VICTORY!", game.getSkin(), "title");
        Label congratsLabel = new Label("You have escaped the maze!", game.getSkin());

        TextButton menuButton = new TextButton("Main Menu", game.getSkin());
        TextButton quitButton = new TextButton("Quit Game", game.getSkin());

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
        table.add(congratsLabel).padBottom(100).row();
        table.add(menuButton).width(300).padBottom(20).row();
        table.add(quitButton).width(300);
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

        Gdx.gl.glClearColor(0.1f, 0.5f, 0.1f, 1);
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