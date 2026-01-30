package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Utility class for managing the persistence of player game states.
 * This class handles saving and loading player profiles to and from local JSON files.
 * It uses LibGDX's JSON serialization to store complex objects like {@link GameState}.
 */
public class SaveManager {
    private static final Json json = new Json();

    static {
        json.setOutputType(JsonWriter.OutputType.json);
    }

    /**
     * Saves the provided game state to a local JSON file.
     * The file is named using the pattern "Profile_[PlayerName].json".
     * This method overwrites any existing save file for that player.
     *
     * @param state The GameState object containing all player progress and stats to be saved.
     */
    public static void save(GameState state) {
        String fileName = "Profile_" + state.playerName + ".json";
        Gdx.files.local(fileName).writeString(json.prettyPrint(state), false);
    }

    /**
     * Loads a player profile by name from the local storage.
     * If a file corresponding to the name exists, it is deserialized into a GameState object.
     * If no such file exists, a new, empty GameState is created for that player.
     *
     * @param name The name of the player profile to load (case-sensitive).
     * @return The loaded GameState object, or a new GameState instance if the profile did not exist.
     */
    public static GameState loadProfile(String name) {
        String fileName = "Profile_" + name + ".json";
        if (!Gdx.files.local(fileName).exists()) {
            GameState state = new GameState();
            state.playerName = name;
            return state;
        }
        return json.fromJson(GameState.class, Gdx.files.local(fileName).readString());
    }

    /**
     * Scans the local directory for all valid saved profile files.
     * It looks for files starting with "Profile_" and ending with ".json".
     *
     * @return A list of strings containing the names of all found player profiles.
     */
    public static java.util.List<String> getAllProfileNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        com.badlogic.gdx.files.FileHandle[] files = Gdx.files.local(".").list();
        for (com.badlogic.gdx.files.FileHandle file : files) {
            if (file.name().startsWith("Profile_") && file.name().endsWith(".json")) {
                names.add(file.name().replace("Profile_", "").replace(".json", ""));
            }
        }
        return names;
    }
}