package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {
    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /* ------------------------------ added fields ------------------------------ */

    /**
     * The data structure for the actions performed by the player
     */
    BlockingQueue<Integer> actions;

    /**
     * The number of tokens placed by the player on the table.
     */
    private int countTokens;

    /**
     * The Dealer object.
     */
    private final Dealer dealer;

    /**
     * The time when the player needs to be frozen.
     */
    private long freezeTime = 0;

    /**
     * The time interval for the sleep method when freezing the player.
     */
    private long sleepFreezeTimeInterval = 1000;




    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;

        this.countTokens = 0;
        this.actions = new ArrayBlockingQueue<>(this.env.config.featureSize);
        this.dealer = dealer;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        System.out.println("Player " + this.id + " started");
        while (!terminate) {

            // TODO implement run() main player loop
            try{
                int slot = actions.take();
                System.out.println("Player " + this.id + " took action on slot " + slot);
                if(table.hasToken(this.id, slot)){
                    table.removeToken(this.id, slot);
                    this.countTokens--;
                }
                else{
                    table.placeToken(this.id, slot);
                    this.countTokens++;
                }
                if(this.countTokens == 3){
                    dealer.notifyPlayerHas3Tokens(id);
                    synchronized (this) {
                        //TODO: use flag instead of wait
                        this.wait();
                    }
                    this.countTokens = 0;
                    table.clearTokens(this.id);
                }
            } catch (InterruptedException e) {}
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        // CHATGPT 5 is SHAKING
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement createArtificialIntelligence() player key press simulator
                // The AI thread generates a random slot.

                int randomSlot = (int) (Math.random() * this.env.config.tableSize);
                keyPressed(randomSlot);
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();

    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement terminate()
        System.out.println("in player terminate()");
        terminate = true;
        if(!human){
            try{
                aiThread.interrupt();
                aiThread.join();
            }
            catch (InterruptedException ignored) {}
        }
        System.out.println("finished player terminate()");
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement keyPressed(int slot)
        if(freezeTime <= 0 && table.slotToCard[slot] != null){
            try {
                actions.put(slot);
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement point()
        System.out.println("point");

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        this.env.ui.setFreeze(id, this.env.config.pointFreezeMillis);
        try {
            freezeTime = this.env.config.pointFreezeMillis;
            while(freezeTime >= sleepFreezeTimeInterval) {
                this.env.ui.setFreeze(id, freezeTime);
                freezeTime -= sleepFreezeTimeInterval;
                Thread.sleep(sleepFreezeTimeInterval);
                dealer.updateTimerDisplayWrapper();
            }
        } catch (InterruptedException ignored1) {}
        this.env.ui.setFreeze(id, 0);
        synchronized (this) {
            this.notify();
        }
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement penalty()
        System.out.println("panelty");
        this.env.ui.setFreeze(id, this.env.config.penaltyFreezeMillis);

        try {
            freezeTime = this.env.config.penaltyFreezeMillis;
            while(freezeTime >= sleepFreezeTimeInterval) {
                this.env.ui.setFreeze(id, freezeTime);
                freezeTime -= sleepFreezeTimeInterval;
                Thread.sleep(sleepFreezeTimeInterval);
                //TODO: return to dealer function
                dealer.updateTimerDisplayWrapper();
            }
        } catch (InterruptedException ignored) {}

        this.env.ui.setFreeze(id, 0);
        synchronized (this) {
            this.notify();
        }
    }

    public int score() {
        return score;
    }


}
