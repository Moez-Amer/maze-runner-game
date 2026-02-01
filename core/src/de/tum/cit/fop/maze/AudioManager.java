package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.HashMap;

/**
 * Manager class for handling all game sound effects (SFX) and background music (BGM).
 * This class provides a centralized, static interface to manage audio assets,
 * handle volume settings (Main, Music, SFX), and persist audio preferences to local storage.
 * It ensures that audio resources are loaded once and properly disposed of to prevent memory leaks.
 */
public class AudioManager {

    private static Sound pickupSound;
    private static Sound potionPickupSound;
    private static Sound pickupKeySound;
    private static Sound hitSound;
    private static Sound hitWithShieldSound;
    private static Sound attackSound;
    private static Sound gotoTheNextLevelSound;
    private static Sound reaperSound;

    private static Music gameOverMusic;
    private static Music victoryMusic;
    private static Music menuMusic;
    private static Music gameMusic;

    private static float mainVolume = 1.0f;
    private static float musicVolume = 1.0f;
    private static float sfxVolume = 1.0f;

    // Flag to ensure assets are only loaded once to prevent multiple music instances
    private static boolean loaded = false;

    /**
     * Loads all audio assets from the internal storage and restores saved volume settings.
     * This method initializes {@link Sound} and {@link Music} objects for the entire game.
     * It should be called once during the game's startup (e.g., in {@link MazeRunnerGame#create()}).
     */
    public static void load() {
        if (loaded) return;

        pickupSound =Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Pickup.wav"));
        potionPickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/potionPickup.wav"));
        pickupKeySound =Gdx.audio.newSound(Gdx.files.internal("audio/sfx/PickupKey.wav"));
        hitSound =Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Hit.wav"));
        hitWithShieldSound =Gdx.audio.newSound(Gdx.files.internal("audio/sfx/HitWithShield.wav"));
        reaperSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/ReaperSound.wav"));
        attackSound =Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Attack.wav"));
        gotoTheNextLevelSound=Gdx.audio.newSound(Gdx.files.internal("audio/sfx/GotoNextLevel.wav"));
        gameOverMusic=Gdx.audio.newMusic(Gdx.files.internal("audio/sfx/Gameover.wav"));
        victoryMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/Victory.wav"));
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/MenuMusic.wav"));
        menuMusic.setLooping(true);
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/GameMusic.wav"));
        gameMusic.setLooping(true);

        loadSettings();
        updateMusicVolume();
        // Mark as loaded to prevent future redundant initializations
        loaded = true;
    }

    /**
     * Sets the main (master) volume level and updates currently playing music.
     * @param vol The new volume level (0.0 to 1.0).
     */
    public static void setMainVolume(float vol) {
        mainVolume = vol;
        updateMusicVolume();
    }
    /** @return The current master volume (0.0 to 1.0). */
    public static float getMainVolume() {
        return mainVolume;
    }
    /**
     * Sets the background music (BGM) volume multiplier.
     * @param vol The new music volume multiplier (0.0 to 1.0).
     */
    public static void setMusicVolume(float vol) {
        musicVolume = vol;
        updateMusicVolume();
    }
    /** @return The current music volume multiplier (0.0 to 1.0). */
    public static float getMusicVolume() {
        return musicVolume;
    }
    /**
     * Sets the sound effects (SFX) volume multiplier.
     * @param volume The new SFX volume multiplier (0.0 to 1.0).
     */
    public static void setSFXVolume(float volume) {
        sfxVolume = volume;
    }
    /** @return The current SFX volume multiplier (0.0 to 1.0). */
    public static float getSFXVolume() {
        return sfxVolume;
    }
    /**
     * Synchronizes the volume of all active music tracks based on Main and Music settings.
     */
    private static void updateMusicVolume() {
        float vol = mainVolume * musicVolume;
        if (menuMusic != null) menuMusic.setVolume(vol);
        if (gameMusic != null) gameMusic.setVolume(vol);
        if (victoryMusic != null) victoryMusic.setVolume(vol);
    }
    /**
     * Calculates the final playback volume for sound effects.
     *
     * @return The combined volume of Main * SFX settings.
     */
    private static float getSFXVol() {
        return mainVolume * sfxVolume;
    }

/**
     * Plays the generic item pickup sound effect.
     */
    public static void playPickupSound() {
        if (pickupSound != null) {
            pickupSound.play(getSFXVol());
        }
    }

    /**
     * Plays the sound effect for picking up a potion (Speed, Power, or Shield).
     * This uses the potionPickup.wav asset and scales the volume based on
     * the combined Main and SFX volume settings.
     */
    public static void playPotionPickupSound() {
        if (potionPickupSound != null) {
            potionPickupSound.play(getSFXVol());
        }
    }


