package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

/**
 * Screen for the Skill Tree/Marketplace where players can upgrade their character stats.
 * <p>
 * This screen provides three upgrade branches:
 * <ul>
 *   <li><b>COMBAT</b> - Upgrades the Power Ring for increased damage</li>
 *   <li><b>AGILITY</b> - Upgrades Swift Boots for increased movement speed</li>
 *   <li><b>SURVIVAL</b> - Upgrades Tank Armor for increased health</li>
 * </ul>
 * Each upgrade requires spending skill points earned through gameplay.
 * Upgrade costs increase exponentially with each level (cost = 2 * 2^level).
 *
 * @author TUM Chair of Information Technology
 * @version 1.0
 * @since 2024
 */
public class SkillTreeScreen implements Screen {
    /** The stage that contains all UI actors for this screen. */
    private final Stage stage;

    /** Reference to the main game instance. */
    private final MazeRunnerGame game;

    /**
     * Constructs a new SkillTreeScreen.
     * <p>
     * Initializes the stage with a screen viewport and builds the UI
     * displaying all available skill upgrades.
     *
     * @param game the main game instance used to access game state and UI skin
     */
    public SkillTreeScreen(MazeRunnerGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        rebuildUI();
    }

    /**
     * Rebuilds the complete UI for the skill tree screen.
     * <p>
     * This method:
     * <ul>
     *   <li>Clears the current stage</li>
     *   <li>Displays current skill point totals for all three categories</li>
     *   <li>Creates three upgrade branches (Combat, Agility, Survival)</li>
     *   <li>Updates button states based on available points</li>
     *   <li>Adds a return button to go back to the main menu</li>
     * </ul>
     * Called initially and after each upgrade to refresh the display.
     */
    private void rebuildUI() {
        stage.clear();
        Gdx.input.setInputProcessor(stage);
        GameState state = game.getGameState();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label("SKILL MARKETPLACE", game.getSkin(), "title")).colspan(3).padBottom(30).row();

        Table pointsTable = new Table();

        Label warLabel = new Label("Warrior Pts: " + state.warriorPoints, game.getSkin());
        warLabel.setColor(com.badlogic.gdx.graphics.Color.PURPLE);

        Label swiftLabel = new Label("Swiftness Pts: " + state.swiftnessPoints, game.getSkin());
        swiftLabel.setColor(new com.badlogic.gdx.graphics.Color(0f, 0f, 0.5f, 1f)); // Dark blue

