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

    // Made static so it is accessible globally for definitions
    private static AchievementManager achievementManager = new AchievementManager();

    // --- Skill Points (Currency) ---
    public int warriorPoints = 0;
    public int swiftnessPoints = 0;
    public int vitalityPoints = 0;

    // --- Total Stats (For Achievements - THESE NEVER RESET) ---
    // The AchievementManager watches these specific variable names ("enemiesKilled", etc)
    public int enemiesKilledCounter = 0;       // Total lifetime kills
    public float distanceSprintedCounter = 0;  // Total lifetime distance
    public int heartsCollectedCounter = 0;     // Total lifetime hearts

    // --- Progress Trackers (For Skill Points - THESE RESET) ---
    // These track "progress toward the next point"
    public int killsProgress = 0;
    public float sprintProgress = 0;
    public int heartsProgress = 0;

    // --- Permanent Upgrades & Achievements ---
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

    public int getSkillLevel(String skillName) {
        return skillLevels.getOrDefault(skillName, 0);
    }
}