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

import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.DiscriminatorType;
import javax.persistence.EnumType;
import javax.persistence.InheritanceType;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.ClassMappingInfo;
import org.apache.openjpa.jdbc.meta.DiscriminatorMappingInfo;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.FieldMappingInfo;
import org.apache.openjpa.jdbc.meta.MappingInfo;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.meta.SequenceMapping;
import org.apache.openjpa.jdbc.meta.strats.EnumValueHandler;
import org.apache.openjpa.jdbc.meta.strats.FlatClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.FullClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.XMLPersistenceMetaDataParser;
import org.apache.openjpa.util.InternalException;

import static org.apache.openjpa.persistence.jdbc.MappingTag.*;
import serp.util.Numbers;

/**
 * Custom SAX parser used by the system to parse persistence mapping files.
 *
 * @author Steve Kim
 * @nojavadoc
 */
public class XMLPersistenceMappingParser
    extends XMLPersistenceMetaDataParser {

    private static final Map<String, MappingTag> _elems =
        new HashMap<String, MappingTag>();

    static {
        _elems.put("association-override", ASSOC_OVERRIDE);
        _elems.put("attribute-override", ATTR_OVERRIDE);
        _elems.put("column", COL);
        _elems.put("column-name", COLUMN_NAME);
        _elems.put("column-result", COLUMN_RESULT);
        _elems.put("discriminator-column", DISCRIM_COL);
        _elems.put("discriminator-value", DISCRIM_VAL);
        _elems.put("entity-result", ENTITY_RESULT);
        _elems.put("enumerated", ENUMERATED);
        _elems.put("field-result", FIELD_RESULT);
        _elems.put("inheritance", INHERITANCE);
        _elems.put("join-column", JOIN_COL);
        _elems.put("inverse-join-column", COL);
        _elems.put("join-table", JOIN_TABLE);
        _elems.put("primary-key-join-column", PK_JOIN_COL);
        _elems.put("secondary-table", SECONDARY_TABLE);
        _elems.put("sql-result-set-mapping", SQL_RESULT_SET_MAPPING);
        _elems.put("table", TABLE);
        _elems.put("table-generator", TABLE_GEN);
        _elems.put("temporal", TEMPORAL);
        _elems.put("unique-constraint", UNIQUE);
    }

    private static final Localizer _loc = Localizer.forPackage
        (XMLPersistenceMappingParser.class);

    private String _override = null;
    private String _schema = null;
    private String _colTable = null;
    private String _secondaryTable = null;
    private List<Column> _cols = null;
    private List<Column> _joinCols = null;
    private List<Column> _supJoinCols = null;
    private boolean _lob = false;
    private TemporalType _temporal = null;
    private EnumSet<UniqueFlag> _unique = EnumSet.noneOf(UniqueFlag.class);
    private DiscriminatorType _discType;
    private Column _discCol;
    private int _resultIdx = 0;

    /**
     * Constructor; supply configuration.
     */
    public XMLPersistenceMappingParser(JDBCConfiguration conf) {
        super(conf);
    }

    @Override
    protected void reset() {
        super.reset();
        clearColumnInfo();
        clearClassInfo();
        clearSecondaryTableInfo();
        _override = null;
        _schema = null;
        _resultIdx = 0;
    }

    @Override
    protected Object startSystemMappingElement(String name, Attributes attrs)
        throws SAXException {
        MappingTag tag = _elems.get(name);
        if (tag == null) {
            if ("schema".equals(name))
                return name;
            return null;
        }

        boolean ret;
        switch (tag) {
            case TABLE_GEN:
                ret = startTableGenerator(attrs);
                break;
            case SQL_RESULT_SET_MAPPING:
                ret = startSQLResultSetMapping(attrs);
                break;
            case ENTITY_RESULT:
                ret = startEntityResult(attrs);
                break;
            case FIELD_RESULT:
                ret = startFieldResult(attrs);
                break;
            case COLUMN_RESULT:
                ret = startColumnResult(attrs);
                break;
            default:
                ret = false;
        }
        return (ret) ? tag : null;
    }

    @Override
    protected void endSystemMappingElement(String name)
        throws SAXException {
        MappingTag tag = _elems.get(name);
        if (tag == null) {
            if ("schema".equals(name))
                _schema = currentText();
            return;
        }

        switch (tag) {
            case SQL_RESULT_SET_MAPPING:
                endSQLResultSetMapping();
                break;
            case ENTITY_RESULT:
                endEntityResult();
                break;
        }
    }

    @Override
    protected Object startClassMappingElement(String name, Attributes attrs)
        throws SAXException {
        MappingTag tag = _elems.get(name);
        if (tag == null)
            return null;

        boolean ret;
        switch (tag) {
            case TABLE:
                ret = startTable(attrs);
                break;
            case SECONDARY_TABLE:
                ret = startSecondaryTable(attrs);
                break;
            case DISCRIM_COL:
                parseDiscriminatorColumn(attrs);
                _discCol = parseColumn(attrs);
                ret = true;
                break;
            case DISCRIM_VAL:
                ret = true;
                break;
            case INHERITANCE:
                ret = startInheritance(attrs);
                break;
            case ASSOC_OVERRIDE:
            case ATTR_OVERRIDE:
                ret = startAttributeOverride(attrs);
                break;
            case PK_JOIN_COL:
                ret = startPrimaryKeyJoinColumn(attrs);
                break;
            case COL:
                ret = startColumn(attrs);
                break;
            case JOIN_COL:
                ret = startJoinColumn(attrs);
                break;
            case JOIN_TABLE:
                ret = startJoinTable(attrs);
                break;
            case TABLE_GEN:
                ret = startTableGenerator(attrs);
                break;
            case UNIQUE:
                ret = startUniqueConstraint(attrs);
                break;
            case TEMPORAL:
            case ENUMERATED:
                ret = true;
                break;
            case SQL_RESULT_SET_MAPPING:
                ret = startSQLResultSetMapping(attrs);
                break;
            case ENTITY_RESULT:
                ret = startEntityResult(attrs);
                break;
            case FIELD_RESULT:
                ret = startFieldResult(attrs);
                break;
            case COLUMN_RESULT:
                ret = startColumnResult(attrs);
                break;
            case COLUMN_NAME:
                ret = true;
                break;
            default:
                ret = false;
        }
        return (ret) ? tag : null;
    }

    @Override
    protected void endClassMappingElement(String name)
        throws SAXException {
        MappingTag tag = _elems.get(name);
        if (tag == null)
            return;

        switch (tag) {
            case SECONDARY_TABLE:
                endSecondaryTable();
                break;
            case DISCRIM_VAL:
                endDiscriminatorValue();
                break;
            case ATTR_OVERRIDE:
                endAttributeOverride();
                break;
            case JOIN_TABLE:
                endJoinTable();
                break;
            case TEMPORAL:
                endTemporal();
                break;
            case ENUMERATED:
                endEnumerated();
                break;
            case SQL_RESULT_SET_MAPPING:
                endSQLResultSetMapping();
                break;
            case ENTITY_RESULT:
                endEntityResult();
                break;
            case UNIQUE:
                endUniqueConstraint();
                break;
            case COLUMN_NAME:
                endColumnName();
                break;
            case TABLE_GEN:
            	endTableGenerator();
            	break;
        }
    }

    @Override
    protected void startClassMapping(ClassMetaData meta, boolean mappedSuper,
        Attributes attrs)
        throws SAXException {
        if (mappedSuper)
            ((ClassMapping) meta).getMappingInfo().setStrategy
                (NoneClassStrategy.ALIAS);
    }

    @Override
    protected void endClassMapping(ClassMetaData meta)
        throws SAXException {
        ClassMapping cm = (ClassMapping) meta;
        if (_schema != null)
            cm.getMappingInfo().setSchemaName(_schema);

        if (_supJoinCols != null)
            cm.getMappingInfo().setColumns(_supJoinCols);

        if (_discCol != null) {
            DiscriminatorMappingInfo dinfo = cm.getDiscriminator()
                    .getMappingInfo();
            switch (_discType) {
                case CHAR:
                    _discCol.setJavaType(JavaTypes.CHAR);
                    cm.getDiscriminator().setJavaType(JavaTypes.CHAR);
                    break;
                case INTEGER:
                    _discCol.setJavaType(JavaTypes.INT);
                    cm.getDiscriminator().setJavaType(JavaTypes.INT);
                    break;
                default:
                    _discCol.setJavaType(JavaTypes.STRING);
                    cm.getDiscriminator().setJavaType(JavaTypes.STRING);
                    break;
            }
            dinfo.setColumns(Arrays.asList(new Column[]{ _discCol }));
        }
        clearClassInfo();
    }

    /**
     * Clear cached class mapping info.
     */
    private void clearClassInfo() {
        _supJoinCols = null;
        _discCol = null;
        _discType = null;
    }

    /**
     * Start tracking secondary table information and columns
     */
    private boolean startSecondaryTable(Attributes attrs)
        throws SAXException {
        _secondaryTable = toTableName(attrs.getValue("schema"),
            attrs.getValue("name"));
        ((ClassMapping)currentElement()).getMappingInfo()
        	.addSecondaryTable(_secondaryTable);
        return true;
    }

    /**
     * Set the secondary table information back to the owning class mapping.
     */
    private void endSecondaryTable() {
        ClassMapping cm = (ClassMapping) currentElement();
        ClassMappingInfo info = cm.getMappingInfo();
        info.setSecondaryTableJoinColumns(_secondaryTable, _joinCols);
        clearSecondaryTableInfo();
    }

    /**
     * Clear cached secondary table info.
     */
    private void clearSecondaryTableInfo() {
        _joinCols = null;
        _secondaryTable = null;
    }

    /**
     * Parse table-generator.
     */
    private boolean startTableGenerator(Attributes attrs) {
        String name = attrs.getValue("name");
        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-gen", name));
        if (getRepository().getCachedSequenceMetaData(name) != null
            && log.isWarnEnabled())
            log.warn(_loc.get("override-gen", name));

        SequenceMapping seq = (SequenceMapping) getRepository().
            addSequenceMetaData(name);
        seq.setSequencePlugin(SequenceMapping.IMPL_VALUE_TABLE);
        seq.setTable(toTableName(attrs.getValue("schema"),
            attrs.getValue("table")));
        seq.setPrimaryKeyColumn(attrs.getValue("pk-column-name"));
        seq.setSequenceColumn(attrs.getValue("value-column-name"));
        seq.setPrimaryKeyValue(attrs.getValue("pk-column-value"));
        String val = attrs.getValue("initial-value");
        if (val != null)
            seq.setInitialValue(Integer.parseInt(val));
        val = attrs.getValue("allocation-size");
        if (val != null)
            seq.setAllocate(Integer.parseInt(val));

        Object cur = currentElement();
        Object scope = (cur instanceof ClassMetaData)
            ? ((ClassMetaData) cur).getDescribedType() : null;
        seq.setSource(getSourceFile(), scope, seq.SRC_XML);
        Locator locator = getLocation().getLocator();
        if (locator != null) {
            seq.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
            seq.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
        }
        pushElement(seq);
        return true;
    }
    
    private void endTableGenerator() {
    	popElement();
    }

    /**
     * Parse inheritance.
     */
    private boolean startInheritance(Attributes attrs) {
        String val = attrs.getValue("strategy");
        if (val == null)
            return true;

        ClassMapping cm = (ClassMapping) currentElement();
        ClassMappingInfo info = cm.getMappingInfo();
        switch (Enum.valueOf(InheritanceType.class, val)) {
            case SINGLE_TABLE:
                info.setHierarchyStrategy(FlatClassStrategy.ALIAS);
                break;
            case JOINED:
                info.setHierarchyStrategy(VerticalClassStrategy.ALIAS);
                break;
            case TABLE_PER_CLASS:
                info.setHierarchyStrategy(FullClassStrategy.ALIAS);
                break;
        }
        return true;
    }

    /**
     * Parse discriminator-value.
     */
    private void endDiscriminatorValue() {
        String val = currentText();
        if (StringUtils.isEmpty(val))
            return;

        ClassMapping cm = (ClassMapping) currentElement();
        cm.getDiscriminator().getMappingInfo().setValue(val);

        if (Modifier.isAbstract(cm.getDescribedType().getModifiers())
                && getLog().isInfoEnabled()) {
            getLog().info(
                    _loc.get("discriminator-on-abstract-class", cm
                            .getDescribedType().getName()));
        }
    }

    /**
     * Parse temporal.
     */
    private void endTemporal() {
        String temp = currentText();
        if (!StringUtils.isEmpty(temp))
            _temporal = Enum.valueOf(TemporalType.class, temp);
    }

    /**
     * Parse enumerated.
     */
    private void endEnumerated() {
        String text = currentText();
        if (StringUtils.isEmpty(text))
            return;
        EnumType type = Enum.valueOf(EnumType.class, text);

        FieldMapping fm = (FieldMapping) currentElement();
        String strat = EnumValueHandler.class.getName() + "(StoreOrdinal="
            + String.valueOf(type == EnumType.ORDINAL) + ")";
        fm.getValueInfo().setStrategy(strat);
    }

    @Override
    protected boolean startLob(Attributes attrs)
        throws SAXException {
        if (super.startLob(attrs)) {
            _lob = true;
            return true;
        }
        return false;
    }

    /**
     * Extend to clear annotation mapping info.
     */
    @Override
    protected void startFieldMapping(FieldMetaData field, Attributes attrs)
        throws SAXException {
        super.startFieldMapping(field, attrs);
        if (getAnnotationParser() != null) {
            FieldMapping fm = (FieldMapping) field;
            fm.getMappingInfo().clear();
            fm.getValueInfo().clear();
            fm.getElementMapping().getValueInfo().clear();
            fm.getKeyMapping().getValueInfo().clear();
        }
    }

    /**
     * Extend to set the columns.
     */
    @Override
    protected void endFieldMapping(FieldMetaData field)
        throws SAXException {
        // setup columns with cached lob and temporal info
        FieldMapping fm = (FieldMapping) field;
        if (_lob || _temporal != null) {
            if (_cols == null) {
                _cols = new ArrayList<Column>(1);
                _cols.add(new Column());
            }
            for (Column col : _cols) {
                if (_lob && (fm.getDeclaredTypeCode() == JavaTypes.STRING
                    || fm.getDeclaredType() == char[].class
                    || fm.getDeclaredType() == Character[].class)) {
                    col.setSize(-1);
                    col.setType(Types.CLOB);
                } else if (_lob)
                    col.setType(Types.BLOB);
                else {
                    switch (_temporal) {
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
            }
        }

        if (_cols != null) {
            switch (fm.getDeclaredTypeCode()) {
                case JavaTypes.ARRAY:
                    Class type = fm.getDeclaredType();
                    if (type == byte[].class || type == Byte[].class
                        || type == char[].class || type == Character[].class ) {
                        fm.getValueInfo().setColumns(_cols);
                        break;
                    }
                    // else no break
                case JavaTypes.COLLECTION:
                    if(!fm.getValue().isSerialized()) {
                        fm.getElementMapping().getValueInfo().setColumns(_cols);
                    } else {
                        fm.getValueInfo().setColumns(_cols);
                    }
                    break;
                case JavaTypes.MAP:
                    fm.getElementMapping().getValueInfo().setColumns(_cols);
                    break;
                default:
                    fm.getValueInfo().setColumns(_cols);
            }
            if (_colTable != null)
                fm.getMappingInfo().setTableName(_colTable);
            setUnique(fm);
        }
        clearColumnInfo();
    }

    /**
     * Set unique for field.
     */
    private void setUnique(FieldMapping fm) {
        if (_unique.size() == 2) // i.e. TRUE & FALSE
            getLog().warn(_loc.get("inconsist-col-attrs", fm));
        else if (_unique.contains(UniqueFlag.TRUE))
            fm.getValueInfo().setUnique(new Unique());
    }

    /**
     * Clear field level column information.
     */
    private void clearColumnInfo() {
        _cols = null;
        _joinCols = null;
        _colTable = null;
        _lob = false;
        _temporal = null;
        _unique.clear();
    }

    /**
     * Parse attribute-override.
     */
    private boolean startAttributeOverride(Attributes attr) {
        _override = attr.getValue("name");
        return true;
    }

    /**
     * Set attribute override into proper mapping.
     */
    private void endAttributeOverride()
        throws SAXException {
        Object elem = currentElement();
        FieldMapping fm;
        if (elem instanceof ClassMapping)
            fm = getAttributeOverride((ClassMapping) elem);
        else
            fm = getAttributeOverride((FieldMapping) elem);
        if (_cols != null) {
            fm.getValueInfo().setColumns(_cols);
            if (_colTable != null)
                fm.getMappingInfo().setTableName(_colTable);
            setUnique(fm);
        }
        clearColumnInfo();
        _override = null;
    }

    /**
     * Return the proper override.
     */
    private FieldMapping getAttributeOverride(ClassMapping cm) {
        FieldMapping sup = (FieldMapping) cm.getDefinedSuperclassField
            (_override);
        if (sup == null)
            sup = (FieldMapping) cm.addDefinedSuperclassField(_override,
                Object.class, Object.class);
        return sup;
    }

    /**
     * Return the proper override.
     */
    private FieldMapping getAttributeOverride(FieldMapping fm)
        throws SAXException {
        ClassMapping embed = fm.getEmbeddedMapping();
        if (embed == null)
            throw getException(_loc.get("not-embedded", fm));

        FieldMapping efm = embed.getFieldMapping(_override);
        if (efm == null)
            throw getException(_loc.get("embed-override-name",
                fm, _override));
        return efm;
    }

    /**
     * Parse table.
     */
    private boolean startTable(Attributes attrs)
        throws SAXException {
        String table = toTableName(attrs.getValue("schema"),
            attrs.getValue("name"));
        if (table != null)
            ((ClassMapping) currentElement()).getMappingInfo().setTableName
                (table);
        return true;
    }

    /**
     * Parse join-table.
     */
    private boolean startJoinTable(Attributes attrs)
        throws SAXException {
        String table = toTableName(attrs.getValue("schema"),
            attrs.getValue("name"));
        if (table != null)
            ((FieldMapping) currentElement()).getMappingInfo().setTableName
                (table);
        return true;
    }

    /**
     * Set the join table information back.
     */
    private void endJoinTable() {
        FieldMapping fm = (FieldMapping) currentElement();
        if (_joinCols != null)
            fm.getMappingInfo().setColumns(_joinCols);
        if (_cols != null)
            fm.getElementMapping().getValueInfo().setColumns(_cols);
        clearColumnInfo();
    }

    /**
     * Parse primary-key-join-column.
     */
    private boolean startPrimaryKeyJoinColumn(Attributes attrs)
        throws SAXException {
        Column col = parseColumn(attrs);
        col.setFlag(Column.FLAG_PK_JOIN, true);
        // pk join columns on fields act as field cols
        if (currentElement() instanceof FieldMapping) {
            if (_cols == null)
                _cols = new ArrayList<Column>(3);
            _cols.add(col);
        } else if (currentParent() == SECONDARY_TABLE) {
            // pk join columns in secondary table acts as join cols
            if (_joinCols == null)
                _joinCols = new ArrayList<Column>(3);
            _joinCols.add(col);
        } else {
            // must be pk join cols from this class to superclass
            if (_supJoinCols == null)
                _supJoinCols = new ArrayList<Column>(3);
            _supJoinCols.add(col);
        }
        return true;
    }

    /**
     * Parse join-column.
     */
    private boolean startJoinColumn(Attributes attrs)
        throws SAXException {
        // only join cols in a join table join field table to class table;
        // others act as data fk cols
        if (currentParent() != JOIN_TABLE)
            return startColumn(attrs);

        if (_joinCols == null)
            _joinCols = new ArrayList<Column>(3);
        _joinCols.add(parseColumn(attrs));
        return true;
    }

    /**
     * Parse column.
     */
    private boolean startColumn(Attributes attrs)
        throws SAXException {
        if (_cols == null)
            _cols = new ArrayList<Column>(3);
        _cols.add(parseColumn(attrs));
        return true;
    }

    /**
     * Create a column with the given attributes.
     */
    private Column parseColumn(Attributes attrs)
        throws SAXException {
        Column col = new Column();
        String val = attrs.getValue("name");
        if (val != null)
            col.setName(val);
        val = attrs.getValue("referenced-column-name");
        if (val != null)
            col.setTarget(val);
        val = attrs.getValue("column-definition");
        if (val != null)
            col.setTypeName(val);
        val = attrs.getValue("precision");
        if (val != null)
            col.setSize(Integer.parseInt(val));
        val = attrs.getValue("length");
        if (val != null)
            col.setSize(Integer.parseInt(val));
        val = attrs.getValue("scale");
        if (val != null)
            col.setDecimalDigits(Integer.parseInt(val));
        val = attrs.getValue("nullable");
        if (val != null)
            col.setNotNull("false".equals(val));
        val = attrs.getValue("insertable");
        if (val != null)
            col.setFlag(Column.FLAG_UNINSERTABLE, "false".equals(val));
        val = attrs.getValue("updatable");
        if (val != null)
            col.setFlag(Column.FLAG_UNUPDATABLE, "false".equals(val));

        val = attrs.getValue("unique");
        if (val != null)
            _unique.add(Enum.valueOf(UniqueFlag.class, val.toUpperCase()));
        val = attrs.getValue("table");
        if (val != null) {
            if (_colTable != null && !_colTable.equals(val))
                throw getException(_loc.get("second-inconsist",
                    currentElement()));
            _colTable = val;
        }
        return col;
    }

    /**
     * Return a table name for the given attributes.
     */
    private String toTableName(String schema, String table) {
        if (StringUtils.isEmpty(table))
            return null;
        if (StringUtils.isEmpty(schema))
            schema = _schema;
        return (StringUtils.isEmpty(schema)) ? table : schema + "." + table;
    }

    /**
     * Start processing <code>sql-result-set-mapping</code> node.
     * Pushes the {@link QueryResultMapping} onto the stack as current element.
     */
    private boolean startSQLResultSetMapping(Attributes attrs) {
        String name = attrs.getValue("name");
        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-sqlrsmapping", name));

        MappingRepository repos = (MappingRepository) getRepository();
        QueryResultMapping result = repos.getCachedQueryResultMapping
            (null, name);
        if (result != null && log.isWarnEnabled())
            log.warn(_loc.get("override-sqlrsmapping", name,
                currentLocation()));

        result = repos.addQueryResultMapping(null, name);
        result.setListingIndex(_resultIdx++);
        addComments(result);

        Object cur = currentElement();
        Object scope = (cur instanceof ClassMetaData)
            ? ((ClassMetaData) cur).getDescribedType() : null;
        result.setSource(getSourceFile(), scope, result.SRC_XML);
        Locator locator = getLocation().getLocator();
        if (locator != null) {
            result.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
            result.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
        }
        pushElement(result);
        return true;
    }

    private void endSQLResultSetMapping()
        throws SAXException {
        popElement();
    }

    /**
     * Start processing <code>entity-result</code> node.
     * Pushes the {@link QueryResultMapping.PCResult}
     * onto the stack as current element.
     */
    private boolean startEntityResult(Attributes attrs)
        throws SAXException {
        Class entityClass = classForName(attrs.getValue("entity-class"));
        String discriminator = attrs.getValue("discriminator-column");

        QueryResultMapping parent = (QueryResultMapping) currentElement();
        QueryResultMapping.PCResult result = parent.addPCResult(entityClass);
        if (!StringUtils.isEmpty(discriminator))
            result.addMapping(result.DISCRIMINATOR, discriminator);
        pushElement(result);
        return true;
    }

    private void endEntityResult()
        throws SAXException {
        popElement();
    }

    /**
     * Process a <code>field-result</code> node.
     */
    private boolean startFieldResult(Attributes attrs)
        throws SAXException {
        String fieldName = attrs.getValue("name");
        String columnName = attrs.getValue("column");

        QueryResultMapping.PCResult parent = (QueryResultMapping.PCResult)
            currentElement();
        parent.addMapping(fieldName, columnName);
        return true;
    }

    /**
     * Process a <code>column-result</code> node.
     */
    private boolean startColumnResult(Attributes attrs)
        throws SAXException {
        QueryResultMapping parent = (QueryResultMapping) currentElement();
        parent.addColumnResult(attrs.getValue("name"));
        return true;
    }

    /** 
     * Starts processing &lt;unique-constraint&gt; provided the tag occurs
     * within a ClassMapping element and <em>not</em> within a secondary
     * table. 
     * Pushes the Unique element in the stack.
     */
    private boolean startUniqueConstraint(Attributes attrs) 
        throws SAXException {
        Unique unique = new Unique();
        pushElement(unique);
        return true;
    }
    
    /**
     * Ends processing &lt;unique-constraint&gt; provided the tag occurs
     * within a ClassMapping element and <em>not</em> within a secondary
     * table. The stack is popped and the Unique element is added to the
     * ClassMappingInfo. 
     */
    private void endUniqueConstraint() {
        Unique unique = (Unique) popElement();
        Object ctx = currentElement();
        String tableName = "?";
        if (ctx instanceof ClassMapping) {
        	ClassMappingInfo info = ((ClassMapping) ctx).getMappingInfo();
        	tableName = (_secondaryTable == null) 
        		? info.getTableName() : _secondaryTable;
        	info.addUnique(tableName, unique);
        } else if (ctx instanceof FieldMapping) {// JoinTable
        	FieldMappingInfo info = ((FieldMapping)ctx).getMappingInfo();
        	info.addJoinTableUnique(unique);
        } else if (ctx instanceof SequenceMapping) {
        	SequenceMapping seq = (SequenceMapping)ctx;
        	unique.setTableName(seq.getTable());
        	Column[] uniqueColumns = unique.getColumns();
        	String[] columnNames = new String[uniqueColumns.length];
        	int i = 0;
        	for (Column uniqueColumn : uniqueColumns)
        		columnNames[i++] = uniqueColumn.getName();
        	seq.setUniqueColumns(columnNames);
        } else {
        	throw new InternalException();
        }
    }
    
    /**
     * Ends processing &lt;column-name&gt; tag by adding the column name in
     * the current Unique element that resides in the top of the stack.
     */
    private boolean endColumnName() {
        Object current = currentElement();
        if (current instanceof Unique) {
            Unique unique = (Unique) current;
            Column column = new Column();
            column.setName(this.currentText());
            unique.addColumn(column);
            return true;
        }
        return false;
    }
    
    /**
     * Track unique column settings.
	 */
	private static enum UniqueFlag
	{
		TRUE,
		FALSE
	}
	
	private void parseDiscriminatorColumn(Attributes attrs) { 
	    String val = attrs.getValue("discriminator-type");
        if (val != null) {
            _discType = Enum.valueOf(DiscriminatorType.class, val);
        }
        else {
            _discType = DiscriminatorType.STRING;
        }
            
	}
}
