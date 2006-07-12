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
package org.apache.openjpa.event;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.ValueMetaData;

/**
 * <p>Does nothing when an orphaned key is discovered.</p>
 *
 * @author Abe White
 * @since 3.2.2
 */
public class NoneOrphanedKeyAction
    implements OrphanedKeyAction {

    public Object orphan(Object oid, OpenJPAStateManager sm,
        ValueMetaData vmd) {
        return null;
    }
}
