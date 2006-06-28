/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.meta;

import org.apache.commons.collections.iterators.*;

import org.apache.openjpa.lib.util.*;

import java.io.*;

import java.net.*;

import java.util.*;


/**
 *  <p>Iterator over a given metadata resource.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public class ResourceMetaDataIterator implements MetaDataIterator {
    private List _urls = null;
    private int _url = -1;

    /**
     *  Constructor; supply the resource to parse.
     */
    public ResourceMetaDataIterator(String rsrc) throws IOException {
        this(rsrc, null);
    }

    /**
     *  Constructor; supply the resource to parse.
     */
    public ResourceMetaDataIterator(String rsrc, ClassLoader loader)
        throws IOException {
        if (loader == null) {
            MultiClassLoader multi = new MultiClassLoader();
            multi.addClassLoader(multi.SYSTEM_LOADER);
            multi.addClassLoader(multi.THREAD_LOADER);
            multi.addClassLoader(getClass().getClassLoader());
            loader = multi;
        }

        Enumeration e = loader.getResources(rsrc);

        while (e.hasMoreElements()) {
            if (_urls == null) {
                _urls = new ArrayList(3);
            }

            _urls.add(e.nextElement());
        }
    }

    public boolean hasNext() {
        return (_urls != null) && ((_url + 1) < _urls.size());
    }

    public Object next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return _urls.get(++_url);
    }

    public InputStream getInputStream() throws IOException {
        if ((_url == -1) || (_url >= _urls.size())) {
            throw new IllegalStateException();
        }

        return ((URL) _urls.get(_url)).openStream();
    }

    public File getFile() throws IOException {
        if ((_url == -1) || (_url >= _urls.size())) {
            throw new IllegalStateException();
        }

        File file = new File(URLDecoder.decode(
                    ((URL) _urls.get(_url)).getFile()));

        return (file.exists()) ? file : null;
    }

    public void close() {
    }
}