        Label vitLabel = new Label("Vitality Pts: " + state.vitalityPoints, game.getSkin());
        vitLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);

        pointsTable.add(warLabel).pad(15);
        pointsTable.add(swiftLabel).pad(15);
        pointsTable.add(vitLabel).pad(15);

        root.add(pointsTable).colspan(3).padBottom(50).row();

        Table combatBranch = createContinuousBranch(state, "COMBAT", "Warrior", "Power Ring", "+50% Dmg per Lvl", "warrior", com.badlogic.gdx.graphics.Color.PURPLE, "free-undead-loot-pixel-art-icons/PNG/Transperent/Icon19.png");
        Table agilityBranch = createContinuousBranch(state, "AGILITY", "Swiftness", "Swiftness Ring", "+25% Speed per Lvl", "swiftness", new com.badlogic.gdx.graphics.Color(0f, 0f, 0.5f, 1f), "free-undead-loot-pixel-art-icons/PNG/Transperent/Icon18.png");
        Table survivalBranch = createContinuousBranch(state, "SURVIVAL", "Vitality", "Health Ring", "+1 Heart per Lvl", "vitality", com.badlogic.gdx.graphics.Color.GREEN, "free-undead-loot-pixel-art-icons/PNG/Transperent/Icon17.png");

        root.add(combatBranch).top().pad(20);
        root.add(agilityBranch).top().pad(20);
        root.add(survivalBranch).top().pad(20);
        root.row();

        TextButton back = new TextButton("Return to Menu", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.goToMenu();
            }
        });
        root.add(back).colspan(3).padTop(60).width(400).height(80);
    }

    /**
     * Creates a vertical UI branch for a specific skill upgrade type.
     * <p>
     * Each branch contains:
     * <ul>
     *   <li>An icon representing the skill (64x64 pixels)</li>
     *   <li>A colored title label</li>
     *   <li>The skill name with current level</li>
     *   <li>A description of the upgrade effect</li>
     *   <li>An upgrade button showing the cost in skill points</li>
     * </ul>
     * The upgrade button is disabled if the player doesn't have enough points.
     * Cost increases exponentially: cost = 2 * 2^currentLevel
     *
     * @param state the current game state containing skill levels and points
     * @param title the visual title of the branch (e.g., "COMBAT")
     * @param skillKey the key used in the skill map to track this skill's level
     * @param name the display name of the skill (e.g., "Upgrade Power Ring")
     * @param description a short description of the skill effect (e.g., "+50% Dmg per Lvl")
     * @param type the type of point currency used ("warrior", "swiftness", or "vitality")
     * @param color the color theme for this branch's text and buttons
     * @param iconPath the file path to the icon image for this skill
     * @return a Table containing all UI elements for this skill branch
     */
    private Table createContinuousBranch(GameState state, String title, final String skillKey, String name, String description, final String type, com.badlogic.gdx.graphics.Color color, String iconPath) {
        Table branch = new Table();

        // Add icon at the top
        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                Texture iconTexture = new Texture(Gdx.files.internal(iconPath));
                Image icon = new Image(iconTexture);
                branch.add(icon).size(64, 64).padBottom(15).row();
            } catch (Exception e) {
                System.err.println("Failed to load skill icon: " + iconPath);
            }
        }

        Label titleLabel = new Label(title, game.getSkin(), "bold");
        titleLabel.setColor(color);
        branch.add(titleLabel).padBottom(20).row();

        int currentLevel = state.getSkillLevel(skillKey);

        final int cost = 2 * (int)Math.pow(2, currentLevel);

        branch.add(new Label(name + " (Lvl " + currentLevel + ")", game.getSkin())).padBottom(5).row();

        Label descLabel = new Label(description, game.getSkin());
        descLabel.setFontScale(0.8f);
        descLabel.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
        branch.add(descLabel).padBottom(15).row();

        int playerPts = type.equals("warrior") ? state.warriorPoints : type.equals("swiftness") ? state.swiftnessPoints : state.vitalityPoints;

        TextButton buy = new TextButton("Upgrade (" + cost + " pts)", game.getSkin());

        if (playerPts < cost) {
            buy.setDisabled(true);
            buy.setColor(com.badlogic.gdx.graphics.Color.DARK_GRAY);
        } else {
            buy.setColor(color);
        }

        buy.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (type.equals("warrior")) state.warriorPoints -= cost;
                else if (type.equals("swiftness")) state.swiftnessPoints -= cost;
                else state.vitalityPoints -= cost;

                state.skillLevels.put(skillKey, currentLevel + 1);

                SaveManager.save(state);
                rebuildUI();
            }
        });

        branch.add(buy).size(400, 80);

        return branch;
    }

    /**
     * Renders the skill tree screen.
     * <p>
     * Clears the screen with a dark blue-gray background and updates/draws all stage actors.
     *
     * @param delta the time in seconds since the last render call
     */
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
    }

    /**
     * Called when this screen becomes the current screen.
     * <p>
     * Currently does nothing as initialization is handled in the constructor.
     */
    @Override public void show() {}

    /**
     * Called when the screen is resized.
     * <p>
     * Updates the stage's viewport to match the new screen dimensions.
     *
     * @param width the new screen width in pixels
     * @param height the new screen height in pixels
     */
    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /**
     * Called when the game is paused (typically on Android).
     * <p>
     * Currently does nothing as no pause-specific logic is needed.
     */
    @Override public void pause() {}

    /**
     * Called when the game is resumed from a paused state (typically on Android).
     * <p>
     * Currently does nothing as no resume-specific logic is needed.
     */
    @Override public void resume() {}

    /**
     * Called when this screen is no longer the current screen.
     * <p>
     * Currently does nothing as no cleanup is needed when hiding.
     */
    @Override public void hide() {}

    /**
     * Disposes of all resources used by this screen.
     * <p>
     * Cleans up the stage and all its actors to prevent memory leaks.
     * This method should be called when the screen is no longer needed.
     */
    @Override public void dispose() {
        stage.dispose();
    }
}