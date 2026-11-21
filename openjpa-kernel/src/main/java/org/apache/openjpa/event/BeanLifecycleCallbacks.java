/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

import org.apache.openjpa.cdi.InjectionManager;
import org.apache.openjpa.cdi.CDIInjectionService;

/**
 * Performs a callback method on a cached bean instance.
 *
 * @author Steve Kim
 */
public class BeanLifecycleCallbacks<T>
    extends MethodLifecycleCallbacks {

    private final Class<T> listenerClass;
    
    private InjectionManager<T> injectorManager;
    
    /**
     * Constructor. Make the callback on an instance of the given type.
     *
     * @param arg whether another argument is expected such as AfterDetach
     */
    public BeanLifecycleCallbacks(Class<T> cls, String method, boolean arg, Class<?> type) {
        this(cls, getMethod(cls, method, arg ? new Class[]{ Object.class, type } : new Class[]{ type }), arg);
    }

    /**
     * Constructor. Make the callback on an instance of the given type.
     */
    public BeanLifecycleCallbacks(Class<T> cls, Method method, boolean arg) {
        super(method, arg);
        injectorManager = CDIInjectionService.getInjectionManager(cls);
        listenerClass = cls;
    }

    private T getListener() {
    	return injectorManager.createInstance(listenerClass);
    }

    @Override
    public void makeCallback(Object obj, Object rel, int eventType) throws Exception {
        Method callback = getCallbackMethod();
        Object listener = getListener();
        if (!callback.canAccess(listener))
            callback.setAccessible(true);
        if (requiresArgument())
            callback.invoke(listener, new Object[]{ obj, rel });
        else
            callback.invoke(listener, new Object[]{ obj });
    	injectorManager.dispose();
    }

    @Override
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }

    @Override
    public void writeExternal(ObjectOutput out)
        throws IOException {
        super.writeExternal(out);
        out.writeObject(listenerClass);
    }
    
}
