package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

import java.util.HashMap;
/**
 * Manages custom key bindings for game actions using the Singleton pattern.
 * Handles loading, saving, and checking input states for mapped actions.
 */
public class KeyBindings {

    private static KeyBindings keyBindings;

    /**
     * Retrieves the global singleton instance of KeyBindings.
     * Creates the instance if it does not exist.
     *
     * @return The singleton KeyBindings instance.
     */
    public static KeyBindings getKeyBindings() {
        if (keyBindings == null) {
            keyBindings = new KeyBindings();
        }
        return keyBindings;
    }

    private final HashMap<String, Integer> bindings;
    private static final String BINDINGS_FILE = "keybindings.json";

    /**
     * Private constructor to enforce singleton usage.
     * Initializes default bindings and loads any saved configurations.
     */
    private KeyBindings() {
        bindings = new HashMap<>();
        setDefaultBindings();
        loadBindings();
    }
    /**
     * Loads key bindings from the local JSON file.
     * Falls back to defaults if the file is missing or invalid.
     */
    private void loadBindings() {
        FileHandle file = Gdx.files.local(BINDINGS_FILE);

        if (file.exists()) {
            try {
                Json json = new Json();
                @SuppressWarnings("unchecked")
                HashMap<String, String> data = json.fromJson(HashMap.class, file);

                for (String action : data.keySet()) {
                    String keyName = data.get(action);
                    int keyCode = getKeyCodeFromName(keyName);
                    if (keyCode != -1) {
                        bindings.put(action, keyCode);
                    }
                }
                System.out.println("Key bindings loaded from file");
            } catch (Exception e) {
                System.err.println("Failed to load bindings: " + e.getMessage());
            }
        } else {
            System.out.println("No bindings file found, using defaults");
            saveBindings();
        }
    }
    /**
     * Resets all actions to their hardcoded default key mappings.
     */
    public void setDefaultBindings() {
        bindings.clear();
        bindings.put("Move Up", Input.Keys.UP);
        bindings.put("Move Down", Input.Keys.DOWN);
        bindings.put("Move Left", Input.Keys.LEFT);
        bindings.put("Move Right", Input.Keys.RIGHT);
        bindings.put("Attack", Input.Keys.SPACE);
        bindings.put("Sprint", Input.Keys.SHIFT_LEFT);
        bindings.put("Debug", Input.Keys.K);
        bindings.put("Open Console", Input.Keys.T);
        System.out.println("Default bindings set");
    }
    /**
     * Saves the current key configuration to a local JSON file.
     */
    public void saveBindings() {
        try {
            HashMap<String, String> data = new HashMap<>();
            for (String action : bindings.keySet()) {
                int keyCode = bindings.get(action);
                String keyName = Input.Keys.toString(keyCode);
                data.put(action, keyName);
            }

            Json json = new Json();
            String jsonString = json.prettyPrint(data);
            FileHandle file = Gdx.files.local(BINDINGS_FILE);
            file.writeString(jsonString, false);
            System.out.println("Bindings saved");
        } catch (Exception e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }
    /**
     * Gets the integer key code assigned to a specific action.
     *
     * @param action The action name (e.g., "Move Up").
     * @return The LibGDX Input.Keys code, or Input.Keys.UNKNOWN.
     */
    public int getKey(String action) {
        Integer keyCode = bindings.get(action);
        if (keyCode == null) {
               return Input.Keys.UNKNOWN;
        }
        return keyCode;
    }
    /**
     * Binds a new key code to a specific action.
     *
     * @param action The action to rebind.
     * @param keyCode The new key code to assign.
     */
    public void setKey(String action, int keyCode) {
        bindings.put(action, keyCode);
    }
    /**
     * Returns a user-friendly display name for a key code.
     *
     * @param keyCode The key code to lookup.
     * @return A readable string (e.g., "Space", "Shift").
     */
    public String getKeyDisplayName(int keyCode) {
        switch (keyCode) {
            case Input.Keys.UP: return "UP";
            case Input.Keys.DOWN: return "DOWN";
            case Input.Keys.LEFT: return "LEFT";
            case Input.Keys.RIGHT: return "RIGHT";
            case Input.Keys.SPACE: return "Space";
            case Input.Keys.SHIFT_LEFT,Input.Keys.SHIFT_RIGHT
                    : return "Shift";
            case Input.Keys.UNKNOWN: return "?";
            default:
                String name = Input.Keys.toString(keyCode);
                return name != null ? name : "?";
        }
    }
    /**
     * Finds the action name currently bound to a specific key code.
     * Used for detecting conflicts.
     *
     * @param keyCode The key code to check.
     * @return The action name, or null if unbound.
     */
    public String getActionForKey(int keyCode) {
        for (String action : bindings.keySet()) {
            if (bindings.get(action) == keyCode) {
                return action;
            }
        }
        return null;
    }
    /**
     * Checks if the key mapped to the action is currently held down.
     *
     * @param action The action to check.
     * @return True if the key is pressed.
     */
    public boolean isKeyPressed(String action) {
        int key = getKey(action);
        if (key == Input.Keys.UNKNOWN) return false;
        return Gdx.input.isKeyPressed(key);
    }
    /**
     * Checks if the key mapped to the action was pressed this frame.
     *
     * @param action The action to check.
     * @return True if the key was just pressed.
     */
    public boolean isKeyJustPressed(String action) {
        int key = getKey(action);
        if (key == Input.Keys.UNKNOWN) return false;
        return Gdx.input.isKeyJustPressed(key);
    }
    /**
     * Converts a key name string back to an integer code using reflection.
     *
     * @param keyName The string name of the key (e.g., "UP").
     * @return The integer key code, or -1 on failure.
     */
    private int getKeyCodeFromName(String keyName) {
        try {
            java.lang.reflect.Field field = Input.Keys.class.getField(keyName);
            return field.getInt(null);
        } catch (Exception e) {
            System.err.println("Unknown key name: " + keyName);
            return -1;
        }
    }
}