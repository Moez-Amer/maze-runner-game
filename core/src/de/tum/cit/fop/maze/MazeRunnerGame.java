package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.Screens.GameScreen;
import de.tum.cit.fop.maze.Screens.MenuScreen;
import de.tum.cit.fop.maze.Screens.PauseScreen;
import de.tum.cit.fop.maze.Screens.SelectMapScreen;
import de.tum.cit.fop.maze.Screens.VictoryScreen;
import de.tum.cit.fop.maze.Screens.GameOverScreen;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {
    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;

    // UI Skin
    private Skin skin;

    // Character animation downwards
    private Animation<TextureRegion> characterDownAnimation;

    /**
     * Constructor for MazeRunnerGame.
     *
     * @param fileChooser The file chooser for the game, typically used in desktop environment.
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    /**
     * Called when the game is created. Initializes the SpriteBatch and Skin.
     */
    @Override
    public void create() {
        spriteBatch = new SpriteBatch(); // Create SpriteBatch
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json")); // Load UI skin
        this.loadCharacterAnimation(); // Load character animation

        AudioManager.load();
        goToMenu();
    }

    /**
     * Switches to the menu screen.
     */
    public void goToMenu() {


        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }

         AudioManager.playMenuMusic();

        this.setScreen(new MenuScreen(this));

    }

    /**
     * Switches to the game screen.
     */
    public void goToGame(String mapPath) {
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }

        AudioManager.playGameMusic();
        this.setScreen(new GameScreen(this, mapPath)); // Set the current screen to GameScreen
    }
    public void goToSelectMap() {
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        this.setScreen(new SelectMapScreen(this));
    }

    public void goToPause(GameScreen currentGameScreen) {
        this.setScreen(new PauseScreen(this, currentGameScreen));
    }

    /**
     * Switches to the victory screen.
     */
    public void goToVictory() {
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        this.setScreen(new VictoryScreen(this));
    }

    /**
     * Switches to the game over screen.
     */
    public void goToGameOver(String mapPath) {
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        this.setScreen(new GameOverScreen(this, mapPath));
    }


    /**
     * Loads the character animation from the character.png file.
     */
    private void loadCharacterAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("character.png"));

        int frameWidth = 16;
        int frameHeight = 32;
        int animationFrames = 4;

        // libGDX internal Array instead of ArrayList because of performance
        Array<TextureRegion> walkFrames = new Array<>(TextureRegion.class);

        // Add all frames to the animation
        for (int col = 0; col < animationFrames; col++) {
            walkFrames.add(new TextureRegion(walkSheet, col * frameWidth, 0, frameWidth, frameHeight));
        }

        characterDownAnimation = new Animation<>(0.1f, walkFrames);
    }

    /**
     * Cleans up resources when the game is disposed.
     */
    @Override
    public void dispose() {
        getScreen().hide();
        getScreen().dispose();
        spriteBatch.dispose();
        skin.dispose();
    }

    private void addButton(Table table, String text, Runnable action, float delay) {
        TextButton button = new TextButton(text, this.getSkin());

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

    // Getter methods
    public Skin getSkin() {
        return skin;
    }

    public Animation<TextureRegion> getCharacterDownAnimation() {
        return characterDownAnimation;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }
}
