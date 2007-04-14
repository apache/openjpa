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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.openjpa.lib.util.MultiClassLoader;
import serp.util.Strings;

/**
 * Iterator over all metadata resources that might contain the
 * metadata for a given class, starting with the most general. Assumes that
 * package-level resources are named "package.&lt;suffix&gt;".
 *
 * @author Abe White
 * @nojavadoc
 */
public class ClassMetaDataIterator implements MetaDataIterator {

    private final ClassLoader _loader;
    private final List _locs;
    private int _loc = -1;
    private final List _urls = new ArrayList(3);
    private int _url = -1;

    /**
     * Constructor; supply the class whose metadata to find, the suffix
     * of metadata files, and whether to parse top-down or bottom-up.
     */
    public ClassMetaDataIterator(Class cls, String suffix, boolean topDown) {
        this(cls, suffix, null, topDown);
    }

    /**
     * Constructor; supply the class whose metadata to find, the suffix
     * of metadata files, and whether to parse top-down or bottom-up.
     */
    public ClassMetaDataIterator(Class cls, String suffix, ClassLoader loader,
        boolean topDown) {
        // skip classes that can't have metadata
        if (cls != null && (cls.isPrimitive()
            || cls.getName().startsWith("java.")
            || cls.getName().startsWith("javax."))) {
            _loader = null;
            _locs = Collections.EMPTY_LIST;
            return;
        }

        if (loader == null) {
            MultiClassLoader multi = new MultiClassLoader();
            multi.addClassLoader(multi.SYSTEM_LOADER);
            multi.addClassLoader(multi.THREAD_LOADER);
            multi.addClassLoader(getClass().getClassLoader());
            if (cls != null && cls.getClassLoader() != null)
                multi.addClassLoader(cls.getClassLoader());
            loader = multi;
        }
        _loader = loader;

        // collect the set of all possible metadata locations; start with
        // system locations
        _locs = new ArrayList();
        _locs.add("META-INF/package" + suffix);
        _locs.add("WEB-INF/package" + suffix);
        _locs.add("package" + suffix);

        // put this legacy location at the end regardless of whether we're
        // going top down or bottom up so we don't have to parse it as often
        // during testing
        if (!topDown)
            _locs.add("system" + suffix);

        if (cls != null) {
            // also check:
            // 1. for each package from the top down to cls' package:
            // <path>/package<suffix>
            // <path>/<package-name><suffix> (legacy support)
            // <path>/../<package-name><suffix> (legacy support)
            // 2. <path>/<class-name><suffix>
            String pkg = Strings.getPackageName(cls).replace('.', '/');
            if (pkg.length() > 0) {
                int idx, start = 0;
                String pkgName, path, upPath = "";
                do {
                    idx = pkg.indexOf('/', start);
                    if (idx == -1) {
                        pkgName = (start == 0) ? pkg : pkg.substring(start);
                        path = pkg + "/";
                    } else {
                        pkgName = pkg.substring(start, idx);
                        path = pkg.substring(0, idx + 1);
                    }

                    _locs.add(path + "package" + suffix);
                    _locs.add(path + pkgName + suffix); // legacy
                    _locs.add(upPath + pkgName + suffix); // legacy
                    if (idx == -1)
                        _locs.add(path + Strings.getClassName(cls) + suffix);

                    start = idx + 1;
                    upPath = path;
                }
                while (idx != -1);
            } else {
                // <class-name><suffix> for top-level classes
                _locs.add(cls.getName() + suffix);
            }
        }
        if (topDown)
            _locs.add("system" + suffix); // legacy
        else
            Collections.reverse(_locs);
    }

    public boolean hasNext() throws IOException {
        Enumeration e;
        while (_url + 1 >= _urls.size()) {
            if (++_loc >= _locs.size())
                return false;

            _url = -1;
            _urls.clear();
            e = _loader.getResources((String) _locs.get(_loc));
            while (e.hasMoreElements())
                _urls.add(e.nextElement());
        }
        return true;
    }

    public Object next() throws IOException {
        if (!hasNext())
            throw new NoSuchElementException();
        return _urls.get(++_url);
    }

    public InputStream getInputStream() throws IOException {
        if (_url == -1 || _url >= _urls.size())
            throw new IllegalStateException();
        return ((URL) _urls.get(_url)).openStream();
    }

    public File getFile() throws IOException {
        if (_url == -1 || _url >= _urls.size())
            throw new IllegalStateException();
        File file = new File(URLDecoder.decode(((URL) _urls.get(_url)).
            getFile()));
        return (file.exists()) ? file : null;
    }

    public void close() {
    }
}
