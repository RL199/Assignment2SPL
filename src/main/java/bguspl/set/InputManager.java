package bguspl.set;

import bguspl.set.ex.Player;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * This class handles the input from the keyboard, translates it to table grid slots and dispatches accordingly.
 */
class InputManager extends KeyAdapter {

    private static final int MAX_KEY_CODE = 255;
    private final Player[] players;
    int[] keyMap = new int[MAX_KEY_CODE + 1];
    int[] keyToSlot = new int[MAX_KEY_CODE + 1];
    private final Logger logger;

    public InputManager(Logger logger, Config config, Player[] players) {
        this.players = players;
        this.logger = logger;

        // initialize the keys
        for (int player = 0; player < config.players; ++player)
            for (int i = 0; i < config.playerKeys(player).length; i++) {
                int keyCode = config.playerKeys(player)[i];
                if (keyCode >= keyMap.length) reallocArrays(keyCode); // enlarge the array for higher key codes
                keyMap[keyCode] = player + 1; // 1 for first player and 2 for second player
                keyToSlot[keyCode] = i;
            }
    }

    private void reallocArrays(int keyCode) {
        keyMap = Arrays.copyOf(keyMap, keyCode + 1);
        keyToSlot = Arrays.copyOf(keyToSlot, keyCode + 1);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // dispatch the key event to the player according to the key map
        int keyCode = e.getKeyCode();
        int player = keyMap[keyCode] - 1;
        if (player >= 0){
            logger.severe("key " + keyCode + " was pressed by player " + (player + 1));
            players[player].keyPressed(keyToSlot[keyCode]);
        }
    }
}
