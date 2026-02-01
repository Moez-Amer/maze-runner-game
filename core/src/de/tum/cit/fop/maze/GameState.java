package de.tum.cit.fop.maze;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistently stores player progress, including distinct skill points earned
 * through specific gameplay actions.
 * <p>
 * Several statistics are tracked with two counters: a lifetime total that
 * grows monotonically and is fed to the AchievementManager so that cumulative
 * achievements fire at the correct thresholds, and a progress accumulator
 * that resets to zero every time enough has been accumulated to award one
 * skill point. This separation ensures that achievements and skill-point
 * awards are evaluated against independent thresholds without interfering
 * with each other.
 * </p>
 */
public class GameState {
    public String playerName = "Guest";

    private static AchievementManager achievementManager = new AchievementManager();

    public int warriorPoints = 0;
    public int swiftnessPoints = 0;
    public int vitalityPoints = 0;
    public int enemiesKilledCounter = 0;
    public float distanceSprintedCounter = 0;
    public int heartsCollectedCounter = 0;
    public int tilesExploredCounter = 0;
    public int keysCollectedCounter = 0;
    public int scrollsCollectedCounter = 0;
    public int coinsCollectedCounter = 0;
    public int potionsUsedCounter = 0;
    public int perfectMazesCounter = 0;
    public int mazesCompletedCounter = 0;
    public int killsProgress = 0;
    public float sprintProgress = 0;
    public int heartsProgress = 0;

    public int survivalBestScore = 0;
    public int survivalBestWave = 0;
    public int survivalLongestTime = 0;

    public List<String> unlockedSkills = new ArrayList<>();
    public List<String> unlockedAchievements = new ArrayList<>();
    public Map<String, Integer> levelHighScores = new HashMap<>();
    public Map<String, Integer> skillLevels = new HashMap<>();

    /**
     * Returns the global achievement manager instance.
     *
     * @return The AchievementManager.
     */
    public static AchievementManager getAchievementManager() {
        return achievementManager;
    }

    /**
     * Records a single enemy kill, updating both the lifetime total and the
     * Warrior skill-point progress tracker. The lifetime enemiesKilledCounter
     * is incremented and forwarded to the AchievementManager. Independently,
     * killsProgress is incremented and when it reaches 2 a Warrior point is
     * awarded and the progress counter resets to 0.
     */
    public void recordKill() {
        enemiesKilledCounter++;
        achievementManager.onEvent(this, "enemiesKilled", enemiesKilledCounter);

        killsProgress++;
        if (killsProgress >= 2) {
            warriorPoints++;
            killsProgress = 0;
        }
    }

    /**
     * Records that the player has sprinted the given distance, updating both
     * the lifetime total and the Swiftness skill-point progress tracker.
     * The supplied distance is added to distanceSprintedCounter and forwarded
     * to the AchievementManager. Independently it is added to sprintProgress
     * and when that accumulator reaches 2000 a Swiftness point is awarded
     * and it resets to 0.
     *
     * @param distance The distance sprinted this frame, in pixels.
     */
    public void recordSprinting(float distance) {
        distanceSprintedCounter += distance;
        achievementManager.onEvent(this, "distanceSprinted", distanceSprintedCounter);
        sprintProgress += distance;
        if (sprintProgress >= 2000) {
            swiftnessPoints++;
            sprintProgress = 0;
        }
    }

    /**
     * Records a single heart collectible pickup, updating both the lifetime
     * total and the Vitality skill-point progress tracker. The lifetime
     * heartsCollectedCounter is incremented and forwarded to the
     * AchievementManager. Independently, heartsProgress is incremented and
     * when it reaches 2 a Vitality point is awarded and the progress counter
     * resets to 0.
     */
    public void recordHeartPickup() {
        heartsCollectedCounter++;
        achievementManager.onEvent(this, "heartsCollected", heartsCollectedCounter);

        heartsProgress++;
        if (heartsProgress >= 2) {
            vitalityPoints++;
            heartsProgress = 0;
        }
    }

