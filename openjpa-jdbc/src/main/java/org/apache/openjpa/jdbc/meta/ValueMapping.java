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
package org.apache.openjpa.jdbc.meta;

import java.io.Serializable;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.MetaDataContext;
import org.apache.openjpa.meta.ValueMetaData;

/**
 * Specialization of value metadata for relational databases.
 *
 * @author Abe White
 * @since 0.4.0
 */
public interface ValueMapping
    extends ValueMetaData, MetaDataContext, Serializable {

    /**
     * Standard forward join.
     */
    int JOIN_FORWARD = 0;

    /**
     * Inverse join.
     */
    int JOIN_INVERSE = 1;

    /**
     * Inverse join that is marked up as a forward join because the
     * backing mapping expects an inverse direction.
     */
    int JOIN_EXPECTED_INVERSE = 2;

    /**
     * A fully polymorphic relation (the default).
     */
    int POLY_TRUE = 0;

    /**
     * A non-polymorphic relation.
     */
    int POLY_FALSE = 1;

    /**
     * A relation that can hold any joinable subclass type.
     */
    int POLY_JOINABLE = 2;

    /**
     * Raw mapping data.
     */
    ValueMappingInfo getValueInfo();

    /**
     * The handler used for this value, or null if none.
     */
    ValueHandler getHandler();

    /**
     * The handler used for this value, or null if none.
     */
    void setHandler(ValueHandler handler);

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getRepository}.
     */
    MappingRepository getMappingRepository();

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getFieldMetaData}.
     */
    FieldMapping getFieldMapping();

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getTypeMetaData}.
     */
    ClassMapping getTypeMapping();

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getDeclaredTypeMetaData}.
     */
    ClassMapping getDeclaredTypeMapping();

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getEmbeddedMetaData}.
     */
    ClassMapping getEmbeddedMapping();

    /**
     * Convenience method to perform cast from
     * {@link ValueMetaData#getValueMappedByMetaData}.
     */
    FieldMapping getValueMappedByMapping();

    /**
     * The columns that hold the data for this value.
     */
    Column[] getColumns();

    /**
     * The columns that hold the data for this value.
     */
    void setColumns(Column[] cols);

    /**
     * I/O information on the foreign key, or columns if this value doesn't
     * have a key.
     */
    ColumnIO getColumnIO();

    /**
     * I/O information on the foreign key, or columns if this value doesn't
     * have a key.
     */
    void setColumnIO(ColumnIO io);

    /**
     * If this value joins to another record, the foreign key.
     */
    ForeignKey getForeignKey();

    /**
     * Return an equivalent of this value's foreign key, but joining to the
     * given target, which may be an unjoined subclass of this value's
     * related type.
     */
    ForeignKey getForeignKey(ClassMapping target);

    /**
     * If this value joins to another record, the foreign key.
     */
    void setForeignKey(ForeignKey fk);

    /**
     * The join direction.
     */
    int getJoinDirection();

    /**
     * The join direction.
     */
    void setJoinDirection(int direction);

    /**
     * Sets this value's foreign key to the given related object. The object
     * may be null.
     */
    void setForeignKey(Row row, OpenJPAStateManager rel)
        throws SQLException;

    /**
     * Sets this value's foreign key to the given related object. The object
     * may be null. If the object is one of2or more foreign keys with the
     * same target, the targetNumber specifies the one to set.
     */
    void setForeignKey(Row row, OpenJPAStateManager rel, int targetNumber)
        throws SQLException;

    /**
     * Sets this value's foreign key to the given related object. The object
     * may be null.
     */
    void whereForeignKey(Row row, OpenJPAStateManager rel)
        throws SQLException;

    /**
     * Return all independently-mapped joinable types for this value, depending
     * on whether this value is polymorphic and how the related type is mapped.
     * Return an empty array if value type is not PC.
     */
    ClassMapping[] getIndependentTypeMappings();

    /**
     * Return the {@link org.apache.openjpa.sql.Select} subclasses constant
     * for loading this relation, based on how the related type is mapped,
     * whether this relation is polymorphic, and whether it is configured to
     * use class criteria.
     */
    int getSelectSubclasses();

    /**
     * Unique constraint on this value's columns, or null if none.
     */
    Unique getValueUnique();

    /**
     * Unique constraint on this value's columns, or null if none.
     */
    void setValueUnique(Unique unq);

    /**
     * Index on this value's columns, or null if none.
     */
    Index getValueIndex();

    /**
     * Index on this value's columns, or null if none.
     */
    void setValueIndex(Index idx);

    /**
     * Whether to use class criteria when joining to related type.
     */
    boolean getUseClassCriteria();

    /**
     * Whether to use class criteria when joining to related type.
     */
    void setUseClassCriteria(boolean criteria);

    /**
     * The degree to which this relation is polymorphic.
     */
    int getPolymorphic();

    /**
     * The degree to which this relation is polymorphic.
     */
    void setPolymorphic(int polymorphic);

    /**
     * Increase the reference count on used schema components.
     */
    void refSchemaComponents();

    /**
     * Map indexes and constraints for this value, using the current
     * {@link ValueMappingInfo}. The foreign key or columns of this value
     * must be set before calling this method.
     * @deprecated
     */
    @Deprecated void mapConstraints(String name, boolean adapt);

    /**
     * Map indexes and constraints for this value, using the current
     * {@link ValueMappingInfo}. The foreign key or columns of this value
     * must be set before calling this method.
     */
    void mapConstraints(DBIdentifier name, boolean adapt);

    /**
     * Clear mapping information, including strategy.
     */
    void clearMapping();

    /**
     * Update {@link MappingInfo} with our current mapping information.
     */
    void syncMappingInfo();

    /**
     * Copy mapping info from the given instance to this one.
     */
    void copyMappingInfo(ValueMapping vm);
}
