package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
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
import de.tum.cit.fop.maze.Screens.*;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {

    private GameState gameState;

    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;

    // UI Skin
    private Skin skin;

    // Character animation downwards
    private Animation<TextureRegion> characterDownAnimation;

    // Developer console
    private DeveloperConsole developerConsole;

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
        spriteBatch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json"));
        this.loadCharacterAnimation();
        AudioManager.load();

        // Initialize developer console
        developerConsole = new DeveloperConsole(this);

        // Fulfills the requirement to sign in/select profile on startup
        goToProfileSelection();
        //goToMenu();
    }

    /** Returns the global game state for the active player. */
    public GameState getGameState() { return gameState; }

    /**
     * Switches to the menu screen.
     */
    public void goToMenu() {
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        AudioManager.stopMusic();
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
        GameScreen newGameScreen = new GameScreen(this, mapPath);

        if (developerConsole != null) {
            developerConsole.setGameScreen(newGameScreen);
        }

        this.setScreen(newGameScreen);
    }

    /**
     * Transitions to Survival Mode game screen.
     * Loads the survival map and starts wave-based gameplay.
     */
    public void goToSurvival() {

        if (getScreen() != null) {
            getScreen().dispose();
        }

        AudioManager.playGameMusic();
        SurvivalGameScreen survivalScreen = new SurvivalGameScreen(this, "maps/survival.properties");

        if (developerConsole != null) {
            developerConsole.setGameScreen(survivalScreen);
        }

        setScreen(survivalScreen);

        System.out.println("Survival Mode started!");
    }

    /**
     * Disposes of any active menu or game screens and navigates to the map selection screen.
     * Ensures memory is freed before creating the new screen.
     */
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
    /**
     * Pauses the current game and switches to the pause menu.
     *
     * @param currentGameScreen The active game screen, preserved to allow resuming.
     */
    public void goToPause(GameScreen currentGameScreen) {
        AudioManager.stopMusic();
        this.setScreen(new PauseScreen(this, currentGameScreen));
    }
    /**
     * Navigates to the settings menu.
     *
     * @param previousScreen The screen to return to when exiting settings.
     */
    public void goToSettings(Screen previousScreen) {
        this.setScreen(new SettingsScreen(this, previousScreen));
    }

    /** Switches to the victory screen and passes the final score. */
    public void goToVictory(int score) {
        if (gameScreen != null) { gameScreen.dispose(); gameScreen = null; }
        this.setScreen(new VictoryScreen(this, score));
    }

    /** Switches to the game over screen and passes the final score. */
    public void goToGameOver(String mapPath, int score) {
        AudioManager.stopMusic();
        if (gameScreen != null) { gameScreen.dispose(); gameScreen = null; }
        this.setScreen(new GameOverScreen(this, mapPath, score));
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
        if (developerConsole != null) {
            developerConsole.dispose();
        }
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


    /**
     * Gets the UI skin for creating interface elements.
     * @return The game's UI skin
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Gets the character walking down animation.
     * @return The character animation
     */
    public Animation<TextureRegion> getCharacterDownAnimation() {
        return characterDownAnimation;
    }

    /**
     * Gets the sprite batch for rendering.
     * @return The sprite batch
     */
    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    /** Navigates to the sign-in/profile selection screen. */
    public void goToProfileSelection() {
        this.setScreen(new ProfileSelectionScreen(this));
    }

    /** Sets the active player profile based on the selected name. */
    public void setProfileAndContinue(String name) {
        this.gameState = SaveManager.loadProfile(name);
        goToMenu();
    }

    /** Navigates to the Marketplace/Skill Tree screen. */
    public void goToMarketplace() {
        this.setScreen(new SkillTreeScreen(this));
    }

    /**
     * Navigates to the leaderboard screen.
     * Displays high scores and rankings.
     */
    public void goToLeaderboard() {
        this.setScreen(new LeaderboardScreen(this));
    }

    /**
     * Navigates to the achievements screen.
     * Shows unlocked and locked achievements.
     */
    public void goToAchievements() {
        this.setScreen(new AchievementScreen(this));
    }


    /**
     * Updates the developer console.
     * @param delta Time since last frame
     */
    public void updateConsole(float delta) {
        if (developerConsole != null) {
            developerConsole.update(delta);
        }
    }
    /**
     * Renders the developer console.
     */
    public void renderConsole() {
        if (developerConsole != null) {
            developerConsole.render();
        }
    }
    /**
     * Gets the developer console instance.
     * @return The developer console
     */
    public DeveloperConsole getConsole() {
        return developerConsole;
    }
}