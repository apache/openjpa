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
package org.apache.openjpa.persistence.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.ColumnResult;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FieldResult;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.kernel.EagerFetchModes;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.ClassMappingInfo;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingInfo;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.meta.SequenceMapping;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.ValueMappingInfo;
import org.apache.openjpa.jdbc.meta.strats.EnumValueHandler;
import org.apache.openjpa.jdbc.meta.strats.FlatClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.FullClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.AnnotationPersistenceMetaDataParser;
import static org.apache.openjpa.persistence.jdbc.MappingTag.*;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.UnsupportedException;
import org.apache.openjpa.util.UserException;

/**
 * Persistence annotation mapping parser.
 *
 * @author Pinaki Poddar
 * @author Steve Kim
 * @author Abe White
 */
public class AnnotationPersistenceMappingParser
    extends AnnotationPersistenceMetaDataParser {

    protected static final int TRUE = 1;
    protected static final int FALSE = 2;

    private static final Localizer _loc = Localizer.forPackage
        (AnnotationPersistenceMappingParser.class);

    private static final Map<Class, MappingTag> _tags =
        new HashMap<Class, MappingTag>();

    static {
        _tags.put(AssociationOverride.class, ASSOC_OVERRIDE);
        _tags.put(AssociationOverrides.class, ASSOC_OVERRIDES);
        _tags.put(AttributeOverride.class, ATTR_OVERRIDE);
        _tags.put(AttributeOverrides.class, ATTR_OVERRIDES);
        _tags.put(javax.persistence.Column.class, COL);
        _tags.put(ColumnResult.class, COLUMN_RESULT);
        _tags.put(DiscriminatorColumn.class, DISCRIM_COL);
        _tags.put(DiscriminatorValue.class, DISCRIM_VAL);
        _tags.put(EntityResult.class, ENTITY_RESULT);
        _tags.put(Enumerated.class, ENUMERATED);
        _tags.put(FieldResult.class, FIELD_RESULT);
        _tags.put(Inheritance.class, INHERITANCE);
        _tags.put(JoinColumn.class, JOIN_COL);
        _tags.put(JoinColumns.class, JOIN_COLS);
        _tags.put(JoinTable.class, JOIN_TABLE);
        _tags.put(PrimaryKeyJoinColumn.class, PK_JOIN_COL);
        _tags.put(PrimaryKeyJoinColumns.class, PK_JOIN_COLS);
        _tags.put(SecondaryTable.class, SECONDARY_TABLE);
        _tags.put(SecondaryTables.class, SECONDARY_TABLES);
        _tags.put(SqlResultSetMapping.class, SQL_RESULT_SET_MAPPING);
        _tags.put(SqlResultSetMappings.class, SQL_RESULT_SET_MAPPINGS);
        _tags.put(Table.class, TABLE);
        _tags.put(Temporal.class, TEMPORAL);
        _tags.put(TableGenerator.class, TABLE_GEN);
        _tags.put(ClassCriteria.class, CLASS_CRIT);
        _tags.put(Columns.class, COLS);
        _tags.put(ContainerTable.class, CONTAINER_TABLE);
        _tags.put(DataStoreIdColumn.class, DATASTORE_ID_COL);
        _tags.put(DiscriminatorStrategy.class, DISCRIM_STRAT);
        _tags.put(EagerFetchMode.class, EAGER_FETCH_MODE);
        _tags.put(ElementClassCriteria.class, ELEM_CLASS_CRIT);
        _tags.put(ElementForeignKey.class, ELEM_FK);
        _tags.put(ElementIndex.class, ELEM_INDEX);
        _tags.put(ElementJoinColumn.class, ELEM_JOIN_COL);
        _tags.put(ElementJoinColumns.class, ELEM_JOIN_COLS);
        _tags.put(ElementNonpolymorphic.class, ELEM_NONPOLY);
        _tags.put(EmbeddedMapping.class, EMBEDDED_MAPPING);
        _tags.put(ForeignKey.class, FK);
        _tags.put(Index.class, INDEX);
        _tags.put(MappingOverride.class, MAPPING_OVERRIDE);
        _tags.put(MappingOverrides.class, MAPPING_OVERRIDES);
        _tags.put(Nonpolymorphic.class, NONPOLY);
        _tags.put(OrderColumn.class, ORDER_COL);
        _tags.put(Strategy.class, STRAT);
        _tags.put(SubclassFetchMode.class, SUBCLASS_FETCH_MODE);
        _tags.put(Unique.class, UNIQUE);
        _tags.put(VersionColumn.class, VERSION_COL);
        _tags.put(VersionColumns.class, VERSION_COLS);
        _tags.put(VersionStrategy.class, VERSION_STRAT);
        _tags.put(XJoinColumn.class, X_JOIN_COL);
        _tags.put(XJoinColumns.class, X_JOIN_COLS);
        _tags.put(XSecondaryTable.class, X_SECONDARY_TABLE);
        _tags.put(XSecondaryTables.class, X_SECONDARY_TABLES);
        _tags.put(XTable.class, X_TABLE);
    }

    public AnnotationPersistenceMappingParser(JDBCConfiguration conf) {
        super(conf);
    }

    @Override
    protected void parsePackageMappingAnnotations(Package pkg) {
        MappingTag tag;
        for (Annotation anno : pkg.getDeclaredAnnotations()) {
            tag = _tags.get(anno.annotationType());
            if (tag == null) {
                handleUnknownPackageMappingAnnotation(pkg, anno);
                continue;
            }

            switch (tag) {
                case TABLE_GEN:
                    parseTableGenerator(pkg, (TableGenerator) anno);
                    break;
                default:
                    throw new UnsupportedException(_loc.get("unsupported", pkg,
                        anno.toString()));
            }
        }
    }

    /**
     * Allow subclasses to handle unknown annotations.
     */
    protected boolean handleUnknownPackageMappingAnnotation(Package pkg,
        Annotation anno) {
        return false;
    }

    /**
     * Parse @TableGenerator.
     */
    private void parseTableGenerator(AnnotatedElement el, TableGenerator gen) {
        String name = gen.name();
        if (StringUtils.isEmpty(name))
            throw new MetaDataException(_loc.get("no-gen-name", el));

        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-gen", name));

        SequenceMapping meta = (SequenceMapping) getRepository().
            getCachedSequenceMetaData(name);
        if (meta != null) {
            if (log.isWarnEnabled())
                log.warn(_loc.get("dup-gen", name, el));
            return;
        }

        meta = (SequenceMapping) getRepository().addSequenceMetaData(name);
        meta.setSequencePlugin(SequenceMapping.IMPL_VALUE_TABLE);
        meta.setTable(toTableName(gen.schema(), gen.table()));
        meta.setPrimaryKeyColumn(gen.pkColumnName());
        meta.setSequenceColumn(gen.valueColumnName());
        meta.setPrimaryKeyValue(gen.pkColumnValue());
        meta.setInitialValue(gen.initialValue());
        meta.setAllocate(gen.allocationSize());
        meta.setSource(getSourceFile(), (el instanceof Class) ? el : null,
            meta.SRC_ANNOTATIONS);

        //### EJB3
        if (gen.uniqueConstraints().length > 0 && log.isWarnEnabled())
            log.warn(_loc.get("unique-constraints", name));
    }

    @Override
    protected void parseClassMappingAnnotations(ClassMetaData meta) {
        ClassMapping cm = (ClassMapping) meta;
        Class cls = cm.getDescribedType();

        MappingTag tag;
        for (Annotation anno : cls.getDeclaredAnnotations()) {
            tag = _tags.get(anno.annotationType());
            if (tag == null) {
                handleUnknownClassMappingAnnotation(cm, anno);
                continue;
            }

            switch (tag) {
                case ASSOC_OVERRIDE:
                    parseAssociationOverrides(cm, (AssociationOverride) anno);
                    break;
                case ASSOC_OVERRIDES:
                    parseAssociationOverrides(cm, ((AssociationOverrides) anno).
                        value());
                    break;
                case ATTR_OVERRIDE:
                    parseAttributeOverrides(cm, (AttributeOverride) anno);
                    break;
                case ATTR_OVERRIDES:
                    parseAttributeOverrides(cm, ((AttributeOverrides) anno).
                        value());
                    break;
                case DISCRIM_COL:
                    parseDiscriminatorColumn(cm, (DiscriminatorColumn) anno);
                    break;
                case DISCRIM_VAL:
                    cm.getDiscriminator().getMappingInfo().setValue
                        (((DiscriminatorValue) anno).value());
                    break;
                case INHERITANCE:
                    parseInheritance(cm, (Inheritance) anno);
                    break;
                case PK_JOIN_COL:
                    parsePrimaryKeyJoinColumns(cm, (PrimaryKeyJoinColumn) anno);
                    break;
                case PK_JOIN_COLS:
                    parsePrimaryKeyJoinColumns(cm,
                        ((PrimaryKeyJoinColumns) anno).
                            value());
                    break;
                case SECONDARY_TABLE:
                    parseSecondaryTables(cm, (SecondaryTable) anno);
                    break;
                case SECONDARY_TABLES:
                    parseSecondaryTables(cm, ((SecondaryTables) anno).value());
                    break;
                case SQL_RESULT_SET_MAPPING:
                    parseSQLResultSetMappings(cm, (SqlResultSetMapping) anno);
                    break;
                case SQL_RESULT_SET_MAPPINGS:
                    parseSQLResultSetMappings(cm, ((SqlResultSetMappings) anno).
                        value());
                    break;
                case TABLE:
                    parseTable(cm, (Table) anno);
                    break;
                case TABLE_GEN:
                    parseTableGenerator(cls, (TableGenerator) anno);
                    break;
                case DATASTORE_ID_COL:
                    parseDataStoreIdColumn(cm, (DataStoreIdColumn) anno);
                    break;
                case DISCRIM_STRAT:
                    cm.getDiscriminator().getMappingInfo().setStrategy
                        (((DiscriminatorStrategy) anno).value());
                    break;
                case FK:
                    parseForeignKey(cm.getMappingInfo(), (ForeignKey) anno);
                    break;
                case MAPPING_OVERRIDE:
                    parseMappingOverrides(cm, (MappingOverride) anno);
                    break;
                case MAPPING_OVERRIDES:
                    parseMappingOverrides(cm,
                        ((MappingOverrides) anno).value());
                    break;
                case STRAT:
                    cm.getMappingInfo().setStrategy(((Strategy) anno).value());
                    break;
                case SUBCLASS_FETCH_MODE:
                    cm.setSubclassFetchMode(toEagerFetchModeConstant
                        (((SubclassFetchMode) anno).value()));
                    break;
                case VERSION_COL:
                    parseVersionColumns(cm, (VersionColumn) anno);
                    break;
                case VERSION_COLS:
                    parseVersionColumns(cm, ((VersionColumns) anno).value());
                    break;
                case VERSION_STRAT:
                    cm.getVersion().getMappingInfo().setStrategy
                        (((VersionStrategy) anno).value());
                    break;
                case X_TABLE:
                case X_SECONDARY_TABLE:
                case X_SECONDARY_TABLES:
                    // no break; not supported yet
                default:
                    throw new UnsupportedException(_loc.get("unsupported", cm,
                        anno));
            }
        }
    }

    /**
     * Allow subclasses to handle unknown annotations.
     */
    protected boolean handleUnknownClassMappingAnnotation(ClassMapping cls,
        Annotation anno) {
        return false;
    }

    /**
     * Parse @AssociationOverride(s).
     */
    private void parseAssociationOverrides(ClassMapping cm,
        AssociationOverride... assocs) {
        FieldMapping sup;
        JoinColumn[] scols;
        int unique;
        List<Column> jcols;
        for (AssociationOverride assoc : assocs) {
            if (StringUtils.isEmpty(assoc.name()))
                throw new MetaDataException(_loc.get("no-override-name", cm));
            sup = (FieldMapping) cm.getDefinedSuperclassField(assoc.name());
            if (sup == null)
                sup = (FieldMapping) cm.addDefinedSuperclassField
                    (assoc.name(), Object.class, Object.class);
            scols = assoc.joinColumns();
            if (scols == null || scols.length == 0)
                continue;

            jcols = new ArrayList<Column>(scols.length);
            unique = 0;
            for (JoinColumn scol : scols) {
                unique |= (scol.unique()) ? TRUE : FALSE;
                jcols.add(newColumn(scol));
            }
            setColumns(sup, sup.getValueInfo(), jcols, unique);
        }
    }

    /**
     * Parse @AttributeOverride(s).
     */
    private void parseAttributeOverrides(ClassMapping cm,
        AttributeOverride... attrs) {
        FieldMapping sup;
        for (AttributeOverride attr : attrs) {
            if (StringUtils.isEmpty(attr.name()))
                throw new MetaDataException(_loc.get("no-override-name", cm));
            sup = (FieldMapping) cm.getDefinedSuperclassField(attr.name());
            if (sup == null)
                sup = (FieldMapping) cm.addDefinedSuperclassField(attr.name(),
                    Object.class, Object.class);
            if (attr.column() != null)
                parseColumns(sup, attr.column());
        }
    }

    /**
     * Parse inheritance @PrimaryKeyJoinColumn(s).
     */
    private void parsePrimaryKeyJoinColumns(ClassMapping cm,
        PrimaryKeyJoinColumn... joins) {
        List<Column> cols = new ArrayList<Column>(joins.length);
        for (PrimaryKeyJoinColumn join : joins)
            cols.add(newColumn(join));
        cm.getMappingInfo().setColumns(cols);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(PrimaryKeyJoinColumn join) {
        Column col = new Column();
        col.setFlag(Column.FLAG_PK_JOIN, true);
        if (!StringUtils.isEmpty(join.name()))
            col.setName(join.name());
        if (!StringUtils.isEmpty(join.columnDefinition()))
            col.setName(join.columnDefinition());
        if (!StringUtils.isEmpty(join.referencedColumnName()))
            col.setTarget(join.referencedColumnName());
        return col;
    }

    /**
     * Parse @SecondaryTable(s).
     */
    private void parseSecondaryTables(ClassMapping cm,
        SecondaryTable... tables) {
        ClassMappingInfo info = cm.getMappingInfo();
        Log log = getLog();

        String name;
        List<Column> joins;
        boolean warnUnique = false;
        for (SecondaryTable table : tables) {
            name = table.name();
            if (StringUtils.isEmpty(name))
                throw new MetaDataException(_loc.get("second-name", cm));
            if (!StringUtils.isEmpty(table.schema()))
                name = table.schema() + "." + name;
            if (table.pkJoinColumns().length > 0) {
                joins = new ArrayList<Column>(table.pkJoinColumns().length);
                for (PrimaryKeyJoinColumn join : table.pkJoinColumns())
                    joins.add(newColumn(join));
                info.setSecondaryTableJoinColumns(name, joins);
            }
            warnUnique |= table.uniqueConstraints().length > 0;
        }

        //### EJB3
        if (warnUnique && log.isWarnEnabled())
            log.warn(_loc.get("unique-constraints", cm));
    }

    /**
     * Set class table.
     */
    private void parseTable(ClassMapping cm, Table table) {
        String tableName = toTableName(table.schema(), table.name());
        if (tableName != null)
            cm.getMappingInfo().setTableName(tableName);

        for (UniqueConstraint uniqueConstraint:table.uniqueConstraints()) {
            Unique unique = newUnique(cm, null, uniqueConstraint.columnNames());
            cm.getMappingInfo().addUnique(unique);
        }
    }

    /**
     * Form a qualified table name from a schema and table name.
     */
    private static String toTableName(String schema, String table) {
        if (StringUtils.isEmpty(table))
            return null;
        if (StringUtils.isEmpty(schema))
            return table;
        return schema + "." + table;
    }

    /**
     * Parses the given annotation to create and cache a
     * {@link SQLResultSetMappingMetaData}.
     */
    private void parseSQLResultSetMappings(ClassMapping cm,
        SqlResultSetMapping... annos) {
        MappingRepository repos = (MappingRepository) getRepository();
        Log log = getLog();
        for (SqlResultSetMapping anno : annos) {
            if (log.isTraceEnabled())
                log.trace(_loc.get("parse-sqlrsmapping", anno.name()));

            QueryResultMapping result = repos.getCachedQueryResultMapping
                (null, anno.name());
            if (result != null) {
                if (log.isWarnEnabled())
                    log.warn(_loc.get("dup-sqlrsmapping", anno.name(), cm));
                continue;
            }

            result = repos.addQueryResultMapping(null, anno.name());
            result.setSource(getSourceFile(), cm.getDescribedType(),
                result.SRC_ANNOTATIONS);

            for (EntityResult entity : anno.entities()) {
                QueryResultMapping.PCResult entityResult = result.addPCResult
                    (entity.entityClass());
                if (!StringUtils.isEmpty(entity.discriminatorColumn()))
                    entityResult.addMapping(entityResult.DISCRIMINATOR,
                        entity.discriminatorColumn());

                for (FieldResult field : entity.fields())
                    entityResult.addMapping(field.name(), field.column());
            }
            for (ColumnResult column : anno.columns())
                result.addColumnResult(column.name());
        }
    }

    /**
     * Parse @DiscriminatorColumn.
     */
    private void parseDiscriminatorColumn(ClassMapping cm,
        DiscriminatorColumn dcol) {
        Column col = new Column();
        if (!StringUtils.isEmpty(dcol.name()))
            col.setName(dcol.name());
        if (!StringUtils.isEmpty(dcol.columnDefinition()))
            col.setTypeName(dcol.columnDefinition());
        switch (dcol.discriminatorType()) {
            case CHAR:
                col.setJavaType(JavaTypes.CHAR);
                break;
            case INTEGER:
                col.setJavaType(JavaTypes.INT);
                if (dcol.length() != 31)
                    col.setSize(dcol.length());
                break;
            default:
                col.setJavaType(JavaTypes.STRING);
                col.setSize(dcol.length());
        }
        cm.getDiscriminator().getMappingInfo().setColumns
            (Arrays.asList(new Column[]{ col }));
    }

    /**
     * Parse @Inheritance.
     */
    private void parseInheritance(ClassMapping cm, Inheritance inherit) {
        ClassMappingInfo info = cm.getMappingInfo();
        switch (inherit.strategy()) {
            case SINGLE_TABLE:
                info.setHierarchyStrategy(FlatClassStrategy.ALIAS);
                break;
            case JOINED:
                info.setHierarchyStrategy(VerticalClassStrategy.ALIAS);
                break;
            case TABLE_PER_CLASS:
                info.setHierarchyStrategy(FullClassStrategy.ALIAS);
                break;
            default:
                throw new InternalException();
        }
    }

    /**
     * Parse class-level @MappingOverride(s).
     */
    private void parseMappingOverrides(ClassMapping cm,
        MappingOverride... overs) {
        FieldMapping sup;
        for (MappingOverride over : overs) {
            if (StringUtils.isEmpty(over.name()))
                throw new MetaDataException(_loc.get("no-override-name", cm));
            sup = (FieldMapping) cm.getDefinedSuperclassField(over.name());
            if (sup == null)
                sup = (FieldMapping) cm.addDefinedSuperclassField(over.name(),
                    Object.class, Object.class);
            populate(sup, over);
        }
    }

    /**
     * Populate the given field from override data.
     */
    private void populate(FieldMapping fm, MappingOverride over) {
        if (over.containerTable().specified())
            parseContainerTable(fm, over.containerTable());
        parseColumns(fm, over.columns());
        parseXJoinColumns(fm, fm.getValueInfo(), true, over.joinColumns());
        parseElementJoinColumns(fm, over.elementJoinColumns());
    }

    /**
     * Parse datastore identity information in @DataStoreIdColumn.
     */
    private void parseDataStoreIdColumn(ClassMapping cm, DataStoreIdColumn id) {
        Column col = new Column();
        if (!StringUtils.isEmpty(id.name()))
            col.setName(id.name());
        if (!StringUtils.isEmpty(id.columnDefinition()))
            col.setTypeName(id.columnDefinition());
        if (id.precision() != 0)
            col.setSize(id.precision());
        col.setFlag(Column.FLAG_UNINSERTABLE, !id.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !id.updatable());
        cm.getMappingInfo().setColumns(Arrays.asList(new Column[]{ col }));
    }

    /**
     * Parse the given foreign key.
     */
    private void parseForeignKey(MappingInfo info, ForeignKey fk) {
        parseForeignKey(info, fk.name(), fk.enabled(), fk.deferred(),
            fk.deleteAction(), fk.updateAction());
    }

    /**
     * Set foreign key data on the given mapping info.
     */
    protected void parseForeignKey(MappingInfo info, String name,
        boolean enabled, boolean deferred, ForeignKeyAction deleteAction,
        ForeignKeyAction updateAction) {
        if (!enabled) {
            info.setCanForeignKey(false);
            return;
        }

        org.apache.openjpa.jdbc.schema.ForeignKey fk =
            new org.apache.openjpa.jdbc.schema.ForeignKey();
        if (!StringUtils.isEmpty(name))
            fk.setName(name);
        fk.setDeferred(deferred);
        fk.setDeleteAction(toForeignKeyAction(deleteAction));
        fk.setUpdateAction(toForeignKeyAction(deleteAction));
        info.setForeignKey(fk);
    }

    /**
     * Convert our FK action enum to an internal OpenJPA action.
     */
    private int toForeignKeyAction(ForeignKeyAction action) {
        switch (action) {
            case RESTRICT:
                return org.apache.openjpa.jdbc.schema.ForeignKey.ACTION_RESTRICT;
            case CASCADE:
                return org.apache.openjpa.jdbc.schema.ForeignKey.ACTION_CASCADE;
            case NULL:
                return org.apache.openjpa.jdbc.schema.ForeignKey.ACTION_NULL;
            case DEFAULT:
                return org.apache.openjpa.jdbc.schema.ForeignKey.ACTION_DEFAULT;
            default:
                throw new InternalException();
        }
    }

    /**
     * Parse the given index.
     */
    private void parseIndex(MappingInfo info, Index idx) {
        parseIndex(info, idx.name(), idx.enabled(), idx.unique());
    }

    /**
     * Set index data on the given mapping info.
     */
    protected void parseIndex(MappingInfo info, String name,
        boolean enabled, boolean unique) {
        if (!enabled) {
            info.setCanIndex(false);
            return;
        }

        org.apache.openjpa.jdbc.schema.Index idx =
            new org.apache.openjpa.jdbc.schema.Index();
        if (!StringUtils.isEmpty(name))
            idx.setName(name);
        idx.setUnique(unique);
        info.setIndex(idx);
    }

    /**
     * Set unique data on the given mapping info.
     */
    private void parseUnique(FieldMapping fm, 
        org.apache.openjpa.persistence.jdbc.Unique anno) {
        ValueMappingInfo info = fm.getValueInfo();
        if (!anno.enabled()) {
            info.setCanUnique(false);
            return;
        }

        org.apache.openjpa.jdbc.schema.Unique unq = 
            new org.apache.openjpa.jdbc.schema.Unique();
        if (!StringUtils.isEmpty(anno.name()))
            unq.setName(anno.name());
        unq.setDeferred(anno.deferred());
        info.setUnique(unq);
    }

    /**
     * Parse @VersionColumn(s).
     */
    private void parseVersionColumns(ClassMapping cm, VersionColumn... vcols) {
        if (vcols.length == 0)
            return;

        List<Column> cols = new ArrayList<Column>(vcols.length);
        for (VersionColumn vcol : vcols)
            cols.add(newColumn(vcol));
        cm.getVersion().getMappingInfo().setColumns(cols);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(VersionColumn anno) {
        Column col = new Column();
        if (!StringUtils.isEmpty(anno.name()))
            col.setName(anno.name());
        if (!StringUtils.isEmpty(anno.columnDefinition()))
            col.setTypeName(anno.columnDefinition());
        if (anno.precision() != 0)
            col.setSize(anno.precision());
        else if (anno.length() != 255)
            col.setSize(anno.length());
        col.setNotNull(!anno.nullable());
        col.setDecimalDigits(anno.scale());
        col.setFlag(Column.FLAG_UNINSERTABLE, !anno.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !anno.updatable());
        return col;
    }

    /**
     * Translate the fetch mode enum value to the internal OpenJPA constant.
     */
    private static int toEagerFetchModeConstant(EagerFetchType type) {
        switch (type) {
            case NONE:
                return EagerFetchModes.EAGER_NONE;
            case JOIN:
                return EagerFetchModes.EAGER_JOIN;
            case PARALLEL:
                return EagerFetchModes.EAGER_PARALLEL;
            default:
                throw new InternalException();
        }
    }

    @Override
    protected void parseLobMapping(FieldMetaData fmd) {
        Column col = new Column();
        if (fmd.getDeclaredTypeCode() == JavaTypes.STRING
            || fmd.getDeclaredType() == char[].class
            || fmd.getDeclaredType() == Character[].class)
            col.setType(Types.CLOB);
        else
            col.setType(Types.BLOB);
        ((FieldMapping) fmd).getValueInfo().setColumns(Arrays.asList
            (new Column[]{ col }));
    }

    @Override
    protected void parseMemberMappingAnnotations(FieldMetaData fmd) {
        FieldMapping fm = (FieldMapping) fmd;
        AnnotatedElement el = (AnnotatedElement) getRepository().
            getMetaDataFactory().getDefaults().getBackingMember(fmd);

        MappingTag tag;
        for (Annotation anno : el.getDeclaredAnnotations()) {
            tag = _tags.get(anno.annotationType());
            if (tag == null) {
                handleUnknownMemberMappingAnnotation(fm, anno);
                continue;
            }

            switch (tag) {
                case ASSOC_OVERRIDE:
                    parseAssociationOverrides(fm, (AssociationOverride) anno);
                    break;
                case ASSOC_OVERRIDES:
                    parseAssociationOverrides(fm, ((AssociationOverrides) anno).
                        value());
                    break;
                case ATTR_OVERRIDE:
                    parseAttributeOverrides(fm, (AttributeOverride) anno);
                    break;
                case ATTR_OVERRIDES:
                    parseAttributeOverrides(fm, ((AttributeOverrides) anno).
                        value());
                    break;
                case COL:
                    parseColumns(fm, (javax.persistence.Column) anno);
                    break;
                case COLS:
                    parseColumns(fm, ((Columns) anno).value());
                    break;
                case ENUMERATED:
                    parseEnumerated(fm, (Enumerated) anno);
                    break;
                case JOIN_COL:
                    parseJoinColumns(fm, fm.getValueInfo(), true,
                        (JoinColumn) anno);
                    break;
                case JOIN_COLS:
                    parseJoinColumns(fm, fm.getValueInfo(), true,
                        ((JoinColumns) anno).value());
                    break;
                case JOIN_TABLE:
                    parseJoinTable(fm, (JoinTable) anno);
                    break;
                case PK_JOIN_COL:
                    parsePrimaryKeyJoinColumns(fm, (PrimaryKeyJoinColumn) anno);
                    break;
                case PK_JOIN_COLS:
                    parsePrimaryKeyJoinColumns(fm,
                        ((PrimaryKeyJoinColumns) anno).
                            value());
                    break;
                case TABLE_GEN:
                    parseTableGenerator(el, (TableGenerator) anno);
                    break;
                case TEMPORAL:
                    parseTemporal(fm, (Temporal) anno);
                    break;
                case CLASS_CRIT:
                    fm.getValueInfo().setUseClassCriteria
                        (((ClassCriteria) anno).value());
                    break;
                case CONTAINER_TABLE:
                    parseContainerTable(fm, (ContainerTable) anno);
                    break;
                case EAGER_FETCH_MODE:
                    fm.setEagerFetchMode(toEagerFetchModeConstant
                        (((EagerFetchMode) anno).value()));
                    break;
                case ELEM_CLASS_CRIT:
                    fm.getElementMapping().getValueInfo().setUseClassCriteria
                        (((ElementClassCriteria) anno).value());
                    break;
                case ELEM_FK:
                    ElementForeignKey efk = (ElementForeignKey) anno;
                    parseForeignKey(fm.getElementMapping().getValueInfo(),
                        efk.name(), efk.enabled(), efk.deferred(),
                        efk.deleteAction(), efk.updateAction());
                    break;
                case ELEM_INDEX:
                    ElementIndex eidx = (ElementIndex) anno;
                    parseIndex(fm.getElementMapping().getValueInfo(),
                        eidx.name(), eidx.enabled(), eidx.unique());
                    break;
                case ELEM_JOIN_COL:
                    parseElementJoinColumns(fm, (ElementJoinColumn) anno);
                    break;
                case ELEM_JOIN_COLS:
                    parseElementJoinColumns(fm, ((ElementJoinColumns) anno).
                        value());
                    break;
                case ELEM_NONPOLY:
                    fm.getElementMapping().setPolymorphic(toPolymorphicConstant
                        (((ElementNonpolymorphic) anno).value()));
                    break;
                case EMBEDDED_MAPPING:
                    parseEmbeddedMapping(fm, (EmbeddedMapping) anno);
                    break;
                case FK:
                    parseForeignKey(fm.getValueInfo(), (ForeignKey) anno);
                    break;
                case INDEX:
                    parseIndex(fm.getValueInfo(), (Index) anno);
                    break;
                case NONPOLY:
                    fm.setPolymorphic(toPolymorphicConstant
                        (((Nonpolymorphic) anno).value()));
                    break;
                case ORDER_COL:
                    parseOrderColumn(fm, (OrderColumn) anno);
                    break;
                case STRAT:
                    fm.getValueInfo().setStrategy(((Strategy) anno).value());
                    break;
                case UNIQUE:
                    parseUnique(fm, 
                        (org.apache.openjpa.persistence.jdbc.Unique) anno);
                    break;
                case X_JOIN_COL:
                    parseXJoinColumns(fm, fm.getValueInfo(), true,
                        (XJoinColumn) anno);
                    break;
                case X_JOIN_COLS:
                    parseXJoinColumns(fm, fm.getValueInfo(), true,
                        ((XJoinColumns) anno).value());
                    break;
                default:
                    throw new UnsupportedException(_loc.get("unsupported", fm,
                        anno.toString()));
            }
        }
    }

    /**
     * Allow subclasses to handle unknown annotations.
     */
    protected boolean handleUnknownMemberMappingAnnotation(FieldMapping fm,
        Annotation anno) {
        return false;
    }

    /**
     * Return the {@link ValueMapping} <code>POLY_*</code> constant for
     * the given enum value.
     */
    protected static int toPolymorphicConstant(NonpolymorphicType val) {
        switch (val) {
            case EXACT:
                return ValueMapping.POLY_FALSE;
            case JOINABLE:
                return ValueMapping.POLY_JOINABLE;
            case FALSE:
                return ValueMapping.POLY_TRUE;
            default:
                throw new InternalException();
        }
    }

    /**
     * Parse given @AssociationOverride annotations on an embedded mapping.
     */
    private void parseAssociationOverrides(FieldMapping fm,
        AssociationOverride... assocs) {
        ClassMapping embed = fm.getEmbeddedMapping();
        if (embed == null)
            throw new MetaDataException(_loc.get("not-embedded", fm));

        FieldMapping efm;
        JoinColumn[] ecols;
        int unique;
        List<Column> jcols;
        for (AssociationOverride assoc : assocs) {
            efm = embed.getFieldMapping(assoc.name());
            if (efm == null)
                throw new MetaDataException(_loc.get("embed-override-name",
                    fm, assoc.name()));
            ecols = assoc.joinColumns();
            if (ecols == null || ecols.length == 0)
                continue;

            unique = 0;
            jcols = new ArrayList<Column>(ecols.length);
            for (JoinColumn ecol : ecols) {
                unique |= (ecol.unique()) ? TRUE : FALSE;
                jcols.add(newColumn(ecol));
            }
            setColumns(efm, efm.getValueInfo(), jcols, unique);
        }
    }

    /**
     * Parse given @AttributeOverride annotations on an embedded mapping.
     */
    private void parseAttributeOverrides(FieldMapping fm,
        AttributeOverride... attrs) {
        ClassMapping embed = fm.getEmbeddedMapping();
        if (embed == null)
            throw new MetaDataException(_loc.get("not-embedded", fm));

        FieldMapping efm;
        for (AttributeOverride attr : attrs) {
            efm = embed.getFieldMapping(attr.name());
            if (efm == null)
                throw new MetaDataException(_loc.get("embed-override-name",
                    fm, attr.name()));
            if (attr.column() != null)
                parseColumns(efm, attr.column());
        }
    }

    /**
     * Parse @Enumerated.
     */
    private void parseEnumerated(FieldMapping fm, Enumerated anno) {
        String strat = EnumValueHandler.class.getName() + "(StoreOrdinal="
            + String.valueOf(anno.value() == EnumType.ORDINAL) + ")";
        fm.getValueInfo().setStrategy(strat);
    }

    /**
     * Parse @Temporal.
     */
    private void parseTemporal(FieldMapping fm, Temporal anno) {
        List cols = fm.getValueInfo().getColumns();
        if (!cols.isEmpty() && cols.size() != 1)
            throw new MetaDataException(_loc.get("num-cols-mismatch", fm,
                String.valueOf(cols.size()), "1"));
        if (cols.isEmpty())
            cols = Arrays.asList(new Column[]{ new Column() });

        Column col = (Column) cols.get(0);
        switch (anno.value()) {
            case DATE:
                col.setType(Types.DATE);
                break;
            case TIME:
                col.setType(Types.TIME);
                break;
            case TIMESTAMP:
                col.setType(Types.TIMESTAMP);
                break;
        }
    }

    /**
     * Parse @Column(s).
     */
    protected void parseColumns(FieldMapping fm,
        javax.persistence.Column... pcols) {
        if (pcols.length == 0)
            return;

        // might already have some column information from mapping annotation
        List cols = fm.getValueInfo().getColumns();
        if (!cols.isEmpty() && cols.size() != pcols.length)
            throw new MetaDataException(_loc.get("num-cols-mismatch", fm,
                String.valueOf(cols.size()), String.valueOf(pcols.length)));

        int unique = 0;
        String secondary = null;
        for (int i = 0; i < pcols.length; i++) {
            if (cols.size() > i)
                setupColumn((Column) cols.get(i), pcols[i]);
            else {
                if (cols.isEmpty())
                    cols = new ArrayList<Column>(pcols.length);
                cols.add(newColumn(pcols[i]));
            }

            unique |= (pcols[i].unique()) ? TRUE : FALSE;
            secondary = trackSecondaryTable(fm, secondary,
                pcols[i].table(), i);
        }

        setColumns(fm, fm.getValueInfo(), cols, unique);
        if (secondary != null)
            fm.getMappingInfo().setTableName(secondary);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(javax.persistence.Column anno) {
        Column col = new Column();
        setupColumn(col, anno);
        return col;
    }

    /**
     * Setup the given column with information from the given annotation.
     */
    private static void setupColumn(Column col, javax.persistence.Column anno) {
        if (!StringUtils.isEmpty(anno.name()))
            col.setName(anno.name());
        if (!StringUtils.isEmpty(anno.columnDefinition()))
            col.setTypeName(anno.columnDefinition());
        if (anno.precision() != 0)
            col.setSize(anno.precision());
        else if (anno.length() != 255)
            col.setSize(anno.length());
        col.setNotNull(!anno.nullable());
        col.setDecimalDigits(anno.scale());
        col.setFlag(Column.FLAG_UNINSERTABLE, !anno.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !anno.updatable());
    }

    /**
     * Set the given columns as the columns for <code>fm</code>.
     *
     * @param unique bitwise combination of TRUE and FALSE for the
     * unique attribute of each column
     */
    protected void setColumns(FieldMapping fm, MappingInfo info,
        List<Column> cols, int unique) {
        info.setColumns(cols);
        if (unique == TRUE)
            info.setUnique(new org.apache.openjpa.jdbc.schema.Unique());

        //### EJB3
        Log log = getLog();
        if (log.isWarnEnabled() && unique == (TRUE | FALSE))
            log.warn(_loc.get("inconsist-col-attrs", fm));
    }

    /**
     * Helper to track the secondary table for a set of columns.
     *
     * @param secondary secondary table for last column
     * @param colSecondary secondary table for current column
     * @return secondary table for field
     */
    private String trackSecondaryTable(FieldMapping fm, String secondary,
        String colSecondary, int col) {
        if (StringUtils.isEmpty(colSecondary))
            colSecondary = null;
        if (col == 0)
            return colSecondary;
        if (!StringUtils.equalsIgnoreCase(secondary, colSecondary))
            throw new MetaDataException(_loc.get("second-inconsist", fm));
        return secondary;
    }

    /**
     * Parse @JoinTable.
     */
    private void parseJoinTable(FieldMapping fm, JoinTable join) {
        fm.getMappingInfo().setTableName(toTableName(join.schema(),
            join.name()));
        parseJoinColumns(fm, fm.getMappingInfo(), false, join.joinColumns());
        parseJoinColumns(fm, fm.getElementMapping().getValueInfo(), false,
            join.inverseJoinColumns());
    }

    /**
     * Parse given @JoinColumn annotations.
     */
    private void parseJoinColumns(FieldMapping fm, MappingInfo info,
        boolean secondaryAllowed, JoinColumn... joins) {
        if (joins.length == 0)
            return;

        List<Column> cols = new ArrayList<Column>(joins.length);
        int unique = 0;
        String secondary = null;
        for (int i = 0; i < joins.length; i++) {
            cols.add(newColumn(joins[i]));
            unique |= (joins[i].unique()) ? TRUE : FALSE;
            secondary = trackSecondaryTable(fm, secondary,
                joins[i].table(), i);
            if (!secondaryAllowed && secondary != null)
                throw new MetaDataException(_loc.get("bad-second", fm));
        }

        setColumns(fm, info, cols, unique);
        if (secondary != null)
            fm.getMappingInfo().setTableName(secondary);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(JoinColumn join) {
        Column col = new Column();
        if (!StringUtils.isEmpty(join.name()))
            col.setName(join.name());
        if (!StringUtils.isEmpty(join.columnDefinition()))
            col.setName(join.columnDefinition());
        if (!StringUtils.isEmpty(join.referencedColumnName()))
            col.setTarget(join.referencedColumnName());
        col.setNotNull(!join.nullable());
        col.setFlag(Column.FLAG_UNINSERTABLE, !join.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !join.updatable());
        return col;
    }

    /**
     * Parse given @PrimaryKeyJoinColumn annotations.
     */
    private void parsePrimaryKeyJoinColumns(FieldMapping fm,
        PrimaryKeyJoinColumn... joins) {
        List<Column> cols = new ArrayList<Column>(joins.length);
        for (PrimaryKeyJoinColumn join : joins)
            cols.add(newColumn(join));
        setColumns(fm, fm.getValueInfo(), cols, 0);
    }

    /**
     * Parse given @XJoinColumn annotations.
     */
    protected void parseXJoinColumns(FieldMapping fm, MappingInfo info,
        boolean secondaryAllowed, XJoinColumn... joins) {
        if (joins.length == 0)
            return;

        List<Column> cols = new ArrayList<Column>(joins.length);
        int unique = 0;
        String secondary = null;
        for (int i = 0; i < joins.length; i++) {
            cols.add(newColumn(joins[i]));
            unique |= (joins[i].unique()) ? TRUE : FALSE;
            secondary = trackSecondaryTable(fm, secondary,
                joins[i].table(), i);
            if (!secondaryAllowed && secondary != null)
                throw new MetaDataException(_loc.get("bad-second", fm));
        }

        setColumns(fm, info, cols, unique);
        if (secondary != null)
            fm.getMappingInfo().setTableName(secondary);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(XJoinColumn join) {
        Column col = new Column();
        if (!StringUtils.isEmpty(join.name()))
            col.setName(join.name());
        if (!StringUtils.isEmpty(join.columnDefinition()))
            col.setName(join.columnDefinition());
        if (!StringUtils.isEmpty(join.referencedColumnName()))
            col.setTarget(join.referencedColumnName());
        if (!StringUtils.isEmpty(join.referencedAttributeName()))
            col.setTargetField(join.referencedAttributeName());
        col.setNotNull(!join.nullable());
        col.setFlag(Column.FLAG_UNINSERTABLE, !join.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !join.updatable());
        return col;
    }

    /**
     * Parse embedded info for the given mapping.
     */
    private void parseEmbeddedMapping(FieldMapping fm, EmbeddedMapping anno) {
        ClassMapping embed = fm.getEmbeddedMapping();
        if (embed == null)
            throw new MetaDataException(_loc.get("not-embedded", fm));

        FieldMapping efm;
        for (MappingOverride over : anno.overrides()) {
            efm = embed.getFieldMapping(over.name());
            if (efm == null)
                throw new MetaDataException(_loc.get("embed-override-name",
                    fm, over.name()));
            populate(efm, over);
        }

        String nullInd = null;
        if (!StringUtils.isEmpty(anno.nullIndicatorAttributeName()))
            nullInd = anno.nullIndicatorAttributeName();
        else if (!StringUtils.isEmpty(anno.nullIndicatorColumnName()))
            nullInd = anno.nullIndicatorColumnName();
        if (nullInd == null)
            return;

        ValueMappingInfo info = fm.getValueInfo();
        if ("false".equals(nullInd))
            info.setCanIndicateNull(false);
        else {
            Column col = new Column();
            if (!"true".equals(nullInd))
                col.setName(nullInd);
            info.setColumns(Arrays.asList(new Column[]{ col }));
        }
    }

    /**
     * Parse @ContainerTable.
     */
    protected void parseContainerTable(FieldMapping fm, ContainerTable ctbl) {
        fm.getMappingInfo().setTableName(toTableName(ctbl.schema(),
            ctbl.name()));
        parseXJoinColumns(fm, fm.getMappingInfo(), false, ctbl.joinColumns());
        if (ctbl.joinForeignKey().specified())
            parseForeignKey(fm.getMappingInfo(), ctbl.joinForeignKey());
        if (ctbl.joinIndex().specified())
            parseIndex(fm.getMappingInfo(), ctbl.joinIndex());
    }

    /**
     * Parse @OrderColumn.
     */
    private void parseOrderColumn(FieldMapping fm, OrderColumn order) {
        if (!order.enabled()) {
            fm.getMappingInfo().setCanOrderColumn(false);
            return;
        }

        Column col = new Column();
        if (!StringUtils.isEmpty(order.name()))
            col.setName(order.name());
        if (!StringUtils.isEmpty(order.columnDefinition()))
            col.setTypeName(order.columnDefinition());
        if (order.precision() != 0)
            col.setSize(order.precision());
        col.setFlag(Column.FLAG_UNINSERTABLE, !order.insertable());
        col.setFlag(Column.FLAG_UNUPDATABLE, !order.updatable());
        fm.getMappingInfo().setOrderColumn(col);
    }

    /**
     * Parse @ElementJoinColumn(s).
     */
    protected void parseElementJoinColumns(FieldMapping fm,
        ElementJoinColumn... joins) {
        if (joins.length == 0)
            return;

        List<Column> cols = new ArrayList<Column>(joins.length);
        int unique = 0;
        for (int i = 0; i < joins.length; i++) {
            cols.add(newColumn(joins[i]));
            unique |= (joins[i].unique()) ? TRUE : FALSE;
        }
        setColumns(fm, fm.getElementMapping().getValueInfo(), cols, unique);
    }

    /**
     * Create a new schema column with information from the given annotation.
     */
    private static Column newColumn(ElementJoinColumn join) {
        Column col = new Column();
        if (!StringUtils.isEmpty(join.name()))
            col.setName(join.name());
        if (!StringUtils.isEmpty(join.columnDefinition()))
            col.setName(join.columnDefinition());
        if (!StringUtils.isEmpty(join.referencedColumnName()))
            col.setTarget(join.referencedColumnName());
        if (!StringUtils.isEmpty(join.referencedAttributeName()))
            col.setTargetField(join.referencedAttributeName());
        col.setNotNull(!join.nullable());
        col.setFlag (Column.FLAG_UNINSERTABLE, !join.insertable ());
		col.setFlag (Column.FLAG_UNUPDATABLE, !join.updatable ());
		return col;
	}
    
    private static Unique newUnique(ClassMapping cm, String name, 
        String[] columnNames) {
        if (columnNames == null || columnNames.length == 0)
            return null;
        Unique uniqueConstraint = new Unique();
        uniqueConstraint.setName(name);
        for (int i=0; i<columnNames.length; i++) {
            if (StringUtils.isEmpty(columnNames[i]))
                throw new UserException(_loc.get("empty-unique-column", 
                    Arrays.toString(columnNames), cm));
            Column column = new Column();
            column.setName(columnNames[i]);
            uniqueConstraint.addColumn(column);
        }
        return uniqueConstraint;
    }
}
