package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.*;
import java.util.ArrayList;
import java.util.Random;

import static de.tum.cit.fop.maze.TiledToPropertiesConverter.*;

/**
 * Survival-mode variant of the main game screen.
 * <p>
 * All per-frame update and rendering logic is inherited from
 * {@link GameScreen}.  The only behavioural difference is <em>how</em>
 * enemies enter the world: instead of being pre-placed on the map at
 * load time, enemies are spawned in discrete waves managed by a
 * {@link WaveManager}.  Between waves a short transition pause gives
 * the player a brief respite before the next, harder group arrives.
 * </p>
 * <p>
 * <b>Lose condition</b> – the player is allowed one "ghost mode"
 * revival per run.  If they die a second time (or after the ghost
 * timer expires) the final score, wave, and elapsed time are recorded
 * </p>
 */
public class SurvivalGameScreen extends GameScreen {

    private WaveManager waveManager;
    private float timeAlive;
    private boolean hasSpawnedCurrentWave;
    private int totalEnemiesSpawnedThisWave;
    private Random random;

    private ArrayList<Vector2> occupiedPositions;
    private static final float MIN_SPAWN_DISTANCE = TILE_SIZE * 5;

    /**
     * Constructs the SurvivalGameScreen by loading the map via the
     * parent {@link GameScreen} constructor and then initialising
     * wave-specific state.
     * @param game    The main {@link MazeRunnerGame} instance.
     * @param mapPath Internal asset path to the map properties file.
     * @param seed    Random seed for reproducible enemy placement.
     */
    public SurvivalGameScreen(MazeRunnerGame game, String mapPath, long seed) {
        super(game, mapPath);

        System.out.println("Map dimensions: " + mapWidth + "x" + mapHeight);

        this.random = new Random(seed);
        this.occupiedPositions = new ArrayList<>();

        ArrayList<Vector2> spawnLocations = extractSpawnLocations();

        this.waveManager = new WaveManager(spawnLocations, this.random);
        this.timeAlive = 0f;
        this.hasSpawnedCurrentWave = false;

        enemies.clear();

        waveManager.startWave(1);
        this.totalEnemiesSpawnedThisWave = waveManager.getTotalEnemiesInWave();
        spawnWaveEnemies();

        this.hasSpawnedCurrentWave = true;
    }

    /**
     * Scans the loaded map and returns every tile position that is safe
     * for enemy spawning.
     * <p>
     * A tile qualifies only when it is {@code TYPE_PATH}, all eight of
     * its neighbours are walkable ({@code TYPE_PATH} or
     * {@code TYPE_ENTRY}), it lies at least {@code WALL_BUFFER} (5)
     * tiles from every map edge, and it is more than 10 tiles (Euclidean)
     * from the player's spawn point.  Each qualifying tile is converted
     * to world-pixel coordinates and added to the returned list.
     * </p>
     * @return A list of {@link Vector2} positions in world pixels that
     *         are safe for enemy placement.
     */
    private ArrayList<Vector2> extractSpawnLocations() {
        ArrayList<Vector2> locations = new ArrayList<>();

        float playerSpawnX = entry.getX() / TILE_SIZE;
        float playerSpawnY = entry.getY() / TILE_SIZE;

        System.out.println("Player spawn: (" + playerSpawnX + ", " + playerSpawnY + ")");
        System.out.println("Map boundaries: X[0-" + (mapWidth-1) + "], Y[0-" + (mapHeight-1) + "]");

        final int WALL_BUFFER = 5;

        int minX = WALL_BUFFER;
        int maxX = mapWidth - WALL_BUFFER - 1;
        int minY = WALL_BUFFER;
        int maxY = mapHeight - WALL_BUFFER - 1;

        System.out.println("Safe spawn area: X[" + minX + "-" + maxX + "], Y[" + minY + "-" + maxY + "]");

        int locationsCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (mapData[x][y] != TYPE_PATH) {
                    continue;
                }

                float distToPlayer = (float) Math.sqrt(
                        Math.pow(x - playerSpawnX, 2) + Math.pow(y - playerSpawnY, 2)
                );

                if (distToPlayer < 10) {
                    continue;
                }

                boolean allNeighborsSafe = true;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx < 0 || nx >= mapWidth || ny < 0 || ny >= mapHeight) {
                            allNeighborsSafe = false;
                            break;
                        }

