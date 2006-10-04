package sun.misc;

/**
 * Compilation stub for pre-1.4.2 JREs. Thanks to it, the whole backport
 * concurrency package compiles and works with 1.4.2 as well as wih earlier 
 * JREs, and takes advantage of native Perf class when running on 1.4.2 while 
 * seamlessly falling back to System.currentTimeMillis() on previous JREs. This
 * class should NOT be included in the binary distribution of backport.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 * @nojavadoc
 */
public final class Perf {

    private static final Perf perf = new Perf();

    public static Perf getPerf() { return perf; }

    private Perf() {}

    public long highResCounter() {
        return System.currentTimeMillis();
    }

    public long highResFrequency() {
        return 1000L;
    }
}
