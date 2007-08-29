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
package org.apache.openjpa.conf;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.kernel.Bootstrap;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.BrokerFactory;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.conf.CacheMarshallersValue;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.meta.SequenceMetaData;

/**
 * Performs maintenance tasks on the metadata caches accessible via the
 * {@link CacheMarshaller} architecture.
 *
 * @since 1.1.0
 */
public class MetaDataCacheMaintenance {

    private final BrokerFactory factory;
    private final OpenJPAConfiguration conf;
    private final boolean devpath;
    private final boolean verbose;
    private PrintStream out = System.out;

    public MetaDataCacheMaintenance(BrokerFactory factory, boolean devpath,
        boolean verbose) {
        this.factory = factory;
        this.conf = factory.getConfiguration();
        this.devpath = devpath;
        this.verbose = verbose;
    }

    public void setOutputStream(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        boolean devpath = opts.getBooleanProperty("scanDevPath", "ScanDevPath",
            true);
        boolean verbose = opts.getBooleanProperty("verbose", "verbose",
            false);

        BrokerFactory factory = Bootstrap.newBrokerFactory();
        try {
            MetaDataCacheMaintenance maint = new MetaDataCacheMaintenance(
                factory, devpath, verbose);

            if (args.length != 1)
                usage();

            if ("store".equals(args[0]))
                maint.store();
            else if ("dump".equals(args[0]))
                maint.dump();
            else
                usage();
        } finally {
            factory.close();
        }
    }

    private static int usage() {
        System.err.println("Usage: java MetaDataCacheMaintenance "
            + "[-scanDevPath t|f] [-verbose t|f] store | dump");
        return -1;
    }

    public void store() {
        MetaDataRepository repos = conf.getMetaDataRepositoryInstance();
        repos.setSourceMode(MetaDataRepository.MODE_META
            | MetaDataRepository.MODE_MAPPING
            | MetaDataRepository.MODE_QUERY);
        Collection types = repos.loadPersistentTypes(devpath, null);
        for (Iterator iter = types.iterator(); iter.hasNext(); )
            repos.getMetaData((Class) iter.next(), null, true);

        loadQueries();

        out.println("The following data will be stored: ");
        log(repos, conf.getQueryCompilationCacheInstance(), verbose, out);

        CacheMarshallersValue.getMarshallerById(conf, getClass().getName())
            .store(new Object[] {
                repos, conf.getQueryCompilationCacheInstance()
            });
    }

    private void loadQueries() {
        Broker broker = factory.newBroker();
        try {
            QueryMetaData[] qmds =
                conf.getMetaDataRepositoryInstance().getQueryMetaDatas();
            for (int i = 0; i < qmds.length; i++)
                loadQuery(broker, qmds[i]);
        } finally {
            broker.close();
        }
    }

    private void loadQuery(Broker broker, QueryMetaData qmd) {
        try {
            Query q = broker.newQuery(qmd.getLanguage(), null);
            qmd.setInto(q);
            q.compile();
        } catch (Exception e) {
            out.println("Skipping named query " + qmd.getName() + ": "
                + e.getMessage());
            if (verbose)
                e.printStackTrace(out);
        }
    }

    public void dump() {
        Object[] os = (Object[])
            CacheMarshallersValue.getMarshallerById(conf, getClass().getName())
            .load();
        if (os == null) {
            out.println("No cached data was found");
            return;
        }
        MetaDataRepository repos = (MetaDataRepository) os[0];
        Map qcc = (Map) os[1];

        out.println("The following data was found: ");
        log(repos, qcc, verbose, out);
    }

    private static void log(MetaDataRepository repos, Map qcc,
        boolean verbose, PrintStream out) {
        ClassMetaData[] metas = repos.getMetaDatas();
        out.println("  Types: " + metas.length);
        if (verbose)
            for (int i = 0; i < metas.length; i++)
                out.println("    " + metas[i].getDescribedType().getName());

        QueryMetaData[] qmds = repos.getQueryMetaDatas();
        out.println("  Queries: " + qmds.length);
        if (verbose)
            for (int i = 0; i < qmds.length; i++)
                out.println("    " + qmds[i].getName() + ": "
                    + qmds[i].getQueryString());

        SequenceMetaData[] smds = repos.getSequenceMetaDatas();
        out.println("  Sequences: " + smds.length);
        if (verbose)
            for (int i = 0; i < smds.length; i++)
                out.println("    " + smds[i].getName());

        out.println("  Compiled queries: "
            + (qcc == null ? "0" : "" + qcc.size()));
        if (verbose && qcc != null)
            for (Iterator iter = qcc.keySet().iterator(); iter.hasNext(); )
                out.println("    " + iter.next());
    }
}
