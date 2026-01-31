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

public class ProfileSelectionScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture background;
    private final float bgZoom = 1.5f;

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
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        background.dispose();
    }
}