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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.meta.ClasspathMetaDataIterator;
import org.apache.openjpa.lib.meta.FileMetaDataIterator;
import org.apache.openjpa.lib.meta.JarFileURLMetaDataIterator;
import org.apache.openjpa.lib.meta.MetaDataFilter;
import org.apache.openjpa.lib.meta.MetaDataIterator;
import org.apache.openjpa.lib.meta.MetaDataParser;
import org.apache.openjpa.lib.meta.MetaDataSerializer;
import org.apache.openjpa.lib.meta.ResourceMetaDataIterator;
import org.apache.openjpa.lib.meta.URLMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipFileMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipStreamMetaDataIterator;
import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.UserException;

/**
 * Base class for factory implementations built around XML metadata files
 * in the common format.
 *
 * @author Abe White
 * @since 0.4.0
 */
public abstract class AbstractCFMetaDataFactory
    extends AbstractMetaDataFactory {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractMetaDataFactory.class);

    protected Collection<File> files = null;
    protected Collection<URL> urls = null;
    protected Collection<String> rsrcs = null;
    protected Collection<String> cpath = null;

    private Set<String> _typeNames = null;
    public static final String PERSISTENCE_UNIT_ROOT_URL = "PersistenceUnitRootUrl";
    public static final String MAPPING_FILE_NAMES = "MappingFileNames";
    public static final String JAR_FILE_URLS = "JarFiles";

    /**
     * Set of {@link File}s of metadata files or directories supplied by user.
     */
    public void setFiles(Collection<File> files) {
        this.files = files;
    }

    /**
     * Set of semicolon-separated {@link File}s of metadata files or
     * directories supplied by user via auto-configuration.
     */
    public void setFiles(String files) {
        if (StringUtil.isEmpty(files))
            this.files = null;
        else {
            String[] strs = StringUtil.split(files, ";", 0);
            this.files = new HashSet<>((int) (strs.length * 1.33 + 1));

            File file;
            for (String str : strs) {
                file = new File(str);
                if (file.exists())
                    this.files.add(file);
            }
        }
    }

    /**
     * Set of {@link URL}s of metadata files or jars supplied by user.
     */
    public void setURLs(Collection<URL> urls) {
        this.urls = urls;
    }

    /**
     * Set of semicolon-separated {@link URL}s of metadata files or jars
     * supplied by user via auto-configuration.
     */
    public void setURLs(String urls) {
        if (StringUtil.isEmpty(urls))
            this.urls = null;
        else {
            String[] strs = StringUtil.split(urls, ";", 0);
            this.urls = new HashSet<>((int) (strs.length * 1.33 + 1));
            try {
                for (String str : strs) {
                    this.urls.add(new URL(str));
                }
            } catch (MalformedURLException mue) {
                throw new UserException(mue);
            }
        }
    }

    /**
     * Set of resource paths of metadata files or jars supplied by user.
     */
    public void setResources(Collection<String> rsrcs) {
        this.rsrcs = rsrcs;
    }

    /**
     * Set of semicolon-separated resource paths of metadata files or jars
     * supplied by user via auto-configuration.
     */
    public void setResources(String rsrcs) {
        // keep list mutable so subclasses can add implicit locations
        this.rsrcs = (StringUtil.isEmpty(rsrcs)) ? null
          : new ArrayList<>(Arrays.asList(StringUtil.split(rsrcs, ";", 0)));
    }

    /**
     * Set of classpath directories or jars to scan for metadata supplied
     * by user.
     */
    public void setClasspathScan(Collection<String> cpath) {
        this.cpath = cpath;
    }

    /**
     * Set of classpath directories or jars to scan for metadata supplied
     * by user via auto-configuration.
     */
    public void setClasspathScan(String cpath) {
        // keep list mutable so subclasses can add implicit locations
        this.cpath = (StringUtil.isEmpty(cpath)) ? null
          : new ArrayList<>(Arrays.asList(StringUtil.split(cpath, ";", 0)));
    }

    @Override
    public boolean store(ClassMetaData[] metas, QueryMetaData[] queries,
        SequenceMetaData[] seqs, int mode, Map<File,String> output) {
        if (mode == MODE_NONE)
            return true;
        if (isMappingOnlyFactory() && (mode & MODE_MAPPING) == 0)
            return true;

        if (!strict && (mode & MODE_META) != 0)
            mode |= MODE_MAPPING;
        Class<?> cls = (metas.length == 0) ? null : metas[0].getDescribedType();
        ClassLoader loader = repos.getConfiguration().
            getClassResolverInstance().getClassLoader(cls, null);
        Map<String,ClassMetaData> clsNames = new HashMap<>
        	((int) (metas.length * 1.33 + 1));
        for (ClassMetaData classMetaData : metas) {
            clsNames.put(classMetaData.getDescribedType().getName(), classMetaData);
        }
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
            if ((mode & MODE_ANN_MAPPING) != 0)
                ser = newAnnotationSerializer();
            else
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

            for (ClassMetaData meta : metas) ser.addMetaData(meta);
            if ((mode & MODE_MAPPING) != 0)
                for (SequenceMetaData seq : seqs) {
                    ser.addSequenceMetaData(seq);
                }
            for (QueryMetaData query : queries)
                if (query.getSourceMode() != MODE_QUERY
                        && (query.getSourceMode() & mode) != 0)
                    ser.addQueryMetaData(query);

            int flags = MetaDataSerializer.PRETTY;
            if ((store & STORE_VERBOSE) != 0)
                flags |= MetaDataSerializer.VERBOSE;
            serialize(ser, output, flags);
        }

        // do we have any queries stored in query files?
        if (!isMappingOnlyFactory()) {
            boolean qFiles = queryFiles != null;
            for (int i = 0; !qFiles && i < queries.length; i++)
                qFiles = queries[i].getSourceMode() == MODE_QUERY;
            if (qFiles) {
                if ((mode & MODE_ANN_MAPPING) != 0)
                    ser = newAnnotationSerializer();
                else
                    ser = newSerializer();
                ser.setMode(MODE_QUERY);
                if (queryFiles != null) {
                    parser = newParser(false);
                    parser.setMode(MODE_QUERY);
                    parser.setClassLoader(loader);
                    parse(parser, queryFiles);
                    ser.addAll(parser.getRepository());
                }
                for (QueryMetaData query : queries)
                    if (query.getSourceMode() == MODE_QUERY)
                        ser.addQueryMetaData(query);
                serialize(ser, output, MetaDataSerializer.PRETTY);
            }
        }
        return true;
    }

    @Override
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
            for (Class cl : cls) {
                if (cl == null)
                    clsNames.add(null);
                else
                    clsNames.add(cl.getName());
                meta = pr.getMetaData(cl, envLoader, false);
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
        for (QueryMetaData query : queries) {
            if (!isMappingOnlyFactory() && query.getSourceFile() != null)
                files.add(query.getSourceFile());
            def = query.getDefiningType();
            rem = (query.getSourceMode() & mode) != 0
                    && clsNames.contains((def == null) ? null : def.getName());
            if (rem || (!isMappingOnlyFactory()
                    && query.getSourceMode() == MODE_QUERY))
                pr.removeQueryMetaData(query);
            if (qqs != null && query.getSourceMode() == MODE_QUERY && !rem)
                qqs.add(query);
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
                for (Class cl : cls)
                    ser.removeMetaData(pr.getMetaData(cl, envLoader,
                            false));
            serialize(ser, null, MetaDataSerializer.PRETTY);
        }
        if (qqs != null && !qqs.isEmpty()) {
            ser = newSerializer();
            ser.setMode(MODE_QUERY);
            for (Object qq : qqs) {
                ser.addQueryMetaData((QueryMetaData) qq);
            }
            serialize(ser, null, MetaDataSerializer.PRETTY);
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
        for (ClassMetaData meta : metas) {
            if (getSourceFile(meta) == null)
                setSourceFile(meta, defaultSourceFile(meta));
            if (getSourceFile(meta).exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(getSourceFile(meta));
            }
        }
        for (QueryMetaData query : queries) {
            if (query.getSourceMode() == MODE_QUERY
                    || (mode & query.getSourceMode()) == 0)
                continue;
            if (query.getSourceFile() == null) {
                File defaultFile = defaultSourceFile(query, clsNames);
                query.setSource(defaultFile, query.getSourceScope(), query.getSourceType(),
                        defaultFile == null ? "" : defaultFile.getPath());
            }
            if (query.getSourceFile().exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(query.getSourceFile());
            }
        }
        if ((mode & MODE_MAPPING) != 0) {
            for (SequenceMetaData seq : seqs) {
                if (getSourceFile(seq) == null)
                    setSourceFile(seq, defaultSourceFile(seq,
                            clsNames));
                if (getSourceFile(seq).exists()) {
                    if (files == null)
                        files = new HashSet();
                    files.add(getSourceFile(seq));
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
        for (QueryMetaData query : queries) {
            if (query.getSourceMode() != MODE_QUERY)
                continue;
            if (query.getSourceFile() == null) {
                File defaultFile = defaultSourceFile(query, clsNames);
                query.setSource(defaultFile, query.getSourceScope(), query.getSourceType(),
                        defaultFile == null ? "" : defaultFile.getPath());
            }
            if (query.getSourceFile().exists()) {
                if (files == null)
                    files = new HashSet();
                files.add(query.getSourceFile());
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
            for (Object file : files) {
                parser.parse((File) file);
            }
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Parse all given classses.
     */
    protected void parse(MetaDataParser parser, Class[] cls) {
        try {
            for (Class cl : cls) {
                parser.parse(cl, isParseTopDown());
            }
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
    protected void serialize(MetaDataSerializer ser, Map<File, String> output,
        int flags) {
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
        for (Object o : files) {
            file = (File) o;
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
        meta.setSource(sourceFile, meta.getSourceType(), sourceFile != null ?
            sourceFile.getPath() : "");
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
     * Create a new annotation metadata serializer.
     */
    protected abstract Serializer newAnnotationSerializer();

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
        for (Object o : clsNames.entrySet()) {
            entry = (Map.Entry) o;
            pkg = ClassUtil.getPackageName((String) entry.getKey());
            if (pkg.length() == 0)
                return (ClassMetaData) entry.getValue();
        }
        return null;
    }

    @Override
    public Set<String> getPersistentTypeNames(boolean devpath, ClassLoader envLoader) {
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
    protected Set<String> parsePersistentTypeNames(ClassLoader loader)
        throws IOException {
        ClassArgParser cparser = newClassArgParser();
        String[] clss;
        Set<String> names = new HashSet<>();
        if (files != null) {
            File file;
            for (File value : files) {
                file = value;
                if (file.isDirectory()) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-directory", file));
                    scan(new FileMetaDataIterator(file, newMetaDataFilter()),
                            cparser, names, true, file);
                }
                else if (file.getName().endsWith(".jar")) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-jar", file));
                    ZipFile zFile = new ZipFile(file);
                    scan(new ZipFileMetaDataIterator(zFile,
                            newMetaDataFilter()), cparser, names, true, file);
                }
                else {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-file", file));
                    clss = cparser.parseTypeNames(new FileMetaDataIterator
                            (file));
                    List<String> newNames = Arrays.asList(clss);
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scan-found-names", newNames, file));
                    names.addAll(newNames);
                    File f = file.getAbsoluteFile();
                	mapPersistentTypeNames(f.toURI().toURL(), clss);
                }
            }
        }
        URL url;
        if (urls != null) {
            for (URL value : urls) {
                url = value;
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
                if ("vfs".equals(url.getProtocol())) {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-vfs-url", url));
                    }

                    final URL finalUrl = url;
                    if (url.toString().endsWith(".jar")) {
                    	ZipInputStream zis = null;
                    	try {
                    		Class vfs = Class.forName("org.jboss.vfs.VFS");
                    		Method getChild = vfs.getDeclaredMethod("getChild", URL.class);
                    		Object jarFile = getChild.invoke(null, finalUrl);
                    		
                    		Class virtualFileClass = Class.forName("org.jboss.vfs.VirtualFile");
                    		Method openStream = virtualFileClass.getDeclaredMethod("openStream");
                    		zis = (ZipInputStream) openStream.invoke(jarFile);
                    	} catch (Exception e) {
                    		log.error(_loc.get("while-scanning-vfs-url", finalUrl), e);
                    		zis = null;
                    	}

                    	if (zis != null) {
                            scan(new ZipStreamMetaDataIterator(zis, newMetaDataFilter()), cparser, names, true, url);
                        }
                    } else {
                        final URLConnection conn = url.openConnection();
                        final Object vfsContent = conn.getContent();
                        File file = null;
                        try {
                            Class virtualFileClass = Class.forName("org.jboss.vfs.VirtualFile");
                            Method getPhysicalFile = virtualFileClass.getDeclaredMethod("getPhysicalFile");
                            file = (File) getPhysicalFile.invoke(vfsContent);
                        } catch (Exception e) {
                            log.error(_loc.get("while-scanning-vfs-url", finalUrl), e);
                            file = null;
                        }
                        
                        if (file != null) {
                            scan(new FileMetaDataIterator(file, newMetaDataFilter()), cparser, names, true, file);
                        }
                    }
                    continue;
                }
                if ("jar".equals(url.getProtocol())) {
                    if (url.getPath().endsWith("!/")) {
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-jar-url", url));
                        scan(new ZipFileMetaDataIterator(url,
                                newMetaDataFilter()), cparser, names, true, url);
                    }
                    else {
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-jar-url", url));
                        scan(new JarFileURLMetaDataIterator(url,
                                newMetaDataFilter()), cparser, names, true, url);
                    }
                }
                else if (url.getPath().endsWith(".jar")) {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-jar-at-url", url));
                    InputStream is = (InputStream) url.openStream();
                    scan(new ZipStreamMetaDataIterator(
                            new ZipInputStream(is),
                            newMetaDataFilter()), cparser, names, true, url);
                }
                else {
                    // Open an InputStream from the URL and sniff for a zip header.  If it is, then this is
                    // a URL with a jar-formated InputStream, as per the JPA specification.  Otherwise, fall back
                    // to URLMetaDataIterator.
                    BufferedInputStream is = new BufferedInputStream((InputStream) url.openStream());

                    // Check for zip header magic 0x50 0x4b 0x03 0x04
                    is.mark(0);
                    boolean zipHeaderMatch = is.read() == 0x50 && is.read() == 0x4b && is.read() == 0x03 &&
                            is.read() == 0x04;
                    is.reset();

                    if (zipHeaderMatch) {
                        // The URL provides a Jar-formatted InputStream, consume it with ZipStreamMetaDataIterator
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-jar-at-url", url));
                        scan(new ZipStreamMetaDataIterator(new ZipInputStream(is), newMetaDataFilter()),
                                cparser, names, true, url);
                    }
                    else {
                        // Fall back to URLMetaDataIterator
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-url", url));
                        clss = cparser.parseTypeNames(new URLMetaDataIterator(url));
                        List<String> newNames = Arrays.asList(clss);
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scan-found-names", newNames, url));
                        names.addAll(newNames);
                        mapPersistentTypeNames(url, clss);
                    }
                }
            }
        }
        if (rsrcs != null) {
            String rsrc;
            MetaDataIterator mitr;
            for (String s : rsrcs) {
                rsrc = s;
                if (rsrc.endsWith(".jar")) {
                	url = loader.getResource(rsrc);
                    if (url != null) {
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scanning-jar-stream-url", url));
                        InputStream is = (InputStream) url.openStream();
                        scan(new ZipStreamMetaDataIterator
                                        (new ZipInputStream(is),
                                                newMetaDataFilter()), cparser, names, true,
                                url);
                    }
                }
                else {
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("scanning-resource", rsrc));
                    mitr = new ResourceMetaDataIterator(rsrc, loader);
                    OpenJPAConfiguration conf = repos.getConfiguration();
                    Map peMap = null;
                    if (conf instanceof OpenJPAConfigurationImpl)
                        peMap = ((OpenJPAConfigurationImpl) conf).getPersistenceEnvironment();
                    URL puUrl = peMap == null ? null : (URL) peMap.get(PERSISTENCE_UNIT_ROOT_URL);
                    List<String> mappingFileNames =
                            peMap == null ? null : (List<String>) peMap.get(MAPPING_FILE_NAMES);
                    List<URL> jars = peMap == null ? null : (List<URL>) peMap.get(JAR_FILE_URLS);
                    String puUrlString = puUrl == null ? null : puUrl.toString();
                    if (log.isTraceEnabled())
                        log.trace(_loc.get("pu-root-url", puUrlString));

                    URL puORMUrl = null;
                    try {
                        if (puUrlString != null) {
                            String puORMUrlStr = puUrlString + (puUrlString.endsWith("/") ? "" : "/") + rsrc;
                            puORMUrl = new URL(puORMUrlStr);
                        }
                    } catch (MalformedURLException e) {
                        throw new IOException("Error generating puORMUrlStr.", e);
                    }

                    List<URL> urls = new ArrayList<>(3);
                    while (mitr.hasNext()) {
                        url = (URL) mitr.next();
                        String urlString = url.toString();
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("resource-url", urlString));
                        if (peMap != null) {
                            //OPENJPA-2102: decode the URL to remove such things a spaces (' ') encoded as '%20'
                            if (puUrlString != null && decode(urlString).indexOf(decode(puUrlString)) != -1) {
                                urls.add(url);
                            }
                            else if (puORMUrl != null && puORMUrl.equals(url)) {
                                // Check URL equality to support encapsulating URL protocols
                                urls.add(url);
                            }
                            if (mappingFileNames != null && mappingFileNames.size() != 0) {
                                for (String mappingFileName : mappingFileNames) {
                                    if (log.isTraceEnabled())
                                        log.trace(_loc.get("mapping-file-name", mappingFileName));
                                    if (urlString.indexOf(mappingFileName) != -1)
                                        urls.add(url);
                                }
                            }

                            if (jars != null && jars.size() != 0) {
                                for (URL jarUrl : jars) {
                                    if (log.isTraceEnabled())
                                        log.trace(_loc.get("jar-file-url", jarUrl));
                                    if (urlString.indexOf(jarUrl.toString()) != -1)
                                        urls.add(url);
                                }
                            }
                        }
                        else {
                            urls.add(url);
                        }
                    }
                    mitr.close();

                    for (Object obj : urls) {
                        url = (URL) obj;
                        clss = cparser.parseTypeNames(new URLMetaDataIterator
                                (url));
                        List<String> newNames = Arrays.asList(clss);
                        if (log.isTraceEnabled())
                            log.trace(_loc.get("scan-found-names", newNames,
                                    rsrc));
                        names.addAll(newNames);
                        mapPersistentTypeNames(url, clss);
                    }
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
        for (Object o : map.entrySet()) {
            entry = (Map.Entry) o;
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
     * Decodes a URL-encoded path string.  For example, an encoded
     * space (%20) is decoded into a normal space (' ') character.
     * Added via OPENJPA-2102.
     * @param s - the encoded URL string
     * @return String decoded - the decoded string.
     */
    public static String decode(String s) {
       if (s == null) {
          return null;
       }

       int i = s.indexOf('%');
       if (i == -1) {
          return s;
       }

       StringBuilder builder = new StringBuilder();
       int begin = 0;

       do {
          builder.append(s, begin, i);
          begin = i + 3;

          char ch = (char) Integer.parseInt(s.substring(i + 1, begin), 16);

          if ((ch & 0x80) != 0) {
             // Decode "modified UTF-8".

             if (s.charAt(begin++) != '%') {
                throw new IllegalArgumentException();
             }

             char ch2 = (char) Integer.parseInt(s.substring(begin, begin + 2), 16);
             begin += 2;

             if ((ch & 0xe0) == 0xc0) {
                ch = (char) (((ch & 0x1f) << 6) | (ch2 & 0x3f));
             } else if ((ch & 0xf0) == 0xe0) {
                if (s.charAt(begin++) != '%') {
                   throw new IllegalArgumentException();
                }

                char ch3 = (char) Integer.parseInt(s.substring(begin, begin + 2), 16);
                begin += 2;

                ch = (char) (((ch & 0x0f) << 12) | ((ch2 & 0x3f) << 6) | (ch3 & 0x3f));
             } else {
                throw new IllegalArgumentException();
             }
          }

          builder.append(ch);
       } while ((i = s.indexOf('%', begin)) != -1);

       builder.append(s, begin, s.length());

       return builder.toString();
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

    @Override
    public void clear() {
        super.clear();
        _typeNames = null;
    }

    /**
     * Internal parser interface.
     */
    public interface Parser
        extends MetaDataParser {

        /**
         * Returns the repository for this parser. If none has been set,
         * creates a new repository and sets it.
         */
        MetaDataRepository getRepository();

        /**
         * The parse mode according to the expected document type.
         */
        void setMode(int mode);
    }

    /**
     * Internal serializer interface.
     */
    public interface Serializer
        extends MetaDataSerializer {

        /**
         * The serialization mode according to the expected document type. The
         * mode constants act as bit flags, and therefore can be combined.
         */
        void setMode(int mode);

        /**
         * Add a class meta data to the set to be serialized.
         */
        void addMetaData(ClassMetaData meta);

        /**
         * Remove a class meta data from the set to be serialized.
         */
        boolean removeMetaData(ClassMetaData meta);

        /**
         * Add a sequence meta data to the set to be serialized.
         */
        void addSequenceMetaData(SequenceMetaData meta);

        /**
         * Add a query meta data to the set to be serialized.
         */
        void addQueryMetaData(QueryMetaData meta);

        /**
         * Add all components in the given repository to the set to be
         * serialized.
         */
        void addAll (MetaDataRepository repos);
    }
}
