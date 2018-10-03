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
package org.apache.openjpa.meta;

import java.io.File;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.meta.ClassArgParser;

/**
 * No-op metadata I/O to prevent attempts to load other classes.
 *
 * @author Abe White
 */
public class NoneMetaDataFactory
    implements MetaDataFactory, MetaDataDefaults {

    private static final NoneMetaDataFactory _instance =
        new NoneMetaDataFactory();

    public static NoneMetaDataFactory getInstance() {
        return _instance;
    }

    @Override
    public void setRepository(MetaDataRepository repos) {
    }

    @Override
    public void setStoreDirectory(File dir) {
    }

    @Override
    public void setStoreMode(int store) {
    }

    @Override
    public void setStrict(boolean strict) {
    }

    @Override
    public void load(Class cls, int mode, ClassLoader envLoader) {
    }

    @Override
    public boolean store(ClassMetaData[] metas, QueryMetaData[] queries,
        SequenceMetaData[] seqs, int mode, Map output) {
        return false;
    }

    @Override
    public boolean drop(Class[] cls, int mode, ClassLoader envLoader) {
        return false;
    }

    @Override
    public MetaDataDefaults getDefaults() {
        return this;
    }

    @Override
    public Set getPersistentTypeNames(boolean classpath,
        ClassLoader envLoader) {
        return null;
    }

    @Override
    public Class getQueryScope(String queryName, ClassLoader loader) {
        return null;
    }

    @Override
    public Class getResultSetMappingScope(String resultSetMappingName,
        ClassLoader loader) {
        return null;
    }

    @Override
    public ClassArgParser newClassArgParser() {
        return new ClassArgParser();
    }

    @Override
    public void clear() {
    }

    @Override
    public void addClassExtensionKeys(Collection exts) {
    }

    @Override
    public void addFieldExtensionKeys(Collection exts) {
    }

    @Override
    public int getDefaultAccessType() {
        return AccessCode.UNKNOWN;
    }

    @Override
    public int getDefaultIdentityType() {
        return ClassMetaData.ID_UNKNOWN;
    }

    @Override
    public int getCallbackMode() {
        return CALLBACK_IGNORE;
    }

    @Override
    public boolean getCallbacksBeforeListeners(int type) {
        return false;
    }

    @Override
    public void setIgnoreNonPersistent(boolean ignore) {
    }

    @Override
    public boolean isDeclaredInterfacePersistent() {
        return false;
    }

    @Override
    public boolean isDataStoreObjectIdFieldUnwrapped() {
        return false;
    }

    @Override
    public void populate(ClassMetaData meta, int access) {
    }

    @Override
    public void populate(ClassMetaData meta, int access, boolean ignoreTransient) {
    }

    @Override
    public Member getBackingMember(FieldMetaData fmd) {
        return null;
    }

    @Override
    public Member getMemberByProperty(ClassMetaData meta, String property,
    		int access, boolean scan) {
    	return null;
    }

    @Override
    public Class<? extends Exception> getUnimplementedExceptionType() {
        return null;
    }

    @Override
    public void loadXMLMetaData(Class<?> cls) {
    }

    @Override
    public String getMetaModelClassName(String managedClassName) {
        return null;
    }
    @Override
    public String getManagedClassName(String metamodelClassName) {
        return null;
    }

    @Override
    public boolean isMetaClass(Class<?> c) {
        return false;
    }

    @Override
    public Class<?> getManagedClass(Class<?> c) {
        return null;
    }

    @Override
    public boolean isAbstractMappingUniDirectional(OpenJPAConfiguration conf) {
        return false;
    }

    @Override
    public boolean isNonDefaultMappingAllowed(OpenJPAConfiguration conf) {
        return false;
    }

    @Override
    public Boolean isDefaultCascadePersistEnabled() {
        return false;
    }

    @Override
    public void setDefaultCascadePersistEnabled(Boolean bool) {

    }

    @Override
    public String getDefaultSchema(){return null;}

    @Override
    public void setDefaultSchema(String schema){}
}
