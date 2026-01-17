package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

/**
 * Screen for the Skill Tree/Marketplace where players can upgrade stats.
 * Supports continuous upgrades with increasing costs.
 */
public class SkillTreeScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;

    /**
     * Constructor to initialize the screen.
     * @param game The main game instance.
     */
    public SkillTreeScreen(MazeRunnerGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        rebuildUI();
    }

    /**
     * Rebuilds the UI elements based on the current game state.
     * Refreshes values and button states.
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
        warLabel.setColor(com.badlogic.gdx.graphics.Color.RED);

        Label swiftLabel = new Label("Swiftness Pts: " + state.swiftnessPoints, game.getSkin());
        swiftLabel.setColor(com.badlogic.gdx.graphics.Color.CYAN);

        Label vitLabel = new Label("Vitality Pts: " + state.vitalityPoints, game.getSkin());
        vitLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);

        pointsTable.add(warLabel).pad(15);
        pointsTable.add(swiftLabel).pad(15);
        pointsTable.add(vitLabel).pad(15);

        root.add(pointsTable).colspan(3).padBottom(50).row();

        Table combatBranch = createContinuousBranch(state, "COMBAT", "Warrior", "Sharpened Blade", "+50% Dmg per Lvl", "warrior", com.badlogic.gdx.graphics.Color.RED);
        Table agilityBranch = createContinuousBranch(state, "AGILITY", "Swiftness", "Fast Feet", "+25% Speed per Lvl", "swiftness", com.badlogic.gdx.graphics.Color.CYAN);
        Table survivalBranch = createContinuousBranch(state, "SURVIVAL", "Vitality", "Tank Armor", "+1 Heart per Lvl", "vitality", com.badlogic.gdx.graphics.Color.GREEN);

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
        // Edited: Increased button size
        root.add(back).colspan(3).padTop(60).width(400).height(80);
    }

    /**
     * Creates a vertical UI branch for a specific skill type.
     * Handles dynamic cost calculation and upgrade logic.
     *
     * @param state The current game state.
     * @param title The visual title of the branch.
     * @param skillKey The key used in the skill map.
     * @param name The display name of the skill.
     * @param description A short description of the skill effect.
     * @param type The type of point currency used (warrior, swiftness, vitality).
     * @param color The color theme for this branch.
     * @return A Table containing the UI elements for this branch.
     */
    private Table createContinuousBranch(GameState state, String title, final String skillKey, String name, String description, final String type, com.badlogic.gdx.graphics.Color color) {
        Table branch = new Table();

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

        // Edited: Increased button size
        // Edited: Massively increase Upgrade button size to 550x140
        branch.add(buy).size(400, 80);

        return branch;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
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