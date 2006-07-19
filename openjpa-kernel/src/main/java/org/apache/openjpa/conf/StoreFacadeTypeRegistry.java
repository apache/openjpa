package org.apache.openjpa.conf;

import java.util.HashMap;
import java.util.Map;


/**
 * Repository of store-specific facade classes. This is used by facade 
 * implementations to wrap store-specific components without knowing 
 * about all possible back-ends.
 */
public class StoreFacadeTypeRegistry {

    private Map _impls = new HashMap();

    public void registerImplementation(Class facadeType, 
        Class implType) {
        _impls.put(facadeType, implType);
    }
    
    public Class getImplementation(Class facadeType) {
        return (Class) _impls.get(facadeType);
    }
}
