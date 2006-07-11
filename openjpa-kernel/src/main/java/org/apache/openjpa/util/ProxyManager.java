/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util;

import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Manager for copying and proxying second class objects. Second class
 * objects are those that are often used as fields of persistent or
 * transactional instances, and which can themselves be modified without
 * resetting the owning class' field. Because these types can change without
 * an explicit call to the owning persistence capable instance, special care
 * must be taken to ensure that their state is managed correctly.
 * Specifically, they must be copied when saving state for rollback, and they
 * must be proxied for any instance whose state is managed by a state manager,
 * where proxying involves creating a second class object that automaticlly
 * notifies its owning instance whenever it is modified. Generally, this
 * factory is only used by the implementation; second class object handling
 * is transparent to client code.
 *
 * @author Abe White
 */
public interface ProxyManager {

    /**
     * Return a copy of the given date with the same information.
     */
    public Date copyDate(Date orig);

    /**
     * Return a copy of the given Calendar with the same information.
     */
    public Calendar copyCalendar(Calendar orig);

    /**
     * Return a new collection of the same type as the given one
     * with a copy of all contained elements.
     * If the given owner is non-null, the returned value should be a proxy
     * for the given owner, otherwise it should not be a proxy.
     */
    public Collection copyCollection(Collection orig);

    /**
     * Return a new map of the same type as the given one
     * with a copy of all contained key/value pairs.
     * If the given owner is non-null, the returned value should be a proxy
     * for the given owner, otherwise it should not be a proxy.
     */
    public Map copyMap(Map orig);

    /**
     * Return a new array of the same component type as the given array
     * and containing the same elements. Works for both primitive and
     * object array types.
     */
    public Object copyArray(Object orig);

    /**
     * Return a copy of the given object with the same
     * information. If this manager cannot proxy the given type, return null.
     * If the given owner is non-null, the returned value should be a proxy
     * for the given owner, otherwise it should not be a proxy.
     *
     * @since 2.5
     */
    public Object copyCustom(Object orig);

    /**
     * Return a new date proxy.
     */
    public Proxy newDateProxy(Class type);

    /**
     * Return a new calendar proxy.
     */
    public Proxy newCalendarProxy(Class type, TimeZone timeZone);

    /**
     * Return a proxy for the given collection type. The returned collection
     * will allow only addition of elements assignable from the given
     * element type and will use the given comparator, if it is not null.
     */
    public Proxy newCollectionProxy(Class type, Class elementType,
        Comparator compare);

    /**
     * Return a proxy for the given map type. The returned map will
     * allow only addition of keys/values assignable from the given
     * keyType/valueType, and will use the given comparator, if it is not null.
     */
    public Proxy newMapProxy(Class type, Class keyType, Class valueType,
        Comparator compare);

    /**
     * Return a proxy for the given object, or null if this manager cannot
     * proxy the object.
     *
     * @since 2.5
     */
    public Proxy newCustomProxy(Object obj);
}
