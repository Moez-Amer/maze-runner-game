package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.Screens.GameScreen;
import java.util.HashMap;
/**
 * Provides an in-game developer console for debugging and testing purposes.
 * <p>
 * The console allows for real-time manipulation of game variables (e.g., teleportation,
 * granting items, god mode) through a command-based system. It handles its own
 * input processing, command history, and visual overlay rendering.
 */
public class DeveloperConsole {
    private MazeRunnerGame game;
    private GameScreen gameScreen;

    private boolean isOpen;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private HashMap<String, Command> commands;
    private Array<String> outputHistory;
    private Array<String> inputHistory;
    private int historyIndex;
    private String currentInput;

    private float consoleHeight;
    private float consoleY;

    private boolean wasKeyPressed;
    private boolean justOpened;

    /**
     * Creates a new developer console.
     * @param game Reference to the main game instance
     */
    public DeveloperConsole(MazeRunnerGame game) {
        this.game = game;
        this.isOpen = false;
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();
        this.font.setColor(Color.GREEN);
        this.font.getData().setScale(1.2f);
        this.commands = new HashMap<>();
        this.outputHistory = new Array<>();
        this.inputHistory = new Array<>();
        this.historyIndex = -1;
        this.currentInput = "";
        this.wasKeyPressed = false;
        this.justOpened = false;
        this.consoleHeight = Gdx.graphics.getHeight() * 0.3f;
        this.consoleY = 0;
        registerCommands();
        outputHistory.add("Developer Console - Press TAB to close");
        outputHistory.add("Type 'help' for list of commands");
        outputHistory.add("");
    }
    /**
     * Sets the current game screen (needed for commands to work).
     * @param screen The active GameScreen
     */
    public void setGameScreen(GameScreen screen) {
        this.gameScreen = screen;
    }
    /**
     * Registers all available console commands.
     */
    private void registerCommands() {
        commands.put("help", new Command(){
            @Override
            public String execute(String[] args) {
                StringBuilder sb=new StringBuilder();
                sb.append("Available commands:\n");
                for (String cmd:commands.keySet()) {
                    sb.append("  ").append(cmd).append(" - ").append(commands.get(cmd).getHelp()).append("\n");
                }
                return sb.toString();
            }
            @Override
            public String getHelp() {
                return "Shows this help message";
            }
        });
        commands.put("clear", new Command() {
            @Override
            public String execute(String[] args) {
                outputHistory.clear();
                return "";
            }
            @Override
            public String getHelp() {
                return "Clears console output";
            }
        });
        commands.put("give_item", new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen == null || gameScreen.getPlayer() == null) {
                    return "Error: No active game";
                }
                if(args.length<1){return "Usage: give_item <key|scroll|speed|power|shield|heart>";}
                Player player = gameScreen.getPlayer();
                String item=args[0].toLowerCase();
                switch (item) {
                    case "key":
                        player.collectKey();
                        return "Gave 1 key (total: " + player.getKeyCount() + ")";
                    case "scroll":
                        player.collectScroll();
                        return "Gave 1 scroll (total: " + player.getScrollCount() + ")";
                    case "speed":
                        player.applySpeedBoost(10f);
                        return "Applied speed boost";
                    case "power":
                        player.applyPowerBoost(10f);
                        return "Applied power boost";
                    case "shield":
                        player.applyShield(10f);
                        return "Applied shield";
                    case "heart":
                        player.addLife();
                        return "Added 1 life (total: " + player.getLives() + ")";
                    default:
                        return "Unknown item: " + item;
                }
            }
            @Override
            public String getHelp() {
                return "Gives item to player (give_item <key|scroll|speed|power|shield|heart>)";
            }
        });
        commands.put("tp",new Command(){
            @Override
            public String execute(String[] args) {
                if (gameScreen==null||gameScreen.getPlayer()==null){return "Error: No active game";}
                if (args.length < 2) {
                    return "Usage: tp <x> <y>";
                }
                try{float x = Float.parseFloat(args[0]);
                    float y = Float.parseFloat(args[1]);
                    gameScreen.getPlayer().setPosition(x, y);
                    return "Teleported to (" + x + ", " + y + ")";
                }catch (NumberFormatException e) {return "Error: Invalid coordinates";}}
            @Override
            public String getHelp() {
                return "Teleports player (tp <x> <y>)";}});
        commands.put("god",new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen==null||gameScreen.getPlayer()==null){
                    return "Error: No active game";}
                boolean enabled=gameScreen.toggleGodMode();
                return "God mode " +(enabled ? "enabled" : "disabled");
            }

            @Override
            public String getHelp() {
                return"Toggles invincibility";
            }
        });
        commands.put("kill_all",new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen == null) {
                    return "Error: No active game";}
                int count =gameScreen.killAllEnemies();
                return "Killed " + count + " enemies";
            }
            @Override
            public String getHelp() {
                return "Kills all enemies on the map";
            }
        });
        commands.put("list_vars", new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen==null||gameScreen.getPlayer()==null) {
                    return "Error: No active game";
                }
                Player player=gameScreen.getPlayer();
                StringBuilder sb=new StringBuilder();
                sb.append("=== Player Variables ===\n");
                sb.append("Lives: ").append(player.getLives()).append("/").append(player.getMaxLives()).append("\n");
                sb.append("Keys: ").append(player.getKeyCount()).append("\n");
                sb.append("Scrolls: ").append(player.getScrollCount()).append("\n");
                sb.append("Position: (").append((int)player.getX()).append(", ").append((int)player.getY()).append(")\n");
                sb.append("Score: ").append(player.getScore()).append("\n");
                sb.append("Has Speed Boost: ").append(player.hasSpeedBoost()).append("\n");
                sb.append("Has Power Boost: ").append(player.hasPowerBoost()).append("\n");
                sb.append("Has Shield: ").append(player.hasShield()).append("\n");
                sb.append("Ghost Mode: ").append(player.isGhostMode()).append("\n");
                return sb.toString();
            }
            @Override
            public String getHelp() {
                return "Lists all modifiable game variables";
            }
        });
        commands.put("spawn", new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen==null||gameScreen.getPlayer()==null) {
                    return "Error: No active game";
                }
                Player player=gameScreen.getPlayer();
                float spawnX=player.getX() + 16;
                float spawnY=player.getY();
                gameScreen.spawnEnemyAtPosition(spawnX, spawnY);
                return "Spawned enemy near player position";
            }
            @Override
            public String getHelp() {
                return "Spawns enemy near player location";
            }
        });
        commands.put("add_score",new Command() {
            @Override
            public String execute(String[] args) {
                if (gameScreen == null||gameScreen.getPlayer()==null) {
                    return "Error: No active game";
                }
                if (args.length<1) {
                    return "Usage: add_score <amount>";
                }
                try{
                    int amount=Integer.parseInt(args[0]);
                    gameScreen.getPlayer().addScore(amount);
                    return "Added " +amount+" points (total: "+gameScreen.getPlayer().getScore()+")";
                } catch (NumberFormatException e) {
                    return "Error: Invalid number";}}
            @Override
            public String getHelp() {
                return "Adds score points (add_score <amount>)";
            }
        });
    }
    /**
     * Updates console state and handles input.
     * @param delta Time since last frame
     */
    public void update(float delta) {
        KeyBindings keys = KeyBindings.getKeyBindings();
        boolean tKeyPressed = keys.isKeyPressed("Open Console");
        if (tKeyPressed && !wasKeyPressed) {
            toggle();
        }
        wasKeyPressed = tKeyPressed;
        if (!isOpen) return;
        handleTextInput();
        handleHistoryNavigation();
    }
    /**
     * Handles text input for the console.
     */
    private void handleTextInput() {
        if (justOpened) {
            justOpened = false;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!currentInput.trim().isEmpty()) {
                executeCommand(currentInput.trim());
                inputHistory.add(currentInput.trim());
                historyIndex = inputHistory.size;
                currentInput = "";
            }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (currentInput.length() > 0) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
            }
            return;
        }
        for (int i = Input.Keys.A; i <= Input.Keys.Z; i++) {
            if (Gdx.input.isKeyJustPressed(i)) {
                char c = (char) ('a' + (i - Input.Keys.A));
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                    c = Character.toUpperCase(c);
                }
                currentInput +=c;
            }
        }
        for (int i = Input.Keys.NUM_0; i <= Input.Keys.NUM_9; i++) {
            if (Gdx.input.isKeyJustPressed(i)) {
                currentInput += (char) ('0' + (i - Input.Keys.NUM_0));
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) currentInput += " ";
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) currentInput += ".";
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                currentInput += "_";
            } else {
                currentInput += "-";
            }
        }
    }
    /**
     * Handles up/down arrow key navigation through command history.
     */
    private void handleHistoryNavigation() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if (inputHistory.size > 0 && historyIndex > 0) {
                historyIndex--;
                currentInput = inputHistory.get(historyIndex);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if (historyIndex < inputHistory.size - 1) {
                historyIndex++;
                currentInput = inputHistory.get(historyIndex);
            } else {
                historyIndex = inputHistory.size;
                currentInput = "";
            }
        }
    }
    /**
     * Executes a console command.
     * @param input The full command string
     */
    private void executeCommand(String input) {
        outputHistory.add("> " + input);
        String[] parts = input.split(" ");
        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        Command command = commands.get(commandName);
        if (command != null) {
            String result = command.execute(args);
            if (!result.isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    outputHistory.add(line);
                }
            }}else{
            outputHistory.add("Unknown command: " + commandName);
            outputHistory.add("Type 'help' for available commands");}
        outputHistory.add("");
        while (outputHistory.size > 20) {
            outputHistory.removeIndex(0);}}
    /**
     * Toggles the console open/closed.
     */
    public void toggle() {
        isOpen = !isOpen;
        if (isOpen) {
            justOpened = true;}
        if (gameScreen != null) {
            gameScreen.setConsolePaused(isOpen);}}
    /**
     * Renders the console overlay.
     */
    public void render() {
        if (!isOpen) return;
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(0, consoleY, Gdx.graphics.getWidth(), consoleHeight);
        shapeRenderer.end();
        game.getSpriteBatch().begin();
        float textX = 10;
        float textY = consoleY + consoleHeight - 10;
        float lineHeight = 18;
        String inputLine = "> " + currentInput + "_";
        font.draw(game.getSpriteBatch(), inputLine, textX, textY);
        textY -= lineHeight + 5;
        for(int i=outputHistory.size-1;i>=0;i--) {
            font.draw(game.getSpriteBatch(), outputHistory.get(i), textX, textY);
            textY -= lineHeight;
            if (textY < consoleY + 10) break;}
        game.getSpriteBatch().end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }
    /**
     * Resizes the console when window size changes.
     * @param width New window width
     * @param height New window height
     */
    public void resize(int width, int height){
        consoleHeight = height * 0.3f;
        consoleY = 0;}
    /**
     * Checks if console is currently open.
     * @return true if console is visible
     */
    public boolean isOpen() {return isOpen;}
    /**
     * Cleans up resources.
     */
    public void dispose(){
        shapeRenderer.dispose();
        font.dispose();}}