package bguspl.set;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * The implementation of the UserInterface interface.
 */
public class UtilImpl implements Util {

    private final Config config;

    public UtilImpl(Config config) {
        this.config = config;
    }

    private void cardToFeatures(int card, int[] features) {
        for (int i = config.featureCount - 1; i >= 0; --i) {
            features[i] = card % config.featureSize;
            card /= config.featureSize;
        }
    }

    @Override
    public int[] cardToFeatures(int card) {
        int[] features = new int[config.featureCount];
        cardToFeatures(card, features);
        return features;
    }

    @Override
    public int[][] cardsToFeatures(int[] cards) {
        int[][] features = new int[cards.length][config.featureCount];
        IntStream.range(0, cards.length).forEach(i -> cardToFeatures(cards[i], features[i]));
        return features;
    }

    @Override
    public boolean testSet(int[] cards) {
        int[][] features = cardsToFeatures(Arrays.copyOf(cards, cards.length));
        for (int i = 0; i < config.featureCount; ++i) {
            boolean sameSame = true, butDifferent = true;

            // check if this features is sameSame in all cards
            for (int j = 1; j < features.length; ++j)
                if (features[0][i] != features[j][i]) {
                    sameSame = false;
                    break;
                }

            // check if this feature is butDifferent in all cards
            for (int j = 1; j < features.length; ++j)
                for (int k = j; k < features.length; ++k)
                    if (features[j - 1][i] == features[k][i]) {
                        butDifferent = false;
                        break;
                    }

            if (sameSame == butDifferent) return false;
        }
        return true;
    }

    @Override
    public List<int[]> findSets(List<Integer> deck, int count) {
        LinkedList<int[]> sets = new LinkedList<>();
        int n = deck.size();
        int r = config.featureSize;
        int[] combination = new int[r];

        for (int i = 0; i < r; ++i)
            combination[i] = i;

        while (combination[r - 1] < deck.size()) {
            int[] cards = Arrays.stream(combination).map(deck::get).sorted().toArray();
            if (testSet(cards)) {
                sets.add(cards);
                if (sets.size() >= count) return sets;
            }

            // generate next combination in lexicographic order
            int t = r - 1;
            while (t != 0 && combination[t] == n - r + t) --t;
            combination[t]++;
            for (int i = t + 1; i < r; i++) combination[i] = combination[i - 1] + 1;
        }
        return sets;
    }

    public void spin() {
        if (config.randomSpinMax <= 0) return;
        long cycles = ThreadLocalRandom.current().nextLong(config.randomSpinMin, config.randomSpinMax);
        for (int i = 0; i < cycles; ++i)
            Thread.yield();
    }
}
