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
package org.apache.openjpa.lib.meta;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * Iterator over all metadata resources in a given zip file.
 * 
 * @author Abe White
 * @nojavadoc
 */
public class ZipFileMetaDataIterator
    implements MetaDataIterator, MetaDataFilter.Resource {
    private final ZipFile _file;
    private final MetaDataFilter _filter;
    private final Enumeration _entries;
    private final boolean _close;
    private ZipEntry _entry = null;
    private ZipEntry _last = null;

    /**
     * Constructor; supply zip/jar URL and optional file filter.
     */
    public ZipFileMetaDataIterator(URL url, MetaDataFilter filter)
        throws IOException {
        _file = (url == null) ? null : (ZipFile) url.getContent();
        _filter = filter;
        _entries = (_file == null) ? null : _file.entries();
        _close = false;
    }

    /**
     * Constructor; supply zip file and optional file filter.
     */
    public ZipFileMetaDataIterator(ZipFile file, MetaDataFilter filter) {
        _file = file;
        _filter = filter;
        _entries = (file == null) ? null : file.entries();
        _close = true;
    }

    public boolean hasNext() throws IOException {
        if (_entries == null)
            return false;

        // search for next metadata file
        while (_entry == null && _entries.hasMoreElements()) {
            _entry = (ZipEntry) _entries.nextElement();
            if (_filter != null && !_filter.matches(this))
                _entry = null;
        }
        return _entry != null;
    }

    public Object next() throws IOException {
        if (!hasNext())
            throw new NoSuchElementException();
        String ret = _entry.getName();
        _last = _entry;
        _entry = null;
        return ret;
    }

    public InputStream getInputStream() throws IOException {
        if (_last == null)
            throw new IllegalStateException();
        return _file.getInputStream(_last);
    }

    public File getFile() {
        if (_last == null)
            throw new IllegalStateException();
        return null;
    }

    public void close() {
        if (_close)
            try { _file.close(); } catch (IOException ioe) {}
    }

    //////////////////////////////////////////
    // MetaDataFilter.Resource implementation
    //////////////////////////////////////////

    public String getName() {
        return _entry.getName();
    }

    public byte[] getContent() throws IOException {
        long size = _entry.getSize();
        if (size == 0)
            return new byte[0];

        InputStream in = _file.getInputStream(_entry);
        byte[] content;
        if (size < 0) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int r; (r = in.read(buf)) != -1; bout.write(buf, 0, r));
            content = bout.toByteArray();
        } else {
            content = new byte[(int) size];
            in.read(content);
        }
        in.close();
        return content;
    }
}

