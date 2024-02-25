package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /* ------------------------------ Added fields ------------------------------ */

    /**
     * The time when the dealer thread wakes up.
     */
    private long dealerWakeUpTime = 10;

    /**
     * The time when the dealer started the game.
     */
//    private long time_start;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

        for (Player player : players) {
            new Thread(player).start();
        }

        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);

            removeCardsFromTable();
            placeCardsOnTable();
        }
        updateTimerDisplay(true);
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement terminate()
        for (Player player : players) {
            player.terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement removeCardsFromTable()
        // If there is a set, remove the cards from the set
        for(final Player player : players) {
            int[] token_cards = new int[env.config.featureSize];

            int count = 0;
            for(int slot = 0; slot < env.config.tableSize; slot++)
                if(table.hasToken(player.id,slot))
                    token_cards[count++] = table.slotToCard[slot];
            if(count == env.config.featureSize) {
                if( env.util.testSet(token_cards)) {
                    player.point();
                    System.out.println("Player " + player.id + " has a set!");
                    for (Integer card : token_cards) {
                        if (card >= 0) {
                            table.removeCard(table.cardToSlot[card]);
                            updateTimerDisplay(true);
                        }
                    }
                }
                else {
                    player.penalty();
                    System.out.println("Player " + player.id + " has no set!");

                }
                table.clearTokens(player.id);
                synchronized (player) {
                    player.notifyAll();
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement placeCardsOnTable()

        int slots_available = this.env.config.tableSize - table.countCards();

        if(slots_available > 0 && !deck.isEmpty()) {
            for (int slot = 0; slot < this.env.config.tableSize; slot++) {
                if (table.slotToCard[slot] == null) {
                     int card_number = (int) (Math.random() * (deck.size()));
                    int card = deck.remove(card_number);
                    table.placeCard(card, slot);
                }
            }
        }


    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement sleepUntilWokenOrTimeout()
        synchronized (this) {
            try {
                this.wait(dealerWakeUpTime);
            } catch (InterruptedException ignored) {
                System.out.println(ignored.getMessage());
            }
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement updateTimerDisplay(boolean reset)
        // For every time, currTime < reshuffleTime
        boolean warning;
        // long currTime = 0;
        if(reset || reshuffleTime == Long.MAX_VALUE) {
//            time_start = currTime;
            reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
            long time = reshuffleTime - System.currentTimeMillis();
            warning = time <= this.env.config.turnTimeoutWarningMillis;
            this.env.ui.setCountdown( env.config.turnTimeoutMillis,warning);
        }
        else {
            long time = reshuffleTime - System.currentTimeMillis();
            warning = time <= this.env.config.turnTimeoutWarningMillis;
            this.env.ui.setCountdown(time,warning);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement removeAllCardsFromTable()
        int numCardsOnTable = table.countCards();
        for(int slot = 0; slot < numCardsOnTable ; slot++) {
            Integer card = table.slotToCard[slot];
            table.removeCard(slot);
            deck.add(card);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement announceWinners()
        int max_score = 0;
        int numWinners = 0;
        for(Player player : players) {
            if(player.score() > max_score) {
                max_score = player.score();
                numWinners = 1;
            }
            else if(player.score() == max_score) {
                numWinners++;
            }
        }
        int[] winners = new int[numWinners];
        for(Player player : players) {
            if(player.score() == max_score) {
                winners[--numWinners] = player.id;
            }
        }
        env.ui.announceWinner(winners);
    }
}
