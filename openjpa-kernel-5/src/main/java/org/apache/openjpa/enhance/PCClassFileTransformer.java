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
package org.apache.openjpa.enhance;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.GeneralException;
import serp.bytecode.Project;

/**
 * <p>Transformer that makes persistent classes implement the
 * {@link PersistenceCapable} interface at runtime.</p>
 *
 * @author Abe White
 * @nojavadoc
 */
public class PCClassFileTransformer
    implements ClassFileTransformer {

    private static final Localizer _loc = Localizer.forPackage
        (PCClassFileTransformer.class);

    private final MetaDataRepository _repos;
    private final PCEnhancer.Flags _flags;
    private final ClassLoader _loader;
    private final Log _log;
    private final Set _names;

    /**
     * Constructor.
     *
     * @param    repos    metadata repository to use internally
     * @param    opts    enhancer configuration options
     * @param    loader    temporary class loader for loading intermediate classes
     */
    public PCClassFileTransformer(MetaDataRepository repos, Options opts,
        ClassLoader loader) {
        this(repos, toFlags(opts), loader, opts.removeBooleanProperty
            ("scanDevPath", "ScanDevPath", false));
    }

    /**
     * Create enhancer flags from the given options.
     */
    private static PCEnhancer.Flags toFlags(Options opts) {
        PCEnhancer.Flags flags = new PCEnhancer.Flags();
        flags.addDefaultConstructor = opts.removeBooleanProperty
            ("addDefaultConstructor", "AddDefaultConstructor",
                flags.addDefaultConstructor);
        flags.enforcePropertyRestrictions = opts.removeBooleanProperty
            ("enforcePropertyRestrictions", "EnforcePropertyRestrictions",
                flags.enforcePropertyRestrictions);
        return flags;
    }

    /**
     * Constructor.
     *
     * @param    repos    metadata repository to use internally
     * @param    flags    enhancer configuration
     * @param    loader    temporary class loader for loading intermediate classes
     * @param    devscan    whether to scan the dev classpath for persistent types
     * if none are configured
     */
    public PCClassFileTransformer(MetaDataRepository repos,
        PCEnhancer.Flags flags, ClassLoader loader, boolean devscan) {
        _repos = repos;
        _log =
            repos.getConfiguration().getLog(OpenJPAConfiguration.LOG_ENHANCE);
        _flags = flags;
        _loader = loader;

        _names = repos.getPersistentTypeNames(devscan, loader);
        if (_names == null && _log.isInfoEnabled())
            _log.info(_loc.get("runtime-enhance-pcclasses"));
    }

    public byte[] transform(ClassLoader loader, String className,
        Class redef, ProtectionDomain domain, byte[] bytes)
        throws IllegalClassFormatException {
        if (loader == _loader)
            return null;

        try {
            Boolean enhance = needsEnhance(className, redef, bytes);
            if (enhance != null && _log.isTraceEnabled())
                _log.trace(_loc.get("needs-runtime-enhance", className,
                    enhance));
            if (enhance != Boolean.TRUE)
                return null;

            PCEnhancer enhancer = new PCEnhancer(_repos.getConfiguration(),
                new Project().loadClass(new ByteArrayInputStream(bytes),
                    _loader), _repos);
            enhancer.setAddDefaultConstructor(_flags.addDefaultConstructor);
            enhancer.setEnforcePropertyRestrictions
                (_flags.enforcePropertyRestrictions);

            if (enhancer.run() == PCEnhancer.ENHANCE_NONE)
                return null;
            return enhancer.getBytecode().toByteArray();
        }
        catch (Throwable t) {
            _log.warn(_loc.get("cft-exception-thrown", className), t);
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof IllegalClassFormatException)
                throw (IllegalClassFormatException) t;
            throw new GeneralException(t);
        }
    }

    /**
     * Return whether the given class needs enhancement.
     */
    private Boolean needsEnhance(String clsName, Class redef, byte[] bytes) {
        if (redef != null) {
            Class[] intfs = redef.getInterfaces();
            for (int i = 0; i < intfs.length; i++)
                if (PersistenceCapable.class.getName().
                    equals(intfs[i].getName()))
                    return Boolean.valueOf(!isEnhanced(bytes));
            return null;
        }

        if (_names != null) {
            if (_names.contains(clsName.replace('/', '.')))
                return Boolean.valueOf(!isEnhanced(bytes));
            return null;
        }

        if (clsName.startsWith("java/") || clsName.startsWith("javax/"))
            return null;
        if (isEnhanced(bytes))
            return Boolean.FALSE;

        try {
            Class c = Class.forName(clsName.replace('/', '.'), false, _loader);
            if (_repos.getMetaData(c, null, false) != null)
                return Boolean.TRUE;
            return null;
        }
        catch (RuntimeException re) {
            throw re;
        }
        catch (Throwable t) {
            throw new GeneralException(t);
        }
    }

    /**
     * Analyze the bytecode to see if the given class definition implements
     * {@link PersistenceCapable}.
     */
    private static boolean isEnhanced(byte[] b) {
        // each entry is the index in the byte array of the data for a const
        // pool entry
        int[] entries = new int[readUnsignedShort(b, 8)];
        int idx = 10;
        for (int i = 1; i < entries.length; i++) {
            entries[i] = idx + 1; // skip entry type
            switch (b[idx]) {
                case 1:        // utf8
                    idx += 3 + readUnsignedShort(b, idx + 1);
                    break;
                case 3:        // integer
                case 4:        // float
                case 9:        // field
                case 10:    // method
                case 11:    // interface method
                case 12:    // name
                    idx += 5;
                    break;
                case 5:        // long
                case 6:        // double
                    idx += 9;
                    i++;    // wide entry
                    break;
                default:
                    idx += 3;
            }
        }

        idx += 6;
        int ifaces = readUnsignedShort(b, idx);
        int clsEntry, utfEntry, len;
        String name;
        for (int i = 0; i < ifaces; i++) {
            idx += 2;
            clsEntry = readUnsignedShort(b, idx);
            utfEntry = readUnsignedShort(b, entries[clsEntry]);
            len = readUnsignedShort(b, entries[utfEntry]);
            try {
                name = new String(b, entries[utfEntry] + 2, len, "UTF-8");
                if ("openjpa/enhance/PersistenceCapable".equals(name))
                    return true;
            }
            catch (UnsupportedEncodingException uee) {
                throw new ClassFormatError(uee.toString());
            }
        }
        return false;
    }

    /**
     *	Read an unsigned short from the given array at the given offset.
     */
    private static int readUnsignedShort(byte[] b, int idx) {
        return ((b[idx] & 0xFF) << 8) | (b[idx + 1] & 0xFF);
	}
}
