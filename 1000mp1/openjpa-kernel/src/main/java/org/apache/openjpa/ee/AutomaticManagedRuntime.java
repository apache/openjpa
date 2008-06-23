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
package org.apache.openjpa.ee;

import java.util.LinkedList;
import java.util.List;
import javax.transaction.TransactionManager;

import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InvalidStateException;

/**
 * Implementation of the {@link ManagedRuntime} interface that searches
 * through a set of known JNDI locations and method invocations to locate the
 * appropriate mechanism for obtaining a TransactionManager.
 *  Built in support is provided for the following Application Servers:
 * <ul>
 * <li>Bluestone</li>
 * <li>HP Application Server</li>
 * <li>JBoss</li>
 * <li>JRun</li>
 * <li>OpenEJB</li>
 * <li>Oracle Application Server</li>
 * <li>Orion</li>
 * <li>SunONE</li>
 * <li>Weblogic</li>
 * <li>Websphere</li>
 * </ul>
 *
 * @author Marc Prud'hommeaux
 */
public class AutomaticManagedRuntime
    implements ManagedRuntime, Configurable {

    private static final String [] JNDI_LOCS = new String []{
        "javax.transaction.TransactionManager", // weblogic
        "java:/TransactionManager", // jboss & jrun
        "java:/DefaultDomain/TransactionManager", // jrun too
        "java:comp/pm/TransactionManager", // orion & oracle
        "java:comp/TransactionManager", // generic
        "java:pm/TransactionManager", // borland
    };
    private static final String [] METHODS = new String[]{
        "com.arjuna.jta.JTA_TransactionManager.transactionManager", // hp
        "com.bluestone.jta.SaTransactionManagerFactory.SaGetTransactionManager",
        "org.openejb.OpenEJB.getTransactionManager",
        "com.sun.jts.jta.TransactionManagerImpl.getTransactionManagerImpl",
        "com.inprise.visitransact.jta.TransactionManagerImpl."
            + "getTransactionManagerImpl", // borland
    };
    private static final WLSManagedRuntime WLS;
    private static final SunOneManagedRuntime SUNONE;
    private static final WASManagedRuntime WAS;

    private static Localizer _loc = Localizer.forPackage
        (AutomaticManagedRuntime.class);

    static {
        ManagedRuntime mr = null;
        try {
            mr = new WLSManagedRuntime();
        } catch (Throwable t) {
        }
        WLS = (WLSManagedRuntime) mr;

        mr = null;
        try {
            mr = new SunOneManagedRuntime();
        } catch (Throwable t) {
        }
        SUNONE = (SunOneManagedRuntime) mr;

        mr = null;
        try {
            mr = new WASManagedRuntime();
        }
        catch(Throwable t) {
        }
        WAS= (WASManagedRuntime) mr;
    }

    private Configuration _conf = null;
    private ManagedRuntime _runtime = null;

    public TransactionManager getTransactionManager()
        throws Exception {
        if (_runtime != null)
            return _runtime.getTransactionManager();

        List errors = new LinkedList();
        TransactionManager tm = null;

        if (WLS != null) {
            try {
                tm = WLS.getTransactionManager();
            } catch (Throwable t) {
                errors.add(t);
            }
            if (tm != null) {
                _runtime = WLS;
                return tm;
            }
        }

        if (WAS != null) {
            try {
                WAS.setConfiguration(_conf);
                WAS.startConfiguration();
                WAS.endConfiguration();
                tm = WAS.getTransactionManager();
            } catch (Throwable t) {
                errors.add(t);
            }
            if (tm != null) {
                _runtime = WAS;
                return tm;
            }
        }

        // try to find a jndi runtime
        JNDIManagedRuntime jmr = new JNDIManagedRuntime();
        for (int i = 0; i < JNDI_LOCS.length; i++) {
            jmr.setTransactionManagerName(JNDI_LOCS[i]);
            try {
                tm = jmr.getTransactionManager();
            } catch (Throwable t) {
                errors.add(t);
            }
            if (tm != null) {
                _runtime = jmr;
                return tm;
            }
        }

        // look for a method runtime
        InvocationManagedRuntime imr = new InvocationManagedRuntime();
        for (int i = 0; i < METHODS.length; i++) {
            imr.setConfiguration(_conf);
            imr.setTransactionManagerMethod(METHODS[i]);
            try {
                tm = imr.getTransactionManager();
            } catch (Throwable t) {
                errors.add(t);
            }
            if (tm != null) {
                _runtime = imr;
                return tm;
            }
        }

        if (SUNONE != null) {
            try {
                tm = SUNONE.getTransactionManager();
            } catch (Throwable t) {
                errors.add(t);
            }
            if (tm != null) {
                _runtime = SUNONE;
                return tm;
            }
        }

        Throwable[] t = (Throwable []) errors.toArray(
            new Throwable [errors.size()]);
        throw new InvalidStateException(_loc.get("tm-not-found")).
            setFatal(true).setNestedThrowables(t);
    }

    public void setConfiguration(Configuration conf) {
        _conf = conf;
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }

    public void setRollbackOnly(Throwable cause)
        throws Exception {
        // check to see if the runtime is cached
        if (_runtime == null)
            getTransactionManager();

        if (_runtime != null)
            _runtime.setRollbackOnly(cause);
    }

    public Throwable getRollbackCause()
        throws Exception {
        // check to see if the runtime is cached
        if (_runtime == null)
            getTransactionManager();

        if (_runtime != null)
            return _runtime.getRollbackCause();

        return null;
    }
}
