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
package org.apache.openjpa.jdbc.meta.strats;

import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.jdbc.meta.DiscriminatorMappingInfo;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * Maps metadata-given values to classes.
 *
 * @author Abe White
 */
public class ValueMapDiscriminatorStrategy
    extends InValueDiscriminatorStrategy {

    private static final long serialVersionUID = 1L;

    public static final String ALIAS = "value-map";

    private static final Localizer _loc = Localizer.forPackage
        (ValueMapDiscriminatorStrategy.class);

    private Map<String, Class<?>> _vals;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
    private final Lock _readLock = rwl.readLock();
    private final Lock _writeLock = rwl.writeLock();

    @Override
    public String getAlias() {
        return ALIAS;
    }

    @Override
    protected int getJavaType() {
        Object val = disc.getValue();
        if (val != null && val != Discriminator.NULL)
            return JavaTypes.getTypeCode(val.getClass());

        // if the user wants the type to be null, we need a jdbc-type
        // on the column or an existing column to figure out the java type
        DiscriminatorMappingInfo info = disc.getMappingInfo();
        List<Column> cols = info.getColumns();
        Column col = (cols.isEmpty()) ? null : cols.get(0);
        if (col != null) {
            if (col.getJavaType() != JavaTypes.OBJECT)
                return col.getJavaType();
            if (col.getType() != Types.OTHER)
                return JavaTypes.getTypeCode(Schemas.getJavaType
                    (col.getType(), col.getSize(), col.getDecimalDigits()));
        }
        return JavaTypes.STRING;
    }

    @Override
    protected Object getDiscriminatorValue(ClassMapping cls) {
        Object val = cls.getDiscriminator().getValue();
        return (val == Discriminator.NULL) ? null : val;
    }

    @Override
    protected Class getClass(Object val, JDBCStore store)
        throws ClassNotFoundException {

        if(_vals == null) {
            _writeLock.lock();
            try {
                if(_vals == null) {
                    _vals = constructCache(disc);
                }
            } finally {
                _writeLock.unlock();
            }
        }

        String className = (val == null) ? null : val.toString();
        _readLock.lock();
        try {
            Class<?> clz = _vals.get(className);
            if (clz != null)
                return clz;
        } finally {
            _readLock.unlock();
        }

        _writeLock.lock();
        try {
            Class<?> clz = _vals.get(className);
            if (clz != null)
                return clz;

            //Rebuild the cache to check for updates
            _vals = constructCache(disc);

            //Try get again
            clz = _vals.get(className);
            if (clz != null)
                return clz;
            throw new ClassNotFoundException(_loc.get("unknown-discrim-value",
                    new Object[]{ className, disc.getClassMapping().getDescribedType().
                    getName(), new TreeSet<>(_vals.keySet()) }).getMessage());
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Build a class cache map from the discriminator
     */
    private static Map<String, Class<?>> constructCache(Discriminator disc) {
        //Build the cache map
        ClassMapping cls = disc.getClassMapping();
        ClassMapping[] subs = cls.getJoinablePCSubclassMappings();
        Map<String, Class<?>> map = new HashMap<>((int) ((subs.length + 1) * 1.33 + 1));
        mapDiscriminatorValue(cls, map);
        for (ClassMapping sub : subs) {
            mapDiscriminatorValue(sub, map);
        }
        return map;
    }

    /**
     * Map the stringified version of the discriminator value of the given type.
     */
    private static void mapDiscriminatorValue(ClassMapping cls, Map<String, Class<?>> map) {
        // possible that some types will never be persisted and therefore
        // can have no discriminator value
        Object val = cls.getDiscriminator().getValue();
        if (val == null)
            return;

        String str = (val == Discriminator.NULL) ? null : val.toString();
        Class<?> exist = map.get(str);
        if (exist != null)
            throw new MetaDataException(_loc.get("dup-discrim-value",
                str, exist, cls));
        map.put(str, cls.getDescribedType());
    }

    @Override
    public void map(boolean adapt) {
        Object val = disc.getMappingInfo().getValue(disc, adapt);
        if (val == null && !Modifier.isAbstract(disc.getClassMapping().
            getDescribedType().getModifiers()))
            throw new MetaDataException(_loc.get("no-discrim-value",
                disc.getClassMapping()));

        // we set the value before mapping to use to calculate the template
        // column's java type
        disc.setValue(val);
        super.map(adapt);
    }
}
