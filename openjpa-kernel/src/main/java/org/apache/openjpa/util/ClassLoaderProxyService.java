/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.util;

import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.openjpa.lib.conf.Configuration;

/**
 * Service to load classes dynamically at runtime.
 * This class got forked from Apache OpenWebBeans
 */
public class ClassLoaderProxyService
{
    private final ProxiesClassLoader loader;

    public ClassLoaderProxyService(Configuration config, final ClassLoader parentLoader)
    {
        if (parentLoader instanceof ProxiesClassLoader) {
            this.loader = (ProxiesClassLoader) parentLoader;
        }
        else {
            this.loader = new ProxiesClassLoader(parentLoader, false);
        }
    }

    protected ClassLoaderProxyService(final ProxiesClassLoader loader)
    {
        this.loader = loader;
    }

    public ClassLoader getProxyClassLoader(final Class<?> forClass)
    {
        return loader;
    }

    public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
    {
        return (Class<T>) loader.getOrRegister(
                name, bytecode, proxiedClass.getPackage(), proxiedClass.getProtectionDomain());
    }

    public <T> T newInstance(final Class<? extends T> proxyClass)
    {
        try
        {
            return proxyClass.getConstructor().newInstance();
        }
        catch (final Exception e)
        {
            throw new IllegalStateException("Failed to create a new Proxy instance of " + proxyClass.getName(), e);
        }
    }


    private static class ProxiesClassLoader extends ClassLoader
    {
        private final boolean skipPackages;
        private final ConcurrentMap<String, Class<?>> classes = new ConcurrentHashMap<>();

        private ProxiesClassLoader(final ClassLoader parentLoader, boolean skipPackages)
        {
            super(parentLoader);
            this.skipPackages = skipPackages;
        }


        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
        {
            final Class<?> clazz = classes.get(name);
            if (clazz == null)
            {
                return getParent().loadClass(name);
            }
            return clazz;
        }

        private Class<?> getOrRegister(final String proxyClassName, final byte[] proxyBytes,
                                       final Package pck, final ProtectionDomain protectionDomain)
        {
            final String key = proxyClassName.replace('/', '.');
            Class<?> existing = classes.get(key);
            if (existing == null)
            {
                synchronized (this)
                {
                    existing = classes.get(key);
                    if (existing == null)
                    {
                        if (!skipPackages)
                        {
                            definePackageFor(pck, protectionDomain);
                        }
                        existing = super.defineClass(proxyClassName, proxyBytes, 0, proxyBytes.length);

                        resolveClass(existing);
                        classes.put(key, existing);
                    }
                }
                try
                {
                    Class.forName(existing.getName(), true, existing.getClassLoader());
                }
                catch (ClassNotFoundException e)
                {
                    // no-op, not critical, will be done at first instantiation but shouldn't happen anyway
                }
            }
            return existing;
        }

        private void definePackageFor(final Package model, final ProtectionDomain protectionDomain)
        {
            if (model == null)
            {
                return;
            }
            if (getPackage(model.getName()) == null)
            {
                if (model.isSealed() && protectionDomain != null &&
                        protectionDomain.getCodeSource() != null &&
                        protectionDomain.getCodeSource().getLocation() != null)
                {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            protectionDomain.getCodeSource().getLocation());
                }
                else
                {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            null);
                }
            }
        }
    }
}
