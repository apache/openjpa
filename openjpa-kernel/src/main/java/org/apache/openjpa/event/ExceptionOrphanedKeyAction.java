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
package org.apache.openjpa.event;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.ObjectNotFoundException;

/**
 * Throw a {@link ObjectNotFoundException} when an orphaned key is discovered.
 *
 * @author Abe White
 * @since 0.3.2.2
 */
public class ExceptionOrphanedKeyAction
    implements OrphanedKeyAction {

    private static final Localizer _loc = Localizer.forPackage
        (ExceptionOrphanedKeyAction.class);

    public Object orphan(Object oid, OpenJPAStateManager sm,
        ValueMetaData vmd) {
        throw new ObjectNotFoundException(_loc.get("orphaned-key", oid, vmd));
    }
}
