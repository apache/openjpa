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
package org.apache.openjpa.junit5.internal;

import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.enhance.AsmAdaptor;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.lib.log.JULLogFactory;
import org.apache.openjpa.lib.log.LogFactory;
import org.apache.openjpa.lib.log.LogFactoryImpl;
import org.apache.openjpa.lib.log.SLF4JLogFactory;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.PersistenceMetaDataFactory;
import org.apache.xbean.asm9.AnnotationVisitor;
import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.shade.commons.EmptyVisitor;
import org.apache.xbean.finder.ClassLoaders;
import serp.bytecode.BCClass;
import serp.bytecode.Project;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.xbean.asm9.ClassReader.SKIP_CODE;
import static org.apache.xbean.asm9.ClassReader.SKIP_DEBUG;
import static org.apache.xbean.asm9.ClassReader.SKIP_FRAMES;

public class OpenJPADirectoriesEnhancer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(OpenJPADirectoriesEnhancer.class.getName());
    public static final StackTraceElement[] NO_STACK_TRACE = new StackTraceElement[0];

    private static final AtomicBoolean AUTO_DONE = new AtomicBoolean(false);

    private final boolean auto;
    private final String[] entities;
    private final Class<?> logFactory;

    public OpenJPADirectoriesEnhancer(final boolean auto, final String[] entities, final Class<?> logFactory) {
        this.auto = auto;
        this.entities = entities;
        this.logFactory = logFactory;
    }

    @Override
    public void run() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final OpenJpaClassLoader enhancementClassLoader = new OpenJpaClassLoader(
                classLoader, createLogFactory(classLoader));
        final Thread thread = Thread.currentThread();
        thread.setContextClassLoader(enhancementClassLoader);
        try {
            if (auto) {
                if (AUTO_DONE.compareAndSet(false, true)) {
                    try {
                        ClassLoaders.findUrls(enhancementClassLoader.getParent()).stream()
                                .map(org.apache.xbean.finder.util.Files::toFile)
                                .filter(File::isDirectory)
                                .map(File::toPath)
                                .forEach(dir -> {
                                    LOGGER.fine(() -> "Enhancing folder '" + dir + "'");
                                    try {
                                        enhanceDirectory(enhancementClassLoader, dir);
                                    } catch (final IOException e) {
                                        throw new IllegalStateException(e);
                                    }
                                });
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                } // else: already done, skip useless work
            } else {
                Stream.of(entities).forEach(e -> {
                    try {
                        enhancementClassLoader.loadClass(e);
                    } catch (final ClassNotFoundException e1) {
                        throw new IllegalArgumentException(e1);
                    }
                });
            }
        } finally {
            thread.setContextClassLoader(enhancementClassLoader.getParent());
        }
    }

    private LogFactory createLogFactory(final ClassLoader classLoader) {
        try {
            if (logFactory == null || logFactory == LogFactory.class) {
                try {
                    return new SLF4JLogFactory();
                } catch (final Error | Exception e) {
                    try {
                        return new LogFactoryImpl();
                    } catch (final Error | Exception e2) {
                        return new JULLogFactory();
                    }
                }
            }
            return logFactory.asSubclass(LogFactory.class).getConstructor().newInstance();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void enhanceDirectory(final OpenJpaClassLoader enhancementClassLoader, final Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".class")) {
                    final String relativeName = dir.relativize(file).toString();
                    try {
                        enhancementClassLoader.handleEnhancement(
                                relativeName.substring(0, relativeName.length() - ".class".length()));
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return super.visitFile(file, attrs);
            }
        });
    }

    private static abstract class BaseClassLoader extends ClassLoader {
        private BaseClassLoader(final ClassLoader parent) {
            super(parent);
        }

        protected abstract Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException;

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (name != null && !name.startsWith("java") && !name.startsWith("sun") && !name.startsWith("jdk")) {
                return doLoadClass(name, resolve);
            }
            return defaultLoadClass(name, resolve);
        }

        protected Class<?> defaultLoadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        protected byte[] loadBytes(final String name) {
            final URL url = findUrl(name);
            if (url == null || "jar".equals(url.getProtocol()) /*assume done in build*/) {
                return null;
            }
            byte[] buffer = new byte[4096];
            final ByteArrayOutputStream inMem = new ByteArrayOutputStream(buffer.length);
            try (final InputStream is = url.openStream()) {
                int read;
                while ((read = is.read(buffer)) >= 0) {
                    if (read > 0) {
                        inMem.write(buffer, 0, read);
                    }
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            return inMem.toByteArray();
        }

        protected URL findUrl(final String name) {
            return getResource(name.replace('.', '/') + ".class");
        }
    }

    private static class OpenJpaClassLoader extends BaseClassLoader {
        private static final String PERSITENCE_CAPABLE = Type.getDescriptor(PersistenceCapable.class);
        private static final String ENTITY = "Ljavax/persistence/Entity;";
        private static final String ENTITY2 = "Ljakarta/persistence/Entity;";
        private static final String EMBEDDABLE = "Ljavax/persistence/Entity;";
        private static final String EMBEDDABLE2 = "Ljakarta/persistence/Entity;";
        private static final String MAPPED_SUPERCLASS = "Ljavax/persistence/Entity;";
        private static final String MAPPED_SUPERCLASS2 = "Ljakarta/persistence/Entity;";

        private final MetaDataRepository repos;
        private final ClassLoader tmpLoader;
        private final Collection<String> alreadyEnhanced = new ArrayList<>();

        private OpenJpaClassLoader(final ClassLoader parent, final LogFactory logFactory) {
            super(parent);

            final OpenJPAConfigurationImpl conf = new OpenJPAConfigurationImpl();
            conf.setLogFactory(logFactory);

            tmpLoader = new CompanionLoader(parent);
            repos = new MetaDataRepository();
            repos.setConfiguration(conf);
            repos.setMetaDataFactory(new PersistenceMetaDataFactory());
        }

        @Override
        protected synchronized Class<?> doLoadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            final Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
            handleEnhancement(name);
            return defaultLoadClass(name, resolve);
        }

        private void handleEnhancement(final String name) throws ClassNotFoundException {
            final byte[] enhanced = ensureEnhancedIfNeeded(name);
            if (enhanced != null && alreadyEnhanced.add(name)) {
                // we could do that but test classes will be loaded with parent loader
                // so just rewrite the class on the fly assuming it was not yet read
                try {
                    Files.write(findTarget(name), enhanced, StandardOpenOption.TRUNCATE_EXISTING);
                    LOGGER.info(() -> "Enhanced '" + name + "'");
                } catch (final IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }
            }
        }

        private Path findTarget(final String name) {
            final URL url = findUrl(name);
            if (!"file".equals(url.getProtocol())) {
                throw new IllegalStateException("Only file urls are supported today: " + url);
            }
            return Paths.get(url.getPath());
        }

        private byte[] enhance(final byte[] classBytes) {
            final Thread thread = Thread.currentThread();
            final ClassLoader old = thread.getContextClassLoader();
            thread.setContextClassLoader(tmpLoader);
            try (final InputStream stream = new ByteArrayInputStream(classBytes)) {
                final PCEnhancer enhancer = new PCEnhancer(
                        repos.getConfiguration(),
                        new Project().loadClass(stream, tmpLoader),
                        repos, tmpLoader);
                if (enhancer.run() == PCEnhancer.ENHANCE_NONE) {
                    return null;
                }
                final BCClass pcb = enhancer.getPCBytecode();
                return AsmAdaptor.toByteArray(pcb, pcb.toByteArray());
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                thread.setContextClassLoader(old);
            }
        }

        private boolean isJpaButNotEnhanced(final byte[] classBytes) {
            try (final InputStream stream = new ByteArrayInputStream(classBytes)) {
                final ClassReader reader = new ClassReader(stream);
                reader.accept(new EmptyVisitor() {
                    @Override
                    public void visit(final int version, final int access, final String name,
                                      final String signature, final String superName, final String[] interfaces) {
                        if (interfaces != null && asList(interfaces).contains(PERSITENCE_CAPABLE)) {
                            throw new AlreadyEnhanced(); // exit
                        }
                        super.visit(version, access, name, signature, superName, interfaces);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
                        if (ENTITY.equals(descriptor) ||
                                EMBEDDABLE.equals(descriptor) ||
                                MAPPED_SUPERCLASS.equals(descriptor) ||
                                ENTITY2.equals(descriptor) ||
                                EMBEDDABLE2.equals(descriptor) ||
                                MAPPED_SUPERCLASS2.equals(descriptor)) {
                            throw new MissingEnhancement(); // we already went into visit() so we miss the enhancement
                        }
                        return new EmptyVisitor().visitAnnotation(descriptor, visible);
                    }
                }, SKIP_DEBUG + SKIP_CODE + SKIP_FRAMES);
                return false;
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } catch (final AlreadyEnhanced alreadyEnhanced) {
                return false;
            } catch (final MissingEnhancement alreadyEnhanced) {
                return true;
            }
        }

        private byte[] ensureEnhancedIfNeeded(final String name) {
            final byte[] classBytes = loadBytes(name);
            if (classBytes == null) {
                return null;
            }
            if (isJpaButNotEnhanced(classBytes)) {
                final byte[] enhanced = enhance(classBytes);
                if (enhanced != null) {
                    return enhanced;
                }
                LOGGER.info("'" + name + "' already enhanced");
            }
            return null;
        }
    }

    private static class CompanionLoader extends BaseClassLoader {
        private CompanionLoader(final ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> doLoadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            final Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
            final byte[] content = loadBytes(name);
            if (content != null) {
                final Class<?> value = super.defineClass(name, content, 0, content.length);
                if (resolve) {
                    resolveClass(value);
                }
                return value;
            }
            return defaultLoadClass(name, resolve);
        }
    }

    private static class MissingEnhancement extends RuntimeException {
        private MissingEnhancement() {
            setStackTrace(NO_STACK_TRACE);
        }
    }

    private static class AlreadyEnhanced extends RuntimeException {
        private AlreadyEnhanced() {
            setStackTrace(NO_STACK_TRACE);
        }
    }
}
