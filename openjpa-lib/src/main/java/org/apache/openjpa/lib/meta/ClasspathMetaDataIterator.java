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
import java.util.Properties;
import java.util.zip.ZipFile;

import org.apache.openjpa.lib.util.StringUtil;

/**
 * Iterator over directories in the classpath.
 *
 * @author Abe White
 */
public class ClasspathMetaDataIterator extends MetaDataIteratorChain {

    /**
     * Default constructor; iterates over all classpath elements.
     */
    public ClasspathMetaDataIterator() throws IOException {
        this(null, null);
    }

    /**
     * Constructor; supply the classpath directories to scan and an optional
     * resource filter. The given directories may be null to scan all
     * classpath directories.
     */
    public ClasspathMetaDataIterator(String[] dirs, MetaDataFilter filter)
        throws IOException {
        Properties props = System.getProperties();
        String path = props.getProperty("java.class.path");
        String[] tokens = StringUtil.split(path,
            props.getProperty("path.separator"), 0);

        for (String token : tokens) {
            if (dirs != null && dirs.length != 0 && !endsWith(token, dirs))
                continue;

            File file = new File(token);
            if (!file.exists())
                continue;
            if (file.isDirectory())
                addIterator(new FileMetaDataIterator(file, filter));
            else if (token.endsWith(".jar")) {
                ZipFile zFile = new ZipFile(file);
                addIterator(new ZipFileMetaDataIterator(zFile, filter));
            }
        }
    }

    /**
     * Return true if the given token ends with any of the given strings.
     */
    private static boolean endsWith(String token, String[] suffs) {
        for (String suff : suffs)
            if (token.endsWith(suff))
                return true;
        return false;
    }
}
