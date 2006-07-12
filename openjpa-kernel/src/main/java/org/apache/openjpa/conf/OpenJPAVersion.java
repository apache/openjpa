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
package org.apache.openjpa.conf;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This class contains version information for OpenJPA. It uses
 * Ant's filter tokens to convert the template into a java
 * file with current information.
 *
 * @author Marc Prud'hommeaux, Patrick Linskey
 */
public class OpenJPAVersion {

    public static final String VERSION_NUMBER = OpenJPAVersion.class
        .getPackage().getImplementationVersion() == null ? "0.0.0" :
        OpenJPAVersion.class.getPackage().getImplementationVersion();
    private static final long RELEASE_SECONDS = 1147454303;

    public static final Date RELEASE_DATE = new Date(RELEASE_SECONDS * 1000);

    public static final String VERSION_ID = VERSION_NUMBER;
    public static final String VENDOR_NAME =
        OpenJPAVersion.class.getPackage().getImplementationVendor();
    public static final int MAJOR_RELEASE;
    public static final int MINOR_RELEASE;
    public static final int PATCH_RELEASE;
    public static final String RELEASE_STATUS;

    static {

        java.util.StringTokenizer tok =
            new java.util.StringTokenizer(VERSION_NUMBER,
                ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        MAJOR_RELEASE =
            tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        MINOR_RELEASE =
            tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        PATCH_RELEASE =
            tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        RELEASE_STATUS = tok.hasMoreTokens() ? tok.nextToken(".") : "";
    }

    public static void main(String [] args) {
        System.out.println(new OpenJPAVersion().toString());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(80 * 30);
        buf.append("OpenJPA");
        buf.append(VERSION_NUMBER);
        buf.append("\n");
        buf.append("version id: ").append(VERSION_ID);
        buf.append("\n\n");

        getProperty("os.name", buf).append("\n");
        getProperty("os.version", buf).append("\n");
        getProperty("os.arch", buf).append("\n\n");

        getProperty("java.version", buf).append("\n");
        getProperty("java.vendor", buf).append("\n\n");

        buf.append("java.class.path:\n");
        StringTokenizer tok = new StringTokenizer
            (System.getProperty("java.class.path"), File.pathSeparator);
        while (tok.hasMoreTokens()) {
            buf.append("\t").append(tok.nextToken());
            buf.append("\n");
        }
        buf.append("\n");

        getProperty("user.dir", buf);

        return buf.toString();
    }

    private StringBuffer getProperty(String prop, StringBuffer buf) {
        buf.append(prop).append(": ").append(System.getProperty(prop));
        return buf;
    }
}
