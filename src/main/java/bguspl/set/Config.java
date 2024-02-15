package bguspl.set;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds all the game's configuration data
 */
public class Config {

    /**
     * Random spin cycles for Config::randomSpin (for debugging / testing)
     */
    public final long randomSpinMin;
    public final long randomSpinMax;

    /**
     * The number of features on the cards (e.g. shape, color etc.)
     */
    public final int featureCount;

    /**
     * The number of choices for each feature (e.g. red, green, blue)
     */
    public final int featureSize;

    /**
     * The total number of cards in the deck (i.e. featureSize ^ featureCount)
     */
    public final int deckSize;

    /**
     * The number of human players in the game.
     */
    public final int humanPlayers;

    /**
     * The number of computer players (i.e. input is simulated)
     */
    public final int computerPlayers;

    /**
     * The total number of players (human + computer) in the game
     */
    public final int players;

    /**
     * Whether to print out hints to the console or not
     */
    public final boolean hints;

    /**
     * The number of milliseconds until the dealer reshuffles the deck (0 show timer since last action, -1 show nothing)
     */
    public final long turnTimeoutMillis;

    /**
     * The number of milliseconds the turn countdown warning should be displayed
     */
    public final long turnTimeoutWarningMillis;

    /**
     * The number of milliseconds a player gets frozen for when he scores a point
     */
    public final long penaltyFreezeMillis;

    /**
     * The number of milliseconds a player gets frozen for when penalized
     */
    public final long pointFreezeMillis;

    /**
     * The number of milliseconds to delay before removing/placing a card on the table
     */
    public final long tableDelayMillis;

    /**
     * The number of milliseconds to pause at the end of the game before closing
     */
    public final long endGamePauseMillies;

    /**
     * The names of the players to display on the screen
     * Note: if there are more players than names, the remaining players will be called "Player 3", "Player 4", etc.
     */
    public final String[] playerNames;

    /**
     * The number of rows in the grid of cards on the table (and on the screen)
     */
    public final int rows;

    /**
     * The number of columns in the grid of cards on the table (and on the screen)
     */
    public final int columns;

    /**
     * The total number of cells in the table grid
     */
    public final int tableSize;

    /**
     * The width (in pixels) of each cell
     */
    public final int cellWidth;

    /**
     * The height (in pixels) of each cell
     */
    public final int cellHeight;

    /**
     * The Width (in pixeks) of player name cell
     */
    public final int playerCellWidth;

    /**
     * The Height (in pixeks) of player name cell
     */
    public final int playerCellHeight;

    /**
     * The size of the displayed font
     */
    public final int fontSize;

    /**
     * The scancodes of the keyboard input data for each player
     * Notes:
     * 1. This should correspond to the number of human players and the dimensions of the table card grid (i.e. the
     * first n codes are for the first row, the 2nd n codes are for the 2nd row etc., n being the number of columns).
     * 2. If the number of entries here does not match the number of human players a warning will be issued
     */
    private final int[][] playerKeys;

    /**
     * The default scan codes data (this is the same as in the default config.properties file)
     */
    private static final String[] playerKeysDefaults = {
            "81,87,69,82,65,83,68,70,90,88,67,86",
            "85,73,79,80,74,75,76,59,77,44,46,47"};

    /**
     * Attempts to read the config properties from the current working directory. Otherwise, tries to load them
     * as a resource.
     *
     * @param filename - the name of the configuration file.
     * @return - a properties object with the configuration file contents.
     */
    private static Properties loadProperties(String filename, Logger logger) {

        Properties properties = new Properties();

        if (filename == null || filename.isEmpty())
            logger.severe("running with default configuration.");
        else try (InputStream is = Files.newInputStream(Paths.get(filename))) {
            properties.load(is);
        } catch (IOException e) {
            logger.severe("cannot read configuration file " + filename + " trying from resources.");
            try (InputStream is = Config.class.getClassLoader().getResourceAsStream(filename)) {
                properties.load(is);
                logger.severe("configuration file was loaded from resources directory.");
            } catch (IOException | InvalidPathException ex) {
                logger.severe("warning: cannot read config file from the resources directory either. Using defaults.");
            }
        }

        return properties;
    }