                        if (mapData[nx][ny] != TYPE_PATH && mapData[nx][ny] != TYPE_ENTRY) {
                            allNeighborsSafe = false;
                            break;
                        }
                    }
                    if (!allNeighborsSafe) break;
                }

                if (!allNeighborsSafe) {
                    continue;
                }

                locations.add(new Vector2(x * TILE_SIZE, y * TILE_SIZE));
                locationsCount++;
            }
        }

        System.out.println("Found " + locationsCount + " safe spawn locations");

        if (locationsCount == 0) {
            System.err.println("ERROR: NO SAFE SPAWN LOCATIONS FOUND!");
            System.err.println("Map data sample around center:");
            int centerX = mapWidth / 2;
            int centerY = mapHeight / 2;
            for (int y = centerY - 3; y <= centerY + 3; y++) {
                for (int x = centerX - 3; x <= centerX + 3; x++) {
                    if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
                        System.err.print(mapData[x][y] + " ");
                    }
                }
                System.err.println();
            }
        }

        return locations;
    }

    /**
     * Selects a spawn position that maintains at least
     * {@link #MIN_SPAWN_DISTANCE} pixels of clearance from every
     * already-occupied position.
     * @param availableLocations The pool of pre-validated spawn tiles
     *                           in world-pixel coordinates.
     * @return A {@link Vector2} position guaranteed to be at least
     *         {@link #MIN_SPAWN_DISTANCE} from all other enemies, or
     *         the best available alternative if perfect spacing cannot
     *         be achieved.
     */
    private Vector2 getSpacedSpawnPosition(ArrayList<Vector2> availableLocations) {
        if (availableLocations.isEmpty()) {
            System.err.println("ERROR: No available spawn locations!");
            return new Vector2(
                    (mapWidth / 2) * TILE_SIZE,
                    (mapHeight / 2) * TILE_SIZE
            );
        }

        final int MAX_ATTEMPTS = 100;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Vector2 candidate = availableLocations.get(random.nextInt(availableLocations.size()));

            int tileX = (int)(candidate.x / TILE_SIZE);
            int tileY = (int)(candidate.y / TILE_SIZE);

            if (tileX < 5 || tileX >= mapWidth - 5 ||
                    tileY < 5 || tileY >= mapHeight - 5) {
                continue;
            }

            boolean tooClose = false;

            for (Vector2 occupied : occupiedPositions) {
                float dx = candidate.x - occupied.x;
                float dy = candidate.y - occupied.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < MIN_SPAWN_DISTANCE) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                occupiedPositions.add(new Vector2(candidate.x, candidate.y));
                return candidate;
            }
        }

        System.out.println("WARNING: Using fallback positioning after " + MAX_ATTEMPTS + " attempts");

        Vector2 bestCandidate = null;
        float maxMinDistance = 0;

        for (Vector2 candidate : availableLocations) {
            int tileX = (int)(candidate.x / TILE_SIZE);
            int tileY = (int)(candidate.y / TILE_SIZE);

            if (tileX < 5 || tileX >= mapWidth - 5 ||
                    tileY < 5 || tileY >= mapHeight - 5) {
                continue;
            }

            float minDistance = Float.MAX_VALUE;

            for (Vector2 occupied : occupiedPositions) {
                float dx = candidate.x - occupied.x;
                float dy = candidate.y - occupied.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                minDistance = Math.min(minDistance, distance);
            }

            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            bestCandidate = new Vector2(
                    (mapWidth / 2) * TILE_SIZE,
                    (mapHeight / 2) * TILE_SIZE
            );
        }

        occupiedPositions.add(new Vector2(bestCandidate.x, bestCandidate.y));
        return bestCandidate;
    }

    /**
     * Spawns all enemies required by the current wave.
     */
    private void spawnWaveEnemies() {
        occupiedPositions.clear();

        occupiedPositions.add(new Vector2(player.getX(), player.getY()));

        float speedMultiplier = waveManager.getSpeedMultiplier();
        float healthMultiplier = waveManager.getHealthMultiplier();

        System.out.println("\n=== Spawning Wave " + waveManager.getCurrentWave() + " ===");
        System.out.println("Enemies to spawn: " + totalEnemiesSpawnedThisWave);
        System.out.println("Speed multiplier: " + speedMultiplier);
        System.out.println("Health multiplier: " + healthMultiplier);

        ArrayList<Vector2> spawnLocations = new ArrayList<>();

        for (int i = 0; i < 2000; i++) {
            Vector2 loc = waveManager.getRandomSpawnPosition();

            int tileX = (int)(loc.x / TILE_SIZE);
            int tileY = (int)(loc.y / TILE_SIZE);

            if (tileX >= 5 && tileX < mapWidth - 5 &&
                    tileY >= 5 && tileY < mapHeight - 5) {

                if (!spawnLocations.contains(loc)) {
                    spawnLocations.add(loc);
                }
            }
        }

        System.out.println("Built spawn pool with " + spawnLocations.size() + " validated locations");

        int successfulSpawns = 0;
        for (int i = 0; i < totalEnemiesSpawnedThisWave; i++) {
            Vector2 spawnPos = getSpacedSpawnPosition(spawnLocations);

            Enemy enemy = new Enemy(
                    spawnPos.x,
                    spawnPos.y,
                    TILE_SIZE,
                    mapData,
                    "Enemy_Assets/Undead executioner puppet/png/",
                    100,
                    100
            );

            enemy.setSpeedMultiplier(speedMultiplier);
            enemy.setHealthMultiplier(healthMultiplier);

            enemy.setEnemies(enemies);
            enemy.setPlayer(player);

            enemies.add(enemy);
            successfulSpawns++;

            int spawnTileX = (int)(spawnPos.x / TILE_SIZE);
            int spawnTileY = (int)(spawnPos.y / TILE_SIZE);

            System.out.println("Enemy " + (i + 1) + "/" + totalEnemiesSpawnedThisWave +
                    " spawned at tile (" + spawnTileX + ", " + spawnTileY + ")" +
                    " - Distance from walls: left=" + spawnTileX +
                    ", right=" + (mapWidth - 1 - spawnTileX) +
                    ", bottom=" + spawnTileY +
                    ", top=" + (mapHeight - 1 - spawnTileY));
        }

        System.out.println("Successfully spawned " + successfulSpawns + " enemies");
        System.out.println("Total active enemies: " + enemies.size);
        System.out.println("=== Wave Spawn Complete ===\n");
    }

    /**
     * Advances one frame of survival-mode logic before delegating to
     * the parent {@link GameScreen#render}.
     * @param delta Time elapsed since the previous frame in seconds.
     */
    @Override
    public void render(float delta) {
        if (!player.isGhostMode()) {
            timeAlive += delta;
        }

        waveManager.update(delta);

        if (waveManager.isTransitioning() && !hasSpawnedCurrentWave) {
        } else if (!waveManager.isTransitioning() && !hasSpawnedCurrentWave) {
            totalEnemiesSpawnedThisWave = waveManager.getTotalEnemiesInWave();
            spawnWaveEnemies();
            hasSpawnedCurrentWave = true;
        } else if (waveManager.isTransitioning() && hasSpawnedCurrentWave) {
            hasSpawnedCurrentWave = false;
        }
        super.render(delta);
    }

    /**
     * Checks whether the player's current sword swing has hit any enemy
     * and, if so, applies damage scaled by the player's damage
     * multiplier.
     * <p>
     * This override adds wave-aware score awarding: when an enemy dies
     * the base kill score (100) is multiplied by the
     * {@link WaveManager#getScoreMultiplier} for the current wave before
     * being credited to the player.  All other logic (hitbox overlap,
     * attack-hit flag, kill-streak tracking, and state persistence) is
     * identical to the parent implementation.
     * </p>
     */
    @Override
    protected void checkPlayerAttackHits() {
        if (!player.isAttacking()) {
            return;
        }

        if (player.hasAttackHit()) {
            return;
        }

        com.badlogic.gdx.math.Rectangle swordHitBox = player.getSwordHitBox();

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);

            com.badlogic.gdx.math.Rectangle enemyDamageBox = enemy.getDamageHitBox();

            if (swordHitBox.overlaps(enemyDamageBox)) {
                float damage = 1.0f * player.getDamageMultiplier();

                enemy.takeDamage(damage);

                player.setAttackHasHit(true);

                if (enemy.isDead()) {
                    enemies.removeIndex(i);
                    game.getGameState().recordKill();

                    float scoreMultiplier = waveManager.getScoreMultiplier();
                    player.addScore((int)(100 * scoreMultiplier));

                    waveManager.onEnemyKilled();

                    SaveManager.save(game.getGameState());

                    killStreak++;
                    timeSinceLastKill = 0f;
                    announceKillStreak(killStreak);
                }
                break;
            }
        }
    }

    /**
     * survival mode has no exit tile to reach, so the
     * parent's win-condition check is deliberately suppressed.
     */
    @Override
    protected void checkWinCondition() {
    }

    /**
     * Checks whether the player has been permanently defeated.
     * <p>
     * On the first death the player enters ghost mode and a
     * {@link VoodooDoll} is spawned at the death location as a revive
     * anchor.  If the player dies again — or if the ghost timer
     * expires without collecting the doll — the run ends.  The final
     * score, wave reached, and total time survived are recorded in the
     * {@link GameState} and persisted before transitioning to the
     * {@link GameOverScreen}.
     * </p>
     */
    @Override
    protected void checkLoseCondition() {
        if (player.isDead() && !player.isGhostMode()) {
            if (!player.hasUsedRevive()) {
                float[] playerBox = player.getFeetCollisionBox();
                float deathX = playerBox[0];
                float deathY = playerBox[1];
                voodooDoll = new VoodooDoll(deathX, deathY, TILE_SIZE * 1.5f, TILE_SIZE * 1.5f);
                player.enterGhostMode(entry.getX(), entry.getY());
                System.out.println("Ghost mode activated!");
            } else {
                int finalScore = player.getScore();
                int finalWave = waveManager.getCurrentWave();
                int finalTime = (int) timeAlive;

                game.getGameState().updateSurvivalScore(finalScore, finalWave, finalTime);
                de.tum.cit.fop.maze.SaveManager.save(game.getGameState());

                System.out.println("\n=== SURVIVAL MODE GAME OVER ===");
                System.out.println("Wave: " + finalWave);
                System.out.println("Score: " + finalScore);
                System.out.println("Time: " + finalTime + "s\n");

                game.goToGameOver(getMapPath(),finalScore);
            }
        }
    }


    public WaveManager getWaveManager() {
        return waveManager;
    }
    public float getGameScreenTime() {
        return timeAlive;
    }
}