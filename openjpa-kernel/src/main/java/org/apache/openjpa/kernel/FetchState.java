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
package org.apache.openjpa.kernel;

import org.apache.openjpa.meta.FieldMetaData;

/**
 * Defines the decision to include fields for selection or loading during
 * a fetch operation.
 *
 * @author <A HREF="mailto:pinaki.poddar@gmail.com>Pinaki Poddar</A>
 * @since 4.1
 */
public interface FetchState {

    /**
     * Returns the immutable fetch configuration this receiver is based on.
     */
    public FetchConfiguration getFetchConfiguration();

    /**
     * Affirms if the given field requires to be selected in the context
     * of current fetch operation.
     * The response can be stateful as the same field argument would generate
     * different responses on different invocation based on the current
     * state of this receiver if changeState flag is true.
     *
     * @param fm field metadata. must not be null.
     * @param changeState true implies that the state of the receiver will
     * change due to invocation.
     */
    public boolean requiresSelect(FieldMetaData fm, boolean changeState);

    /**
     * Affirms if the given field of the given instance requires to be loaded
     * in the context of current fetch operation.
     * The response is stateful as the same arguments would generate different
     * responses on different invocation based on the current state of this
     * receiver.
     *
     * @param sm state manager being populated
     * @param fm field metadata
     */
    public boolean requiresLoad(OpenJPAStateManager sm, FieldMetaData fm);

    /**
     * Affirms if the given field is to be loaded as default. The field itself
     * is aware of whether it belongs to the default fetch group. This method
     * adds an extra constraint to verify that current configuration includes
     * default fetch group.
     *
     * @param fm field metadata
     */
    public boolean isDefault(FieldMetaData fm);
}
