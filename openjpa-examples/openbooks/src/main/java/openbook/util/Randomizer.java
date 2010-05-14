package openbook.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * A set of static utility functions for simulating pseudo-randomness.
 * 
 * @author Pinaki Poddar
 *
 */
public class Randomizer {
    private static final Random rng = new Random(System.currentTimeMillis());
    private static final int MIN_ALPHA = (int)'A';
    private static final int MAX_ALPHA = (int)'Z';

    /**
     * Returns true with a probability of p.
     */
    public static boolean probability(double p) {
        return rng.nextDouble() < p;
    }
    
    /**
     * Picks a random number between 0 (inclusive) and N (exclusive).
     */
    public static int random(int n) {
        return rng.nextInt(n);
    }
    
    /**
     * Picks a uniformly distributed random integer within the given range. 
     */
    public static int random(int min, int max) {
        return min + rng.nextInt(max-min);
    }
    public static double random(double min, double max) {
        return min + rng.nextDouble()*(max-min);
    }
    
    /**
     * Generates a random alphanumeric String with each segment separated by a dash. 
     */
    public static String randomString(int...segments) {
        StringBuffer tmp = new StringBuffer();
        for (int s : segments) {
            tmp.append(tmp.length() == 0 ? (char)random(MIN_ALPHA, MAX_ALPHA) : '-');
            for (int j = 0; j < s; j++)
                tmp.append(random(10));
        }
        return tmp.toString();
    }
    
    /**
     * Picks a random element from the given list.
     */
    public static <T> T selectRandom(List<T> list) {
        if (list == null || list.isEmpty())
            return null;
        if (list.size() == 1)
            return list.get(0);
        return list.get(random(list.size()));
    }
    
    /**
     * Selects n elements randomly from the given list.
     * @param <T>
     * @param list
     * @param n
     * @return
     */
    public static <T> List<T> selectRandom(List<T> list, int n) {
        if (list == null || list.isEmpty())
            return Collections.emptyList();
        Set<Integer> indices = new HashSet<Integer>();
        List<T> selected = new ArrayList<T>();
        int m = list.size();
        if (n >= m) {
            selected.addAll(list);
        } else {
            while (indices.size() < n) {
                indices.add(random(m));
            }
            for (Integer index : indices) {
                selected.add(list.get(index));
            }
        }
        return selected;
    }
}
