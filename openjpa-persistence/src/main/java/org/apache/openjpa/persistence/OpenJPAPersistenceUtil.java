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
package org.apache.openjpa.persistence;

import java.util.BitSet;
import java.util.Collection;

import javax.persistence.spi.LoadState;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.FieldMetaData;

public class OpenJPAPersistenceUtil {

    /**
     * Returns the identifier of the persistent entity.
     * @param entity
     * @return The identifier of the entity or null if the entity
     * is not persistent.
     */
    public static Object getIdentifier(Object entity) {
        return getIdentifier(null, entity);
    }

    /**
     * Get the object identifier for a persistent entity managed by one
     * of the entity managers of the specified entity manager factory.
     * @return The identifier of the entity or null if the entity does
     * not have an identifier assigned or is not managed by any of the
     * entity managers of the entity manager factory.
     */
    public static Object getIdentifier(OpenJPAEntityManagerFactory emf, 
        Object entity) {

        if (entity instanceof PersistenceCapable) {
            PersistenceCapable pc = (PersistenceCapable)entity;
            // Per contract, if not managed by the owning emf, return null.
            if (emf != null) {
                if (!OpenJPAPersistenceUtil.isManagedBy(emf, pc)) {
                    return null;
                }
            }
            StateManager sm = pc.pcGetStateManager();
            
            if (sm != null && sm instanceof OpenJPAStateManager) {
                OpenJPAStateManager osm = (OpenJPAStateManager)sm;
                return osm.getObjectId();                
            }
        }
        return null;
    }

    /**
     * Determines whether the specified state manager is managed by a broker
     * within the persistence unit of this util instance.
     * @param sm StateManager
     * @return true if this state manager is managed by a broker within
     * this persistence unit.
     */
    public static boolean isManagedBy(OpenJPAEntityManagerFactory emf, 
        Object entity) {
        if (emf == null || !emf.isOpen()) {
            return false;
        }
        Object abfobj = JPAFacadeHelper.toBrokerFactory(emf);
        if (abfobj == null) {
            return false;
        }
        if (abfobj instanceof AbstractBrokerFactory) {
            AbstractBrokerFactory abf = (AbstractBrokerFactory)abfobj;
            Collection<?> brokers = abf.getOpenBrokers();
            if (brokers == null || brokers.size() == 0) {
                return false;
            }
            // Cycle through all brokers managed by this factory.  
            Broker[] brokerArr = brokers.toArray(new Broker[brokers.size()]);
            for (Broker broker : brokerArr) {
                if (broker != null && !broker.isClosed() && 
                    broker.isPersistent(entity))
                    return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the attribute on the specified object is loaded.
     * 
     * @return LoadState.LOADED - if the attribute is loaded.
     *         LoadState.NOT_LOADED - if the attribute is not loaded or any
     *         EAGER fetch attributes of the entity are not loaded.
     *         LoadState.UNKNOWN - if the entity is not managed by this
     *         provider or if it does not contain the persistent
     *         attribute.
     */
    public static LoadState isLoaded(Object obj, String attr) {
        return isLoaded(null, obj, attr);
    }

    /**
     * Determines whether the attribute on the specified object is loaded and
     * is managed by one of the entity managers of the specified entity manager
     * factory.
     * 
     * @return LoadState.LOADED - if the attribute is loaded.
     *         LoadState.NOT_LOADED - if the attribute is not loaded or any
     *         EAGER fetch attributes of the entity are not loaded.
     *         LoadState.UNKNOWN - if the entity is not managed by this
     *         provider or one of the entity managers of the specified 
     *         entity manager factory, or if it does not contain the persistent
     *         attribute.
     */
    public static LoadState isLoaded(OpenJPAEntityManagerFactory emf, 
        Object obj, String attr) {

        if (obj == null) {
            return LoadState.UNKNOWN;
        }
        
        // If the object has a state manager, call it directly.
        if (obj instanceof PersistenceCapable) {
            PersistenceCapable pc = (PersistenceCapable)obj;
            if (emf != null) {
                if (!OpenJPAPersistenceUtil.isManagedBy(emf, pc))
                    return LoadState.UNKNOWN;
            }
            StateManager sm = pc.pcGetStateManager();
            if (sm != null && sm instanceof OpenJPAStateManager) {
                return isLoaded((OpenJPAStateManager)sm, attr);
            }
        }        
        return LoadState.UNKNOWN;
    }

    private static LoadState isLoaded(OpenJPAStateManager sm, String attr) {
        boolean isLoaded = true;
        BitSet loadSet = sm.getLoaded();
        if (attr != null) {
            FieldMetaData fmd = sm.getMetaData().getField(attr);
            // Could not find field metadata for the specified attribute.
            if (fmd == null) {
                return LoadState.UNKNOWN;
            }
            // Otherwise, return the load state
            if(!loadSet.get(fmd.getIndex())) {
                return LoadState.NOT_LOADED;
            }
        }
        FieldMetaData[] fmds = sm.getMetaData().getFields();
        // Check load state of all persistent eager fetch attributes
        for (FieldMetaData fmd : fmds) {
            if (fmd.isInDefaultFetchGroup()) {
                if (!loadSet.get(fmd.getIndex())) {
                    isLoaded = false;
                    break;
                }
                // TODO: Complete contract for collections
            }
        } 
        return isLoaded ? LoadState.LOADED : LoadState.NOT_LOADED;        
    }
}
