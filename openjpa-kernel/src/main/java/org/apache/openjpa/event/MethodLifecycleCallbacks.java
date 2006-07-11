/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.event;

import java.lang.reflect.Method;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * Callback adapter that invokes a callback method via reflection.
 *
 * @author Steve Kim
 */
public class MethodLifecycleCallbacks implements LifecycleCallbacks {

    private static final Localizer _loc = Localizer.forPackage
        (MethodLifecycleCallbacks.class);
    private Method _callback;
    private boolean _arg;

    /**
     * Constructor. Supply callback class and its callback method name.
     *
     * @arg Whether we expect a further argument such as in AfterDetach
     */
    public MethodLifecycleCallbacks(Class cls, String method, boolean arg) {
        Class[] args = arg ? new Class[]{ Object.class } : null;
        _callback = getMethod(cls, method, args);
        _arg = arg;
    }

    /**
     * Constructor. Supply callback method.
     */
    public MethodLifecycleCallbacks(Method method, boolean arg) {
        _callback = method;
        _arg = arg;
    }

    /**
     * The callback method.
     */
    public Method getCallbackMethod() {
        return _callback;
    }

    /**
     * Returns if this callback expects another argument
     */
    public boolean requiresArgument() {
        return _arg;
    }

    public boolean hasCallback(Object obj, int eventType) {
        return true;
    }

    public void makeCallback(Object obj, Object arg, int eventType)
        throws Exception {
        if (!_callback.isAccessible())
            _callback.setAccessible(true);
        if (_arg)
            _callback.invoke(obj, new Object[]{ arg });
        else _callback.invoke(obj, (Object[]) null);
    }

    public String toString() {
        return getClass().getName() + ":" + _callback;
    }

    /**
     * Helper method to return the named method of the given class, throwing
     * the proper exception on error.
     */
    protected static Method getMethod(Class cls, String method, Class[] args) {
        try {
            return cls.getMethod(method, args);
        } catch (Throwable t) {
            try {
                // try again with the declared methods, which will
                // check private and protected methods
                Method m = cls.getDeclaredMethod(method, args);
                if (!m.isAccessible())
                    m.setAccessible(true);
                return m;
            } catch (Throwable t2) {
                throw new UserException(_loc.get("method-notfound",
                    cls.getName(), method), t);
            }
        }
    }
}
