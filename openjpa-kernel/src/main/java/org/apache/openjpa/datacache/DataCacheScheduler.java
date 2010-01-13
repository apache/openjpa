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
package org.apache.openjpa.datacache;

import java.security.AccessController;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.UserException;
import serp.util.Strings;

/**
 * Cron-style cache eviction. Understands schedules based on cron format:
 * <code>minute hour mday month wday</code>
 * For example:
 * <code>15,30 6,19 2,10 1 2 </code>
 * Would run at 15 and 30 past the 6AM and 7PM, on the 2nd and 10th
 * of January when its a Monday.
 *
 * @author Steve Kim
 */
public class DataCacheScheduler
    implements Runnable {

    private static final Localizer _loc = Localizer.forPackage
        (DataCacheScheduler.class);

    private Map _caches = new ConcurrentHashMap();
    private boolean _stop = false;
    private int _interval = 2;
    private Log _log;
    private Thread _thread;

    public DataCacheScheduler(OpenJPAConfiguration conf) {
        _log = conf.getLogFactory().getLog(OpenJPAConfiguration.LOG_DATACACHE);
    }

    /**
     * The interval time in minutes between cache checks. Defaults to 2.
     */
    public int getInterval() {
        return _interval;
    }

    /**
     * The interval time in minutes between cache checks. Defaults to 2.
     */
    public void setInterval(int interval) {
        _interval = interval;
    }

    /**
     * Stop the associated thread if there and stop the current runnable.
     */
    public synchronized void stop() {
        _stop = true;
    }

    private boolean isStopped() {
        return _stop;
    }

    /**
     * Schedule the given cache for eviction. Starts the scheduling thread
     * if not started.
     */
    public synchronized void scheduleEviction(DataCache cache, String times) {
        if (times == null)
            return;

        Schedule schedule = new Schedule(times);
        _caches.put(cache, schedule);
        _stop = false;
        if (_thread == null) {
            _thread = (Thread) AccessController.doPrivileged(J2DoPrivHelper
                .newDaemonThreadAction(this, _loc.get("scheduler-name")
                    .getMessage()));
            _thread.start();
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("scheduler-start", _thread.getName()));
        }
    }

    /**
     * Remove the given cache from scheduling.
     */
    public synchronized void removeFromSchedule(DataCache cache) {
        _caches.remove(cache);
        if (_caches.size() == 0)
            stop();
    }

    public void run() {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("scheduler-interval", _interval + ""));

        Date lastRun = new Date();
        DateFormat fom = new SimpleDateFormat("E HH:mm:ss");
        while (!isStopped()) {
            try {
                Thread.sleep(_interval * 60 * 1000);

                Date now = new Date();
                DataCache cache;
                Schedule schedule;
                Map.Entry entry;
                for (Iterator i = _caches.entrySet().iterator(); i.hasNext();) {
                    entry = (Map.Entry) i.next();
                    cache = (DataCache) entry.getKey();
                    schedule = (Schedule) entry.getValue();
                    if (schedule.matches(lastRun, now)) {
                        if (_log.isTraceEnabled())
                            _log.trace(_loc.get("scheduler-clear",
                                cache.getName(), fom.format(now)));
                        evict(cache);
                    }
                }
                lastRun = now;
            } catch (Exception e) {
                throw new InvalidStateException(_loc.get("scheduler-fail"), e).
                    setFatal(true);
            }
        }

        _log.info(_loc.get("scheduler-stop"));
        synchronized (this) {
            if (isStopped())
                _thread = null; // be sure to deref the thread so it can restart
        }
    }

    protected void evict(DataCache cache) {
        cache.clear();
    }

    /**
     * Simple class which represents the given time schedule.
     */
    private static class Schedule {

        static final int[] WILDCARD = new int[0];
        static final int[] UNITS = {
            Calendar.MONTH,
            Calendar.DAY_OF_MONTH,
            Calendar.DAY_OF_WEEK,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE
        };
        final int[] month;
        final int[] dayOfMonth;
        final int[] dayOfWeek;
        final int[] hour;
        final int[] min;

        public Schedule(String date) {
            StringTokenizer token = new StringTokenizer(date, " \t");
            if (token.countTokens() != 5)
                throw new UserException(_loc.get("bad-count", date)).
                    setFatal(true);
            try {
                min = parse(token.nextToken(), 0, 60);
                hour = parse(token.nextToken(), 0, 24);
                dayOfMonth = parse(token.nextToken(), 1, 31);
                month = parse(token.nextToken(), 1, 13);
                dayOfWeek = parse(token.nextToken(), 1, 8);
            } catch (Throwable t) {
                throw new UserException(_loc.get("bad-schedule", date), t).
                    setFatal(true);
            }
        }

        private int[] parse(String token, int min, int max) {
            if ("*".equals(token.trim()))
                return WILDCARD;
            String[] tokens = Strings.split(token, ",", 0);
            int [] times = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                try {
                    times[i] = Integer.parseInt(tokens[i]);
                } catch (Throwable t) {
                    throw new UserException(_loc.get("not-number", token)).
                        setFatal(true);
                }
                if (times[i] < min || times[i] >= max)
                    throw new UserException(_loc.get("not-range", token,
                        String.valueOf(min), String.valueOf(max))).
                        setFatal(true);
            }
            return times;
        }

        boolean matches(Date last, Date now) {
            Calendar time = Calendar.getInstance();
            time.setTime(now);
            time.set(Calendar.SECOND, 0);
            time.set(Calendar.MILLISECOND, 0);

            int[][] all =
                new int[][]{ month, dayOfMonth, dayOfWeek, hour, min };
            return matches(last, now, time, all, 0);
        }

        private boolean matches(Date last, Date now, Calendar time,
            int[][] times, int depth) {
            if (depth == UNITS.length) {
                Date compare = time.getTime();
                return compare.compareTo(last) >= 0 &&
                    compare.compareTo(now) < 0;
            }

            if (times[depth] != WILDCARD) {
                for (int i = 0; i < times[depth].length; i++) {
                    time.set(UNITS[depth], times[depth][i]);
                    if (matches(last, now, time, times, depth + 1))
                        return true;
                }
            }
            return matches(last, now, time, times, depth + 1);
		}
	}
}
