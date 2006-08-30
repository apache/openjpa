/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.io.Serializable;
import java.util.Properties;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.util.Closeable;

/**
 * Factory for {@link Broker} instances.
 *
 * @author Abe White
 * @since 0.4.0
 */
public interface BrokerFactory
    extends Serializable, Closeable {

    /**
     * Return the configuration for this factory.
     */
    public OpenJPAConfiguration getConfiguration();

    /**
     * Return properties describing this runtime.
     */
    public Properties getProperties();

    /**
     * Put the specified key-value pair into the map of user objects.
     */
    public Object putUserObject(Object key, Object val);

    /**
     * Get the value for the specified key from the map of user objects.
     */
    public Object getUserObject(Object key);

    /**
     * Return a broker with default settings.
     */
    public Broker newBroker();

    /**
     * Return a broker using the given credentials and in the given
     * transaction and connection retain mode, optionally finding
     * existing broker in the global transaction.
     */
    public Broker newBroker(String user, String pass, boolean managed,
        int connRetainMode, boolean findExisting);

    /**
     * Register a listener for lifecycle-related events on the specified
     * classes. If the classes are null, all events will be propagated to
     * the listener. The listener will be passed on to all new brokers.
     *
     * @since 0.3.3
     */
    public void addLifecycleListener(Object listener, Class[] classes);

    /**
     * Remove a listener for lifecycle-related events.
     *
     * @since 0.3.3
     */
    public void removeLifecycleListener(Object listener);

    /**
     * Close the factory.
     */
    public void close();

    /**
     * Returns true if this broker factory is closed.
     */
    public boolean isClosed();

    /**
     * Synchronizes on an internal lock.
     */
    public void lock();

    /**
     * Release the internal lock.
	 */
	public void unlock ();
}
