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
    private long time_start;

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
        time_start = System.currentTimeMillis();
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
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement terminate()
        for (Player player : players) {
            player.terminate();
        }
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
        for(int i = 0; i < env.config.players; i++) {
            int[] token_cards = new int[env.config.featureSize];
            for(int j = 0; j < token_cards.length; j++)
                token_cards[j] = -1;

            int count = 0;
            for(int slot = 0; slot < env.config.tableSize; slot++)
                if(table.hasToken(i,slot))
                    token_cards[count++] = table.slotToCard[slot];

            boolean is_a_set = env.util.testSet(token_cards);
            if(is_a_set) {
                for(Integer card : token_cards) {
                    if(card > 0) {
                        System.out.println(card);
                        table.removeCard(table.cardToSlot[card]);
                    }
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
         try {
             Thread.currentThread().sleep(this.env.config.turnTimeoutMillis);
         } catch (InterruptedException ignored) {
             //TODO check if need to do anything in case of exception
             System.out.println(ignored.getMessage());
         }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement updateTimerDisplay(boolean reset)
        long millies = 0;
        if(reset) {
            time_start = System.currentTimeMillis();
            this.env.ui.setCountdown(millies,false);
        } else {
            millies = System.currentTimeMillis() - time_start;
            millies = this.env.config.turnTimeoutMillis - millies;
            boolean warning = millies <= this.env.config.turnTimeoutWarningMillis;
            this.env.ui.setCountdown(millies,warning);
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
