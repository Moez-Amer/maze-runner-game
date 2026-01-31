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

public class SelectMapScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;
    private final Texture background;
    private final float bgZoom = 1.0f;

    public SelectMapScreen(MazeRunnerGame game) {
        this.game = game;
        var camera = new OrthographicCamera();
        // zoom = 1.0 means 1:1 scale. Increasing this makes the UI look smaller.
        camera.zoom = 1.0f;
        background = new Texture(Gdx.files.internal("SelectLevelBG.jpg"));
        this.stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
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
            // Uses a Checkmark (✓) for completion. Alternatively, use "★"
            String btnText = "LVL " + (i + 1) + (completed ? " \u2713" : "");

            // Using the COMPACT helper defined below
            addCompactLevelRow(table, btnText, scoreText, () -> game.goToGame(path), 0.05f * i, completed);
        }

        // Back Button
        addSmallMenuButton(table, "Return to Menu", game::goToMenu, 0.4f);
    }

    /**
     * THE FIX: This forces the level buttons to be very small (180x35).
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

        // Forced Fade-in Animation
        button.getColor().a = 0;
        scoreLabel.getColor().a = 0;
        button.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.4f)));
        scoreLabel.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.4f)));

        // SIZING: width(80) for score and width(180) for button
        table.add(scoreLabel).width(80).padRight(15).right();
        table.add(button).width(180).height(35).padBottom(6).left().row();
    }

    /**
     * Small version of the menu button (220x45).
     */
    private void addSmallMenuButton(Table table, String text, Runnable action, float delay) {
        TextButton button = new TextButton(text, game.getSkin());
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });

        button.getColor().a = 0;
        button.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.5f)));

        // Fixed sizing for the menu button
        table.add(button).width(350).height(65).padTop(20).colspan(2).row();
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
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
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); }
}