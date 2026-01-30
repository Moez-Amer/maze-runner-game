package de.tum.cit.fop.maze.Screens;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Survival Mode Game Screen
 * Extends GameScreen and uses identical update/render logic.
 * Only difference: enemies spawn from waves instead of being pre-placed.
 */
public class SurvivalGameScreen extends GameScreen {

    private WaveManager waveManager;
    private float timeAlive;
    private boolean hasSpawnedCurrentWave;
    private int totalEnemiesSpawnedThisWave;
    private Random random;

    private ArrayList<Vector2> occupiedPositions;
    private static final float MIN_SPAWN_DISTANCE = TILE_SIZE * 5; // 5 tiles minimum spacing

    public SurvivalGameScreen(MazeRunnerGame game, String mapPath) {
        super(game, mapPath);

        System.out.println("Map dimensions: " + mapWidth + "x" + mapHeight);

        this.random = new Random();
        this.occupiedPositions = new ArrayList<>();

        ArrayList<Vector2> spawnLocations = extractSpawnLocations();

        this.waveManager = new WaveManager(spawnLocations);
        this.timeAlive = 0f;
        this.hasSpawnedCurrentWave = false;

        enemies.clear();

        waveManager.startWave(1);
        this.totalEnemiesSpawnedThisWave = waveManager.getTotalEnemiesInWave();
        spawnWaveEnemies();

        this.hasSpawnedCurrentWave = true;
    }

    /**
     * Extracts all safe spawn locations from map.
     * Uses LARGE buffer from walls to prevent spawning near boundaries.
     *
     * Map is 50x36, walls are at boundaries (x=0, x=49, y=0, y=35)
     * We use a 5-tile buffer from ALL walls for maximum safety.
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
                if (mapData[x][y] != 1) {
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

                        if (mapData[nx][ny] != 1 && mapData[nx][ny] != 6) {
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
     * Gets a spawn position that is guaranteed not to overlap with existing enemies
     * or be too close to walls.
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
     * Spawns all enemies for the current wave with guaranteed spacing.
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
     * Override checkPlayerAttackHits to apply wave score multiplier.
     * Same logic as parent, but scales score by wave difficulty.
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
     * No win condition in survival mode.
     */
    @Override
    protected void checkWinCondition() {
    }

    /**
     * Lose condition: Player dies twice (once + ghost mode expiration).
     */
    @Override
    protected void checkLoseCondition() {
        if (player.isDead() && !player.isGhostMode()) {
            // Check if can use revive
            if (!player.hasUsedRevive()) {
                // Spawn voodoo doll at death location
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