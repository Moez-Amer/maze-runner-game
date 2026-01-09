package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Manager class for handling all game sound effects and background music.
 * Provides static methods to play specific sounds from anywhere in the game.
 */

public class AudioManager {
    // Sound: Small audio clips loaded into RAM for low-latency playback (e.g., collisions, pickups)
    private static Sound pickupSound;
    private static Sound pickupKeySound;
    private static Sound hitSound;
    private static Sound hitWithShieldSound;
    private static Sound attackSound;
    private static Sound gotoTheNextLevelSound;
    // Music: Larger audio files streamed from disk to save memory (e.g., background tracks)
    private static Music victoryMusic;
    private static Music menuMusic;
    private static Music gameMusic;

    /**
     * Loads all audio assets from the internal storage.
     * Paths must exactly match the 'assets' directory structure.
     */
    public static void load(){
        // --- Sound Effects (SFX) Loading ---
        pickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Pickup.wav"));
        pickupKeySound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/PickupKey.wav"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Hit.wav"));
        hitWithShieldSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/HitWithShield.wav"));
        attackSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/Attack.wav"));
        gotoTheNextLevelSound=Gdx.audio.newSound(Gdx.files.internal("audio/sfx/GotoNextLevel.wav"));

        // --- Background Music (BGM) Loading ---
        // Victory is loaded as Music due to its length
        victoryMusic= Gdx.audio.newMusic(Gdx.files.internal("audio/music/Victory.wav"));

        // Menu Background Music
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/MenuMusic.mp3"));
        menuMusic.setLooping(true);
        menuMusic.setVolume(0.5f);

        // Gameplay Background Music
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/GameMusic.mp3"));
        gameMusic.setLooping(true);
        gameMusic.setVolume(0.5f);
    }

    // --- SFX Playback Methods ---

    public static void playPickupSound(){
        if(pickupSound!=null){
            pickupSound.play(1.0f);
        }
    }

    public static void playPickupKeySound(){
        if(pickupKeySound!=null){
            pickupKeySound.play(1.0f);
        }
    }

    public static void playHitSound(){
        if(hitSound!=null){
            hitSound.play(1.0f);
        }
    }

    public static void playHitWithShieldSound(){
        if(hitWithShieldSound!=null){
            hitWithShieldSound.play(1.0f);
        }
    }

    public static void playAttackSound(){
        if(attackSound!=null){
            attackSound.play(1.0f);
        }
    }

    public static void playVictorySound(){
        if(victoryMusic!=null){
            victoryMusic.setVolume(1.0f);
        }
    }

    public static void playGotoNextLevelSound(){
        if(gotoTheNextLevelSound!=null){
            gotoTheNextLevelSound.play(1.0f);
        }
    }

    /**
     * Switches to the Menu Background Music.
     * Automatically stops Gameplay music to prevent overlapping.
     */
    public static void playMenuMusic(){
        if(gameMusic!=null && gameMusic.isPlaying()){
            gameMusic.stop();
        }
        if(menuMusic!=null && !menuMusic.isPlaying()){
            menuMusic.play();
        }
    }

    /**
     * Switches to the Gameplay Background Music.
     * Stops Menu music and initiates the epic game track.
     */
    public static void playGameMusic(){
        if (menuMusic != null) menuMusic.stop();

        if(gameMusic!=null && !gameMusic.isPlaying()){
            gameMusic.play();
        }
    }


    /**
     * Releases all audio resources to prevent memory leaks.
     * Should be called when the game is closed (Main class dispose).
     */
    public static void dispose(){
        if(pickupSound!=null){
            pickupSound.dispose();
        }

        if (pickupKeySound!=null){
            pickupKeySound.dispose();
        }

        if(hitSound!=null){
            hitSound.dispose();
        }

        if(hitWithShieldSound!=null){
            hitWithShieldSound.dispose();
        }

        if(attackSound!=null){
            attackSound.dispose();
        }

        if(victoryMusic!=null){
            victoryMusic.dispose();
        }

        if(gotoTheNextLevelSound!=null){
            gotoTheNextLevelSound.dispose();
        }

        if(menuMusic!=null){
            menuMusic.dispose();
        }

        if(gameMusic!=null){
            gameMusic.dispose();
        }
    }

}
