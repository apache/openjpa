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

import serp.util.*;

import java.io.*;

import java.util.*;
import java.util.zip.*;


/**
 *  <p>Iterator over directories in the classpath.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public class ClasspathMetaDataIterator extends MetaDataIteratorChain {
    /**
     *  Default constructor; iterates over all classpath elements.
     */
    public ClasspathMetaDataIterator() throws IOException {
        this(null, null);
    }

    /**
     *  Constructor; supply the classpath directories to scan and an optional
     *  resource filter.  The given directories may be null to scan all
     *  classpath directories.
     */
    public ClasspathMetaDataIterator(String[] dirs, MetaDataFilter filter)
        throws IOException {
        Properties props = System.getProperties();
        String path = props.getProperty("java.class.path");
        String[] tokens = Strings.split(path,
                props.getProperty("path.separator"), 0);

        for (int i = 0; i < tokens.length; i++) {
            if ((dirs != null) && (dirs.length != 0) &&
                    !endsWith(tokens[i], dirs)) {
                continue;
            }

            File file = new File(tokens[i]);

            if (!file.exists()) {
                continue;
            }

            if (file.isDirectory()) {
                addIterator(new FileMetaDataIterator(file, filter));
            } else if (tokens[i].endsWith(".jar")) {
                addIterator(new ZipFileMetaDataIterator(new ZipFile(file),
                        filter));
            }
        }
    }

    /**
     *  Return true if the given token ends with any of the given strings.
     */
    private static boolean endsWith(String token, String[] suffs) {
        for (int i = 0; i < suffs.length; i++)
            if (token.endsWith(suffs[i])) {
                return true;
            }

        return false;
    }
}
