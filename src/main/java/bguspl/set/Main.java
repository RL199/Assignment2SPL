package bguspl.set;

import bguspl.set.ex.Dealer;
import bguspl.set.ex.Player;
import bguspl.set.ex.Table;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.*;

/**
 * This class contains the game's main function.
 */
public class Main {

    private static Dealer dealer;
    private static Thread mainThread;

    private static boolean xButtonPressed = false;
    private static Logger logger;

    public static void xButtonPressed() throws InterruptedException {
        if (logger != null) logger.severe("exit button pressed");
        xButtonPressed = true;
        if (dealer != null) dealer.terminate();
        mainThread.join();
    }

    /**
     * The game's main function. Creates all data structures and initializes the threads.
     *
     * @param args - unused.
     */
    public static void main(String[] args) {

        mainThread = Thread.currentThread();

        // create the game environment objects
        logger = initLogger();
        ThreadLogger.logStart(logger, Thread.currentThread().getName());
        Config config = new Config(logger, "config.properties");
        Util util = new UtilImpl(config);

        Player[] players = new Player[config.players];
        UserInterface ui = null;
        try {
            ui = new UserInterfaceSwing(logger, config, players);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            logger.severe("error creating swing user interface: " + e.getMessage());
            logger.severe("will try to run without user interface");
            if (config.humanPlayers > 0)
                logger.severe("warning: running with human players with no user interface");
        }
        ui = new UserInterfaceDecorator(logger, util, ui);

        Env env = new Env(logger, config, ui, util);

        // create the game entities
        Table table = new Table(env);
        dealer = new Dealer(env, table, players);
        for (int i = 0; i < players.length; i++)
            players[i] = new Player(env, dealer, table, i, i < env.config.humanPlayers);

        // start the dealer thread
        ThreadLogger dealerThread = new ThreadLogger(dealer, "dealer", logger);
        dealerThread.startWithLog();

        try {
            // shutdown stuff
            dealerThread.joinWithLog();
            if (!xButtonPressed && config.endGamePauseMillies > 0) Thread.sleep(config.endGamePauseMillies);
        } catch (InterruptedException ignored) {
        } finally {
            logger.severe("thanks for playing... it was fun!");
            System.out.println("Thanks for playing... it was fun!");
            ThreadLogger.logStop(logger, Thread.currentThread().getName());
            if (!xButtonPressed) env.ui.dispose();
            for (Handler h : logger.getHandlers()) h.flush();
        }
    }

    private static Logger initLogger() {

        //just to make our log file nicer :)
        SimpleDateFormat format = new SimpleDateFormat("M-d_HH-mm-ss");
        FileHandler handler;
        try {
            //noinspection ResultOfMethodCallIgnored
            new File("./logs/").mkdirs();
            handler = new FileHandler("./logs/" + format.format(Calendar.getInstance().getTime()) + ".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("SetGameLogger");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        setLoggerLevelAndFormat(logger, Level.ALL, "[%1$tT.%1$tL] [%2$-7s] %3$s%n");

        return logger;
    }

    public static void setLoggerLevelAndFormat(Logger logger, Level level, String format) {
        Handler[] handlers = logger.getHandlers();
        if (handlers != null) Arrays.stream(handlers).forEach(h -> h.setFormatter(new SimpleFormatter() {
            // default format (with timestamp)  = "[%1$tF %1$tT] [%2$-7s] %3$s%n";
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format, new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(), lr.getMessage()
                );
            }
        }));
        logger.setLevel(level);
    }
}
