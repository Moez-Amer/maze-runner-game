package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import de.tum.cit.fop.maze.MazeRunnerGame;

public class SelectMapScreen implements Screen {
private final MazeRunnerGame game;
private final Stage stage;
    public SelectMapScreen(MazeRunnerGame game) {

        this.game = game;
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        this.stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.add(new Label("Select Level", game.getSkin(),"title")).
                padTop(80)
                .row();

        addButton(table, "LEVEL 1", () -> {
            game.goToGame("maps/level-1.properties");
        }, 0.1f);
        addButton(table, "LEVEL 2",()-> {
            game.goToGame("maps/level-2.properties");
        }, 0.2F);
        addButton(table, "LEVEL 3",()-> {
            game.goToGame("maps/level-3.properties");
        }, 0.3F);
        addButton(table, "LEVEL 4",()-> {
            game.goToGame("maps/level-4.properties");
        }, 0.4F);
        addButton(table, "LEVEL 5",()-> {
            game.goToGame("maps/level-5.properties");
        }, 0.5F);
        addButton(table, "Go Back",()-> {
            game.goToMenu();
        }, 0.6F);


    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)); // Update the stage
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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

    @Override
    public void dispose() {
        stage.dispose();

    }
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
