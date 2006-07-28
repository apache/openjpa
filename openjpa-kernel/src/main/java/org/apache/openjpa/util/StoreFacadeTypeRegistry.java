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

    public void registerImplementation(Class facadeType, Class implType) {
        _impls.put(facadeType, implType);
    }
    
    public Class getImplementation(Class facadeType) {
        return (Class) _impls.get(facadeType);
    }
}
