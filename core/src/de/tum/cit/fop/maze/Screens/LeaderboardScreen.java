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

// Added missing imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardScreen implements Screen {
    private final Stage stage;
    private final MazeRunnerGame game;
    private String selectedLevel = "maps/level-1.properties";

    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        // Reuse game's SpriteBatch for efficiency
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        rebuildUI();
    }

    private void rebuildUI() {
        stage.clear();
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title Row
        table.add(new Label("HALL OF FAME", game.getSkin(), "title")).colspan(2).padBottom(20).row();

        // Level Selector Row
        Table levelSelect = new Table();
        String[] levels = {"maps/level-1.properties", "maps/level-2.properties", "maps/level-3.properties", "maps/level-4.properties", "maps/level-5.properties"};
        for (final String lvl : levels) {
            String num = lvl.replaceAll("\\D+", "");
            TextButton btn = new TextButton("Lvl " + num, game.getSkin());
            if (lvl.equals(selectedLevel)) btn.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            btn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    selectedLevel = lvl;
                    rebuildUI();
                }
            });
            levelSelect.add(btn).width(120).height(40).pad(5);
        }
        table.add(levelSelect).colspan(2).padBottom(20).row();

        // Data Loading and Sorting
        List<PlayerScore> scores = new ArrayList<>();
        for (String name : SaveManager.getAllProfileNames()) {
            GameState gs = SaveManager.loadProfile(name);
            if (gs.levelHighScores.containsKey(selectedLevel)) {
                scores.add(new PlayerScore(name, gs.levelHighScores.get(selectedLevel)));
            }
        }
        Collections.sort(scores, (a, b) -> b.score - a.score);

        // Display Score Table
        Table scoreTable = new Table();
        scoreTable.add(new Label("PLAYER", game.getSkin(), "bold")).padRight(100);
        scoreTable.add(new Label("SCORE", game.getSkin(), "bold")).row();

        int rank = 1;
        for (PlayerScore ps : scores) {
            scoreTable.add(new Label(rank + ". " + ps.name, game.getSkin())).left();
            scoreTable.add(new Label("" + ps.score, game.getSkin())).right().row();
            if (++rank > 10) break; // Display top 10
        }

        // FIX: colspan(2) ensures the leaderboard list is centered
        table.add(scoreTable).colspan(2).padBottom(40).row();

        TextButton back = new TextButton("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) { game.goToMenu(); }
        });

        // FIX: colspan(2) ensures the back button is centered
        table.add(back).colspan(2).width(200);
    }

    private static class PlayerScore {
        String name; int score;
        PlayerScore(String n, int s) { name = n; score = s; }
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() {
        // Essential for mouse input
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() { stage.dispose(); }
    @Override public void pause() {}
    @Override public void resume() {}
}