package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * A reusable full-screen cutscene screen backed by the gdx-video extension.
 * <p>
 * The screen plays a single video file and, once playback completes or the
 * player skips, fires a caller-supplied {@link Runnable} callback so that
 * the owning screen or game class can navigate to the next destination
 * without this screen needing to know what comes next.
 * </p>
 * <p>
 * Several defensive measures are built in to handle environments where the
 * video back-end is unavailable or the file is missing:
 * <ul>
 *   <li>If {@link VideoPlayerCreator#createVideoPlayer()} returns
 *       {@code null} the screen transitions immediately.</li>
 *   <li>If the requested file does not exist on disk the screen logs an
 *       error and transitions immediately.</li>
 *   <li>A {@link #FALLBACK_TIMEOUT}-second watchdog fires when no frame
 *       has been received, allowing the game to continue even if the
 *       decoder hangs.</li>
 * </ul>
 * </p>
 * <p>
 * The video is letterboxed into the viewport: it is scaled uniformly to
 * fit the smaller axis and centred on the other.  A short skip-hint label
 * is drawn after {@link #SKIP_DELAY} seconds when skipping is enabled.
 * </p>
 */
public class CutsceneVideoScreen implements Screen {

    private static final String TAG = "CutsceneVideoScreen";

    private final MazeRunnerGame game;
    private final String videoPath;
    private final Runnable onFinished;
    private final boolean allowSkip;

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private Viewport viewport;

    private VideoPlayer videoPlayer;
    private boolean videoReady = false;
    private boolean videoFinished = false;
    private boolean transitioned = false;

    private int videoWidth = 0;
    private int videoHeight = 0;

    /** Accumulates elapsed time; skip input is ignored until this exceeds {@link #SKIP_DELAY}. */
    private float skipTimer = 0f;
    /** Minimum seconds before the player is allowed to skip the video. */
    private static final float SKIP_DELAY = 0.5f;

    /** Accumulates elapsed time since the last successfully decoded frame. */
    private float fallbackTimer = 0f;
    /** Maximum seconds to wait without a decoded frame before giving up. */
    private static final float FALLBACK_TIMEOUT = 5.0f;

    /**
     * Creates a new CutsceneVideoScreen with full control over skip behaviour.
     *
     * @param game       The main {@link MazeRunnerGame} instance, used to access
     *                   shared resources such as the {@link SpriteBatch}.
     * @param videoPath  Path to the video file relative to the assets root
     *                   (e.g. {@code "trailer.webm"}).
     * @param onFinished A {@link Runnable} executed (via
     *                   {@link com.badlogic.gdx.Application#postRunnable}) when
     *                   playback completes or the player skips.  Must not be
     *                   {@code null}.
     * @param allowSkip  {@code true} to let the player press ESC, SPACE, or
     *                   ENTER to exit early; {@code false} to force the full
     *                   video to play.
     */
    public CutsceneVideoScreen(MazeRunnerGame game, String videoPath, Runnable onFinished, boolean allowSkip) {
        this.game = game;
        this.videoPath = videoPath;
        this.onFinished = onFinished;
        this.allowSkip = allowSkip;
    }

    /**
     * Convenience constructor that enables skipping by default.
     *
     * @param game       The main {@link MazeRunnerGame} instance.
     * @param videoPath  Path to the video file relative to the assets root.
     * @param onFinished A {@link Runnable} executed when playback finishes or
     *                   is skipped.
     */
    public CutsceneVideoScreen(MazeRunnerGame game, String videoPath, Runnable onFinished) {
        this(game, videoPath, onFinished, true);
    }

    /**
     * Initialises rendering resources and begins video playback.
     * <p>
     * A {@link SpriteBatch}, {@link BitmapFont}, {@link OrthographicCamera},
     * and {@link FitViewport} are created.  The {@link VideoPlayer} is then
     * obtained from the gdx-video back-end and the target file is opened.
     * If any step fails the screen transitions immediately via
     * {@link #finishAndTransition} so that the game is never left in a
     * broken state.
     * </p>
     */
    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        camera = new OrthographicCamera();
        viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        viewport.apply(true);

        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();

            if (videoPlayer == null) {
                Gdx.app.error(TAG, "VideoPlayerCreator returned null - video backend not available");
                finishAndTransition();
                return;
            }
            videoPlayer.setOnCompletionListener(file -> {
                Gdx.app.log(TAG, "Video playback completed");
                videoFinished = true;
            });

            videoPlayer.setOnVideoSizeListener((width, height) -> {
                Gdx.app.log(TAG, "Video size: " + width + "x" + height);
                videoWidth = (int) width;
                videoHeight = (int) height;
            });

            FileHandle videoFile = Gdx.files.internal(videoPath);
            if (!videoFile.exists()) {
                Gdx.app.error(TAG, "Video file not found: " + videoPath);
                finishAndTransition();
                return;
            }

            videoPlayer.play(videoFile);
            videoReady = true;
            Gdx.app.log(TAG, "Video playback started: " + videoPath);

        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to initialize video player: " + e.getMessage(), e);
            finishAndTransition();
        }
    }

    /**
     * Processes one frame of the cutscene.
     * <p>
     * Each frame the method advances both the skip and fallback timers,
     * checks for skip input, polls the {@link VideoPlayer} for a new
     * texture, and draws it centred and letterboxed into the viewport.
     * When no frame has been decoded the loading placeholder is shown
     * instead.  The fallback watchdog fires if {@link #FALLBACK_TIMEOUT}
     * elapses without a successful decode.
     * </p>
     *
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {
        if (transitioned) return;

        skipTimer += delta;
        fallbackTimer += delta;

        if (allowSkip && skipTimer > SKIP_DELAY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                Gdx.app.log(TAG, "Video skipped by user");
                finishAndTransition();
                return;
            }
        }

        if (videoFinished) {
            finishAndTransition();
            return;
        }

        if (fallbackTimer > FALLBACK_TIMEOUT && !videoReady) {
            Gdx.app.error(TAG, "Video failed to start within timeout, skipping");
            finishAndTransition();
            return;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (videoPlayer != null && videoReady) {
            try {
                videoPlayer.update();

                Texture frameTexture = videoPlayer.getTexture();

                if (frameTexture != null) {
                    fallbackTimer = 0f;

                    float screenWidth = viewport.getWorldWidth();
                    float screenHeight = viewport.getWorldHeight();

                    float texWidth = videoWidth > 0 ? videoWidth : frameTexture.getWidth();
                    float texHeight = videoHeight > 0 ? videoHeight : frameTexture.getHeight();

                    float scaleX = screenWidth / texWidth;
                    float scaleY = screenHeight / texHeight;
                    float scale = Math.min(scaleX, scaleY);

                    float drawWidth = texWidth * scale;
                    float drawHeight = texHeight * scale;
                    float drawX = (screenWidth - drawWidth) / 2f;
                    float drawY = (screenHeight - drawHeight) / 2f;

                    TextureRegion region;
                    if (videoWidth > 0 && videoHeight > 0 &&
                            (frameTexture.getWidth() != videoWidth || frameTexture.getHeight() != videoHeight)) {
                        region = new TextureRegion(frameTexture, 0, 0, videoWidth, videoHeight);
                    } else {
                        region = new TextureRegion(frameTexture);
                    }

                    batch.setProjectionMatrix(camera.combined);
                    batch.begin();
                    batch.draw(region, drawX, drawY, drawWidth, drawHeight);

                    if (allowSkip && skipTimer > SKIP_DELAY) {
                        font.getData().setScale(0.8f);
                        font.setColor(1f, 1f, 1f, 0.6f);
                        font.draw(batch, "Press SPACE or ESC to skip", 20f, 40f);
                        font.getData().setScale(1f);
                        font.setColor(Color.WHITE);
                    }

                    batch.end();
                } else {
                    drawLoadingScreen();
                }

            } catch (Exception e) {
                Gdx.app.error(TAG, "Error during video playback: " + e.getMessage(), e);
                finishAndTransition();
            }
        } else {
            drawLoadingScreen();
        }
    }

    /**
     * Draws a centred "Loading video…" placeholder and, after the skip
     * delay, an optional skip-hint label.  This is shown whenever the
     * {@link VideoPlayer} has not yet produced a decodable frame.
     */
    private void drawLoadingScreen() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(1f, 1f, 1f, 0.8f);
        font.draw(batch, "Loading video...", viewport.getWorldWidth() / 2f - 60f, viewport.getWorldHeight() / 2f);

        if (allowSkip && skipTimer > SKIP_DELAY) {
            font.getData().setScale(0.8f);
            font.setColor(1f, 1f, 1f, 0.5f);
            font.draw(batch, "Press SPACE or ESC to skip", 20f, 40f);
            font.getData().setScale(1f);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Stops playback, disposes the video player, and posts the
     * {@link #onFinished} callback onto the main thread via
     * {@link com.badlogic.gdx.Application#postRunnable}.
     * A {@link #transitioned} guard ensures the callback is fired at
     * most once even if multiple termination paths race.
     */
    private void finishAndTransition() {
        if (transitioned) return;
        transitioned = true;

        Gdx.app.log(TAG, "Transitioning from video screen");

        disposeVideoPlayer();

        if (onFinished != null) {
            Gdx.app.postRunnable(onFinished);
        }
    }

    /**
     * Safely stops and disposes the {@link VideoPlayer}.
     * Both {@code stop()} and {@code dispose()} are wrapped in
     * individual try-catch blocks so that a failure in one does not
     * prevent the other from executing.  The reference is set to
     * {@code null} afterwards to allow GC and to make subsequent
     * null-checks reliable.
     */
    private void disposeVideoPlayer() {
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                videoPlayer.dispose();
            } catch (Exception ignored) {
            }
            videoPlayer = null;
        }
    }

    /**
     * Recentres the camera and updates the viewport when the window is
     * resized so that the letterboxing calculation in {@link #render}
     * remains correct.
     *
     * @param width  The new window width in pixels.
     * @param height The new window height in pixels.
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0f);
        camera.update();
    }

    /**
     * Pauses video playback when the application loses focus.
     * Failure is silently ignored so that a broken back-end does not
     * crash the application on alt-tab.
     */
    @Override
    public void pause() {
        if (videoPlayer != null) {
            try {
                videoPlayer.pause();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Resumes video playback when the application regains focus,
     * provided the video has not already finished.  Failure is
     * silently ignored for the same reason as {@link #pause}.
     */
    @Override
    public void resume() {
        if (videoPlayer != null && !videoFinished) {
            try {
                videoPlayer.resume();
            } catch (Exception ignored) {
            }
        }
    }

    /** No-op; cleanup is handled by {@link #dispose}. */
    @Override
    public void hide() {
    }

    /**
     * Releases all resources owned by this screen.
     * The video player is stopped and disposed first, then the
     * {@link SpriteBatch} and {@link BitmapFont} are disposed.
     * Each resource is null-checked before disposal to make this
     * method safe to call multiple times.
     */
    @Override
    public void dispose() {
        Gdx.app.log(TAG, "Disposing CutsceneVideoScreen");

        disposeVideoPlayer();

        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}