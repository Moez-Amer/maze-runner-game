package de.tum.cit.fop.maze;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.Random;

/**
 * Manages wave progression and difficulty scaling for Survival Mode.
 * Calculates how many enemies spawn and how difficult they are.
 */
public class WaveManager {
    private int currentWave;
    private int enemiesRemainingInWave;
    private int totalEnemiesInWave;
    private float waveTransitionTimer;
    private boolean isTransitioning;

    private static final float WAVE_TRANSITION_DELAY = 2.0f;
    private Random random;
    private ArrayList<Vector2> spawnLocations;

    public WaveManager(ArrayList<Vector2> spawnLocations, Random random) {
        this.currentWave = 0;
        this.enemiesRemainingInWave = 0;
        this.totalEnemiesInWave = 0;
        this.waveTransitionTimer = 0f;
        this.isTransitioning = false;
        this.random = random;
        this.spawnLocations = spawnLocations;

        System.out.println("WaveManager created with " + spawnLocations.size() + " spawn locations");
    }

    /**
     * Starts a new wave.
     */
    public void startWave(int waveNumber) {
        this.currentWave = waveNumber;
        this.isTransitioning = false;
        this.waveTransitionTimer = 0f;

        if (waveNumber == 1) {
            this.totalEnemiesInWave = 3;
        } else {
            this.totalEnemiesInWave = (int) (3 * Math.pow(2, waveNumber - 1));
        }

        this.enemiesRemainingInWave = this.totalEnemiesInWave;

        System.out.println(">>> Wave " + currentWave + " started with " + totalEnemiesInWave + " enemies");
    }

    public void update(float delta) {
        if (isTransitioning) {
            waveTransitionTimer += delta;
            if (waveTransitionTimer >= WAVE_TRANSITION_DELAY) {
                startWave(currentWave + 1);
            }
        }
    }

    public void onEnemyKilled() {
        enemiesRemainingInWave--;
        if (enemiesRemainingInWave <= 0 && !isTransitioning) {
            isTransitioning = true;
            waveTransitionTimer = 0f;
            System.out.println(">>> Wave " + currentWave + " complete!");
        }
    }

    /**
     * Gets random spawn position from available locations.
     */
    public Vector2 getRandomSpawnPosition() {
        if (spawnLocations.isEmpty()) {
            return new Vector2(0, 0);
        }
        return spawnLocations.get(random.nextInt(spawnLocations.size()));
    }

    public float getSpeedMultiplier() {
        return 1.0f + (currentWave - 1) * 0.15f;
    }

    public float getHealthMultiplier() {
        return (float) Math.pow(1.2, currentWave - 1);
    }

    public float getScoreMultiplier() {
        return 1.0f + (currentWave - 1) * 0.25f;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getEnemiesRemaining() {
        return enemiesRemainingInWave;
    }

    public int getTotalEnemiesInWave() {
        return totalEnemiesInWave;
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public float getTransitionTimeRemaining() {
        return isTransitioning ? Math.max(0, WAVE_TRANSITION_DELAY - waveTransitionTimer) : 0f;
    }
}