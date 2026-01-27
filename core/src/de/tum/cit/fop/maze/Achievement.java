package de.tum.cit.fop.maze;

/**
 * Data model for an achievement loaded from an external file.
 */
public class Achievement {
    public String id;
    public String name;
    public String description;
    public String statName;   // e.g., "enemiesKilled", "distanceSprinted"
    public float targetValue; // Value needed to unlock
    public String iconPath;   // Path to achievement icon image
}