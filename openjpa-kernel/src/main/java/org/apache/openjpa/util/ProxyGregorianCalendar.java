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
package org.apache.openjpa.util;

import java.io.ObjectStreamException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Extension of the {@link Calendar} type that calls the <code>dirty</code>
 * method on its owning persistence capable instance on modification. This
 * class does not support modification via any deprecated method of the
 * date class.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class ProxyGregorianCalendar
    extends GregorianCalendar
    implements ProxyCalendar {

    private transient OpenJPAStateManager _sm = null;
    private transient int _field = -1;

    public ProxyGregorianCalendar() {
        super();
    }

    public ProxyGregorianCalendar(int year, int month, int dayOfMonth) {
        super(year, month, dayOfMonth);
    }

    public ProxyGregorianCalendar(int year, int month, int dayOfMonth,
        int hourOfDay, int minute) {
        super(year, month, dayOfMonth, hourOfDay, minute);
    }

    public ProxyGregorianCalendar(int year, int month, int dayOfMonth,
        int hourOfDay, int minute, int second) {
        super(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    public ProxyGregorianCalendar(Locale aLocale) {
        super(aLocale);
    }

    public ProxyGregorianCalendar(TimeZone zone) {
        super(zone);
    }

    public ProxyGregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    public ProxyCalendar newInstance(TimeZone timeZone) {
        if (timeZone == null)
            return new ProxyGregorianCalendar();
        else
            return new ProxyGregorianCalendar(timeZone);
    }

    public void setOwner(OpenJPAStateManager sm, int field) {
        _sm = sm;
        _field = field;
    }

    public OpenJPAStateManager getOwner() {
        return _sm;
    }

    public int getOwnerField() {
        return _field;
    }

    public ChangeTracker getChangeTracker() {
        return null;
    }

    public Object copy(Object orig) {
        Calendar origCal = (Calendar) orig;

        GregorianCalendar cal = new GregorianCalendar(origCal.getTimeZone());
        cal.setTime(origCal.getTime());

        return cal;
    }

    protected Object writeReplace()
        throws ObjectStreamException {
        if (_sm != null && _sm.isDetached())
            return this;
        return copy(this);
    }

    protected void computeFields() {
        // Calendar.computeFields() is called whenever a mutation
        // occurs in order to recalculate all the fields
        Proxies.dirty(this);
        super.computeFields();
    }

    public void setTimeInMillis(long millis) {
        if (millis != getTimeInMillis()) {
            Proxies.dirty(this);
            super.setTimeInMillis(millis);
        }
    }

    /* This is "final" in JDK 1.3 (not in 1.4 or 1.5)
 public void set (int field, int value)
 {
 if (get (field) != value)
 {
 Proxies.dirty (this);
 super.set (field, value);
 }
 }
     */

    public void add(int field, int amount) {
        if (amount != 0) {
            Proxies.dirty(this);
            super.add(field, amount);
        }
    }

    public void roll(int field, boolean up) {
        Proxies.dirty(this);
        super.roll(field, up);
    }

    public void roll(int field, int amount) {
        if (amount != 0) {
            Proxies.dirty(this);
            super.roll(field, amount);
        }
    }

    public void setTimeZone(TimeZone value) {
        Proxies.dirty(this);
        super.setTimeZone(value);
    }

    public void setLenient(boolean lenient) {
        if (isLenient() != lenient) {
            Proxies.dirty(this);
            super.setLenient(lenient);
        }
    }

    public void setFirstDayOfWeek(int value) {
        if (getFirstDayOfWeek() != value) {
            Proxies.dirty(this);
            super.setFirstDayOfWeek(value);
        }
    }

    public void setMinimalDaysInFirstWeek(int value) {
        if (getMinimalDaysInFirstWeek() != value) {
            Proxies.dirty(this);
            super.setMinimalDaysInFirstWeek(value);
        }
    }
}
