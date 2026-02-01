package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.GameState;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.SaveManager;

/**
 * Marketplace screen where players spend earned skill points to
 * upgrade character stats across three independent branches.
 * <p>
 * Each branch upgrades a single attribute via a consumable ring item:
 * <ul>
 *   <li><b>COMBAT (Warrior)</b> – Power Ring, grants +50 % damage per
 *       level.</li>
 *   <li><b>AGILITY (Swiftness)</b> – Swiftness Ring, grants +25 % speed
 *       per level.</li>
 *   <li><b>SURVIVAL (Vitality)</b> – Health Ring, grants +1 heart per
 *       level.</li>
 * </ul>
 * Each branch draws from its own dedicated point pool (Warrior,
 * Swiftness, or Vitality points) rather than from a shared currency
 * </p>
 */
public class SkillTreeScreen implements Screen {
    private final Stage stage;

    private final MazeRunnerGame game;

    private final Texture background;

    private final Texture panelTexture;

    private final float bgZoom = 1.5f;

    /**
     * Constructs the SkillTreeScreen, creates shared textures, and
     * performs the initial UI build.
     * <p>
     * A 1×1 {@link com.badlogic.gdx.graphics.Pixmap} is generated and
     * uploaded to a {@link Texture} to serve as the semi-transparent
     * panel background used behind each upgrade branch and each
     * point-balance badge.
     * </p>
     *
     * @param game The main {@link MazeRunnerGame} instance, used to access
     *             the current {@link GameState} and the shared UI skin.
     */
    public SkillTreeScreen(MazeRunnerGame game) {
        this.game = game;
        var camera = new OrthographicCamera();
        camera.zoom = 1.5f;
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("MarketBG.png"));
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.6f);
        pixmap.fill();
        panelTexture = new Texture(pixmap);
        pixmap.dispose();
        rebuildUI();
    }

    /**
     * Clears the stage and reconstructs every UI element from the
     * current {@link GameState}.
     * <p>
     * This method is called once at construction and again after every
     * successful upgrade purchase so that skill levels, remaining
     * points, next-upgrade costs, and button enabled-states are all
     * refreshed atomically.  It also re-registers the stage as the
     * active input processor so that the freshly created actors receive
     * events immediately.
     * </p>
     */
    private void rebuildUI() {
        stage.clear();
        Gdx.input.setInputProcessor(stage);
        GameState state = game.getGameState();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        root.add(new Label("SKILL MARKETPLACE", game.getSkin(), "title")).colspan(3).padBottom(15).row();

        TextureRegionDrawable panelDrawable = new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(panelTexture));

        Label warLabel = new Label("Warrior Pts: " + state.warriorPoints, game.getSkin());
        warLabel.setColor(com.badlogic.gdx.graphics.Color.PURPLE);

        Label swiftLabel = new Label("Swiftness Pts: " + state.swiftnessPoints, game.getSkin());
        swiftLabel.setColor(new com.badlogic.gdx.graphics.Color(0f, 0f, 0.5f, 1f));

        Label vitLabel = new Label("Vitality Pts: " + state.vitalityPoints, game.getSkin());
        vitLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);

        Table warPanel = new Table();
        warPanel.setBackground(panelDrawable);
        warPanel.add(warLabel).pad(8, 16, 8, 16);

        Table swiftPanel = new Table();
        swiftPanel.setBackground(panelDrawable);
        swiftPanel.add(swiftLabel).pad(8, 16, 8, 16);

        Table vitPanel = new Table();
        vitPanel.setBackground(panelDrawable);
        vitPanel.add(vitLabel).pad(8, 16, 8, 16);

        root.add(warPanel).padBottom(10).expandX();
        root.add(swiftPanel).padBottom(10).expandX();
        root.add(vitPanel).padBottom(10).expandX();
        root.row();

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
        root.add(back).colspan(3).padTop(60).width(350).height(55);
    }

    /**
     * Builds the UI panel for a single upgrade branch.
     * <p>
     * The panel contains, from top to bottom: an icon image loaded from
     * {@code iconPath} (silently skipped if the file is missing), a
     * coloured branch title, the item name with its current level, a
     * description of the per-level benefit, and an Upgrade button whose
     * label shows the next cost.  The button is disabled and greyed when
     * the player lacks sufficient points.
     * </p>
     * <p>
     * When the Upgrade button is clicked the method deducts the cost
     * from the appropriate point pool, increments the skill level in
     * {@link GameState}, persists the change via {@link SaveManager},
     * and calls {@link #rebuildUI} to refresh the entire screen.
     * </p>
     *
     * @param state       The current {@link GameState}, read for levels
     *                    and point balances.
     * @param title       The branch heading displayed at the top of the
     *                    panel (e.g. {@code "COMBAT"}).
     * @param skillKey    The key used to look up and store the skill level
     *                    in {@link GameState#skillLevels} (e.g.
     *                    {@code "Warrior"}).
     * @param name        The item name shown below the title (e.g.
     *                    {@code "Power Ring"}).
     * @param description A short description of the per-level benefit.
     * @param type        The point-pool identifier: {@code "warrior"},
     *                    {@code "swiftness"}, or {@code "vitality"}.
     * @param color       The {@link com.badlogic.gdx.graphics.Color} used
     *                    to tint the title label and the enabled Upgrade
     *                    button.
     * @param iconPath    Internal asset path for the branch icon image,
     *                    or {@code null} / empty to skip the icon.
     * @return A fully populated {@link Table} representing the branch panel.
     */
    private Table createContinuousBranch(GameState state, String title, final String skillKey, String name, String description, final String type, com.badlogic.gdx.graphics.Color color, String iconPath) {
        Table branch = new Table();
        branch.setBackground(new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(panelTexture)));

        Table inner = new Table();
        inner.pad(15);

        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                Texture iconTexture = new Texture(Gdx.files.internal(iconPath));
                Image icon = new Image(iconTexture);
                inner.add(icon).size(48, 48).padBottom(8).row();
            } catch (Exception e) {
                System.err.println("Failed to load skill icon: " + iconPath);
            }
        }

        Label titleLabel = new Label(title, game.getSkin(), "bold");
        titleLabel.setColor(color);
        inner.add(titleLabel).padBottom(8).row();

        int currentLevel = state.getSkillLevel(skillKey);
        final int cost = 2 * (int)Math.pow(2, currentLevel);

        Label nameLabel = new Label(name + " (Lvl " + currentLevel + ")", game.getSkin());
        nameLabel.setFontScale(0.85f);
        inner.add(nameLabel).padBottom(4).row();

        Label descLabel = new Label(description, game.getSkin());
        descLabel.setFontScale(0.75f);
        descLabel.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
        inner.add(descLabel).padBottom(10).row();

        int playerPts = type.equals("warrior") ? state.warriorPoints : type.equals("swiftness") ? state.swiftnessPoints : state.vitalityPoints;

        TextButton buy = new TextButton("Upgrade (" + cost + " pts)", game.getSkin());
        buy.getLabelCell().padLeft(15).padRight(15);

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

        inner.add(buy).width(280).height(50);

        branch.add(inner);
        return branch;
    }

    /**
     * Renders one frame of the marketplace screen.
     * The background is drawn centred at the configured zoom and the
     * Scene2D stage is rendered on top.
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
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

    /**
     * Registers the {@link Stage} as the active input processor.
     */
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    /**
     * Updates the stage viewport when the window is resized.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}

    @Override public void resume() {}

    @Override public void hide() {}

    /**
     * Releases the Scene2D {@link Stage}, the background {@link Texture},
     * and the panel {@link Texture}.
     */
    @Override public void dispose() {
        stage.dispose();
        background.dispose();
        panelTexture.dispose();
    }
}