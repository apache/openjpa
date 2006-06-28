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
package org.apache.openjpa.lib.util;

import java.io.*;

import java.util.*;
import java.util.zip.*;


/**
 *  <p>{@link ResourceBundleProvider} that expects the
 *  {@link ClassLoader#getResourceAsStream} method to return a zipped input
 *  stream.  Created for use under Weblogic RARs.</p>
 *
 *  @author Patrick Linskey
 */
class ZipResourceBundleProvider implements ResourceBundleProvider {
    public ResourceBundle findResource(String name, Locale locale,
        ClassLoader loader) {
        String rsrc = name.replace('.', '/') + ".properties";

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        InputStream in = loader.getResourceAsStream(rsrc);

        if (in == null) {
            return null;
        }

        ZipInputStream zip = new ZipInputStream(in);
        ResourceBundle bundle = null;

        try {
            ZipEntry ze;

            while (true) {
                ze = zip.getNextEntry();

                if (ze == null) {
                    break;
                }

                if (rsrc.equals(ze.getName())) {
                    return new PropertyResourceBundle(zip);
                }

                zip.closeEntry();
            }
        } catch (Exception e) {
        } finally {
            try {
                zip.close();
            } catch (IOException ioe) {
            }
        }

        return null;
    }
}
