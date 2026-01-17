package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

public class ProfileSelectionScreen implements Screen {
    private final Stage stage;

    public ProfileSelectionScreen(MazeRunnerGame game) {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

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

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
    }
    @Override public void show() {}
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}