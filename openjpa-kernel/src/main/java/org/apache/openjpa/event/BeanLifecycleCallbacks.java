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

import org.apache.openjpa.cdi.EntityListenerCachedFactory;
import org.apache.openjpa.cdi.EntityListenerFactory;
import org.apache.openjpa.cdi.EntityListenerWrapper;

/**
 * Performs a callback method on a entity listener factory provided bean instance.
 *
 * @author Steve Kim
 */
public class BeanLifecycleCallbacks
    extends MethodLifecycleCallbacks {

    private final Class<?> listenerClass;
    
    private final EntityListenerFactory factory;
    
    /**
     * Constructor. Make the callback on an instance of the given type.
     *
     * @param arg whether another argument is expected such as AfterDetach
     */
    public BeanLifecycleCallbacks(Class<?> cls, String method, boolean arg, Class<?> type) {
        this(cls, getMethod(cls, method, arg ? new Class[]{ Object.class, type } : new Class[]{ type }), arg, null);
    }

    public BeanLifecycleCallbacks(Class<?> cls, String method, boolean arg, Class<?> type, EntityListenerFactory factory) {
        this(cls, getMethod(cls, method, arg ? new Class[]{ Object.class, type } : new Class[]{ type }), arg, factory);
    }

    /**
     * Constructor. Make the callback on an instance of the given type.
     */
    public BeanLifecycleCallbacks(Class<?> cls, Method method, boolean arg) {
        this(cls, method, arg, null);
    }
    
    public BeanLifecycleCallbacks(Class<?> cls, Method method, boolean arg, EntityListenerFactory factory) {
    	super(method, arg);
    	listenerClass = cls;
    	this.factory = factory != null ? factory : new EntityListenerCachedFactory();
    }

    private EntityListenerWrapper<?> getListenerWrapper() {
    	return factory.getWrapper(listenerClass);
    }

    @Override
    public void makeCallback(Object obj, Object rel, int eventType) throws Exception {
        Method callback = getCallbackMethod();
        EntityListenerWrapper<?> wrapper = getListenerWrapper();
        try {
	        if (!callback.canAccess(wrapper.listener()))
	            callback.setAccessible(true);
	        if (requiresArgument())
	            callback.invoke(wrapper.listener(), new Object[]{ obj, rel });
	        else
	            callback.invoke(wrapper.listener(), new Object[]{ obj });
        } finally {
	        if (!wrapper.cacheable()) {
	        	wrapper.dispose();
	        }
        }
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
