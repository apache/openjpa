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
package org.apache.openjpa.jdbc.meta;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;

/**
 * Installs mapping strategies.
 *
 * @author Abe White
 * @nojavadoc
 * @since 4.0
 */
public abstract class StrategyInstaller {

    protected final JDBCConfiguration conf;
    protected final MappingRepository repos;

    /**
     * Constructor; supply repository.
     */
    public StrategyInstaller(MappingRepository repos) {
        this.repos = repos;
        this.conf = (JDBCConfiguration) repos.getConfiguration();
    }

    /**
     * Return whether this installer adapts the given mapping data and
     * schema, vs requiring that all information be supplied correctly.
     */
    public boolean isAdapting() {
        return false;
    }

    /**
     * Install a strategy on the given mapping.
     */
    public abstract void installStrategy(ClassMapping cls);

    /**
     * Install a strategy on the given mapping.
     */
    public abstract void installStrategy(FieldMapping fm);

    /**
     * Install a strategy on the given mapping.
     */
    public abstract void installStrategy(Version version);

    /**
     * Install a strategy on the given mapping.
     */
    public abstract void installStrategy(Discriminator discrim);
}