    public Config(Logger logger, String configFilename) {
        this(logger, loadProperties(configFilename, logger));
    }

    public Config(Logger logger, Properties properties) {

        // logger settings
        Level logLevel = Level.parse(properties.getProperty("LogLevel", "ALL"));
        String logFormat = properties.getProperty("LogFormat", "[%1$tT.%1$tL] [%2$-7s] %3$s%n");
        Main.setLoggerLevelAndFormat(logger, logLevel, logFormat);

        // for debugging
        randomSpinMin = Long.parseLong(properties.getProperty("RandomSpinMin", "0"));
        randomSpinMax = Long.parseLong(properties.getProperty("RandomSpinMax", "0"));
        if (randomSpinMax < randomSpinMin || randomSpinMin < 0)
            logger.severe("invalid random spin cycles: max: " + randomSpinMax + " min: " + randomSpinMin);

        // cards settings
        featureSize = Integer.parseInt(properties.getProperty("FeatureSize", "3"));
        featureCount = Integer.parseInt(properties.getProperty("FeatureCount", "4"));
        deckSize = (int) Math.pow(featureSize, featureCount);

        // gameplay settings
        humanPlayers = Integer.parseInt(properties.getProperty("HumanPlayers", "2"));
        computerPlayers = Integer.parseInt(properties.getProperty("ComputerPlayers", "0"));
        players = humanPlayers + computerPlayers;

        hints = Boolean.parseBoolean(properties.getProperty("Hints", "False"));
        turnTimeoutMillis = (long) (Double.parseDouble(properties.getProperty("TurnTimeoutSeconds", "60")) * 1000.0);
        turnTimeoutWarningMillis = (long) (Double.parseDouble(properties.getProperty("TurnTimeoutWarningSeconds", "60")) * 1000.0);
        pointFreezeMillis = (long) (Double.parseDouble(properties.getProperty("PointFreezeSeconds", "1")) * 1000.0);
        penaltyFreezeMillis = (long) (Double.parseDouble(properties.getProperty("PenaltyFreezeSeconds", "3")) * 1000.0);
        tableDelayMillis = (long) (Double.parseDouble(properties.getProperty("TableDelaySeconds", "0.1")) * 1000.0);
        endGamePauseMillies = (long) (Double.parseDouble(properties.getProperty("EndGamePauseSeconds", "5")) * 1000.0);

        // ui settings
        String[] names = properties.getProperty("PlayerNames", "Player 1, Player 2").split(",");
        playerNames = new String[players];
        Arrays.setAll(playerNames, i -> i < names.length ? names[i].trim() : "Player " + (i + 1));

        rows = Integer.parseInt(properties.getProperty("Rows", "3"));
        columns = Integer.parseInt(properties.getProperty("Columns", "4"));
        tableSize = rows * columns;
        cellWidth = Integer.parseInt(properties.getProperty("CellWidth", "258"));
        cellHeight = Integer.parseInt(properties.getProperty("CellHeight", "167"));
        playerCellWidth = Integer.parseInt(properties.getProperty("PlayerCellWidth", "300"));
        playerCellHeight = Integer.parseInt(properties.getProperty("PlayerCellHeight", "40"));
        fontSize = Integer.parseInt(properties.getProperty("FontSize", "40"));

        // keyboard input data
        playerKeys = new int[players][rows * columns];
        for (int i = 0; i < players; i++) {
            String defaultCodes = "";
            if (i < 2) defaultCodes = playerKeysDefaults[i];
            String playerKeysString = properties.getProperty("PlayerKeys" + (i + 1), defaultCodes);
            if (playerKeysString.length() > 0) {
                String[] codes = playerKeysString.split(",");
                if (codes.length != tableSize)
                    logger.severe("warning: player " + (i + 1) + " keys (" + codes.length + ") mismatch table size (" + tableSize + ").");
                for (int j = 0; j < Math.min(codes.length, tableSize); ++j) // parse the key codes string
                    playerKeys[i][j] = Integer.parseInt(codes[j]);
            }
        }
    }

    public int[] playerKeys(int player) {
        return playerKeys[player];
    }
}
