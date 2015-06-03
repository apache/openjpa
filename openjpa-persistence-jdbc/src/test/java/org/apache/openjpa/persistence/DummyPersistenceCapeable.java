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

import org.apache.openjpa.enhance.FieldConsumer;
import org.apache.openjpa.enhance.FieldSupplier;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.StateManager;

/**
 * This Object is here for the sole purpose of testing pcGetEnhancementContractVersion. This object isn't a tested
 * PersistenceCapable implementation so it shouldn't be used unless you are fully aware of what you are doing.
 */
public class DummyPersistenceCapeable implements PersistenceCapable {
    private static int pcInheritedFieldCount;
    private static String pcFieldNames[] = {};
    private static Class pcFieldTypes[];
    private static byte pcFieldFlags[] = {};
    private static Class pcPCSuperclass;
    protected transient boolean pcVersionInit;
    protected transient StateManager pcStateManager;
    private transient Object pcDetachedState;

    static {
        Class aclass[] = new Class[0];
        pcFieldTypes = aclass;
        PCRegistry.register(DummyPersistenceCapeable.class, pcFieldNames, pcFieldTypes, pcFieldFlags, pcPCSuperclass,
            "DummyPersistenceCapeable", new DummyPersistenceCapeable());
    }

    public int pcGetEnhancementContractVersion() {
        return PCEnhancer.ENHANCER_VERSION - 1;
    }

    public PersistenceCapable pcNewInstance(StateManager sm, boolean clear) {
        return new DummyPersistenceCapeable();
    }

    public void pcCopyFields(Object fromObject, int[] fields) {

    }

    public void pcCopyKeyFieldsFromObjectId(FieldConsumer consumer, Object obj) {
    }

    public void pcCopyKeyFieldsToObjectId(FieldSupplier supplier, Object obj) {
    }

    public void pcCopyKeyFieldsToObjectId(Object obj) {
    }

    public void pcDirty(String fieldName) {
    }

    public Object pcFetchObjectId() {
        return null;
    }

    public Object pcGetDetachedState() {
        return null;
    }

    public Object pcGetGenericContext() {
        return null;
    }

    public StateManager pcGetStateManager() {
        return null;
    }

    public Object pcGetVersion() {
        return null;
    }

    public boolean pcIsDeleted() {
        return false;
    }

    public Boolean pcIsDetached() {
        return null;
    }

    public boolean pcIsDirty() {
        return false;
    }

    public boolean pcIsNew() {
        return false;
    }

    public boolean pcIsPersistent() {
        return false;
    }

    public boolean pcIsTransactional() {
        return false;
    }

    public PersistenceCapable pcNewInstance(StateManager sm, Object obj, boolean clear) {
        return null;
    }

    public Object pcNewObjectIdInstance() {
        return null;
    }

    public Object pcNewObjectIdInstance(Object obj) {
        return null;
    }

    public void pcProvideField(int fieldIndex) {

    }

    public void pcProvideFields(int[] fieldIndices) {

    }

    public void pcReplaceField(int fieldIndex) {

    }

    public void pcReplaceFields(int[] fieldIndex) {

    }

    public void pcReplaceStateManager(StateManager sm) {

    }

    public void pcSetDetachedState(Object state) {

    }

    public DummyPersistenceCapeable() {
    }
}
