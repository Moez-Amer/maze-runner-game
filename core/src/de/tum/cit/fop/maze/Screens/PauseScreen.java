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
import de.tum.cit.fop.maze.MazeRunnerGame;

public class PauseScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final GameScreen currentGameScreen;
    private final ShapeRenderer shapeRenderer;
    private static final Color OVERLAY_COLOR= new Color(0,0,0,0.6f);

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
            game.setScreen(currentGameScreen);
    },0.1f);

    addButton(table,"Restart",()->{
        String currentMap = currentGameScreen.getMapPath();
        currentGameScreen.dispose();
        game.goToGame(currentMap);
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


    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {

        currentGameScreen.render(0);

        drawDarkOverlay();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)); // Update the stage
        stage.draw();

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(currentGameScreen);
        }
    }
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

    @Override
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
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
