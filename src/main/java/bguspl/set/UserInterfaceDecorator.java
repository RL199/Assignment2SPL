package bguspl.set;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserInterfaceDecorator implements UserInterface {

    private final Logger logger;
    private final Util util;
    private final UserInterface ui;

    public UserInterfaceDecorator(Logger logger, Util util, UserInterface ui) {
        this.ui = ui;
        this.logger = logger;
        this.util = util;

        if (ui == null) System.out.println("running without a user interface. Check logs.");
    }

    @Override
    public void placeCard(int card, int slot) {
        logger.severe("placing card " + card + " in slot " + slot);
        util.spin();
        if (ui != null) ui.placeCard(card, slot);
    }

    @Override
    public void removeCard(int slot) {
        logger.severe("removing card from slot " + slot);
        util.spin();
        if (ui != null) ui.removeCard(slot);
    }

    @Override
    public void placeToken(int player, int slot) {
        logger.severe("player " + (player + 1) + " placing token on slot " + slot);
        util.spin();
        if (ui != null) ui.placeToken(player, slot);
    }

    @Override
    public void removeTokens() {
        logger.severe("removing all tokens");
        util.spin();
        if (ui != null) ui.removeTokens();
    }

    @Override
    public void removeTokens(int slot) {
        logger.severe("removing tokens from slot " + slot);
        util.spin();
        if (ui != null) ui.removeTokens(slot);
    }

    @Override
    public void removeToken(int player, int slot) {
        logger.severe("removing player " + (player + 1) + " token from slot " + slot);
        util.spin();
        if (ui != null) ui.removeToken(player, slot);
    }

    @Override
    public void setCountdown(long millies, boolean warn) {
        if (!warn || millies % 1000L == 0L)
            logger.severe("updating countdown to " + millies);
        if (ui != null) ui.setCountdown(millies, warn);
    }

    @Override
    public void setElapsed(long millies) {
        logger.severe("updating elapsed time to " + millies);
        util.spin();
        if (ui != null) ui.setElapsed(millies);
    }

    @Override
    public void setFreeze(int player, long millies) {
        logger.severe("setting player " + (player + 1) + " freeze to " + millies);
        util.spin();
        if (ui != null) ui.setFreeze(player, millies);
    }

    @Override
    public void setScore(int player, int score) {
        logger.severe("setting player " + (player + 1) + " score to " + score);
        util.spin();
        if (ui != null) ui.setScore(player, score);
    }

    @Override
    public void announceWinner(int[] players) {
        List<String> winners = Arrays.stream(players).mapToObj(id -> "player " + (id + 1)).collect(Collectors.toList());
        logger.severe("announcing winner(s): " + String.join(", ", winners));
        if (ui != null) ui.announceWinner(players);
    }

    @Override
    public void dispose() {
        logger.severe("disposing of user interface elements");
        if (ui != null) ui.dispose();
    }
}
