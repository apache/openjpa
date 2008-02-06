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
package org.apache.openjpa.slice;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.openjpa.event.RemoteCommitEventManager;
import org.apache.openjpa.event.RemoteCommitProvider;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.util.UserException;

/**
 * Value type used to represent a {@link ExecutorService}.
 * This value controls the thread pool parameters. The thread pool is used
 * to execute the queries.
 * 
 * @author Pinaki Poddar
 * @nojavadoc
 */

public class ExecutorServiceValue extends PluginValue {
    private static List<String> known =
            Arrays.asList(new String[] { "cached", "fixed" });

    private static Localizer _loc =
            Localizer.forPackage(ExecutorServiceValue.class);

    public ExecutorServiceValue() {
        super("ThreadingPolicy", true);
        setDefault("cached");
    }

    public void setProperties(String props) {
        super.setProperties(props);
    }

    /**
     * Configures a cached or fixed thread pool.
     */
    @Override
    public Object instantiate(Class type, Configuration conf, boolean fatal) {
        Object obj = null;
        int defaultSize = 10;
        String cls = getClassName();
        if (!known.contains(cls))
            cls = "cached";

        Options opts = Configurations.parseProperties(getProperties());

        ThreadFactory factory = null;
        if (opts.containsKey("ThreadFactory")) {
            String fName = opts.getProperty("ThreadFactory");
            try {
                factory = (ThreadFactory) Class.forName(fName).newInstance();
                Configurations.configureInstance(factory, conf, opts,
                        getProperty());
            } catch (Throwable t) {
                throw new UserException(_loc.get("bad-thread-factory", fName), t);
            } finally {
                opts.removeProperty("ThreadFactory");
            }
        } else {
            factory = Executors.defaultThreadFactory();
        }
        if ("cached".equals(cls)) {
            obj = Executors.newCachedThreadPool(factory);
        } else if ("fixed".equals(cls)) {
            long keepAliveTime = 60L;
            if (opts.containsKey("KeepAliveTime")) {
                keepAliveTime = opts.getLongProperty("KeepAliveTime");
                opts.removeLongProperty("KeepAliveTime");
            }
            obj = new ThreadPoolExecutor(defaultSize, defaultSize,
                            keepAliveTime, TimeUnit.SECONDS,
                            new PriorityBlockingQueue<Runnable>(), factory);

            Configurations.configureInstance(obj, conf, opts, getProperty());
        }
        set(obj, true);
        return obj;
    }
}
