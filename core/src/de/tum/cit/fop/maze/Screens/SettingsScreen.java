package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.AudioManager;
import de.tum.cit.fop.maze.KeyBindings;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Settings screen that presents two side-by-side panels: one for
 * keyboard control bindings and one for audio-volume sliders.
 * <p>
 * <b>Key-binding workflow</b> – each action row displays the current
 * key name inside a semi-transparent panel and a "Change" button.
 * Clicking "Change" enters <em>listening mode</em>: the global
 * {@link InputProcessor} is replaced with a minimal one that captures
 * the very next {@code keyDown} event.  If the captured key is already
 * bound to a different action a {@link Dialog} is shown offering to
 * swap the two bindings.  Pressing ESC while listening cancels the
 * operation and restores normal input.  A "Reset" button returns all
 * bindings to their defaults and rebuilds the entire UI.
 * </p>
 */
public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Screen previousScreen;
    private final KeyBindings keys;
    private final Texture background;
    private final Texture panelTexture;
    private final float bgZoom = 1.5f;
    private boolean isListening = false;
    private String listeningAction = null;
    private TextButton listeningButton = null;

    /**
     * Constructs the SettingsScreen, creates all shared textures, and
     * builds the initial UI layout.
     *
     * @param game           The main {@link MazeRunnerGame} instance, used
     *                       for the shared skin and {@link com.badlogic.gdx.graphics.g2d.SpriteBatch}.
     * @param previousScreen The {@link Screen} to return to when the
     *                       player clicks Back or presses ESC.
     */
    public SettingsScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.keys = KeyBindings.getKeyBindings();
        var camera = new OrthographicCamera();
        background = new Texture(Gdx.files.internal("SettingsBG.png"));
        camera.zoom = 1.5f;
        stage = new Stage(new ScreenViewport(camera), game.getSpriteBatch());
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.6f);
        pixmap.fill();
        panelTexture = new Texture(pixmap);
        pixmap.dispose();

        buildUI();
    }

    /**
     * Constructs the root {@link Table} and populates it with the
     * controls panel, audio panel, and navigation buttons.  This method
     * is also called after a successful key rebind or a reset so that
     * the UI always reflects the current bindings and volumes.
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
     * Creates the Controls panel containing one row per rebindable
     * action and a Reset button at the bottom.
     * <p>
     * Each row is built by {@link #addKeyRow}.  The Reset button
     * calls {@link KeyBindings#setDefaultBindings} and then invokes
     * {@link #buildUI} to refresh the entire screen.
     * </p>
     *
     * @return A {@link Table} ready to be inserted into the root layout.
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
     * Appends a single key-binding row to the Controls table.
     * <p>
     * The row contains three cells: the action name on the left, a
     * {@link Table} panel showing the current key name in royal-blue
     * text over the semi-transparent {@link #panelTexture} in the
     * centre, and a "Change" {@link TextButton} on the right.
     * Clicking "Change" delegates to {@link #startListening}.
     * </p>
     *
     * @param table  The Controls {@link Table} to which the row is appended.
     * @param action The human-readable action name (e.g. {@code "Move Up"}).
     *               This string is also the key used to look up and set
     *               the binding in {@link KeyBindings}.
     */
    private void addKeyRow(Table table, String action) {
        table.add(new Label(action, game.getSkin())).left().padBottom(10);

        int keyCode = keys.getKey(action);
        Label keyLabel = new Label(keys.getKeyDisplayName(keyCode), game.getSkin());
        keyLabel.setColor(Color.ROYAL);
        Table keyPanel = new Table();
        keyPanel.setBackground(new TextureRegionDrawable(new TextureRegion(panelTexture)));
        keyPanel.add(keyLabel).pad(4, 12, 4, 12);
        table.add(keyPanel).center().padBottom(10);

        TextButton button = new TextButton("Change", game.getSkin());
        button.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                startListening(action, button);
            }
        });
        table.add(button).width(200).padBottom(10).row();
    }

    /**
     * Enters listening mode for a key rebind.
     * <p>
     * The button label is changed to "Press key" and coloured black to
     * signal the active capture state.  The global {@link InputProcessor}
     * is replaced with a minimal implementation that intercepts the next
     * {@code keyDown} event.  If the key is already bound to another
     * action {@link #showConflict} is invoked; otherwise the binding is
     * applied immediately, listening mode is exited, and the UI is
     * rebuilt.  ESC during capture cancels via {@link #stopListening}.
     * </p>
     *
     * @param action  The action name being rebound.
     * @param buttons The "Change" {@link TextButton} that was clicked;
     *                its label is toggled to indicate the capture state.
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
     * Exits listening mode and restores normal Stage-based input.
     * The "Change" button label and colour are reset to their default
     * appearance, and all listening-state fields are cleared.
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
     * Shows a modal {@link Dialog} asking the player whether to swap
     * two conflicting key bindings.
     * <p>
     * If the player confirms, the key currently bound to {@code conflict}
     * is reassigned to the action that previously held it (i.e. the two
     * bindings are swapped).  In either case listening mode is exited
     * and, on confirmation, the UI is rebuilt to reflect the new state.
     * </p>
     *
     * @param keycode  The key code that caused the conflict.
     * @param conflict The name of the action that is currently using
     *                 {@code keycode}.
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
     * Creates the Audio panel containing sliders for Main, Music, and
     * SFX volumes.
     * <p>
     * Each slider is built by {@link #addSlider}, which reads the current
     * value from {@link AudioManager} and wires a {@link ChangeListener}
     * back to the corresponding setter.
     * </p>
     *
     * @return A {@link Table} ready to be inserted into the root layout.
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
     * Appends a labelled volume slider row to the Audio table.
     * <p>
     * The slider range is 0 – 100 (integer steps).  The {@code current}
     * value (0.0 – 1.0) is scaled to this range on initialisation.  A
     * percentage {@link Label} beside the slider is updated live via a
     * {@link ChangeListener} that also forwards the normalised value to
     * the {@link VolumeChange} callback.
     * </p>
     *
     * @param table    The Audio {@link Table} to which the row is appended.
     * @param label    The text displayed to the left of the slider
     *                 (e.g. {@code "Main Volume:"}).
     * @param current  The current normalised volume (0.0 – 1.0), used to
     *                 set the slider's initial position.
     * @param onChange A {@link VolumeChange} callback that receives the
     *                 new normalised volume every time the slider moves.
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

    /**
     * Functional interface used to forward slider values to the
     * appropriate {@link AudioManager} volume setter without a concrete
     * anonymous class.
     */
    private interface VolumeChange {

        void set(float volume);
    }



    /**
     * Registers the {@link Stage} as the active input processor so that
     * buttons and sliders receive events.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders one frame of the settings screen.
     * <p>
     * The background is drawn centred at the configured zoom and the
     * Scene2D stage is rendered on top.  When not in listening mode an
     * ESC key press navigates back to {@link #previousScreen}.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.getSpriteBatch().begin();
        float width = Gdx.graphics.getWidth() * bgZoom;
        float height = Gdx.graphics.getHeight() * bgZoom;
        float x = (Gdx.graphics.getWidth() - width) / 2;
        float y = (Gdx.graphics.getHeight() - height) / 2;
        game.getSpriteBatch().draw(background, x, y, width, height);
        game.getSpriteBatch().end();
        stage.act(Math.min(delta, 1/30f));
        stage.draw();

        if (!isListening && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(previousScreen);
        }
    }

    /**
     * Creates a settings-panel button with hover-scale and staggered
     * entrance animations and adds it to the supplied table.
     * <p>
     * The animation sequence mirrors that of {@link MenuScreen}: the
     * button starts invisible and 20 pixels below its target, waits
     * {@code delay} seconds, then fades in while sliding upward with a
     * quadratic ease-out.  A {@link ClickListener} scales the button to
     * 110 % on hover and back to 100 % on exit.
     * </p>
     *
     * @param table  The {@link Table} to which the button row is appended.
     * @param text   The label displayed on the button.
     * @param action The {@link Runnable} executed when the button is clicked.
     * @param delay  Seconds to wait before the entrance animation begins.
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
    /**
     * Updates the stage viewport when the window is resized.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    /**
     * Cancels any in-progress key capture when the screen is hidden so
     * that the custom {@link InputProcessor} does not linger.
     */
    @Override
    public void hide() {
        if (isListening) stopListening();
    }

    /**
     * Releases the Scene2D {@link Stage} and the 1×1 panel
     * {@link Texture}.  The background texture is also disposed here
     * because it is owned exclusively by this screen.
     */
    @Override
    public void dispose() {
        stage.dispose();
        panelTexture.dispose();
    }
}