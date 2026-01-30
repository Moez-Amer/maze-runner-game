package de.tum.cit.fop.maze;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistently stores player progress, including distinct skill points earned
 * through specific gameplay actions.
 */
public class GameState {
    public String playerName = "Guest";

    private static AchievementManager achievementManager = new AchievementManager();

    public int warriorPoints = 0;
    public int swiftnessPoints = 0;
    public int vitalityPoints = 0;
    public int enemiesKilledCounter = 0;       // Total lifetime kills
    public float distanceSprintedCounter = 0;  // Total lifetime distance
    public int heartsCollectedCounter = 0;     // Total lifetime hearts
    public int tilesExploredCounter = 0;       // Total lifetime tiles explored
    public int keysCollectedCounter = 0;       // Total lifetime keys collected
    public int scrollsCollectedCounter = 0;
    public int coinsCollectedCounter = 0;      // Total lifetime coins collected
    public int potionsUsedCounter = 0;         // Total lifetime potions used
    public int perfectMazesCounter = 0;        // Total lifetime perfect mazes (no damage)
    public int mazesCompletedCounter = 0;      // Total lifetime mazes completed
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
     * @return The AchievementManager.
     */
    public static AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public void recordKill() {
        // 1. Update Total (For Achievements) - continuously grows 1, 2, 3, 4, 5...
        enemiesKilledCounter++;
        achievementManager.onEvent(this, "enemiesKilled", enemiesKilledCounter);

        // 2. Update Progress (For Skill Points) - resets 1, 2 -> 0
        killsProgress++;
        if (killsProgress >= 2) {
            warriorPoints++;
            killsProgress = 0; // Only reset the progress tracker, not the total!
        }
    }

    public void recordSprinting(float distance) {
        // 1. Update Total
        distanceSprintedCounter += distance;
        achievementManager.onEvent(this, "distanceSprinted", distanceSprintedCounter);

        // 2. Update Progress
        sprintProgress += distance;
        if (sprintProgress >= 2000) {
            swiftnessPoints++;
            sprintProgress = 0;
        }
    }

    public void recordHeartPickup() {
        // 1. Update Total
        heartsCollectedCounter++;
        achievementManager.onEvent(this, "heartsCollected", heartsCollectedCounter);

        // 2. Update Progress
        heartsProgress++;
        if (heartsProgress >= 2) {
            vitalityPoints++;
            heartsProgress = 0;
        }
    }

    public void recordTileExplored() {
        tilesExploredCounter++;
        achievementManager.onEvent(this, "tilesExplored", tilesExploredCounter);
    }

    public void recordKeyCollected() {
        keysCollectedCounter++;
        achievementManager.onEvent(this, "keysCollected", keysCollectedCounter);
    }

    public void recordCoinCollected() {
        coinsCollectedCounter++;
        achievementManager.onEvent(this, "coinsCollected", coinsCollectedCounter);
    }

    public void recordPotionUsed() {
        potionsUsedCounter++;
        achievementManager.onEvent(this, "potionsUsed", potionsUsedCounter);
    }

    public void recordScrollCollected() {
        scrollsCollectedCounter++;
        // This string "scrollsCollected" must match the statName in your JSON
        achievementManager.onEvent(this, "scrollsCollected", scrollsCollectedCounter);
    }

    public void recordPerfectMaze() {
        perfectMazesCounter++;
        achievementManager.onEvent(this, "perfectMazes", perfectMazesCounter);
    }

    public void recordMazeCompleted() {
        mazesCompletedCounter++;
        achievementManager.onEvent(this, "mazesCompleted", mazesCompletedCounter);
    }

    /**
     * Updates survival mode personal best records.
     * Called when a survival game ends to check if new records were set.
     *
     * @param score     The final score achieved
     * @param wave      The final wave reached
     * @param timeAlive The total time survived in seconds
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

    /**
     * Gets the best score achieved in survival mode.
     *
     * @return The highest score
     */
    public int getSurvivalBestScore() {
        return survivalBestScore;
    }

    /**
     * Gets the highest wave reached in survival mode.
     *
     * @return The highest wave number
     */
    public int getSurvivalBestWave() {
        return survivalBestWave;
    }

    /**
     * Gets the longest time survived in survival mode.
     *
     * @return The longest survival time in seconds
     */
    public int getSurvivalLongestTime() {
        return survivalLongestTime;
    }

    public int getSkillLevel(String skillName) {
        return skillLevels.getOrDefault(skillName, 0);
    }
}