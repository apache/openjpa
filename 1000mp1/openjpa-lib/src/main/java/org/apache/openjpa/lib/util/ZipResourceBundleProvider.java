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
package org.apache.openjpa.lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * {@link ResourceBundleProvider} that expects the
 * {@link ClassLoader#getResourceAsStream} method to return a zipped input
 * stream. Created for use under Weblogic RARs.
 *
 * @author Patrick Linskey
 */
class ZipResourceBundleProvider implements ResourceBundleProvider {

    public ResourceBundle findResource(String name, Locale locale,
        ClassLoader loader) {
        String rsrc = name.replace('.', '/') + ".properties";
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();

        InputStream in = loader.getResourceAsStream(rsrc);
        if (in == null)
            return null;

        ZipInputStream zip = new ZipInputStream(in);
        try {
            ZipEntry ze;
            while (true) {
                ze = zip.getNextEntry();
                if (ze == null)
                    break;

                if (rsrc.equals(ze.getName()))
                    return new PropertyResourceBundle(zip);

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
