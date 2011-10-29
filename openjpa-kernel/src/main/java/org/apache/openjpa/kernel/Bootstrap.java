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
package org.apache.openjpa.kernel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;

import org.apache.openjpa.conf.BrokerFactoryValue;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.MapConfigurationProvider;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UserException;

/**
 * Helper methods for acquiring {@link BrokerFactory} objects
 *
 * @since 0.4.0.0
 */
public class Bootstrap {

    private static final Class<?>[] CONFIGURATION_ARG = { ConfigurationProvider.class };

    private static Localizer s_loc = Localizer.forPackage(Bootstrap.class);

    /**
     * Return a new factory for the default configuration.
     */
    public static BrokerFactory newBrokerFactory() {
        return Bootstrap.newBrokerFactory(null);
    }

    /**
     * Return a new factory for the given configuration. 
     */
    public static BrokerFactory newBrokerFactory(ConfigurationProvider conf) {
        try {
            BrokerFactory factory = invokeFactory(conf, "newInstance", CONFIGURATION_ARG, new Object[] { conf });
            factory.postCreationCallback();
            return factory;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            if (cause instanceof OpenJPAException)
                throw (OpenJPAException) cause;
            throw new InternalException(s_loc.get("new-brokerfactory-excep", getFactoryClassName(conf), cause));
        } catch (Exception e) {
            throw new UserException(s_loc.get("bad-new-brokerfactory", getFactoryClassName(conf)), e).setFatal(true);
        }
    }

    /**
     * Return a pooled factory for the default configuration.
     */
    public static BrokerFactory getBrokerFactory() {
        return Bootstrap.getBrokerFactory(null);
    }

    /**
     * Return a pooled factory for the given configuration. 
     */
    public static BrokerFactory getBrokerFactory(ConfigurationProvider conf) {
        try {
            return invokeFactory(conf, "getInstance", CONFIGURATION_ARG, new Object[] { conf});
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            if (cause instanceof OpenJPAException)
                throw (OpenJPAException) cause;
            throw new InternalException(s_loc.get("brokerfactory-excep", getFactoryClassName(conf)), cause);
        } catch (Exception e) {
            throw new UserException(s_loc.get("bad-brokerfactory", getFactoryClassName(conf)), e).setFatal(true);
        }
    }

    private static BrokerFactory invokeFactory(ConfigurationProvider conf, String methodName, 
    		Class<?>[] argTypes, Object[] args)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (conf == null)
            conf = new MapConfigurationProvider();
        ProductDerivations.beforeConfigurationConstruct(conf);

        Class<?> cls = getFactoryClass(conf);
        Method meth = cls.getMethod(methodName, argTypes); 

        return (BrokerFactory) meth.invoke(null, args);
    }

    private static String getFactoryClassName(ConfigurationProvider conf) {
        try {
            return getFactoryClass(conf).getName();
        } catch (Exception e) {
            return "<" + e.toString() + ">";
        }
    }

    /**
     * Instantiate the factory class designated in properties.
     */
    private static Class<?> getFactoryClass(ConfigurationProvider conf) {
        Object cls = BrokerFactoryValue.get(conf);
        if (cls instanceof Class)
            return (Class<?>) cls;

        BrokerFactoryValue value = new BrokerFactoryValue();
        value.setString((String) cls);
        String clsName = value.getClassName();
        if (clsName == null)
            throw new UserException(s_loc.get("no-brokerfactory", conf.getProperties())).setFatal(true);

        try {
            return Class.forName(clsName, true, conf.getClassLoader());
        } catch (Exception e) {
            throw new UserException(s_loc.get("bad-brokerfactory-class", clsName), e).setFatal(true);
		}
	}
}
