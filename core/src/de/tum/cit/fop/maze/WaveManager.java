package de.tum.cit.fop.maze;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.Random;

/**
 * Orchestrates wave progression and difficulty scaling for Survival Mode.
 * <p>
 * Each wave defines how many enemies will spawn and how their base
 * stats are multiplied.  Enemy count grows exponentially
 * ({@code 3 × 2^(wave-1)}), while speed and health scale linearly
 * and exponentially respectively, ensuring that later waves become
 * progressively harder.  A short transition delay between waves gives
 * the player a brief respite before the next group arrives.
 * </p>
 * <p>
 * The manager does not spawn enemies itself; it exposes the current
 * multipliers and the remaining enemy count so that the owning
 * survival-mode screen can poll them every frame and act accordingly.
 * </p>
 *
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

    /**
     * Constructs a WaveManager with the given spawn-point pool.
     * The manager starts at wave 0 with no enemies remaining; call
     * {@link #startWave(int)} to begin the first wave.
     *
     * @param spawnLocations A list of world-space {@link Vector2} positions
     *                       where enemies may be placed.  Must not be empty;
     *                       if it is, {@link #getRandomSpawnPosition} will
     *                       return the origin.
     * @param random         A {@link Random} instance used to select from the
     *                       spawn-location pool.  Passing a seeded instance
     *                       allows deterministic replay.
     */
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
     * Initialises and activates the specified wave.
     * <p>
     * The enemy count for wave 1 is hard-coded to 3.  For every
     * subsequent wave the count doubles: {@code 3 × 2^(waveNumber-1)}.
     * The transition flag is cleared and the transition timer is reset
     * so that the owning screen can begin spawning enemies immediately.
     * </p>
     *
     * @param waveNumber The 1-based wave index to start.
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

    /**
     * Advances the wave-manager state by one frame.
     * <p>
     * If a transition is in progress the method accumulates {@code delta}
     * into the transition timer.  When the timer reaches
     * {@link #WAVE_TRANSITION_DELAY} the next wave is started
     * automatically via {@link #startWave}.  When no transition is
     * active this method is a no-op.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    public void update(float delta) {
        if (isTransitioning) {
            waveTransitionTimer += delta;
            if (waveTransitionTimer >= WAVE_TRANSITION_DELAY) {
                startWave(currentWave + 1);
            }
        }
    }

    /**
     * Notifies the manager that one enemy has been killed.
     * <p>
     * The remaining-enemy counter is decremented.  When it reaches zero
     * the wave is marked as complete and the inter-wave transition
     * countdown begins.  If the transition is already underway (e.g.
     * due to a race with a late enemy death) the call is a no-op.
     * </p>
     */
    public void onEnemyKilled() {
        enemiesRemainingInWave--;
        if (enemiesRemainingInWave <= 0 && !isTransitioning) {
            isTransitioning = true;
            waveTransitionTimer = 0f;
            System.out.println(">>> Wave " + currentWave + " complete!");
        }
    }

    /**
     * Selects a spawn position uniformly at random from the pool of
     * available {@link #spawnLocations}.
     *
     * @return A {@link Vector2} chosen at random, or {@code (0, 0)} if
     *         the spawn-location list is empty.
     */
    public Vector2 getRandomSpawnPosition() {
        if (spawnLocations.isEmpty()) {
            return new Vector2(0, 0);
        }
        return spawnLocations.get(random.nextInt(spawnLocations.size()));
    }

    /**
     * Calculates the speed multiplier that should be applied to every
     * enemy spawned in the current wave.  The multiplier increases by
     * 0.15 per wave, starting at 1.0 for wave 1.
     *
     * @return The speed multiplier for the current wave.
     */
    public float getSpeedMultiplier() {
        return 1.0f + (currentWave - 1) * 0.15f;
    }

    /**
     * Calculates the health multiplier that should be applied to every
     * enemy spawned in the current wave.  The multiplier grows
     * exponentially at a base of 1.2 per wave, starting at 1.0 for
     * wave 1.
     *
     * @return The health multiplier for the current wave.
     */
    public float getHealthMultiplier() {
        return (float) Math.pow(1.2, currentWave - 1);
    }

    /**
     * Calculates the score multiplier applied to points awarded for
     * each enemy kill in the current wave.  The multiplier increases
     * by 0.25 per wave, starting at 1.0 for wave 1.
     *
     * @return The score multiplier for the current wave.
     */
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

    /**
     * Returns the time remaining on the current inter-wave transition.
     * If no transition is in progress the value is {@code 0}.  The
     * owning screen can display this as a countdown to the next wave.
     *
     * @return Seconds remaining until the next wave starts, or {@code 0}
     *         if no transition is active.
     */
    public float getTransitionTimeRemaining() {
        return isTransitioning ? Math.max(0, WAVE_TRANSITION_DELAY - waveTransitionTimer) : 0f;
    }
}