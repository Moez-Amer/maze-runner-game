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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen responsible for displaying the Survival Mode Leaderboard.
 * It scans all local player profiles to rank players based on their highest
 * survival scores, wave reached, and time survived.
 */
public class LeaderboardScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private final Texture background;
    private final float bgZoom = 1.0f;

    /**
     * Constructs the LeaderboardScreen.
     * * @param game The main game instance used to access skins and navigation.
     */
    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        background = new Texture(Gdx.files.internal("LeadershipBG.png"));
        // Reuse game's SpriteBatch for rendering efficiency
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        rebuildUI();
    }

    /**
     * Rebuilds the UI components.
     * This method fetches all player profiles from the SaveManager, sorts them
     * by survival score, and constructs the Hall of Fame table.
     */
    private void rebuildUI() {
        stage.clear();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title Row
        table.add(new Label("SURVIVAL HALL OF FAME", game.getSkin(), "title")).colspan(4).padBottom(20).row();

        // Data Loading and Sorting
        List<PlayerScore> scores = new ArrayList<>();
        // Iterate through all found profile names in local storage
        for (String name : SaveManager.getAllProfileNames()) {
            GameState gs = SaveManager.loadProfile(name);
            // Only include players who have established a survival record
            if (gs.getSurvivalBestScore() > 0) {
                scores.add(new PlayerScore(
                        name,
                        gs.getSurvivalBestScore(),
                        gs.getSurvivalBestWave(),
                        gs.getSurvivalLongestTime()
                ));
            }
        }

        // Sort descending: Highest score at the top
        Collections.sort(scores, (a, b) -> b.score - a.score);

        // Display Score Table Headers
        Table scoreTable = new Table();
        scoreTable.add(new Label("PLAYER", game.getSkin(), "bold")).padRight(40).left();
        scoreTable.add(new Label("SCORE", game.getSkin(), "bold")).padRight(40).right();
        scoreTable.add(new Label("WAVE", game.getSkin(), "bold")).padRight(40).right();
        scoreTable.add(new Label("TIME", game.getSkin(), "bold")).right().row();

        // Populate Table with Top 10
        int rank = 1;
        for (PlayerScore ps : scores) {
            scoreTable.add(new Label(rank + ". " + ps.name, game.getSkin())).padRight(40).left();
            scoreTable.add(new Label("" + ps.score, game.getSkin())).padRight(40).right();
            scoreTable.add(new Label("" + ps.wave, game.getSkin())).padRight(40).right();
            scoreTable.add(new Label(ps.time + "s", game.getSkin())).right().row();

            if (++rank > 10) break;
        }

        // Add the scrollable/list table to the main layout
        table.add(scoreTable).colspan(4).padBottom(40).row();

        // Back Button to return to Menu
        TextButton back = new TextButton("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.goToMenu();
            }
        });

        table.add(back).colspan(4).width(200);
    }

    /**
     * Static helper class to encapsulate player survival data for ranking.
     */
    private static class PlayerScore {
        String name;
        int score;
        int wave;
        int time;

        /**
         * @param n Player name
         * @param s Highest survival score reached
         * @param w Highest wave reached
         * @param t Longest time survived in seconds
         */
        PlayerScore(String n, int s, int w, int t) {
            this.name = n;
            this.score = s;
            this.wave = w;
            this.time = t;
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
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        // Required to enable mouse/touch interaction
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
}