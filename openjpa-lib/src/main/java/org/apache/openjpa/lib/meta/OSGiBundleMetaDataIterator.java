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
package org.apache.openjpa.lib.meta;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Created by andrei on 24.07.2014.
 */
public class OSGiBundleMetaDataIterator implements MetaDataIterator, MetaDataFilter.Resource {

    private InputStream stream;
    private Bundle bundle;
    private Enumeration<URL> entries;
    private MetaDataFilter filter;
    private URL entry;
    private URL last;
    private byte[] buf;

    public OSGiBundleMetaDataIterator(URL bundleUrl, MetaDataFilter filter) {

        BundleContext ctx = FrameworkUtil.getBundle(OSGiBundleMetaDataIterator.class).getBundleContext();
        long bundleId = Long.parseLong(bundleUrl.getHost().substring(0, bundleUrl.getHost().indexOf(".")));
        this.bundle = ctx.getBundle(bundleId);
        entries = this.bundle.findEntries("/", "*.class", true);
        this.filter = filter;

    }

    @Override
    public boolean hasNext() throws IOException {

        if (entries == null) {
            return false;
        }

        if (entry != null) {
            return true;
        }

        last = null;
        buf = null;

        if (!entries.hasMoreElements()) {
            return false;
        }

        URL tmp;
        while ( entry == null && (entries.hasMoreElements() && (tmp = this.entries.nextElement()) != null)) {
            entry = tmp;
            stream = entry.openStream();
            if (filter != null && !this.filter.matches(this)) {
                entry = null;
            }

        }

        return entry != null;

    }

    @Override
    public Object next() throws IOException {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String name = entry.toString();
        last = entry;
        entry = null;

        return name;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (last == null)
            throw new IllegalStateException();

        return last.openStream();
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public String getName() {
        return entry.toString();
    }

    @Override
    public byte[] getContent() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        for (int r; (r = stream.read(buf)) != -1; bout.write(buf, 0, r)) ;
        buf = bout.toByteArray();
        stream.close();
        return buf;
    }
}
