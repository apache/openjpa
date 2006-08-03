package org.apache.openjpa.util;

import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link ProxyGregorianCalendar} subclass that overrides the mutating
 * {@link #set} method, which is final in Java 1.3 but public in Java 1.4
 * and higher.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class ProxyGregorianCalendar4
    extends ProxyGregorianCalendar {

    public ProxyGregorianCalendar4() {
        super();
    }

    public ProxyGregorianCalendar4(int year, int month, int dayOfMonth) {
        super(year, month, dayOfMonth);
    }

    public ProxyGregorianCalendar4(int year, int month, int dayOfMonth,
        int hourOfDay, int minute) {
        super(year, month, dayOfMonth, hourOfDay, minute);
    }

    public ProxyGregorianCalendar4(int year, int month, int dayOfMonth,
        int hourOfDay, int minute, int second) {
        super(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    public ProxyGregorianCalendar4(Locale aLocale) {
        super(aLocale);
    }

    public ProxyGregorianCalendar4(TimeZone zone) {
        super(zone);
    }

    public ProxyGregorianCalendar4(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    public void set(int field, int value) {
        if (get(field) != value) {
            Proxies.dirty(this);
            super.set(field, value);
        }
    }

    public ProxyCalendar newInstance(TimeZone timeZone) {
        if (timeZone == null)
            return new ProxyGregorianCalendar4();
        else
            return new ProxyGregorianCalendar4(timeZone);
    }
}
