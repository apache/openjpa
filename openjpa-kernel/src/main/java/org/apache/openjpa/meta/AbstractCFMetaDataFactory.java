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
package org.apache.openjpa.meta;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.meta.ClasspathMetaDataIterator;
import org.apache.openjpa.lib.meta.FileMetaDataIterator;
import org.apache.openjpa.lib.meta.MetaDataFilter;
import org.apache.openjpa.lib.meta.MetaDataIterator;
import org.apache.openjpa.lib.meta.MetaDataParser;
import org.apache.openjpa.lib.meta.MetaDataSerializer;
import org.apache.openjpa.lib.meta.ResourceMetaDataIterator;
import org.apache.openjpa.lib.meta.URLMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipFileMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipStreamMetaDataIterator;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.UserException;
import serp.util.Strings;

/**
 * Base class for factory implementations built around XML metadata files
 * in the common fomat.
 *
 * @author Abe White
 * @since 0.4.0
 */
public abstract class AbstractCFMetaDataFactory
    extends AbstractMetaDataFactory {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractMetaDataFactory.class);

    protected Collection files = null;
    protected Collection urls = null;
    protected Collection rsrcs = null;
    protected Collection cpath = null;

    private Set _typeNames = null;

    /**
     * Set of {@link File}s of metadata files or directories supplied by user.
     */
    public void setFiles(Collection files) {
        this.files = files;
    }

    /**
     * Set of semicolon-separated {@link File}s of metadata files or
     * directories supplied by user via auto-configuration.
     */
    public void setFiles(String files) {
        if (StringUtils.isEmpty(files))
            this.files = null;
        else {
            String[] strs = Strings.split(files, ";", 0);
            this.files = new HashSet((int) (strs.length * 1.33 + 1));

            File file;
            for (int i = 0; i < strs.length; i++) {
                file = new File(strs[i]);
                if (file.exists())
                    this.files.add(file);
            }
        }
    }

    /**
     * Set of {@link URL}s of metadata files or jars supplied by user.
     */
    public void setURLs(Collection urls) {
        this.urls = urls;
    }

    /**
     * Set of semicolon-separated {@link URL}s of metadata files or jars
     * supplied by user via auto-configuration.
     */
    public void setURLs(String urls) {
        if (StringUtils.isEmpty(urls))
            this.urls = null;
        else {
            String[] strs = Strings.split(urls, ";", 0);
            this.urls = new HashSet((int) (strs.length * 1.33 + 1));
            try {
                for (int i = 0; i < strs.length; i++)
                    this.urls.add(new URL(strs[i]));
            } catch (MalformedURLException mue) {
                throw new UserException(mue);
            }
        }
    }

    /**
     * Set of resource paths of metadata files or jars supplied by user.
     */
    public void setResources(Collection rsrcs) {
        this.rsrcs = rsrcs;
    }

    /**
     * Set of semicolon-separated resource paths of metadata files or jars
     * supplied by user via auto-configuration.
     */
    public void setResources(String rsrcs) {
        // keep list mutable so subclasses can add implicit locations
        this.rsrcs = (StringUtils.isEmpty(rsrcs)) ? null
            : new ArrayList(Arrays.asList(Strings.split(rsrcs, ";", 0)));
    }

    /**
     * Set of classpath directories or jars to scan for metadata supplied
     * by user.
     */
    public void setClasspathScan(Collection cpath) {
        this.cpath = cpath;
    }

    /**
     * Set of classpath directories or jars to scan for metadata supplied
     * by user via auto-configuration.
     */
    public void setClasspathScan(String cpath) {
        // keep list mutable so subclasses can add implicit locations
        this.cpath = (StringUtils.isEmpty(cpath)) ? null
            : new ArrayList(Arrays.asList(Strings.split(cpath, ";", 0)));
    }

    public boolean store(ClassMetaData[] metas, QueryMetaData[] queries,
        SequenceMetaData[] seqs, int mode, Map output) {
        if (mode == MODE_NONE)
            return true;
        if (isMappingOnlyFactory() && (mode & MODE_MAPPING) == 0)
            return true;

        if (!strict && (mode & MODE_META) != 0)
            mode |= MODE_MAPPING;
        Class cls = (metas.length == 0) ? null : metas[0].getDescribedType();
        ClassLoader loader = repos.getConfiguration().
            getClassResolverInstance().getClassLoader(cls, null);
        Map clsNames = new HashMap((int) (metas.length * 1.33 + 1));
        for (int i = 0; i < metas.length; i++)
            clsNames.put(metas[i].getDescribedType().getName(), metas[i]);

        // assign default files if in metadata mode (in other modes we assume
        // the files would have to be read already to create the metadatas)
        Set metaFiles = null;
        Set queryFiles = null;
        if (isMappingOnlyFactory() || (mode & MODE_META) != 0)
            metaFiles = assignDefaultMetaDataFiles(metas, queries, seqs, mode,
                clsNames);
        if (!isMappingOnlyFactory() && (mode & MODE_QUERY) != 0)
            queryFiles = assignDefaultQueryFiles(queries, clsNames);

        // parse all files to be sure we don't delete existing metadata when
        // writing out new metadata, then serialize
        Serializer ser;
        Parser parser;
        if (mode != MODE_QUERY) {
            int sermode = (isMappingOnlyFactory()) ? mode : mode | MODE_META;
            ser = newSerializer();
            ser.setMode(sermode);
            if (metaFiles != null) {
                parser = newParser(false);
                parser.setMode(sermode);
                parser.setClassLoader(loader);
                parse(parser, metaFiles);

                MetaDataRepository pr = parser.getRepository();
                pr.setSourceMode(mode);
                if (isMappingOnlyFactory())
                    pr.setResolve(MODE_NONE);
                else
                    pr.setResolve(MODE_MAPPING, false);
                ser.addAll(pr);
            }

            for (int i = 0; i < metas.length; i++)
                ser.addMetaData(metas[i]);
            if ((mode & MODE_MAPPING) != 0)
                for (int i = 0; i < seqs.length; i++)
                    ser.addSequenceMetaData(seqs[i]);
            for (int i = 0; i < queries.length; i++)
                if (queries[i].getSourceMode() != MODE_QUERY
                    && (queries[i].getSourceMode() & mode) != 0)
                    ser.addQueryMetaData(queries[i]);

            int flags = ser.PRETTY;
            if ((store & STORE_VERBOSE) != 0)
                flags |= ser.VERBOSE;
            serialize(ser, output, flags);
        }

        // do we have any queries stored in query files?
        if (!isMappingOnlyFactory()) {
            boolean qFiles = queryFiles != null;
            for (int i = 0; !qFiles && i < queries.length; i++)
                qFiles = queries[i].getSourceMode() == MODE_QUERY;
            if (qFiles) {
                ser = newSerializer();
                ser.setMode(MODE_QUERY);
                if (queryFiles != null) {
                    parser = newParser(false);
                    parser.setMode(MODE_QUERY);
                    parser.setClassLoader(loader);
                    parse(parser, queryFiles);
                    ser.addAll(parser.getRepository());
                }
                for (int i = 0; i < queries.length; i++)
                    if (queries[i].getSourceMode() == MODE_QUERY)
                        ser.addQueryMetaData(queries[i]);
                serialize(ser, output, ser.PRETTY);
            }
        }
        return true;
    }

    public boolean drop(Class[] cls, int mode, ClassLoader envLoader) {
        if (mode == MODE_NONE)
            return true;
        if (isMappingOnlyFactory() && (mode & MODE_MAPPING) == 0)
            return true;

        Parser parser = newParser(false);
        MetaDataRepository pr = parser.getRepository();
        pr.setSourceMode(MODE_MAPPING, false);
        pr.setResolve(MODE_MAPPING, false);

        // parse metadata for all these classes
        if ((mode & (MODE_META | MODE_MAPPING)) != 0) {
            parser.setMode((isMappingOnlyFactory()) ? mode
                : MODE_META | MODE_MAPPING | MODE_QUERY);
            parse(parser, cls);
        }
        if (!isMappingOnlyFactory() && (mode & MODE_QUERY) != 0) {
            parser.setMode(MODE_QUERY);
            parse(parser, cls);
        }

        // remove metadatas from repository or clear their mappings
        Set files = new HashSet();
        Set clsNames = null;
        if ((mode & (MODE_META | MODE_MAPPING)) != 0) {
            clsNames = new HashSet((int) (cls.length * 1.33 + 1));
            ClassMetaData meta;
            for (int i = 0; i < cls.length; i++) {
                if (cls[i] == null)
                    clsNames.add(null);
                else
                    clsNames.add(cls[i].getName());
                meta = pr.getMetaData(cls[i], envLoader, false);
                if (meta != null) {
                    if (getSourceFile(meta) != null)
                        files.add(getSourceFile(meta));
                    if ((mode & MODE_META) != 0)
                        pr.removeMetaData(meta);
                    else if (!isMappingOnlyFactory())
                        clearMapping(meta);
                }
            }
        }

        // remove query mode metadatas so we can store them separately
        QueryMetaData[] queries = pr.getQueryMetaDatas();
        List qqs = (!isMappingOnlyFactory() && (mode & MODE_QUERY) == 0)
            ? null : new ArrayList();
        boolean rem;
        Class def;
        for (int i = 0; i < queries.length; i++) {
            if (!isMappingOnlyFactory() && queries[i].getSourceFile() != null)
                files.add(queries[i].getSourceFile());
            def = queries[i].getDefiningType();
            rem = (queries[i].getSourceMode() & mode) != 0
                && clsNames.contains((def == null) ? null : def.getName());
            if (rem || (!isMappingOnlyFactory()
                && queries[i].getSourceMode() == MODE_QUERY))
                pr.removeQueryMetaData(queries[i]);
            if (qqs != null && queries[i].getSourceMode() == MODE_QUERY && !rem)
                qqs.add(queries[i]);
        }

        // write new metadata without removed instances
        backupAndDelete(files);
        Serializer ser;
        if ((mode & (MODE_META | MODE_MAPPING)) != 0) {
            ser = newSerializer();
            ser.setMode((isMappingOnlyFactory()) ? mode : mode | MODE_META);
            ser.addAll(pr);
            // remove from serializer rather than from repository above so that
            // calling code can take advantage of metadata still in repos
            if (isMappingOnlyFactory())
                for (int i = 0; i < cls.length; i++)
                    ser.removeMetaData(pr.getMetaData(cls[i], envLoader,
                        false));
            serialize(ser, null, ser.PRETTY);
        }
        if (qqs != null && !qqs.isEmpty()) {
            ser = newSerializer();
            ser.setMode(MODE_QUERY);
            for (int i = 0; i < qqs.size(); i++)
                ser.addQueryMetaData((QueryMetaData) qqs.get(i));
            serialize(ser, null, ser.PRETTY);
        }
        return true;
    }

    /**
     * Assign default source files to the given metadatas.
     *
     * @param clsNames map of class names to metadatas
     * @return set of existing files used by these metadatas, or
     * null if no existing files
     */
    private Set assignDefaultMetaDataFiles(ClassMetaData[] metas,
        QueryMetaData[] queries, SequenceMetaData[] seqs, int mode,
        Map clsNames) {
        Set files = null;
        for (int i = 0; i < metas.length; i++) {
            if (getSourceFile(metas[i]) == null)
                setSourceFile(metas[i], defaultSourceFile(metas[i]));
            if (getSourceFile(metas[i]).exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(getSourceFile(metas[i]));
            }
        }
        for (int i = 0; i < queries.length; i++) {
            if (queries[i].getSourceMode() == MODE_QUERY
                || (mode & queries[i].getSourceMode()) == 0)
                continue;
            if (queries[i].getSourceFile() == null)
                queries[i].setSource(defaultSourceFile(queries[i],
                    clsNames), queries[i].getSourceScope(),
                    queries[i].getSourceType());
            if (queries[i].getSourceFile().exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(queries[i].getSourceFile());
            }
        }
        if ((mode & MODE_MAPPING) != 0) {
            for (int i = 0; i < seqs.length; i++) {
                if (getSourceFile(seqs[i]) == null)
                    setSourceFile(seqs[i], defaultSourceFile(seqs[i],
                        clsNames));
                if (getSourceFile(seqs[i]).exists()) {
                    if (files == null)
                        files = new HashSet();
                    files.add(getSourceFile(seqs[i]));
                }
            }
        }
        return files;
    }

    /**
     * Assign default source files to the given queries.
     *
     * @param clsNames map of class names to metadatas
     * @return set of existing files used by these metadatas, or
     * null if no existing files
     */
    private Set assignDefaultQueryFiles(QueryMetaData[] queries,
        Map clsNames) {
        Set files = null;
        for (int i = 0; i < queries.length; i++) {
            if (queries[i].getSourceMode() != MODE_QUERY)
                continue;
            if (queries[i].getSourceFile() == null)
                queries[i].setSource(defaultSourceFile(queries[i], clsNames),
                    queries[i].getSourceScope(), queries[i].getSourceType());
            if (queries[i].getSourceFile().exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(queries[i].getSourceFile());
            }
        }
        return files;
    }

    /**
     * Return true if this factory deals only with mapping data, and relies
     * on a separate factory for metadata.
     */
    protected boolean isMappingOnlyFactory() {
        return false;
    }

    /**
     * Parse all given files.
     */
    protected void parse(MetaDataParser parser, Collection files) {
        try {
            for (Iterator itr = files.iterator(); itr.hasNext();)
                parser.parse((File) itr.next());
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Parse all given classses.
     */
    protected void parse(MetaDataParser parser, Class[] cls) {
        try {
            for (int i = 0; i < cls.length; i++)
                parser.parse(cls[i], isParseTopDown());
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Whether to parse classes top down. Defaults to false.
     */
    protected boolean isParseTopDown() {
        return false;
    }

    /**
     * Tell the given serialier to write its metadatas.
     */
    protected void serialize(MetaDataSerializer ser, Map output, int flags) {
        try {
            if (output == null)
                ser.serialize(flags);
            else
                ser.serialize(output, flags);
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Backup and delete the source files for the given metadatas.
     */
    protected void backupAndDelete(Collection files) {
        File file;
        for (Iterator itr = files.iterator(); itr.hasNext();) {
            file = (File) itr.next();
            if (Files.backup(file, false) != null)
                file.delete();
        }
    }

    /**
     * Clear mapping information from the given metadata.
     */
    protected void clearMapping(ClassMetaData meta) {
        meta.setSourceMode(MODE_MAPPING, false);
    }

    /**
     * Return the current source file of the given metadata.
     */
    protected File getSourceFile(ClassMetaData meta) {
        return meta.getSourceFile();
    }

    /**
     * Set the current source file of the given metadata.
     */
    protected void setSourceFile(ClassMetaData meta, File sourceFile) {
        meta.setSource(sourceFile, meta.getSourceType());
    }

    /**
     * Return the current source file of the given metadata.
     */
    protected File getSourceFile(SequenceMetaData meta) {
        return meta.getSourceFile();
    }

    /**
     * Set the current source file of the given metadata.
     */
    protected void setSourceFile(SequenceMetaData meta, File sourceFile) {
        meta.setSource(sourceFile, meta.getSourceScope(),
            meta.getSourceType());
    }

    /**
     * Return the default file for the given metadata.
     */
    protected abstract File defaultSourceFile(ClassMetaData meta);

    /**
     * Return a default file for the given query.
     */
    protected abstract File defaultSourceFile(QueryMetaData query,
        Map clsNames);

    /**
     * Return a default file for the given sequence.
     */
    protected abstract File defaultSourceFile(SequenceMetaData seq,
        Map clsNames);

    /**
     * Create a new metadata parser.
     *
     * @param loading if true, this will be the cached parser used for
     * loading metadata
     */
    protected abstract Parser newParser(boolean loading);

    /**
     * Create a new metadata serializer.
     */
    protected abstract Serializer newSerializer();

    /**
     * Return the metadata that defines the given query, if any.
     *
     * @param clsNames map of class names to metadatas
     */
    protected ClassMetaData getDefiningMetaData(QueryMetaData query,
        Map clsNames) {
        Class def = query.getDefiningType();
        if (def != null)
            return (ClassMetaData) clsNames.get(def.getName());

        Map.Entry entry;
        String pkg;
        for (Iterator itr = clsNames.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            pkg = Strings.getPackageName((String) entry.getKey());
            if (pkg.length() == 0)
                return (ClassMetaData) entry.getValue();
        }
        return null;
    }

    public Set getPersistentTypeNames(boolean devpath, ClassLoader envLoader) {
        // some configured locations might be implicit in spec, so return
        // null if we don't find any classes, rather than if we don't have
        // any locations
        if (_typeNames != null)
            return (_typeNames.isEmpty()) ? null : _typeNames;

        try {
            ClassLoader loader = repos.getConfiguration().
                getClassResolverInstance().getClassLoader(getClass(),
                envLoader);
            long start = System.currentTimeMillis();

            Set names = parsePersistentTypeNames(loader);
            if (names.isEmpty() && devpath)
                scan(new ClasspathMetaDataIterator(null, newMetaDataFilter()),
                    newClassArgParser(), names, false, null);
            else // we don't cache a full dev cp scan
                _typeNames = names;

            if (log.isTraceEnabled())
                log.trace(_loc.get("found-pcs", String.valueOf(names.size()),
                    String.valueOf(System.currentTimeMillis() - start)));
            return (names.isEmpty()) ? null : names;
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Parse persistent type names.
     */
    private Set parsePersistentTypeNames(ClassLoader loader)
        throws IOException {
        ClassArgParser cparser = newClassArgParser();
        String[] clss;
        Set names = new HashSet();
        if (files != null) {
            File file;
            for (Iterator itr = files.iterator(); itr.hasNext();) {
                file = (File) itr.next();
                if (file.isDirectory()) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-directory", file));
                    scan(new FileMetaDataIterator(file, newMetaDataFilter()),
                        cparser, names, true, file);
                } else if (file.getName().endsWith(".jar")) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-jar", file));
                    scan(new ZipFileMetaDataIterator(new ZipFile(file),
                        newMetaDataFilter()), cparser, names, true, file);
                } else {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-file", file));
                    clss = cparser.parseTypeNames(new FileMetaDataIterator
                        (file));
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scan-found-names", clss, file));
                    names.addAll(Arrays.asList(clss));
                    mapPersistentTypeNames(file.getAbsoluteFile().toURL(),
                        clss);
                }
            }
        }
        URL url;
        if (urls != null) {
            for (Iterator itr = urls.iterator(); itr.hasNext();) {
                url = (URL) itr.next();
                if ("file".equals(url.getProtocol())) {
                    File file = new File(url.getFile()).getAbsoluteFile();
                    if (files != null && files.contains(file)) {
                        continue;
                    } else if (file.isDirectory()) {
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-directory", file));
                        scan(new FileMetaDataIterator(file, newMetaDataFilter()),
                                cparser, names, true, file);
                        continue;
                    }
                }
                if ("jar".equals(url.getProtocol())
                    && url.getPath().endsWith("!/")) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-jar-url", url));
                    scan(new ZipFileMetaDataIterator(url,
                        newMetaDataFilter()), cparser, names, true, url);
                } else if (url.getPath().endsWith(".jar")) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-jar-at-url", url));
                    scan(new ZipStreamMetaDataIterator(
                        new ZipInputStream(url.openStream()),
                        newMetaDataFilter()), cparser, names, true, url);
                } else {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-url", url));
                    clss = cparser.parseTypeNames(new URLMetaDataIterator(url));
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scan-found-names", clss, url));
                    names.addAll(Arrays.asList(clss));
                    mapPersistentTypeNames(url, clss);
                }
            }
        }
        if (rsrcs != null) {
            String rsrc;
            MetaDataIterator mitr;
            for (Iterator itr = rsrcs.iterator(); itr.hasNext();) {
                rsrc = (String) itr.next();
                if (rsrc.endsWith(".jar")) {
                    url = loader.getResource(rsrc);
                    if (url != null) {
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-jar-stream-url", url));
                        scan(new ZipStreamMetaDataIterator
                            (new ZipInputStream(url.openStream()),
                                newMetaDataFilter()), cparser, names, true,
                                url);
                    }
                } else {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-resource", rsrc));
                    mitr = new ResourceMetaDataIterator(rsrc, loader);
                    while (mitr.hasNext()) {
                        url = (URL) mitr.next();
                        clss = cparser.parseTypeNames(new URLMetaDataIterator
                            (url));
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scan-found-names", clss, rsrc));
                        names.addAll(Arrays.asList(clss));
                        mapPersistentTypeNames(url, clss);
                    }
                    mitr.close();
                }
            }
        }
        if (cpath != null) {
            String[] dirs = (String[]) cpath.toArray(new String[cpath.size()]);
            scan(new ClasspathMetaDataIterator(dirs, newMetaDataFilter()),
                cparser, names, true, dirs);
        }
        if (types != null)
            names.addAll(types);

        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-found-names", names));
        
        return names;
    }

    /**
     * Scan for persistent type names using the given metadata iterator.
     */
    private void scan(MetaDataIterator mitr, ClassArgParser cparser, Set names,
        boolean mapNames, Object debugContext)
        throws IOException {
        Map map;
        try {
            map = cparser.mapTypeNames(mitr);
        } finally {
            mitr.close();
        }

        Map.Entry entry;
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            if (mapNames)
                mapPersistentTypeNames(entry.getKey(), (String[])
                    entry.getValue());
            List newNames = Arrays.asList((String[]) entry.getValue());
            if (log.isTraceEnabled())
                log.trace(_loc.get("scan-found-names", newNames, debugContext));
            names.addAll(newNames);
        }
    }

    /**
     * Implement this method to map metadata resources to the persistent
     * types contained within them. The method will be called when
     * {@link #getPersistentTypeNames} is invoked.
     */
    protected void mapPersistentTypeNames(Object rsrc, String[] names) {
    }

    /**
     * Return a metadata filter that identifies metadata resources when
     * performing jar and classpath scans.
     */
    protected abstract MetaDataFilter newMetaDataFilter();

    public void clear() {
        super.clear();
        _typeNames = null;
    }

    /**
     * Internal parser interface.
     */
    public static interface Parser
        extends MetaDataParser {

        /**
         * Returns the repository for this parser. If none has been set,
         * creates a new repository and sets it.
         */
        public MetaDataRepository getRepository();

        /**
         * The parse mode according to the expected document type.
         */
        public void setMode(int mode);
    }

    /**
     * Internal serializer interface.
     */
    public static interface Serializer
        extends MetaDataSerializer {

        /**
         * The serialization mode according to the expected document type. The
         * mode constants act as bit flags, and therefore can be combined.
         */
        public void setMode(int mode);

        /**
         * Add a class meta data to the set to be serialized.
         */
        public void addMetaData(ClassMetaData meta);

        /**
         * Remove a class meta data from the set to be serialized.
         */
        public boolean removeMetaData(ClassMetaData meta);

        /**
         * Add a sequence meta data to the set to be serialized.
         */
        public void addSequenceMetaData(SequenceMetaData meta);

        /**
         * Add a query meta data to the set to be serialized.
         */
        public void addQueryMetaData(QueryMetaData meta);

        /**
         * Add all components in the given repository to the set to be
         * serialized.
         */
        public void addAll (MetaDataRepository repos);
    }
}
