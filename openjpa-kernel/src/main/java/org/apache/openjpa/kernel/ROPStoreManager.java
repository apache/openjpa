/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.kernel;

import java.util.BitSet;

import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.StoreException;

/**
 * <p>Wraps the native store manager to handle calls using custom
 * {@link PCResultObjectProvider}s.</p>
 *
 * @author Abe White
 */
class ROPStoreManager
    extends DelegatingStoreManager {

    public ROPStoreManager(StoreManager delegate) {
        super(delegate);
    }

    public boolean exists(OpenJPAStateManager sm, Object context) {
        if (context instanceof PCResultObjectProvider)
            context = null;
        return super.exists(sm, context);
    }

    public boolean initialize(OpenJPAStateManager sm, PCState state,
        FetchState fetchState, Object context) {
        if (context instanceof PCResultObjectProvider) {
            try {
                ((PCResultObjectProvider) context).initialize
                    (sm, state, fetchState);
            }
            catch (OpenJPAException ke) {
                throw ke;
            }
            catch (Exception e) {
                throw new StoreException(e);
            }
            return true;
        }
        return super.initialize(sm, state, fetchState, context);
    }

    public boolean syncVersion(OpenJPAStateManager sm, Object context) {
        // the only way this gets called with a rop context is if the
        // rop didn't load any version info on initialize, so just null
        // it out so we don't get unexpected results when our delegate
        // expectes a different context type
        if (context instanceof PCResultObjectProvider)
            context = null;
        return super.syncVersion(sm, context);
    }

    public boolean load(OpenJPAStateManager sm, BitSet fields,
        FetchState fetchState, int lockLevel, Object context) {
        // the only way this gets called with a rop context is if the
        // rop didn't load the field on initialize, so just null
        // it out so we don't get unexpected results when our delegate
        // expectes a different context type
        if (context instanceof PCResultObjectProvider)
            context = null;
        return super.load(sm, fields, fetchState, lockLevel, context);
    }
}