    /**
     * Plays the specific key pickup sound effect.
     */
    public static void playPickupKeySound() {
        if (pickupKeySound != null){
            pickupKeySound.play(getSFXVol());
        }
    }
    /**
     * Plays the damage taken sound effect.
     */
    public static void playHitSound() {
        if (hitSound != null){
            hitSound.play(getSFXVol());
        }
    }
    /**
     * Plays the shield block sound effect.
     */
    public static void playHitWithShieldSound() {
        if (hitWithShieldSound != null) {
            hitWithShieldSound.play(getSFXVol());
        }
    }
    /**
     * Plays the player attack sound effect.
     */
    public static void playAttackSound() {
        if (attackSound != null){
            attackSound.play(getSFXVol());
        }
    }
    /**
     * Plays the victory music track once.
     */
    public static void playVictorySound() {
        if (victoryMusic != null) {
            victoryMusic.setVolume(getMainVolume() * getMusicVolume());
            victoryMusic.play();
        }
    }
    /**
     * Plays the level transition sound effect.
     */
    public static void playGotoNextLevelSound() {
        if (gotoTheNextLevelSound != null){
            gotoTheNextLevelSound.play(getSFXVol());
        }
    }
    /**
     * Plays the music when player failed.
     */
    public static void playGameOverMusic() {
        if (gameOverMusic != null) {
            gameOverMusic.play();
        }
    }

    /** Stops any active gameplay music and starts the main menu music. */
    public static void playMenuMusic() {
        if (gameMusic != null && gameMusic.isPlaying()) {
            gameMusic.stop();
        }
        if (menuMusic != null && !menuMusic.isPlaying()){
            menuMusic.play();
        }

    }
    /** * Stops all other music and starts the gameplay music.
     * This ensures no music overlap when entering a level.
     */
    public static void playGameMusic() {
        // Stop ALL music first to prevent overlaps
        if (menuMusic != null) {
            menuMusic.stop();
        }
        if (victoryMusic != null) {
            victoryMusic.stop();
        }
        if (gameOverMusic != null) {
            gameOverMusic.stop();
        }
        // Now start game music
        if (gameMusic != null && !gameMusic.isPlaying()) {
            gameMusic.play();
        }
    }
    /**
     * Serializes and saves the current volume settings to "audio.json" in local storage.
     */
    public static void saveSettings() {
        try {
            HashMap<String, Float> data = new HashMap<>();
            data.put("main", mainVolume);
            data.put("music", musicVolume);
            data.put("sfx", sfxVolume);

            Json json = new Json();
            String str = json.prettyPrint(data);
            Gdx.files.local("audio.json").writeString(str, false);
        } catch (Exception e) {
            System.err.println("Audio save failed: " + e.getMessage());
        }
    }
    /**
     * Loads volume settings from "audio.json". If the file is missing, defaults to 1.0f.
     */
    public static void loadSettings() {
        try {
            FileHandle file = Gdx.files.local("audio.json");
            if (file.exists()) {

                Json json = new Json();
                HashMap<String, Float> data = json.fromJson(HashMap.class, file);

                mainVolume = data.getOrDefault("main", 1.0f);
                musicVolume = data.getOrDefault("music", 1.0f);
                sfxVolume = data.getOrDefault("sfx", 1.0f);
            }
        } catch (Exception e) {
            System.err.println("Audio load failed: " + e.getMessage());
        }
    }

    /**
     * Plays the reaper/chase sound effect.
     * This sound is triggered when an {@link Enemy} starts chasing the player.
     * * @return The ID of the sound instance, which can be used to stop or modify the sound later.
     */
    public static void playReaperSound() {
        if (reaperSound != null) {
            reaperSound.play(getSFXVol());
        }
    }
    /** Stops all currently playing music tracks. */
    public static void stopMusic() {
        if(gameMusic != null){
            gameMusic.stop();
        }
        if(menuMusic != null){
            menuMusic.stop();
        }
        if(victoryMusic != null){
            victoryMusic.stop();
        }

        if(gameOverMusic != null){
            gameOverMusic.stop();
        }
    }

    /**
     * Disposes of all loaded audio resources to free up memory.
     * This method must be called when the game is closed to prevent memory leaks.
     */
    public static void dispose() {
        if (pickupSound != null) pickupSound.dispose();
        if (potionPickupSound != null) potionPickupSound.dispose();
        if (pickupKeySound != null) pickupKeySound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (hitWithShieldSound != null) hitWithShieldSound.dispose();
        if (attackSound != null) attackSound.dispose();
        if (victoryMusic != null) victoryMusic.dispose();
        if (gotoTheNextLevelSound != null) gotoTheNextLevelSound.dispose();
        if (menuMusic != null) menuMusic.dispose();
        if (gameMusic != null) gameMusic.dispose();
        if (gameOverMusic != null) gameOverMusic.dispose();
        loaded = false;
    }
}