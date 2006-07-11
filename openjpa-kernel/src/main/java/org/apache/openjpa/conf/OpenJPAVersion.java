package org.apache.openjpa.conf;

import java.io.File;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This class contains version information for Kodo. It uses
 * Ant's filter tokens to convert the template into a java
 * file with current information.
 *
 * @author Marc Prud'hommeaux, Patrick Linskey
 */
public class OpenJPAVersion {

    public static final String VERSION_NUMBER = "4.1.0EA1";
    private static final long RELEASE_SECONDS = 1147454303;
    public static final Date RELEASE_DATE = new Date(RELEASE_SECONDS * 1000);
    public static final String VERSION_ID = "kodo-4.1.0EA1-20060710-0004";
    public static final String VENDOR_NAME = "BEA";
    public static final int MAJOR_RELEASE = 4;
    public static final int MINOR_RELEASE = 1;
    public static final int PATCH_RELEASE = 0;
    public static final String RELEASE_STATUS = "EA1";

    public static void main(String [] args) {
        System.out.println(new OpenJPAVersion().toString());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(80 * 30);
        buf.append("Kodo");
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
