package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.AudioManager;
import de.tum.cit.fop.maze.KeyBindings;
import de.tum.cit.fop.maze.MazeRunnerGame;
/**
 * Manages the settings menu for configuring key bindings and audio volumes.
 * Handles user input for key rebinding, conflict resolution, and saving preferences.
 */
public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Screen previousScreen;
    private final KeyBindings keys;

    private boolean isListening = false;
    private String listeningAction = null;
    private TextButton listeningButton = null;

    /**
     * Initializes the settings screen, camera, and stage.
     *
     * @param game The main game instance for asset access.
     * @param previousScreen The screen to return to when exiting settings.
     */
    public SettingsScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.keys = KeyBindings.getKeyBindings();
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());
        buildUI();
    }
    /**
     * Builds the main UI layout including controls, audio, and navigation buttons.
     */
    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label("SETTINGS", game.getSkin(), "title")).colspan(5).padBottom(30).row();


        root.add(buildKeyBindings()).top().padRight(200);
        root.add(buildAudio()).top().row();

        Table buttons = new Table();
        addButton(buttons, "Back",
                () -> game.setScreen(previousScreen),
                0.5f);

        addButton(buttons, "Save",
                () -> {
            keys.saveBindings();
            AudioManager.saveSettings();
        }, 0.6f);

        root.add(buttons).colspan(10).padTop(20);
    }

    /**
     * Creates the table containing all key binding rows and the reset button.
     *
     * @return The table with control settings.
     */
    private Table buildKeyBindings() {
        Table table = new Table();

        Label title = new Label("CONTROL", game.getSkin(), "title");
        title.setFontScale(0.6f);
        table.add(title).colspan(3).padBottom(60).row();

        table.add(new Label("Action", game.getSkin())).padRight(150).padBottom(50);
        table.add(new Label("Key", game.getSkin())).padLeft(280).padBottom(50);
        table.add();
        table.row();

        addKeyRow(table, "Move Up");
        addKeyRow(table, "Move Down");
        addKeyRow(table, "Move Left");
        addKeyRow(table, "Move Right");
        addKeyRow(table, "Attack");
        addKeyRow(table, "Sprint");
        addKeyRow(table, "Debug");
        addKeyRow(table, "Open Console");

        addButton(table, "Reset", () -> {
            keys.setDefaultBindings();
            stage.clear();
            buildUI();
        }, 0.4f);

        return table;
    }
    /**
     * Adds a configuration row for a specific game action to the table.
     *
     * @param table The target table.
     * @param action The name of the action to bind.
     */
    private void addKeyRow(Table table, String action) {
        table.add(new Label(action, game.getSkin())).left().padBottom(10);

        int keyCode = keys.getKey(action);
        Label keyLabel = new Label(keys.getKeyDisplayName(keyCode), game.getSkin());
        keyLabel.setColor(Color.ROYAL);
        table.add(keyLabel).center().padBottom(10);

        TextButton button = new TextButton("Change", game.getSkin());
        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                startListening(action, button);
            }
        });
        table.add(button).width(200).padBottom(10).row();
    }
    /**
     * Activates listening mode to capture the next key press for an action.
     *
     * @param action The action being rebound.
     * @param buttons The button that triggered the listening mode.
     */
    private void startListening(String action, TextButton buttons) {
        isListening = true;
        listeningAction = action;
        listeningButton = buttons;

        buttons.setText("Press key");
        buttons.setColor(Color.BLACK);

        Gdx.input.setInputProcessor(new InputProcessor() {
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    stopListening();
                    return true;
                }

                String conflict = keys.getActionForKey(keycode);
                if (conflict != null && !conflict.equals(action)) {
                    showConflict(keycode, conflict);
                } else {
                    keys.setKey(action, keycode);
                    stopListening();
                    stage.clear();
                    buildUI();
                }
                return true;
            }

            public boolean keyUp(int k) { return false; }
            public boolean keyTyped(char c) { return false; }
            public boolean touchDown(int x, int y, int p, int b) { return false; }
            public boolean touchUp(int x, int y, int p, int b) { return false; }
            public boolean touchDragged(int x, int y, int p) { return false; }
            public boolean mouseMoved(int x, int y) { return false; }
            public boolean scrolled(float x, float y) { return false; }
            public boolean touchCancelled(int x, int y, int p, int b) { return false; }
        });
    }
    /**
     * Deactivates listening mode and restores normal UI interaction.
     */
    private void stopListening() {
        if (listeningButton != null) {
            listeningButton.setText("Change");
            listeningButton.setColor(Color.WHITE);
        }
        isListening = false;
        listeningAction = null;
        listeningButton = null;
        Gdx.input.setInputProcessor(stage);
    }
    /**
     * Displays a dialog to resolve duplicate key bindings.
     *
     * @param keycode The key that caused the conflict.
     * @param conflict The name of the action currently using the key.
     */
    private void showConflict(int keycode, String conflict) {
        String keyName = keys.getKeyDisplayName(keycode);

        Dialog dialog = new Dialog("Conflict", game.getSkin()) {
            protected void result(Object obj) {
                if ((Boolean)obj) {
                    int oldKey = keys.getKey(listeningAction);
                    keys.setKey(listeningAction, keycode);
                    keys.setKey(conflict, oldKey);
                    stage.clear();
                    buildUI();
                }
                stopListening();
            }
        };

        dialog.text(keyName + " is used by " + conflict + "\n\nSwap keys?");
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(stage);
    }
    /**
     * Creates the table containing sliders for Main, Music, and SFX volumes.
     *
     * @return The table with audio settings.
     */
    private Table buildAudio() {
        Table table = new Table();

        Label title = new Label("AUDIO", game.getSkin(), "title");
        title.setFontScale(0.6f);
        table.add(title).colspan(3).padBottom(330).row();

        addSlider(table, "Main Volume:", AudioManager.getMainVolume(),
                AudioManager::setMainVolume);

        addSlider(table, "Music Volume:", AudioManager.getMusicVolume(),
                AudioManager::setMusicVolume);

        addSlider(table, "SFX Volume:", AudioManager.getSFXVolume(),
                AudioManager::setSFXVolume);

        return table;
    }
    /**
     * Adds a volume slider with a label and change listener to the table.
     *
     * @param table The target table.
     * @param label The text label for the slider.
     * @param current The current volume value (0.0 to 1.0).
     * @param onChange The callback to execute when value changes.
     */
    private void addSlider(Table table, String label, float current, VolumeChange onChange) {
        table.add(new Label(label, game.getSkin())).left().padRight(10);

        Slider slider = new Slider(0, 100, 1, false, game.getSkin());
        slider.setValue(current * 100);

        Label percent = new Label("100%", game.getSkin());
        percent.setText((int)slider.getValue() + "%");

        slider.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                onChange.set(slider.getValue() / 100f);
                percent.setText((int)slider.getValue() + "%");
            }
        });

        table.add(slider).width(200).padRight(10);
        table.add(percent).width(50).left().padBottom(15).row();
    }

    private interface VolumeChange {
        void set(float volume);
    }
    /**
     * Creates and adds an animated button with hover effects to the table.
     *
     * @param table The target table.
     * @param text The button text.
     * @param action The code to run when clicked.
     * @param delay The delay before the entrance animation plays.
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

        table.add(button).width(300).padTop(20).row();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(delta, 1/30f));
        stage.draw();

        if (!isListening && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(previousScreen);
        }
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
        if (isListening) stopListening();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}