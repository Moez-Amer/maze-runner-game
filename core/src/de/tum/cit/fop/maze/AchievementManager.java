package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the loading, tracking, and unlocking of achievements.
 * Implements the Observer pattern to notify the UI when an achievement is unlocked.
 */
public class AchievementManager {
    private List<Achievement> achievements;
    private AchievementListener listener;

    /**
     * Interface for listening to achievement unlock events.
     * Used by the GameScreen/HUD to display notifications.
     */
    public interface AchievementListener {
        /**
         * Called when an achievement is successfully unlocked.
         * @param achievement The achievement object that was unlocked.
         */
        void onAchievementUnlocked(Achievement achievement);
    }

    /**
     * Initializes the manager and loads achievement definitions from JSON.
     */
    public AchievementManager() {
        loadAchievements();
    }

    public void setListener(AchievementListener listener) {
        this.listener = listener;
    }

    public List<Achievement> getAllAchievements() {
        return achievements;
    }

    /**
     * Loads achievement definitions from the internal 'achievements.json' file.
     */
    private void loadAchievements() {
        try {
            Json json = new Json();
            achievements = json.fromJson(ArrayList.class, Achievement.class, Gdx.files.internal("achievements.json"));
            if (achievements == null) achievements = new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Failed to load achievements: " + e.getMessage());
            achievements = new ArrayList<>();
        }
    }

    /**
     * Evaluates game events to check if any achievements should be unlocked.
     * @param state The current game state containing player progress.
     * @param statName The name of the statistic that was updated (e.g., "enemiesKilled").
     * @param currentValue The new value of the statistic.
     */
    public void onEvent(GameState state, String statName, float currentValue) {
        if (achievements == null) return;

        for (Achievement ach : achievements) {
            if (ach.statName.equals(statName) && !state.unlockedAchievements.contains(ach.id)) {
                if (currentValue >= ach.targetValue) {
                    unlock(state, ach);
                }
            }
        }
    }

    /**
     * Internal method to process the unlocking logic.
     * Updates the state and notifies listeners.
     * @param state The game state to update.
     * @param ach The achievement to unlock.
     */
    private void unlock(GameState state, Achievement ach) {
        state.unlockedAchievements.add(ach.id);
        System.out.println("ACHIEVEMENT UNLOCKED: " + ach.name + " - " + ach.description);
        if (listener != null) {
            listener.onAchievementUnlocked(ach);
        }
    }
}