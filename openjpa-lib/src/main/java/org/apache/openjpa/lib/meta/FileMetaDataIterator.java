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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Iterator over a file, or over all metadata resources below a given directory.
 *
 * @author Abe White
 */
public class FileMetaDataIterator implements MetaDataIterator {

    private static final long SCAN_LIMIT = 100000;

    private static final Localizer _loc = Localizer.forPackage
        (FileMetaDataIterator.class);

    private final Iterator<File> _itr;
    private File _file = null;

    /**
     * Constructor; supply metadata file.
     */
    public FileMetaDataIterator(File file) {
        _itr = Collections.singleton(file).iterator();
    }

    /**
     * Constructor; supply root of directory tree to search and optional
     * file filter.
     */
    public FileMetaDataIterator(File dir, MetaDataFilter filter)
        throws IOException {
        if (dir == null)
            _itr = null;
        else {
            Collection<File> metas = new ArrayList<>();
            FileResource rsrc = (filter == null) ? null : new FileResource();
            scan(dir, filter, rsrc, metas, 0);
            _itr = metas.iterator();
        }
    }

    /**
     * Scan all files below the given one for metadata files, adding them
     * to the given collection.
     */
    private int scan(File file, MetaDataFilter filter, FileResource rsrc,
        Collection<File> metas, int scanned) throws IOException {
        if (scanned > SCAN_LIMIT)
            throw new IllegalStateException(_loc.get("too-many-files",
                String.valueOf(SCAN_LIMIT)).getMessage());
        scanned++;

        if (filter == null)
            metas.add(file);
        else {
            rsrc.setFile(file);
            if (filter.matches(rsrc))
                metas.add(file);
            else {
                File[] files = (File[]) file.listFiles();
                if (files != null)
                    for (File value : files) {
                        scanned = scan(value, filter, rsrc, metas, scanned);
                    }
            }
        }
        return scanned;
    }

    @Override
    public boolean hasNext() {
        return _itr != null && _itr.hasNext();
    }

    @Override
    public URL next() throws IOException {
        if (_itr == null)
            throw new NoSuchElementException();

        _file = _itr.next();
        File f = _file.getAbsoluteFile();
        return f.toURI().toURL();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (_file == null)
            throw new IllegalStateException();
        FileInputStream fis = null;
        fis = new FileInputStream(_file);
        return fis;
    }

    @Override
    public File getFile() {
        if (_file == null)
            throw new IllegalStateException();
        return _file;
    }

    @Override
    public void close() {
    }

    private static class FileResource implements MetaDataFilter.Resource {

        private File _file = null;

        public void setFile(File file) {
            _file = file;
        }

        @Override
        public String getName() {
            return _file.getName();
        }

        @Override
        public byte[] getContent() throws IOException {
            long len = _file.length();
            FileInputStream fin = null;
            fin = new FileInputStream(_file);
            try {
                byte[] content;
                if (len <= 0 || len > Integer.MAX_VALUE) {
                    // some JVMs don't return a proper length
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    for (int r; (r = fin.read(buf)) != -1;)
                        bout.write(buf, 0, r);
                    content = bout.toByteArray();
                } else {
                    content = new byte[(int) len];
                    for (int r, o = 0; o < content.length && (r = fin.
                        read(content, o, content.length - o)) != -1; o += r);
                }
                return content;
            } finally {
                try { fin.close(); } catch (IOException ioe) {}
            }
        }
    }
}
