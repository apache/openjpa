/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.meta;

import java.lang.reflect.Member;

import org.apache.openjpa.event.CallbackModes;

/**
 * Populates new metadata with default values.
 *
 * @author Abe White
 */
public interface MetaDataDefaults
    extends CallbackModes {

    /**
     * Return the default access type for a base persistent class with
     * {@link ClassMetaData#ACCESS_UNKNOWN} access type.
     */
    public int getDefaultAccessType();

    /**
     * Return the default identity type for unmapped classes without primary
     * key fields.
     */
    public int getDefaultIdentityType();

    /**
     * What to do on lifecycle callback exceptions.
     */
    public int getCallbackMode();

    /**
     * If callbacks are fired before listeners for the given
     * event type. Defaults to false.
     */
    public boolean getCallbacksBeforeListeners(int type);
    
    /**
     * Whether to ignore members which are not persistent by default
     * during metadata population. Defaults to true.
     */
    public void setIgnoreNonPersistent(boolean ignore);

    /**
     * Whether declared interfaces of a class are treated as persistent
     * types. Defaults to true.
     */
    public boolean isDeclaredInterfacePersistent();

    /**
     * Populate the given metadata with default settings.
     *
     * @param access access type constant from {@link ClassMetaData}
     */
    public void populate(ClassMetaData meta, int access);

    /**
     * Return the backing member for the given field metadata.
     */
    public Member getBackingMember(FieldMetaData field);

    /**
     * Return a runtime exception class to throw for un-implemented
     * managed interface methods.
     */
    public Class getUnimplementedExceptionType();
}
