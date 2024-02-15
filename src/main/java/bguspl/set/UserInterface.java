package bguspl.set;

/**
 * This interface contains all methods used to display the graphical user interface.
 */
public interface UserInterface {

    /**
     * Draw the card image corresponding to the card id in the specified slot.
     * @param card - the card id.
     * @param slot - the slot number (for grid; slot = row*row.length + column).
     */
    void placeCard(int card, int slot);

    /**
     * Draw an empty card image in the specified slot.
     * @param slot - the slot number (for grid; slot = row*row.length + column).
     */
    void removeCard(int slot);

    /**
     * Draw a player name text in the specified slot.
     * @param player - the card id.
     * @param slot - the slot number (for grid; slot = row*row.length + column).
     */
    void placeToken(int player, int slot);

    /**
     * Remove all players names text from all slot.
     */
    void removeTokens();

    /**
     * Remove all player names text in the specified slot.
     * @param slot - the slot number (for grid; slot = row*row.length + column).
     */
    void removeTokens(int slot);

    /**
     * Remove player name text in the specified slot.
     * @param player - the card id.
     * @param slot - the slot number (for grid; slot = row*row.length + column).
     */
    void removeToken(int player, int slot);

    /**
     * Set the countdown time to the specified number of milliseconds.
     * @param millies - the milliseconds to be shown.
     * @param warn    - if true, the timer will be painted in red and will display milliseconds
     */
    void setCountdown(long millies, boolean warn);

    /**
     * Set the elapsed time to the specified number of milliseconds.
     * @param millies - the milliseconds to be shown.
     */
    void setElapsed(long millies);

    /**
     * Set the player text in the score panel to show remaining freeze time.
     * If milliseconds > 0, show player name in red, and add freeze time.
     * If milliseconds <= 0, set player name to default black name without freeze.
     * @param player  - the player id.
     * @param millies - the freeze time in milliseconds.
     */
    void setFreeze(int player, long millies);

    /**
     * Set the score for the relevent player in the player score panel.
     * @param player - the player id.
     * @param score - the score to value.
     */
    void setScore(int player, int score);

    /**
     * Hide player score panel from view and show text announcing the winner(s).
     * If players length == 1, declare him as a winner.
     * If players length > 1, declare tie between all players in players list.
     * @param players - the players ids.
     */
    void announceWinner(int[] players);

    /**
     * Programmatically closes the window.
     */
    void dispose();
}
