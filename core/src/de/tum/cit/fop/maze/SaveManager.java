package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Utility for saving and loading player profiles using JSON serialization.
 */
public class SaveManager {
    private static final Json json = new Json();

    static {
        json.setOutputType(JsonWriter.OutputType.json);
    }

    /** Saves a specific profile. */
    public static void save(GameState state) {
        String fileName = "Profile_" + state.playerName + ".json";
        Gdx.files.local(fileName).writeString(json.prettyPrint(state), false);
    }

    /** Loads or creates a profile by name. */
    public static GameState loadProfile(String name) {
        String fileName = "Profile_" + name + ".json";
        if (!Gdx.files.local(fileName).exists()) {
            GameState state = new GameState();
            state.playerName = name;
            return state;
        }
        return json.fromJson(GameState.class, Gdx.files.local(fileName).readString());
    }


    /** * Scans the local directory for all saved profiles.
     * @return A list of player names found.
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