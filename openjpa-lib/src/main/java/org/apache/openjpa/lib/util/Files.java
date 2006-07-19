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
package org.apache.openjpa.lib.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.lang.exception.NestableRuntimeException;
import serp.util.Strings;

/**
 * Utility operations on files.
 *
 * @author Abe White
 * @nojavadoc
 */
public class Files {

    /**
     * Backup the given file to a new file called &lt;file-name&gt;~. If
     * the file does not exist or a backup could not be created, returns null.
     */
    public static File backup(File file, boolean copy) {
        if (file == null || !file.exists())
            return null;

        // create new file object copy so we don't modify the original
        File clone = new File(file.getAbsolutePath());
        File bk = new File(file.getAbsolutePath() + "~");
        if (!clone.renameTo(bk))
            return null;
        if (copy) {
            try {
                copy(bk, file);
            } catch (IOException ioe) {
                throw new NestableRuntimeException(ioe);
            }
        }
        return bk;
    }

    /**
     * Revert the given backup file to the original location. If the given
     * file's name does not end in '~', the '~' is appended before proceeding.
     * If the backup file does not exist or could not be reverted, returns null.
     */
    public static File revert(File backup, boolean copy) {
        if (backup == null)
            return null;
        if (!backup.getName().endsWith("~"))
            backup = new File(backup.getPath() + "~");
        if (!backup.exists())
            return null;

        // create new file object copy so we don't modify the original
        String path = backup.getAbsolutePath();
        File clone = new File(path);
        File orig = new File(path.substring(0, path.length() - 1));
        if (!clone.renameTo(orig))
            return null;
        if (copy) {
            try {
                copy(orig, backup);
            } catch (IOException ioe) {
                throw new NestableRuntimeException(ioe);
            }
        }
        return orig;
    }

    /**
     * Return the source file for the given class, or null if the
     * source is not in the CLASSPATH.
     */
    public static File getSourceFile(Class cls) {
        return getClassFile(cls, ".java");
    }

    /**
     * Return the class file of the given class, or null if the
     * class is in a jar.
     */
    public static File getClassFile(Class cls) {
        return getClassFile(cls, ".class");
    }

    /**
     * Return the file for the class resource with the given extension.
     */
    private static File getClassFile(Class cls, String ext) {
        String name = Strings.getClassName(cls);

        // if it's an inner class, use the parent class name
        int innerIdx = name.indexOf('$');
        if (innerIdx != -1)
            name = name.substring(0, innerIdx);

        URL rsrc = cls.getResource(name + ext);
        if (rsrc != null && rsrc.getProtocol().equals("file"))
            return new File(URLDecoder.decode(rsrc.getFile()));
        return null;
    }

    /**
     * Return the file for the given package. If the given base directory
     * matches the given package structure, it will be used as-is. If not,
     * the package structure will be added beneath the base directory. If
     * the base directory is null, the current working directory will be
     * used as the base.
     */
    public static File getPackageFile(File base, String pkg, boolean mkdirs) {
        if (base == null)
            base = new File(System.getProperty("user.dir"));
        if (pkg == null || pkg.length() == 0) {
            if (mkdirs && !base.exists())
                base.mkdirs();
            return base;
        }

        pkg = pkg.replace('.', File.separatorChar);
        File file = null;
        try {
            if (base.getCanonicalPath().endsWith(pkg))
                file = base;
            else
                file = new File(base, pkg);
        } catch (IOException ioe) {
            throw new NestableRuntimeException(ioe);
        }

        if (mkdirs && !file.exists())
            file.mkdirs();
        return file;
    }

    /**
     * Check the given string for a matching file. The string is first
     * tested to see if it is an existing file path. If it does not
     * represent an existing file, it is checked as a resource name of a
     * file. If no resource exists, then it is interpreted as a path
     * to a file that does not exist yet.
     *
     * @param name the file path or resource name
     * @param loader a class loader to use in resource lookup, or null
     * to use the thread's context loader
     */
    public static File getFile(String name, ClassLoader loader) {
        if (name == null)
            return null;

        File file = new File(name);
        if (file.exists())
            return file;

        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(name);
        if (url != null) {
            String urlFile = url.getFile();
            if (urlFile != null) {
                File rsrc = new File(URLDecoder.decode(urlFile));
                if (rsrc.exists())
                    return rsrc;
            }
        }

        // go back to original non-existant file path
        return file;
    }

    /**
     * Return a writer to the stream(stdout or stderr) or file named by the
     * given string.
     *
     * @see #getFile
     */
    public static Writer getWriter(String file, ClassLoader loader)
        throws IOException {
        if (file == null)
            return null;
        if ("stdout".equals(file))
            return new PrintWriter(System.out);
        if ("stderr".equals(file))
            return new PrintWriter(System.err);
        try {
            return new FileWriter(getFile(file, loader));
        } catch (IOException ioe) {
            throw new NestableRuntimeException(ioe);
        }
    }

    /**
     * Return an output stream to the stream(stdout or stderr) or file named
     * by the given string.
     *
     * @see #getFile
     */
    public static OutputStream getOutputStream(String file,
        ClassLoader loader) {
        if (file == null)
            return null;
        if ("stdout".equals(file))
            return System.out;
        if ("stderr".equals(file))
            return System.err;
        try {
            return new FileOutputStream(getFile(file, loader));
        } catch (IOException ioe) {
            throw new NestableRuntimeException(ioe);
        }
    }

    /**
     * Copy a file. Return false if <code>from</code> does not exist.
     */
    public static boolean copy(File from, File to) throws IOException {
        if (from == null || to == null || !from.exists())
            return false;

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            BufferedInputStream inbuf = new BufferedInputStream(in);
            out = new FileOutputStream(to);
            BufferedOutputStream outbuf = new BufferedOutputStream(out);
            for (int b; (b = inbuf.read()) != -1; outbuf.write(b)) ;
            outbuf.flush();
            return true;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception e) {
                }
            if (out != null)
                try {
                    out.close();
                } catch (Exception e) {
                }
        }
    }
}
