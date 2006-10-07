/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.conf;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class contains version information for OpenJPA. It uses
 * Ant's filter tokens to convert the template into a java
 * file with current information.
 *
 * @author Marc Prud'hommeaux, Patrick Linskey
 */
public class OpenJPAVersion {

    public static final String VERSION_NUMBER;
    public static final String VERSION_ID;
    public static final String VENDOR_NAME = "OpenJPA";
    public static final int MAJOR_RELEASE;
    public static final int MINOR_RELEASE;
    public static final int PATCH_RELEASE;
    public static final String RELEASE_STATUS;
    public static final String REVISION_NUMBER;

    static {
        Package pack = OpenJPAVersion.class.getPackage();
        String vers = pack == null ? null : pack.getImplementationVersion();
        if (vers == null || vers.length() == 0)
            vers = "0.0.0";

        VERSION_NUMBER = vers;

        StringTokenizer tok = new StringTokenizer(VERSION_NUMBER, ".-");

        int major, minor, patch;

        try {
            major = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            major = 0;
        }

        try {
            minor = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            minor = 0;
        }

        try {
            patch = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            patch = 0;
        }

        String revision = "";
        try {
            InputStream in = OpenJPAVersion.class.
                getResourceAsStream("/META-INF/revision.properties");
            if (in != null) {
                try {
                    Properties props = new Properties();
                    props.load(in);
                    revision = props.getProperty("revision.number");
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
        }

        MAJOR_RELEASE = major;
        MINOR_RELEASE = minor;
        PATCH_RELEASE = patch;
        RELEASE_STATUS = tok.hasMoreTokens() ? tok.nextToken("!") : "";
        REVISION_NUMBER = revision;
        VERSION_ID = VERSION_NUMBER + "-r" + REVISION_NUMBER;
    }

    public static void main(String [] args) {
        System.out.println(new OpenJPAVersion().toString());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(80 * 30);
        appendOpenJPABanner(buf);
        buf.append("\n");

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

    public void appendOpenJPABanner(StringBuffer buf) {
        buf.append(VENDOR_NAME).append(" ");
        buf.append(VERSION_NUMBER);
        buf.append("\n");
        buf.append("version id: ").append(VERSION_ID);
        buf.append("\n");
        buf.append("Apache svn revision: ").append(REVISION_NUMBER);
        buf.append("\n");
    }

    private StringBuffer getProperty(String prop, StringBuffer buf) {
        buf.append(prop).append(": ").append(System.getProperty(prop));
        return buf;
    }
}
