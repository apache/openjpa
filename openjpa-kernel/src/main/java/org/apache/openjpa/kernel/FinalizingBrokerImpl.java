package org.apache.openjpa.kernel;

/**
 * Subtype of {@link BrokerImpl} that automatically closes itself during
 * finalization. This implementation guards against potential resource leaks,
 * but also incurs a scalability bottleneck as instances are enlisted in the
 * finalization queue and subsequently cleaned up.
 *
 * @since 0.9.7
 */
public class FinalizingBrokerImpl
    extends BrokerImpl {

    /**
     * Close on finalize.
     */
    protected void finalize()
        throws Throwable {
        super.finalize();
        if (!isClosed())
            free();
    }
}
