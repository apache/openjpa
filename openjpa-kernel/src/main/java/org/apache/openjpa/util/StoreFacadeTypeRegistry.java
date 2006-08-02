package org.apache.openjpa.util;

import java.util.Map;

import org.apache.openjpa.lib.util.concurrent.ConcurrentHashMap;

/**
 * Repository of store-specific facade classes. This is used by facade 
 * implementations to wrap store-specific components without knowing 
 * about all possible back-ends.
 */
public class StoreFacadeTypeRegistry {

    private Map _impls = new ConcurrentHashMap();

    /**
     * Register a facade implementation.
     *
     * @param facadeType the facade interface
     * @param storeType the store's 
     * {@link org.apache.openjpa.kernel.StoreManager} type, or null for generic
     * @param implType the class implementing the facade
     */
    public void registerImplementation(Class facadeType, Class storeType, 
        Class implType) {
        Object key = (storeType == null) ? (Object)facadeType 
            : new Key(facadeType, storeType);
        _impls.put(key, implType);
    }
    
    /**
     * Return the implementation for the given facade and store.
     *
     * @param facadeType the facade interface
     * @param storeType the store's 
     * {@link org.apache.openjpa.kernel.StoreManager} type, or null for generic
     * @param implType the registered implementor
     */
    public Class getImplementation(Class facadeType, Class storeType) {
        Object key = (storeType == null) ? (Object)facadeType 
            : new Key(facadeType, storeType);
        Class c = (Class) _impls.get(key);
        // if no store-specific type, see if there is a generic avaialble
        if (c == null && storeType != null)
            c = (Class) _impls.get(facadeType);
        return c; 
    }

    /**
     * Lookup key for facade+store hash.
     */
    private static class Key {
        private final Class _facadeType;
        private final Class _storeType;

        public Key(Class facadeType, Class storeType) {
            _facadeType = facadeType;
            _storeType = storeType;
        }

        public int hashCode() {
            return _facadeType.hashCode() ^ _storeType.hashCode();
        }

        public boolean equals(Object other) {
            if (other == this)
                return true;
            Key k = (Key) other;
            return _facadeType == k._facadeType && _storeType == k._storeType;
        }
    }
}
