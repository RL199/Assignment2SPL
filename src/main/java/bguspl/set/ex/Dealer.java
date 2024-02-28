package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

    //Queue for players with potential set
    private BlockingQueue<Player> playersWithPotSet;

    //Queue for cards to be removed
    private BlockingQueue<Integer> cardsToRemove;

    //Boolean to check if there is a potential set
    private boolean hasPotSet;

    //Array of player threads
    private Thread[] playerThreads;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());

        this.playersWithPotSet = new ArrayBlockingQueue<>(this.players.length);
        //TODO perhaps change capacity of queue
        this.cardsToRemove = new ArrayBlockingQueue<>(env.config.tableSize);
        //The maximum number of cards to be removed and/or empty slots after wards, is all the cards o_O
        this.hasPotSet = false;
        this.playerThreads = new Thread[env.config.players];
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        for (Player player : players) {
            Thread playerThread = new Thread(player);
            playerThreads[player.id] = playerThread;
            playerThread.start();
        }

        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        terminate();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);

            //Check if any player has a set
            try {
                while(hasPotSet) {
                    Player player = playersWithPotSet.take();
                    int player_id = player.id;
                    int[] playerTokenCards = getPlayerTokenCards(player_id);
                    if (checkIfSet(playerTokenCards)){
                        handlePlayerPoint(player);
                        for(int card : playerTokenCards){
                            cardsToRemove.put(card);
                        }
                    }
                    else{
                        handlePlayerPenalty(player);
                    }
                    hasPotSet = !(playersWithPotSet.isEmpty());
                }
            } catch (InterruptedException ignored) {}
            removeCardsFromTable();
            placeCardsOnTable();

        }
        if(System.currentTimeMillis() >= reshuffleTime)
            updateTimerDisplay(true);
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement terminate()

        System.out.println("in dealer terminate()");
        for (int i = env.config.players - 1; i >= 0; i--) {
            Player player = players[i];
            player.terminate();
            try {
                playerThreads[i].interrupt();
                playerThreads[i].join();
            } catch (InterruptedException ignored) {}
        }
        terminate = true;
        System.out.println("finished dealer terminate()");
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    private int[] getPlayerTokenCards(int player_id) {
        int[] token_cards = new int[env.config.featureSize];

        int count = 0;
        for(int slot = 0; slot < env.config.tableSize; slot++)
            if(table.hasToken(player_id,slot) && table.slotToCard[slot] != null)
                token_cards[count++] = table.slotToCard[slot];

        return token_cards;
    }

    private boolean checkIfSet(int[] token_cards) {
        for (int card : token_cards) {
            if (table.cardToSlot[card] == null) {
                return false;
            }
        }
        return env.util.testSet(token_cards);
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement removeCardsFromTable()

        try {
            while(!cardsToRemove.isEmpty()) {
                int card = cardsToRemove.take();
                if(table.cardToSlot[card] != null){
                    int slot = table.cardToSlot[card];
                    System.out.println("card " + card + " removed from slot " + slot);
                    table.removeCard(slot);
                }
            }
        } catch (InterruptedException ignored) {}
    }

    private void handlePlayerPoint(final Player player) {
        //notify the player thread
        synchronized(playerThreads[player.id]) {
            playerThreads[player.id].notify();
        }
        player.point();
        System.out.println("Player " + player.id + " has a set!");

    }

    private void handlePlayerPenalty(final Player player) {
        synchronized(playerThreads[player.id]) {
            playerThreads[player.id].notify();
        }
        player.penalty();

        System.out.println("Player " + player.id + " has no set!");
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement placeCardsOnTable()

        int slots_available = this.env.config.tableSize - table.countCards();
        if(slots_available > 0 && !deck.isEmpty()) {
            for (int slot = 0; slot < this.env.config.tableSize && !deck.isEmpty(); slot++) {
                if (table.slotToCard[slot] == null) {
                    int card_number = (int) (Math.random() * (deck.size()));
                    int card = deck.remove(card_number);
                    table.placeCard(card, slot);
                }
            }
        }
        if(slots_available > 0 && env.config.hints) {
            table.hints();
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
            } catch (InterruptedException ignored) {}
        }
    }


    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement updateTimerDisplay(boolean reset)
        if(reset || reshuffleTime == Long.MAX_VALUE) {
            reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
        }
        //Game timer
        long time = reshuffleTime - System.currentTimeMillis();
        boolean warning = time <= this.env.config.turnTimeoutWarningMillis;
        this.env.ui.setCountdown(time,warning);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement removeAllCardsFromTable()
        for(int slot = 0; slot < env.config.tableSize ; slot++) {
            if(table.slotToCard[slot] != null){
                Integer card = table.slotToCard[slot];
                table.removeCard(slot);
                deck.add(card);
            }
        }
        reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
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


    /**
     * Notify the dealer that a player has set a set.
     *
     * @param player_id - the id of the player that has set a set.
     * @throws InterruptedException - if the thread is interrupted.
     */
    public void notifyPlayerHasPotSet(int player_id) {
        hasPotSet = true;
        try {
            playersWithPotSet.put(players[player_id]);
        } catch (InterruptedException ignored) {}
    }

}