    /**
     * Records that a new tile has been explored.
     * Increments the lifetime tilesExploredCounter and notifies the
     * AchievementManager so that exploration-based achievements can be evaluated.
     */
    public void recordTileExplored() {
        tilesExploredCounter++;
        achievementManager.onEvent(this, "tilesExplored", tilesExploredCounter);
    }

    /**
     * Records that a key has been collected.
     * Increments the lifetime keysCollectedCounter and notifies the
     * AchievementManager so that key-based achievements can be evaluated.
     */
    public void recordKeyCollected() {
        keysCollectedCounter++;
        achievementManager.onEvent(this, "keysCollected", keysCollectedCounter);
    }

    /**
     * Records that a coin has been collected.
     * Increments the lifetime coinsCollectedCounter and notifies the
     * AchievementManager so that coin-based achievements can be evaluated.
     */
    public void recordCoinCollected() {
        coinsCollectedCounter++;
        achievementManager.onEvent(this, "coinsCollected", coinsCollectedCounter);
    }

    /**
     * Records that a potion has been consumed.
     * Increments the lifetime potionsUsedCounter and notifies the
     * AchievementManager so that potion-usage achievements can be evaluated.
     */
    public void recordPotionUsed() {
        potionsUsedCounter++;
        achievementManager.onEvent(this, "potionsUsed", potionsUsedCounter);
    }

    /**
     * Records that a scroll has been collected.
     * Increments the lifetime scrollsCollectedCounter and notifies the
     * AchievementManager using the event name "scrollsCollected", which must
     * match the statName defined in the achievements JSON configuration file.
     */
    public void recordScrollCollected() {
        scrollsCollectedCounter++;
        // This string "scrollsCollected" must match the statName in your JSON
        achievementManager.onEvent(this, "scrollsCollected", scrollsCollectedCounter);
    }

    /**
     * Records that a maze was completed without taking any damage (a perfect run).
     * Increments the lifetime perfectMazesCounter and notifies the
     * AchievementManager so that perfect-run achievements can be evaluated.
     */
    public void recordPerfectMaze() {
        perfectMazesCounter++;
        achievementManager.onEvent(this, "perfectMazes", perfectMazesCounter);
    }

    /**
     * Records that a maze has been completed.
     * Increments the lifetime mazesCompletedCounter and notifies the
     * AchievementManager so that completion-based achievements can be evaluated.
     */
    public void recordMazeCompleted() {
        mazesCompletedCounter++;
        achievementManager.onEvent(this, "mazesCompleted", mazesCompletedCounter);
    }

    /**
     * Updates survival mode personal best records.
     * Called when a survival game ends to check if new records were set.
     * Each of score, wave, and time is compared independently against the
     * current best and updated if exceeded.
     *
     * @param score     The final score achieved.
     * @param wave      The final wave reached.
     * @param timeAlive The total time survived in seconds.
     */
    public void updateSurvivalScore(int score, int wave, int timeAlive) {
        boolean newRecord = false;

        if (score > survivalBestScore) {
            survivalBestScore = score;
            newRecord = true;
            System.out.println("New survival best score: " + survivalBestScore);
        }

        if (wave > survivalBestWave) {
            survivalBestWave = wave;
            newRecord = true;
            System.out.println("New survival best wave: " + survivalBestWave);
        }

        if (timeAlive > survivalLongestTime) {
            survivalLongestTime = timeAlive;
            newRecord = true;
            System.out.println("New survival longest time: " + survivalLongestTime + " seconds");
        }

        if (newRecord) {
            System.out.println("=== New Survival Record Set! ===");
        }
    }


    public int getSurvivalBestScore() {
        return survivalBestScore;
    }

    public int getSurvivalBestWave() {
        return survivalBestWave;
    }


    public int getSurvivalLongestTime() {
        return survivalLongestTime;
    }

    /**
     * Returns the current upgrade level of the named skill.
     * If the skill has never been upgraded it returns 0.
     *
     * @param skillName The identifier of the skill to look up.
     * @return The skill's current level, or 0 if not yet upgraded.
     */
    public int getSkillLevel(String skillName) {
        return skillLevels.getOrDefault(skillName, 0);
    }
}