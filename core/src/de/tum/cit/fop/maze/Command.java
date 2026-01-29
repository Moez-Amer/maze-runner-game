package de.tum.cit.fop.maze;

/**
 * Interface for developer console commands.
 * Each command implements this to execute its specific functionality.
 */
public interface Command {
    /**
     * Executes the command with the given arguments.
     * @param args Command arguments (excluding the command name itself)
     * @return Result message to display in console
     */
    String execute(String[] args);

    /**
     * Gets the help text for this command.
     * @return Help text describing command usage
     */
    String getHelp();
}